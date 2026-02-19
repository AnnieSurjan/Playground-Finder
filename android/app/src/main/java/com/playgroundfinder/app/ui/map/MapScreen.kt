package com.playgroundfinder.app.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

// Budapest koordinátái alapértelmezettként
private val BUDAPEST = LatLng(47.4979, 19.0402)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(BUDAPEST, 11f)
    }

    // Helymeghatározás engedély kérő launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onEvent(MapEvent.ToggleLocateUser(true))
            viewModel.onEvent(MapEvent.GetCurrentLocation)
        } else {
            Toast.makeText(context, "Helymeghatározás nélkül csak az alapértelmezett nézet érhető el.", Toast.LENGTH_LONG).show()
        }
    }

    // Hibaüzenet megjelenítése Snackbar-ban
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(MapEvent.ErrorShown)
        }
    }

    // Indulásnál engedély ellenőrzése
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.onEvent(MapEvent.ToggleLocateUser(true))
            viewModel.onEvent(MapEvent.GetCurrentLocation)
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Ha megvan a felhasználó helyzete: térkép odaugrik és keresés indul
    LaunchedEffect(state.userLocation) {
        state.userLocation?.let { location ->
            val latLng = LatLng(location.latitude, location.longitude)
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(latLng, 14f),
                durationMs = 1000
            )
            viewModel.onEvent(
                MapEvent.SearchPlaygrounds(
                    query = state.searchQuery,
                    location = "${location.latitude},${location.longitude}",
                    radius = state.searchRadius
                )
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        viewModel.onEvent(MapEvent.GetCurrentLocation)
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = "Saját helyzetem",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Google Térkép
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = state.mapUiSettings,
                properties = state.mapProperties
            ) {
                // Játszótér jelölők
                state.playgrounds.forEach { playground ->
                    Marker(
                        state = MarkerState(
                            position = LatLng(playground.latitude, playground.longitude)
                        ),
                        title = playground.name,
                        snippet = playground.address,
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (playground.isFavorite) BitmapDescriptorFactory.HUE_RED
                            else BitmapDescriptorFactory.HUE_GREEN
                        ),
                        onClick = {
                            viewModel.onEvent(MapEvent.SelectPlayground(playground))
                            true
                        }
                    )
                }
            }

            // Keresőmező felül
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.onEvent(MapEvent.UpdateSearchQuery(it)) },
                onSearch = {
                    val location = state.userLocation?.let { "${it.latitude},${it.longitude}" }
                    viewModel.onEvent(
                        MapEvent.SearchPlaygrounds(
                            query = state.searchQuery,
                            location = location,
                            radius = state.searchRadius
                        )
                    )
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )

            // Töltési indikátor
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Játszótér részletek BottomSheet
            state.selectedPlayground?.let { playground ->
                PlaygroundDetailsBottomSheet(
                    playground = playground,
                    onDismiss = { viewModel.onEvent(MapEvent.SelectPlayground(null)) },
                    onNavigateClick = {
                        openGoogleMapsNavigation(context, playground.latitude, playground.longitude)
                    },
                    onToggleFavorite = {
                        viewModel.onEvent(MapEvent.ToggleFavorite(playground))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Keress játszóteret...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Keresés"
            )
        },
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        singleLine = true,
        maxLines = 1
    )
}

/**
 * Google Maps navigáció indítása a megadott koordinátákhoz.
 */
fun openGoogleMapsNavigation(context: Context, lat: Double, lng: Double) {
    val uri = Uri.parse("google.navigation:q=$lat,$lng")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        // Fallback: böngészőben nyitja meg
        val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}
