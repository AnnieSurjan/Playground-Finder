package com.playgroundfinder.app.ui.map

import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.playgroundfinder.app.domain.model.Location
import com.playgroundfinder.app.domain.model.Playground

data class MapState(
    val playgrounds: List<Playground> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val userLocation: Location? = null,
    val selectedPlayground: Playground? = null,
    val searchQuery: String = "játszótér",
    val searchRadius: Int = 5000,
    val mapUiSettings: MapUiSettings = MapUiSettings(
        zoomControlsEnabled = true,
        myLocationButtonEnabled = false, // Saját FAB-ot használunk
        compassEnabled = true,
        rotationGesturesEnabled = true,
        scrollGesturesEnabled = true,
        tiltGesturesEnabled = false,
        zoomGesturesEnabled = true
    ),
    val mapProperties: MapProperties = MapProperties(
        isMyLocationEnabled = false // Az engedély alapján állítjuk be
    )
)
