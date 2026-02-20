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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap as GmsGoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.playgroundfinder.app.domain.model.PlaygroundSource

// Budapest koordinátái alapértelmezettként
private val BUDAPEST = LatLng(47.4979, 19.0402)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
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
                // Közvetlen SDK hívás a mapType-ra — a properties alapú megközelítés
                // nem mindig érvényesül az AndroidView belső renderelése miatt
                MapEffect(state.isHybridView) { googleMap ->
                    googleMap.mapType = if (state.isHybridView)
                        GmsGoogleMap.MAP_TYPE_HYBRID
                    else
                        GmsGoogleMap.MAP_TYPE_NORMAL
                }

                // Játszótér jelölők
                // Szín: piros = kedvenc | kék = OSM (térképen nem jelölt) | zöld = Google
                state.playgrounds.forEach { playground ->
                    val markerHue = when {
                        playground.isFavorite -> BitmapDescriptorFactory.HUE_RED
                        playground.source == PlaygroundSource.OSM -> BitmapDescriptorFactory.HUE_AZURE
                        else -> BitmapDescriptorFactory.HUE_GREEN
                    }
                    Marker(
                        state = MarkerState(
                            position = LatLng(playground.latitude, playground.longitude)
                        ),
                        title = playground.name,
                        snippet = if (playground.source == PlaygroundSource.OSM)
                            "OSM · ${playground.address}"
                        else
                            playground.address,
                        icon = BitmapDescriptorFactory.defaultMarker(markerHue),
                        onClick = {
                            viewModel.onEvent(MapEvent.SelectPlayground(playground))
                            true
                        }
                    )
                }
            }

            // Keresőmező + felső gombok egy sorban
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Műholdkép toggle
                MapIconButton(
                    onClick = { viewModel.onEvent(MapEvent.ToggleMapType) },
                    selected = state.isHybridView,
                    icon = { Icon(Icons.Filled.Layers, contentDescription = "Műholdkép") }
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Prémium
                MapIconButton(
                    onClick = onNavigateToSubscription,
                    selected = state.subscriptionStatus.isPremium,
                    icon = {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Prémium",
                            tint = if (state.subscriptionStatus.isPremium) Color(0xFFFFD700)
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Profil
                MapIconButton(
                    onClick = onNavigateToProfile,
                    selected = false,
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profil") }
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Kijelentkezés
                MapIconButton(
                    onClick = onLogout,
                    selected = false,
                    icon = { Icon(Icons.Filled.ExitToApp, contentDescription = "Kijelentkezés") }
                )
            }

            // Töltési indikátor
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Jelmagyarázat (bal alsó sarok)
            MapLegend(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 80.dp)
            )

            // Játszótér részletek BottomSheet
            state.selectedPlayground?.let { playground ->
                PlaygroundDetailsBottomSheet(
                    playground = playground,
                    weather = state.weather,
                    isWeatherLoading = state.isWeatherLoading,
                    isPremium = state.subscriptionStatus.isPremium,
                    onDismiss = { viewModel.onEvent(MapEvent.SelectPlayground(null)) },
                    onNavigateClick = {
                        openGoogleMapsNavigation(context, playground.latitude, playground.longitude)
                    },
                    onToggleFavorite = {
                        viewModel.onEvent(MapEvent.ToggleFavorite(playground))
                    },
                    onShare = {
                        val text = buildString {
                            append("🛝 ${playground.name}\n")
                            append("📍 ${playground.address}\n")
                            playground.rating?.let { append("⭐ ${"%.1f".format(it)} / 5.0\n") }
                            append("https://www.google.com/maps/search/?api=1&query=${playground.latitude},${playground.longitude}")
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Játszótér megosztása"))
                    }
                )
            }
        }
    }
}

@Composable
private fun MapIconButton(
    onClick: () -> Unit,
    selected: Boolean,
    icon: @Composable () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 2.dp,
        modifier = Modifier.size(44.dp)
    ) {
        IconButton(onClick = onClick) { icon() }
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
        modifier = modifier,
        placeholder = { Text("Keress játszóteret...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Keresés"
            )
        },
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        singleLine = true,
        maxLines = 1
    )
}

@Composable
private fun MapLegend(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LegendItem(color = Color(0xFF4CAF50), label = "Google Maps")
        LegendItem(color = Color(0xFF2196F3), label = "OSM (rejtett)")
        LegendItem(color = Color(0xFFE53935), label = "Kedvenc")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = color, shape = CircleShape)
        )
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
    }
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
