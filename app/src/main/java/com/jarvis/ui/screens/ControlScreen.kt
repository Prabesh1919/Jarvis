package com.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ui.theme.LocalJarvisColors

@Composable
fun ControlScreen() {
    val colors = LocalJarvisColors.current
    val scrollState = rememberScrollState()

    var wifiState by remember { mutableStateOf(true) }
    var bluetoothState by remember { mutableStateOf(false) }
    var mobileDataState by remember { mutableStateOf(true) }
    var airplaneModeState by remember { mutableStateOf(false) }
    var dndState by remember { mutableStateOf(true) }
    var autoCleanState by remember { mutableStateOf(true) }

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
            text = "[SUBSYSTEM CONTROL CENTER]",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = colors.accent,
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceGlass)
                .border(1.dp, colors.surfaceBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ControlToggleRow("Wi-Fi Connection", "Connected", wifiState) { wifiState = it }
                HorizontalDivider(color = colors.surfaceBorder.copy(alpha = 0.3f))

                ControlToggleRow("Bluetooth", if (bluetoothState) "Searching" else "Off", bluetoothState) { bluetoothState = it }
                HorizontalDivider(color = colors.surfaceBorder.copy(alpha = 0.3f))

                ControlToggleRow("Mobile Data Network", if (mobileDataState) "On" else "Off", mobileDataState) { mobileDataState = it }
                HorizontalDivider(color = colors.surfaceBorder.copy(alpha = 0.3f))

                ControlToggleRow("Airplane Mode", if (airplaneModeState) "Active" else "Off", airplaneModeState) { airplaneModeState = it }
                HorizontalDivider(color = colors.surfaceBorder.copy(alpha = 0.3f))

                ControlToggleRow("Do Not Disturb", if (dndState) "Quiet Mode Active" else "Off", dndState) { dndState = it }
                HorizontalDivider(color = colors.surfaceBorder.copy(alpha = 0.3f))

                ControlToggleRow("Auto Space Clean", "Runs every 24h", autoCleanState) { autoCleanState = it }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceGlass)
                .border(1.dp, colors.surfaceBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "> HARDWARE TELEMETRY CONTROLS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Screen Brightness", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = colors.onSurface)
                        Text("${(brightness * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.accent)
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

                HorizontalDivider(color = colors.surfaceBorder.copy(alpha = 0.3f))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("System Sound Volume", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = colors.onSurface)
                        Text("${(volume * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.secondaryGlow)
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
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.onSurface)
            Text(statusText, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = colors.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.onSurfaceVariant,
                uncheckedTrackColor = colors.surfaceVariant
            )
        )
    }
}
