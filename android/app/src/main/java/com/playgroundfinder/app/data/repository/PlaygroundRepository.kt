package com.playgroundfinder.app.data.repository

import com.playgroundfinder.app.data.local.PlaygroundDao
import com.playgroundfinder.app.data.local.PlaygroundEntity
import com.playgroundfinder.app.data.remote.PlacesApiService
import com.playgroundfinder.app.data.remote.dto.PlaceDto
import com.playgroundfinder.app.domain.model.Playground
import com.playgroundfinder.app.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaygroundRepository @Inject constructor(
    private val placesApiService: PlacesApiService,
    private val playgroundDao: PlaygroundDao
) {
    /**
     * FONTOS: Cseréld le a saját Google Places API kulcsodra!
     * Szerezd be: https://console.cloud.google.com/
     * Engedélyezd: Places API
     *
     * Éles alkalmazásban használj BuildConfig-ot:
     * BuildConfig.PLACES_API_KEY
     * és add hozzá a build.gradle.kts-ben:
     * buildConfigField("String", "PLACES_API_KEY", "\"your_key_here\"")
     */
    private val API_KEY = "YOUR_GOOGLE_PLACES_API_KEY"

    /**
     * Játszóterek keresése a felhasználó helyzetéhez közel.
     * Kombinálja az API eredményeket a helyi kedvencekkel.
     */
    suspend fun searchPlaygrounds(
        query: String = "játszótér",
        location: String? = null,
        radius: Int? = 5000
    ): Flow<Resource<List<Playground>>> = flow {
        emit(Resource.Loading())

        try {
            val response = placesApiService.searchTextPlaces(
                query = query,
                location = location,
                radius = radius,
                type = "park",
                apiKey = API_KEY
            )

            when (response.status) {
                "OK" -> {
                    val remotePlaygrounds = response.results
                        .filter { place ->
                            place.types?.any { it in listOf("park", "amusement_park", "tourist_attraction") } == true
                                || place.name.contains("játszótér", ignoreCase = true)
                                || place.name.contains("playground", ignoreCase = true)
                                || place.name.contains("spielplatz", ignoreCase = true)
                                || place.name.contains("igrište", ignoreCase = true)
                        }
                        .map { it.toPlayground() }

                    // Kedvencek lekérése és kombinálása
                    val favoriteIds = getFavoriteIds()
                    val playgroundsWithFavorites = remotePlaygrounds.map { playground ->
                        playground.copy(isFavorite = playground.id in favoriteIds)
                    }
                    emit(Resource.Success(playgroundsWithFavorites))
                }
                "ZERO_RESULTS" -> emit(Resource.Success(emptyList()))
                "REQUEST_DENIED" -> emit(Resource.Error("API kulcs hiba. Ellenőrizd a Places API kulcsod."))
                "OVER_QUERY_LIMIT" -> emit(Resource.Error("API limit elérve. Próbálkozz később."))
                else -> emit(Resource.Error("Hiba: ${response.status}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Hálózati hiba: ${e.localizedMessage ?: "Ismeretlen hiba"}"))
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
        val favorites = mutableSetOf<String>()
        playgroundDao.getFavoritePlaygrounds().collect { entities ->
            favorites.addAll(entities.map { it.id })
        }
        return favorites
    }

    // --- Mapper függvények ---

    private fun PlaceDto.toPlayground(): Playground = Playground(
        id = placeId,
        name = name,
        latitude = geometry.location.lat,
        longitude = geometry.location.lng,
        address = formattedAddress ?: vicinity ?: "Ismeretlen cím",
        rating = rating,
        userRatingsTotal = userRatingsTotal,
        photoReference = photos?.firstOrNull()?.photoReference,
        isOpenNow = openingHours?.openNow,
        isFavorite = false
    )

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
