package com.jarvis.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ui.theme.LocalJarvisColors
import com.jarvis.knowledge.LocalRagEngine
import com.jarvis.safety.SafetyLayer
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// 1. SYSTEM OPTIMIZER (SCAN & CLEAN)
// -------------------------------------------------------------
@Composable
fun OptimizerScreen(onBack: () -> Unit) {
    val colors = LocalJarvisColors.current
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var isCleaning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Animate scan from 0 to 75%
        animate(0f, 75f, animationSpec = tween(2500, easing = FastOutSlowInEasing)) { value, _ ->
            scanProgress = value
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text("◀ BACK", modifier = Modifier.clickable { onBack() }, color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Text("SYSTEM OPTIMIZER", fontSize = 18.sp, fontWeight = FontWeight.Black, color = colors.accent)

        // Progress Dial
        Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f - 10.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)

                drawCircle(color = colors.accent.copy(alpha = 0.1f), radius = radius, style = Stroke(width = 6.dp.toPx()))

                drawArc(
                    color = colors.accent,
                    startAngle = -90f,
                    sweepAngle = (scanProgress / 100f) * 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${scanProgress.toInt()}%", fontSize = 32.sp, fontWeight = FontWeight.Black, color = colors.onSurface)
                Text(if (scanProgress < 75f) "SCANNING..." else "STOPPED", fontSize = 10.sp, color = colors.onSurfaceVariant)
            }
        }

        // Checklist Items
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SCAN ANALYSIS RESULTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                
                OptimizerItemRow("Cache Files", "2.45 GB", scanProgress >= 30f)
                OptimizerItemRow("Residual Files", "1.32 GB", scanProgress >= 45f)
                OptimizerItemRow("Apk Files", "880 MB", scanProgress >= 60f)
                OptimizerItemRow("System Junk", "560 MB", scanProgress >= 70f)
                OptimizerItemRow("Thumbnails", "320 MB", scanProgress >= 75f)
            }
        }

        Button(
            onClick = {
                if (scanProgress >= 75f) {
                    isCleaning = true
                    scanProgress = 100f
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isCleaning) "SYSTEM CLEANED!" else "CLEAN SYSTEM JUNK", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun OptimizerItemRow(title: String, sizeStr: String, analyzed: Boolean) {
    val colors = LocalJarvisColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 13.sp, color = colors.onSurface)
        Text(
            if (analyzed) sizeStr else "Calculating...",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (analyzed) colors.accent else colors.onSurfaceVariant
        )
    }
}

// -------------------------------------------------------------
// 2. PERFORMANCE BOOSTER (ROCKET SHAKE)
// -------------------------------------------------------------
@Composable
fun PerformanceScreen(onBack: () -> Unit) {
    val colors = LocalJarvisColors.current
    var boostProgress by remember { mutableStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "shake")
    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeOffset"
    )

    LaunchedEffect(Unit) {
        for (i in 0..100 step 2) {
            boostProgress = i
            kotlinx.coroutines.delay(50)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text("◀ BACK", modifier = Modifier.clickable { onBack() }, color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Text("BOOST PERFORMANCE", fontSize = 18.sp, fontWeight = FontWeight.Black, color = colors.accent)

        Spacer(modifier = Modifier.height(30.dp))

        // Shaking Rocket Visual
        Box(
            modifier = Modifier
                .offset(y = if (boostProgress < 100) shakeOffset.dp else 0.dp)
                .size(150.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.1f))
                .border(2.dp, colors.accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🚀", fontSize = 68.sp)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (boostProgress < 100) "BOOSTING..." else "BOOST COMPLETE!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = colors.onSurface
            )
            Text(
                "Closing background tasks and clearing CPU cache...",
                fontSize = 12.sp,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Percentage status
        Text("$boostProgress%", fontSize = 36.sp, fontWeight = FontWeight.Black, color = colors.accent)

        // Status Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BoostBadge("CPU Boost", active = boostProgress > 20, modifier = Modifier.weight(1f))
            BoostBadge("RAM Boost", active = boostProgress > 50, modifier = Modifier.weight(1f))
            BoostBadge("GPU Boost", active = boostProgress > 80, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun BoostBadge(name: String, active: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalJarvisColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) colors.surfaceVariant else colors.surface)
            .border(1.dp, if (active) colors.accent else Color.Transparent, RoundedCornerShape(8.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) colors.accent else colors.onSurfaceVariant
        )
    }
}

// -------------------------------------------------------------
// 3. SECURITY SUITE (SHIELD CHECK)
// -------------------------------------------------------------
@Composable
fun SecurityScreen(onBack: () -> Unit) {
    val colors = LocalJarvisColors.current
    var isSecuring by remember { mutableStateOf(false) }
    var secureProgress by remember { mutableStateOf(0) }

    LaunchedEffect(isSecuring) {
        if (isSecuring) {
            for (i in 0..100 step 5) {
                secureProgress = i
                kotlinx.coroutines.delay(80)
            }
            isSecuring = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text("◀ BACK", modifier = Modifier.clickable { onBack() }, color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Text("SECURITY SUITE", fontSize = 18.sp, fontWeight = FontWeight.Black, color = colors.accent)

        Spacer(modifier = Modifier.height(20.dp))

        // Shield Indicator
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(if (secureProgress == 100 || secureProgress == 0) Color(0xFF1B5E20).copy(alpha = 0.1f) else colors.accent.copy(alpha = 0.1f))
                .border(2.dp, if (secureProgress == 100 || secureProgress == 0) Color.Green else colors.accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(if (secureProgress == 100 || secureProgress == 0) "🛡️" else "🔍", fontSize = 60.sp)
        }

        Text(
            text = if (isSecuring) "SCANNING FOR VIRUSES ($secureProgress%)" else "YOUR DEVICE IS SECURE",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (secureProgress == 100 || secureProgress == 0) Color.Green else colors.onSurface
        )

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("PROTECTION LAYER STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)

                SecurityStatusRow("Virus Threat Engine", if (isSecuring && secureProgress < 40) "Analyzing..." else "No threats found", true)
                SecurityStatusRow("Malware Protection", if (isSecuring && secureProgress < 70) "Analyzing..." else "No threats found", true)
                SecurityStatusRow("Real-Time Web Shield", "Active (Secure)", true)
                SecurityStatusRow("Financial Vault Gating", "Shield active", true)
            }
        }

        Button(
            onClick = {
                isSecuring = true
                secureProgress = 0
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
            shape = RoundedCornerShape(12.dp),
            enabled = !isSecuring
        ) {
            Text("TRIGGER FULL SECURE SCAN", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun SecurityStatusRow(title: String, desc: String, secure: Boolean) {
    val colors = LocalJarvisColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Text(desc, fontSize = 10.sp, color = colors.onSurfaceVariant)
        }
        Text(if (secure) "SAFE" else "RISK", color = if (secure) Color.Green else Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

// -------------------------------------------------------------
// 4. APP LOCK SCREEN (PIN PAD)
// -------------------------------------------------------------
@Composable
fun AppLockScreen(onBack: () -> Unit) {
    val colors = LocalJarvisColors.current
    var pinText by remember { mutableStateOf("") }
    val maxPinLength = 5

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text("◀ BACK", modifier = Modifier.clickable { onBack() }, color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Text("APP LOCK ENGINE", fontSize = 18.sp, fontWeight = FontWeight.Black, color = colors.accent)

        Spacer(modifier = Modifier.height(10.dp))

        Text("🔐", fontSize = 48.sp)
        Text("Enter Operator PIN to Unlock", fontSize = 13.sp, color = colors.onSurfaceVariant)

        // PIN Indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until maxPinLength) {
                val filled = i < pinText.length
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (filled) colors.accent else colors.surfaceVariant)
                        .border(1.dp, colors.accent, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Numerical Pin Pad
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val numRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("⌫", "0", "🔓")
            )

            for (row in numRows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    for (key in row) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.6f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surface)
                                .clickable {
                                    when (key) {
                                        "⌫" -> {
                                            if (pinText.isNotEmpty()) pinText = pinText.dropLast(1)
                                        }
                                        "🔓" -> {
                                            if (pinText.length == maxPinLength) {
                                                // mock success
                                                pinText = ""
                                            }
                                        }
                                        else -> {
                                            if (pinText.length < maxPinLength) {
                                                pinText += key
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(key, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. BATTERY SAVER SCREEN
// -------------------------------------------------------------
@Composable
fun BatterySaverScreen(onBack: () -> Unit) {
    val colors = LocalJarvisColors.current
    var selectedMode by remember { mutableIntStateOf(0) } // 0: Normal, 1: Power Saving, 2: Ultra Power Saving

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text("◀ BACK", modifier = Modifier.clickable { onBack() }, color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Text("BATTERY STANDBY SAVER", fontSize = 18.sp, fontWeight = FontWeight.Black, color = colors.accent)

        // Large Battery Dial
        Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f - 10.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)

                drawCircle(color = colors.secondaryGlow.copy(alpha = 0.1f), radius = radius, style = Stroke(width = 6.dp.toPx()))

                drawArc(
                    color = colors.secondaryGlow,
                    startAngle = -90f,
                    sweepAngle = 0.84f * 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("84%", fontSize = 36.sp, fontWeight = FontWeight.Black, color = colors.onSurface)
                Text("12h 45m Left", fontSize = 11.sp, color = colors.onSurfaceVariant)
            }
        }

        // Saver Modes
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("CHOOSE POWER STANDBY MODE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)

                BatteryModeRow("Normal Mode", "12h 45m standard", selectedMode == 0) { selectedMode = 0 }
                Divider(color = colors.onSurfaceVariant.copy(alpha = 0.08f))
                BatteryModeRow("Power Saving", "16h 30m standby limits", selectedMode == 1) { selectedMode = 1 }
                Divider(color = colors.onSurfaceVariant.copy(alpha = 0.08f))
                BatteryModeRow("Ultra Power Saving", "38h 15m absolute sleep", selectedMode == 2) { selectedMode = 2 }
            }
        }

        Button(
            onClick = { /* enable mode */ },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("ENABLE POWER MODE", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun BatteryModeRow(title: String, desc: String, active: Boolean, onClick: () -> Unit) {
    val colors = LocalJarvisColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) colors.surfaceVariant else Color.Transparent)
            .clickable { onClick() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Text(desc, fontSize = 10.sp, color = colors.onSurfaceVariant)
        }
        RadioButton(
            selected = active,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = colors.accent)
        )
    }
}

// -------------------------------------------------------------
// 6. GAME BOOSTER SCREEN
// -------------------------------------------------------------
@Composable
fun GameBoosterScreen(onBack: () -> Unit) {
    val colors = LocalJarvisColors.current
    var isBoosting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text("◀ BACK", modifier = Modifier.clickable { onBack() }, color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Text("HARDWARE GAME BOOSTER", fontSize = 18.sp, fontWeight = FontWeight.Black, color = colors.accent)

        // Gamepad Visual
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.1f))
                .border(2.dp, colors.accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🎮", fontSize = 56.sp)
        }

        Text(
            if (isBoosting) "ULTRA GAME MODE ACTIVE" else "READY TO LAUNCH GAME",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isBoosting) colors.accent else colors.onSurface
        )

        // Optimization toggles
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("OPTIMIZATION SCHEMES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)

                GameToggleRow("Clear Active RAM", "Freed 1.25 GB", true)
                Divider(color = colors.onSurfaceVariant.copy(alpha = 0.08f))
                GameToggleRow("Optimize CPU Clock", "CPU Priority assigned", true)
                Divider(color = colors.onSurfaceVariant.copy(alpha = 0.08f))
                GameToggleRow("Network Boost Routing", "Ping reducer active", true)
                Divider(color = colors.onSurfaceVariant.copy(alpha = 0.08f))
                GameToggleRow("Block System Notifications", "Suppress heads-up banners", isBoosting) { isBoosting = it }
            }
        }

        Button(
            onClick = { isBoosting = !isBoosting },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isBoosting) "DISABLE BOOST" else "START GAMING", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun GameToggleRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    val colors = LocalJarvisColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Text(desc, fontSize = 10.sp, color = colors.onSurfaceVariant)
        }
        if (onCheckedChange != null) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = colors.accent)
            )
        } else {
            Text("ACTIVE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Green)
        }
    }
}

// -------------------------------------------------------------
// 7. RAG SEARCH SCREEN
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RagSearchScreen(onBack: () -> Unit) {
    val colors = LocalJarvisColors.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var documentInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var searchResult by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text("◀ BACK", modifier = Modifier.clickable { onBack() }, color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Text("LOCAL RAG SEARCH ENGINE", fontSize = 18.sp, fontWeight = FontWeight.Black, color = colors.accent)

        // Ingestion Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("DOCUMENT INDEXER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                
                OutlinedTextField(
                    value = documentInput,
                    onValueChange = { documentInput = it },
                    placeholder = { Text("Enter document text to embed...", color = colors.onSurfaceVariant.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.onSurface,
                        unfocusedTextColor = colors.onSurface,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (documentInput.isNotBlank()) {
                                LocalRagEngine.ingestDocument(context, "Manual Ingest", "internal://manual_doc", documentInput)
                                documentInput = ""
                                Toast.makeText(context, "Passage indexed successfully!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("INDEX PASSAGE", color = Color.White)
                }
            }
        }

        // Search Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SEMANTIC RETRIEVAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Ask document base...", color = colors.onSurfaceVariant.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.onSurface,
                        unfocusedTextColor = colors.onSurface,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.onSurfaceVariant.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (searchQuery.isNotBlank()) {
                                val results = LocalRagEngine.queryRag(context, searchQuery, topN = 2)
                                searchResult = if (results.isNotEmpty()) {
                                    results.joinToString("\n\n") { "▶ $it" }
                                } else {
                                    "No matching document base chunks found."
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryGlow)
                ) {
                    Text("RUN SEARCH", color = Color.White)
                }

                if (searchResult.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = searchResult,
                        fontSize = 11.sp,
                        color = colors.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceVariant)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 8. AUTOMATION SCREEN
// -------------------------------------------------------------
@Composable
fun AutomationScreen(
    isAutomationAllowed: Boolean,
    onBack: () -> Unit
) {
    val colors = LocalJarvisColors.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text("◀ BACK", modifier = Modifier.clickable { onBack() }, color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Text("AUTOMATION VERIFICATION", fontSize = 18.sp, fontWeight = FontWeight.Black, color = colors.accent)

        Spacer(modifier = Modifier.height(10.dp))

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("AUTOMATION STATE GATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isAutomationAllowed) "Automation Active" else "Automation Stopped",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAutomationAllowed) Color.Green else Color.Red
                        )
                        Text(
                            text = "Safety Layer emergency stop control",
                            fontSize = 10.sp,
                            color = colors.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            if (isAutomationAllowed) {
                                SafetyLayer.emergencyStop()
                            } else {
                                SafetyLayer.setAutomationAllowed(true)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAutomationAllowed) colors.accent else Color.Green
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isAutomationAllowed) "EMERGENCY HALT" else "RESTORE ENGINE", color = Color.White)
                    }
                }
            }
        }

        // WhatsApp Accessibility Mock Card
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SCOPED WORKFLOW DEMO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                Text(
                    text = "Runs a mock automation workflow setting message content in WhatsApp and executing clicks. Make sure Jarvis Accessibility service is active in Settings.",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            com.jarvis.accessibility.AccessibilityDemo.runDemoWorkflow(context)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryGlow)
                ) {
                    Text("TRIGGER ACCESSIBILITY DEMO", color = Color.White)
                }
            }
        }
    }
}
