package com.playgroundfinder.app.ui.map

import com.playgroundfinder.app.domain.model.Playground

sealed class MapEvent {
    data class ToggleLocateUser(val enable: Boolean) : MapEvent()
    object GetCurrentLocation : MapEvent()
    data class SearchPlaygrounds(
        val query: String = "játszótér",
        val location: String? = null,
        val radius: Int? = 5000
    ) : MapEvent()
    data class SelectPlayground(val playground: Playground?) : MapEvent()
    data class ToggleFavorite(val playground: Playground) : MapEvent()
    object ErrorShown : MapEvent()
    data class UpdateSearchQuery(val query: String) : MapEvent()
}
