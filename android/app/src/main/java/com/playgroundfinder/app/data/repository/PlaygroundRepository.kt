package com.playgroundfinder.app.data.repository

import com.playgroundfinder.app.BuildConfig
import com.playgroundfinder.app.data.local.PlaygroundDao
import com.playgroundfinder.app.data.local.PlaygroundEntity
import com.playgroundfinder.app.data.remote.OverpassApiService
import com.playgroundfinder.app.data.remote.PlacesApiService
import com.playgroundfinder.app.data.remote.dto.OverpassElement
import com.playgroundfinder.app.data.remote.dto.PlaceDto
import com.playgroundfinder.app.domain.model.Playground
import com.playgroundfinder.app.domain.model.PlaygroundSource
import com.playgroundfinder.app.util.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class PlaygroundRepository @Inject constructor(
    private val placesApiService: PlacesApiService,
    private val overpassApiService: OverpassApiService,
    private val playgroundDao: PlaygroundDao
) {
    private val API_KEY = BuildConfig.MAPS_API_KEY

    /**
     * Játszóterek keresése a felhasználó helyzetéhez közel.
     * Google Places API + OpenStreetMap Overpass API eredményeit egyesíti.
     * Az OSM forrásból azok a játszóterek is megjelennek, amelyek nincsenek
     * feltüntetve a Google térképen — csak műholdképen láthatók.
     */
    suspend fun searchPlaygrounds(
        query: String = "játszótér",
        location: String? = null,
        radius: Int? = 5000
    ): Flow<Resource<List<Playground>>> = flow {
        emit(Resource.Loading())

        try {
            val favoriteIds = getFavoriteIds()
            val searchRadius = radius ?: 5000

            val (googlePlaygrounds, osmPlaygrounds) = coroutineScope {
                val googleDeferred = async {
                    fetchGooglePlaygrounds(query, location, searchRadius, favoriteIds)
                }
                val osmDeferred = async {
                    if (location != null) {
                        fetchOsmPlaygrounds(location, searchRadius, favoriteIds)
                    } else {
                        emptyList()
                    }
                }
                Pair(googleDeferred.await(), osmDeferred.await())
            }

            // OSM eredmények közül csak azokat tartjuk meg, amelyek
            // nem fednek át meglévő Google-eredménnyel (50 m-es küszöb)
            val uniqueOsmPlaygrounds = osmPlaygrounds.filter { osm ->
                googlePlaygrounds.none { google ->
                    haversineMeters(osm.latitude, osm.longitude, google.latitude, google.longitude) < 50.0
                }
            }

            emit(Resource.Success(googlePlaygrounds + uniqueOsmPlaygrounds))
        } catch (e: Exception) {
            emit(Resource.Error("Hálózati hiba: ${e.localizedMessage ?: "Ismeretlen hiba"}"))
        }
    }

    private suspend fun fetchGooglePlaygrounds(
        query: String,
        location: String?,
        radius: Int,
        favoriteIds: Set<String>
    ): List<Playground> {
        return try {
            val response = placesApiService.searchTextPlaces(
                query = query,
                location = location,
                radius = radius,
                type = "park",
                apiKey = API_KEY
            )
            when (response.status) {
                "OK" -> {
                    response.results
                        .filter { place ->
                            place.types?.any { it in listOf("park", "amusement_park", "tourist_attraction") } == true
                                || place.name.contains("játszótér", ignoreCase = true)
                                || place.name.contains("playground", ignoreCase = true)
                                || place.name.contains("spielplatz", ignoreCase = true)
                                || place.name.contains("igrište", ignoreCase = true)
                        }
                        .map { it.toPlayground(isFavorite = it.placeId in favoriteIds) }
                }
                "REQUEST_DENIED" -> throw Exception("Google API kulcs hiba")
                "OVER_QUERY_LIMIT" -> throw Exception("Google API limit elérve")
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchOsmPlaygrounds(
        location: String,
        radius: Int,
        favoriteIds: Set<String>
    ): List<Playground> {
        return try {
            val parts = location.split(",")
            val lat = parts[0].toDouble()
            val lng = parts[1].toDouble()
            val query = OverpassApiService.buildQuery(lat, lng, radius)
            val response = overpassApiService.queryPlaygrounds(query)
            response.elements
                .filter { it.centerLat != null && it.centerLon != null }
                .map { it.toPlayground(isFavorite = "osm:${it.id}" in favoriteIds) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Kedvenc játszóterek lekérése a helyi adatbázisból.
     */
    fun getFavoritePlaygrounds(): Flow<List<Playground>> {
        return playgroundDao.getFavoritePlaygrounds().map { entities ->
            entities.map { it.toPlayground(isFavorite = true) }
        }
    }

    /**
     * Kedvenc állapot váltása: ha kedvenc → törlés, ha nem → mentés.
     */
    suspend fun toggleFavoritePlayground(playground: Playground) {
        val existingFavorite = playgroundDao.getFavoritePlaygroundById(playground.id)
        if (existingFavorite == null) {
            playgroundDao.insertPlayground(playground.toPlaygroundEntity())
        } else {
            playgroundDao.deletePlayground(existingFavorite)
        }
    }

    private suspend fun getFavoriteIds(): Set<String> {
        return playgroundDao.getFavoritePlaygrounds().first().map { it.id }.toSet()
    }

    // --- Haversine távolságszámítás (méterben) ---

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    // --- Mapper függvények ---

    private fun PlaceDto.toPlayground(isFavorite: Boolean): Playground = Playground(
        id = placeId,
        name = name,
        latitude = geometry.location.lat,
        longitude = geometry.location.lng,
        address = formattedAddress ?: vicinity ?: "Ismeretlen cím",
        rating = rating,
        userRatingsTotal = userRatingsTotal,
        photoReference = photos?.firstOrNull()?.photoReference,
        isOpenNow = openingHours?.openNow,
        isFavorite = isFavorite,
        source = PlaygroundSource.GOOGLE
    )

    private fun OverpassElement.toPlayground(isFavorite: Boolean): Playground {
        val name = tags?.get("name") ?: tags?.get("name:hu") ?: "OSM játszótér"
        val street = tags?.get("addr:street") ?: ""
        val city = tags?.get("addr:city") ?: ""
        val address = listOf(city, street).filter { it.isNotEmpty() }.joinToString(", ")
            .ifEmpty { "OpenStreetMap" }
        return Playground(
            id = "osm:$id",
            name = name,
            latitude = centerLat!!,
            longitude = centerLon!!,
            address = address,
            rating = null,
            userRatingsTotal = null,
            isFavorite = isFavorite,
            source = PlaygroundSource.OSM
        )
    }

    private fun Playground.toPlaygroundEntity(): PlaygroundEntity = PlaygroundEntity(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        address = address,
        rating = rating,
        userRatingsTotal = userRatingsTotal
    )

    private fun PlaygroundEntity.toPlayground(isFavorite: Boolean): Playground = Playground(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        address = address,
        rating = rating,
        userRatingsTotal = userRatingsTotal,
        isFavorite = isFavorite
    )
}
