package com.playgroundfinder.app.data.repository

import android.util.Base64
import android.util.Log
import com.playgroundfinder.app.BuildConfig
import com.playgroundfinder.app.data.remote.VisionApiService
import com.playgroundfinder.app.data.remote.dto.AnnotateImageRequest
import com.playgroundfinder.app.data.remote.dto.NormalizedVertex
import com.playgroundfinder.app.data.remote.dto.VisionAnnotateRequest
import com.playgroundfinder.app.data.remote.dto.VisionFeature
import com.playgroundfinder.app.data.remote.dto.VisionImage
import com.playgroundfinder.app.domain.model.Playground
import com.playgroundfinder.app.domain.model.PlaygroundSource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos

private const val TAG = "SatelliteDetection"

/**
 * Játszóterek felismerése műholdképből a Google Maps Static API
 * és a Google Cloud Vision API kombinálásával.
 *
 * Működés:
 *  1. 5×5 csempe-rácsot épít a felhasználó pozíciója köré (~650 m × 650 m).
 *  2. Minden csempéhez letölt egy 640×640 px műholdképet (zoom 19, ~0,20 m/px).
 *  3. A képet elküldi a Cloud Vision API-nak (LABEL_DETECTION + OBJECT_LOCALIZATION).
 *  4. Ha a felismert elemek között játszótér-jellegű fogalom szerepel ≥ 0.65 megbízhatósággal,
 *     a csempe-középpontot (OBJECT_LOCALIZATION esetén pontosabb koordinátát) Playground-ként adja vissza.
 *
 * Előfeltétel: A Google Cloud Console-ban engedélyezni kell a
 * "Cloud Vision API"-t ugyanahhoz a projekthez, amelyhez a Maps API kulcs tartozik.
 */
@Singleton
class SatelliteDetectionRepository @Inject constructor(
    private val visionApiService: VisionApiService,
    private val okHttpClient: OkHttpClient
) {
    private val API_KEY = BuildConfig.MAPS_API_KEY

    // zoom 19 → ~0,20 m/px (47° szélességen)
    private val ZOOM = 19
    private val TILE_PX = 640

    // 5×5 rács: -2,-1,0,+1,+2 → 25 csempe
    private val GRID_HALF = 2

    // Minimális megbízhatóság, amely felett pozitívnak tekintjük a találatot
    private val CONFIDENCE_THRESHOLD = 0.65f

    // Keresett fogalmak (kisbetű) a Vision API válaszban
    private val PLAYGROUND_LABELS = setOf(
        "playground", "play", "outdoor play equipment", "jungle gym",
        "swing", "slide", "climbing frame", "sandpit", "sandbox",
        "recreation", "játszótér", "spielplatz", "homokozó",
        "amusement park", "leisure", "children"
    )

    /**
     * Játszóterek detektálása a megadott GPS-koordináta körüli műholdképekből.
     *
     * @param lat  Középpont szélességi foka
     * @param lng  Középpont hosszúsági foka
     * @return  Felismert játszóterek listája (üres, ha semmi sem található vagy API hiba lép fel)
     */
    suspend fun detectPlaygrounds(lat: Double, lng: Double): List<Playground> {
        val tileMeters = groundResolutionMeters(lat) * TILE_PX
        val tileDegLat = tileMeters / 111_320.0
        val tileDegLng = tileMeters / (111_320.0 * cos(Math.toRadians(lat)))

        val tileCenters = buildList {
            for (row in -GRID_HALF..GRID_HALF) {
                for (col in -GRID_HALF..GRID_HALF) {
                    add(lat + row * tileDegLat to lng + col * tileDegLng)
                }
            }
        }

        return coroutineScope {
            tileCenters.mapIndexed { idx, (tileLat, tileLng) ->
                async { analyzeOneTile(tileLat, tileLng, idx) }
            }.mapNotNull { it.await() }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun analyzeOneTile(
        tileLat: Double,
        tileLng: Double,
        tileIdx: Int
    ): Playground? = try {
        val imageBytes = fetchSatelliteImage(tileLat, tileLng) ?: run {
            Log.w(TAG, "Tile $tileIdx: kép letöltése sikertelen")
            return@try null
        }

        val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        val visionRequest = VisionAnnotateRequest(
            requests = listOf(
                AnnotateImageRequest(
                    image = VisionImage(content = base64),
                    features = listOf(
                        VisionFeature(type = "LABEL_DETECTION", maxResults = 20),
                        VisionFeature(type = "OBJECT_LOCALIZATION", maxResults = 10)
                    )
                )
            )
        )

        val response = visionApiService.annotateImages(
            apiKey = API_KEY,
            request = visionRequest
        )

        val annotation = response.responses.firstOrNull() ?: return@try null

        val labelHit = annotation.labelAnnotations.orEmpty().any { label ->
            label.score >= CONFIDENCE_THRESHOLD &&
                label.description.lowercase() in PLAYGROUND_LABELS
        }

        val bestObject = annotation.localizedObjectAnnotations.orEmpty().firstOrNull { obj ->
            obj.score >= CONFIDENCE_THRESHOLD &&
                obj.name.lowercase() in PLAYGROUND_LABELS
        }

        if (!labelHit && bestObject == null) return@try null

        // Ha objektum-lokalizáció is megvan, precízebb koordinátát számolunk
        val (detectedLat, detectedLng) = if (bestObject?.boundingPoly != null) {
            vertexCenterToGps(tileLat, tileLng, bestObject.boundingPoly.normalizedVertices)
        } else {
            tileLat to tileLng
        }

        Log.d(TAG, "Tile $tileIdx: játszótér felismerve @ ${"%.5f".format(detectedLat)}, ${"%.5f".format(detectedLng)}")

        Playground(
            id = "satellite:${"%.4f".format(detectedLat)}_${"%.4f".format(detectedLng)}",
            name = "Műhold által észlelt játszótér",
            latitude = detectedLat,
            longitude = detectedLng,
            address = "Műholdkép-elemzés · ${"%.5f".format(detectedLat)}, ${"%.5f".format(detectedLng)}",
            rating = null,
            userRatingsTotal = null,
            isFavorite = false,
            source = PlaygroundSource.SATELLITE_AI
        )
    } catch (e: Exception) {
        Log.w(TAG, "Tile $tileIdx elemzése sikertelen: ${e.message}")
        null
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Letölti a Google Maps Static API műholdképét a megadott koordinátákra.
     * Visszatér a nyers JPEG/PNG bájtokkal, vagy null-lal hálózati hiba esetén.
     */
    private fun fetchSatelliteImage(lat: Double, lng: Double): ByteArray? {
        val url = "https://maps.googleapis.com/maps/api/staticmap" +
            "?center=$lat,$lng" +
            "&zoom=$ZOOM" +
            "&size=${TILE_PX}x${TILE_PX}" +
            "&maptype=satellite" +
            "&key=$API_KEY"
        return try {
            okHttpClient.newCall(Request.Builder().url(url).build())
                .execute()
                .use { resp -> if (resp.isSuccessful) resp.body?.bytes() else null }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * A Vision API által visszaadott normalizált befoglaló téglalap középpontját
     * GPS-koordinátává alakítja.
     *
     * A normalizált koordináták: x ∈ [0,1] (bal→jobb = W→E),
     *                             y ∈ [0,1] (fel→le  = N→S).
     */
    private fun vertexCenterToGps(
        tileLat: Double,
        tileLng: Double,
        vertices: List<NormalizedVertex>
    ): Pair<Double, Double> {
        if (vertices.isEmpty()) return tileLat to tileLng

        val cx = vertices.map { it.x }.average()
        val cy = vertices.map { it.y }.average()

        val tileMeters = groundResolutionMeters(tileLat) * TILE_PX
        val halfDegLat = (tileMeters / 2.0) / 111_320.0
        val halfDegLng = (tileMeters / 2.0) / (111_320.0 * cos(Math.toRadians(tileLat)))

        // cx=0.5, cy=0.5 → csempe-középpont; Y tengely invertált (fent = észak)
        val detectedLat = tileLat + (0.5 - cy) * 2.0 * halfDegLat
        val detectedLng = tileLng + (cx - 0.5) * 2.0 * halfDegLng

        return detectedLat to detectedLng
    }

    /**
     * Zoom szinthez tartozó terepi felbontás (méter/pixel) a megadott szélességi fokon.
     *
     * Képlet: groundResolution = (2π × R × cos(lat)) / (256 × 2^zoom)
     * ahol R = 6 378 137 m (WGS-84 félnagytengelye).
     */
    private fun groundResolutionMeters(lat: Double): Double {
        val earthCircumference = 2.0 * Math.PI * 6_378_137.0
        return earthCircumference * cos(Math.toRadians(lat)) / (256.0 * Math.pow(2.0, ZOOM.toDouble()))
    }
}
