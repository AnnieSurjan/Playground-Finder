package com.playgroundfinder.app.ui.map

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.maps.android.compose.MapType
import com.playgroundfinder.app.data.repository.AuthRepository
import com.playgroundfinder.app.data.repository.PlaygroundRepository
import com.playgroundfinder.app.data.repository.SubscriptionRepository
import com.playgroundfinder.app.data.repository.WeatherRepository
import com.playgroundfinder.app.domain.model.Location
import com.playgroundfinder.app.domain.model.Playground
import com.playgroundfinder.app.domain.model.SubscriptionStatus
import com.playgroundfinder.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: PlaygroundRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val weatherRepository: WeatherRepository,
    private val authRepository: AuthRepository,
    private val application: Application,
    private val fusedLocationProviderClient: FusedLocationProviderClient
) : ViewModel() {

    private val _state = MutableStateFlow(MapState())
    val state = _state.asStateFlow()

    init {
        searchPlaygrounds("játszótér", "47.4979,19.0402", 10000)
        observeSubscription()
    }

    private fun observeSubscription() {
        viewModelScope.launch {
            subscriptionRepository.subscriptionStatus.collectLatest { status ->
                // Admin fiók mindig Prémium
                val effective = if (authRepository.isAdmin) SubscriptionStatus.PREMIUM_YEARLY else status
                _state.update { it.copy(subscriptionStatus = effective) }
            }
        }
    }

    fun onEvent(event: MapEvent) {
        when (event) {
            is MapEvent.ToggleLocateUser -> {
                _state.update { it.copy(
                    mapUiSettings = it.mapUiSettings.copy(zoomControlsEnabled = true),
                    mapProperties = it.mapProperties.copy(isMyLocationEnabled = event.enable)
                ) }
            }

            is MapEvent.GetCurrentLocation -> getCurrentLocation()

            is MapEvent.SearchPlaygrounds -> {
                searchPlaygrounds(event.query, event.location, event.radius)
            }

            is MapEvent.SelectPlayground -> {
                _state.update { it.copy(selectedPlayground = event.playground, weather = null) }
                event.playground?.let { fetchWeather(it.latitude, it.longitude) }
            }

            is MapEvent.ToggleFavorite -> toggleFavoriteStatus(event.playground)

            is MapEvent.ErrorShown -> {
                _state.update { it.copy(error = null) }
            }

            is MapEvent.UpdateSearchQuery -> {
                _state.update { it.copy(searchQuery = event.query) }
            }

            is MapEvent.ToggleMapType -> {
                if (!_state.value.subscriptionStatus.isPremium) {
                    _state.update { it.copy(error = "A műholdkép Prémium funkció. Iratkozz fel a beállításokban!") }
                    return
                }
                _state.update { current ->
                    val newHybrid = !current.isHybridView
                    current.copy(
                        isHybridView = newHybrid,
                        mapProperties = current.mapProperties.copy(
                            mapType = if (newHybrid) MapType.HYBRID else MapType.NORMAL
                        )
                    )
                }
            }
        }
    }

    private fun getCurrentLocation() {
        val hasPermission = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return

        _state.update { it.copy(isLoading = true) }

        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    _state.update { it.copy(
                        userLocation = Location(location.latitude, location.longitude),
                        isLoading = false
                    ) }
                } else {
                    _state.update { it.copy(
                        error = "Nem sikerült lekérni a helyzeted. Ellenőrizd, hogy be van-e kapcsolva a GPS.",
                        isLoading = false
                    ) }
                }
            }
            .addOnFailureListener { e ->
                _state.update { it.copy(
                    error = "Helymeghatározási hiba: ${e.localizedMessage}",
                    isLoading = false
                ) }
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
                    is Resource.Success -> _state.update { it.copy(
                        playgrounds = result.data ?: emptyList(),
                        isLoading = false
                    ) }
                    is Resource.Error -> _state.update { it.copy(
                        error = result.message,
                        isLoading = false
                    ) }
                    is Resource.Loading -> _state.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun fetchWeather(lat: Double, lng: Double) {
        viewModelScope.launch {
            _state.update { it.copy(isWeatherLoading = true) }
            when (val result = weatherRepository.getWeather(lat, lng)) {
                is Resource.Success -> _state.update { it.copy(weather = result.data, isWeatherLoading = false) }
                is Resource.Error   -> _state.update { it.copy(isWeatherLoading = false) }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun toggleFavoriteStatus(playground: Playground) {
        if (!_state.value.subscriptionStatus.isPremium) {
            _state.update { it.copy(error = "A kedvencek mentése Prémium funkció. Iratkozz fel a beállításokban!") }
            return
        }
        viewModelScope.launch {
            repository.toggleFavoritePlayground(playground)
            _state.update { current ->
                current.copy(
                    playgrounds = current.playgrounds.map { p ->
                        if (p.id == playground.id) p.copy(isFavorite = !p.isFavorite) else p
                    },
                    selectedPlayground = current.selectedPlayground?.let { sel ->
                        if (sel.id == playground.id) sel.copy(isFavorite = !sel.isFavorite) else sel
                    }
                )
            }
        }
    }
}
