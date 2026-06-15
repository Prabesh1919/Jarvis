package com.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.jarvis.ui.theme.LocalJarvisColors

@Composable
fun ControlScreen() {
    val colors = LocalJarvisColors.current
    val scrollState = rememberScrollState()

    // Control toggles state
    var wifiState by remember { mutableStateOf(true) }
    var bluetoothState by remember { mutableStateOf(false) }
    var mobileDataState by remember { mutableStateOf(true) }
    var airplaneModeState by remember { mutableStateOf(false) }
    var dndState by remember { mutableStateOf(true) }
    var autoCleanState by remember { mutableStateOf(true) }

    // Sliders state
    var brightness by remember { mutableFloatStateOf(0.75f) }
    var volume by remember { mutableFloatStateOf(0.6f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "CONTROL CENTER",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = colors.accent,
            letterSpacing = 1.5.sp
        )

        // Toggles List Container
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ControlToggleRow("Wi-Fi Connection", "Connected", wifiState) { wifiState = it }
                Divider(color = colors.onSurfaceVariant.copy(alpha = 0.08f))

                ControlToggleRow("Bluetooth", if (bluetoothState) "Searching" else "Off", bluetoothState) { bluetoothState = it }
                Divider(color = colors.onSurfaceVariant.copy(alpha = 0.08f))

                ControlToggleRow("Mobile Data Network", if (mobileDataState) "On" else "Off", mobileDataState) { mobileDataState = it }
                Divider(color = colors.onSurfaceVariant.copy(alpha = 0.08f))

                ControlToggleRow("Airplane Mode", if (airplaneModeState) "Active" else "Off", airplaneModeState) { airplaneModeState = it }
                Divider(color = colors.onSurfaceVariant.copy(alpha = 0.08f))

                ControlToggleRow("Do Not Disturb", if (dndState) "Quiet Mode Active" else "Off", dndState) { dndState = it }
                Divider(color = colors.onSurfaceVariant.copy(alpha = 0.08f))

                ControlToggleRow("Auto Space Clean", "Runs every 24h", autoCleanState) { autoCleanState = it }
            }
        }

        // Adjustments Container (Sliders)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "HARDWARE ADJUSTMENTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                // Brightness Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Screen Brightness", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        Text("${(brightness * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Black, color = colors.accent)
                    }
                    Slider(
                        value = brightness,
                        onValueChange = { brightness = it },
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accent,
                            activeTrackColor = colors.accent,
                            inactiveTrackColor = colors.accent.copy(alpha = 0.2f)
                        )
                    )
                }

                Divider(color = colors.onSurfaceVariant.copy(alpha = 0.08f))

                // Volume Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("System Sound Volume", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        Text("${(volume * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Black, color = colors.secondaryGlow)
                    }
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        colors = SliderDefaults.colors(
                            thumbColor = colors.secondaryGlow,
                            activeTrackColor = colors.secondaryGlow,
                            inactiveTrackColor = colors.secondaryGlow.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ControlToggleRow(
    title: String,
    statusText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalJarvisColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Text(statusText, fontSize = 11.sp, color = colors.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.onSurfaceVariant,
                uncheckedTrackColor = colors.surfaceVariant
            )
        )
    }
}
