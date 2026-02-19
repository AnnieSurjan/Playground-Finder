package com.playgroundfinder.app.ui.map

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.playgroundfinder.app.data.repository.PlaygroundRepository
import com.playgroundfinder.app.domain.model.Location
import com.playgroundfinder.app.domain.model.Playground
import com.playgroundfinder.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: PlaygroundRepository,
    private val application: Application,
    private val fusedLocationProviderClient: FusedLocationProviderClient
) : ViewModel() {

    private val _state = MutableStateFlow(MapState())
    val state = _state.asStateFlow()

    init {
        // Alapértelmezett keresés Budapest körül, amíg nincs felhasználói helyzet
        searchPlaygrounds("játszótér", "47.4979,19.0402", 10000)
    }

    fun onEvent(event: MapEvent) {
        when (event) {
            is MapEvent.ToggleLocateUser -> {
                _state.value = _state.value.copy(
                    mapUiSettings = _state.value.mapUiSettings.copy(
                        zoomControlsEnabled = true
                    ),
                    mapProperties = _state.value.mapProperties.copy(
                        isMyLocationEnabled = event.enable
                    )
                )
            }

            is MapEvent.GetCurrentLocation -> getCurrentLocation()

            is MapEvent.SearchPlaygrounds -> {
                searchPlaygrounds(event.query, event.location, event.radius)
            }

            is MapEvent.SelectPlayground -> {
                _state.value = _state.value.copy(selectedPlayground = event.playground)
            }

            is MapEvent.ToggleFavorite -> toggleFavoriteStatus(event.playground)

            is MapEvent.ErrorShown -> {
                _state.value = _state.value.copy(error = null)
            }

            is MapEvent.UpdateSearchQuery -> {
                _state.value = _state.value.copy(searchQuery = event.query)
            }
        }
    }

    private fun getCurrentLocation() {
        val hasPermission = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return

        _state.value = _state.value.copy(isLoading = true)

        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    _state.value = _state.value.copy(
                        userLocation = Location(location.latitude, location.longitude),
                        isLoading = false
                    )
                } else {
                    _state.value = _state.value.copy(
                        error = "Nem sikerült lekérni a helyzeted. Ellenőrizd, hogy be van-e kapcsolva a GPS.",
                        isLoading = false
                    )
                }
            }
            .addOnFailureListener { e ->
                _state.value = _state.value.copy(
                    error = "Helymeghatározási hiba: ${e.localizedMessage}",
                    isLoading = false
                )
            }
    }

    private fun searchPlaygrounds(
        query: String,
        location: String? = null,
        radius: Int? = 5000
    ) {
        viewModelScope.launch {
            repository.searchPlaygrounds(query, location, radius).collectLatest { result ->
                when (result) {
                    is Resource.Success -> {
                        _state.value = _state.value.copy(
                            playgrounds = result.data ?: emptyList(),
                            isLoading = false
                        )
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(
                            error = result.message,
                            isLoading = false
                        )
                    }
                    is Resource.Loading -> {
                        _state.value = _state.value.copy(isLoading = true)
                    }
                }
            }
        }
    }

    private fun toggleFavoriteStatus(playground: Playground) {
        viewModelScope.launch {
            repository.toggleFavoritePlayground(playground)

            // UI azonnali frissítése
            val updatedPlaygrounds = _state.value.playgrounds.map { p ->
                if (p.id == playground.id) p.copy(isFavorite = !p.isFavorite) else p
            }
            val updatedSelected = _state.value.selectedPlayground?.let { selected ->
                if (selected.id == playground.id) selected.copy(isFavorite = !selected.isFavorite) else selected
            }

            _state.value = _state.value.copy(
                playgrounds = updatedPlaygrounds,
                selectedPlayground = updatedSelected
            )
        }
    }
}
