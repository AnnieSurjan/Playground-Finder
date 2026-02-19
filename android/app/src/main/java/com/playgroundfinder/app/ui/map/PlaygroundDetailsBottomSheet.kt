package com.playgroundfinder.app.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playgroundfinder.app.data.remote.dto.WeatherResponse
import com.playgroundfinder.app.domain.model.Playground
import com.playgroundfinder.app.domain.model.PlaygroundSource
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaygroundDetailsBottomSheet(
    playground: Playground,
    weather: WeatherResponse?,
    isWeatherLoading: Boolean,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onNavigateClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Cím sor és kedvenc gomb
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = playground.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleFavorite) {
                    if (!isPremium) {
                        Icon(Icons.Filled.Lock, contentDescription = "Prémium funkció", tint = Color.Gray)
                    } else {
                        Icon(
                            imageVector = if (playground.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (playground.isFavorite) "Eltávolítás a kedvencekből" else "Hozzáadás a kedvencekhez",
                            tint = if (playground.isFavorite) Color(0xFFE53935) else Color.Gray
                        )
                    }
                }
            }

            // OSM forrás jelzése
            if (playground.source == PlaygroundSource.OSM) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "OpenStreetMap forrás — térképen jelöletlen játszótér",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2196F3),
                    modifier = Modifier
                        .background(
                            color = Color(0xFF2196F3).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cím
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = playground.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Értékelés
            playground.rating?.let { rating ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Értékelés",
                        tint = Color(0xFFFFC107)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${"%.1f".format(rating)} / 5.0",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    playground.userRatingsTotal?.let { total ->
                        Text(
                            text = " ($total értékelés)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Nyitvatartás
            playground.isOpenNow?.let { isOpen ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = if (isOpen) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOpen) "Nyitva" else "Zárva",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOpen) Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Időjárás szekció (prémium)
            WeatherSection(
                weather = weather,
                isLoading = isWeatherLoading,
                isPremium = isPremium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Gombok
            Row(modifier = Modifier.fillMaxWidth()) {
                // Kedvenc gomb
                OutlinedButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (!isPremium) Icons.Filled.Lock
                            else if (playground.isFavorite) Icons.Filled.Favorite
                            else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (playground.isFavorite && isPremium) Color(0xFFE53935)
                               else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (!isPremium) "Prémium"
                        else if (playground.isFavorite) "Kedvenc"
                        else "Mentés"
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Útvonal gomb
                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onNavigateClick()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Filled.Directions, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Útvonal")
                }
            }
        }
    }
}

@Composable
private fun WeatherSection(
    weather: WeatherResponse?,
    isLoading: Boolean,
    isPremium: Boolean
) {
    val bgColor = if (isPremium) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        when {
            !isPremium -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Időjárás-előrejelzés — Prémium funkció",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            isLoading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Időjárás betöltése...", style = MaterialTheme.typography.bodySmall)
                }
            }
            weather != null -> {
                val condition = weather.weather.firstOrNull()
                Column {
                    Text(
                        text = "Időjárás most",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Hőmérséklet
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Thermostat, null, tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${weather.main.temp.roundToInt()}°C",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        // Páratartalom
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.WaterDrop, null, tint = Color(0xFF2196F3), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${weather.main.humidity}%", fontSize = 14.sp)
                        }
                        // Szél
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Air, null, tint = Color(0xFF78909C), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${"%.1f".format(weather.wind.speed)} m/s", fontSize = 14.sp)
                        }
                    }
                    condition?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it.description.replaceFirstChar { c -> c.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                Text(
                    text = "Időjárás nem elérhető",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}
