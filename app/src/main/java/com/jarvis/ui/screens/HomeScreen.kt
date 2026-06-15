package com.jarvis.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.context.DeviceContext
import com.jarvis.ui.theme.LocalJarvisColors
import com.jarvis.brain.LlmClient
import com.jarvis.voice.SpeechState
import com.jarvis.voice.SpeechToTextManager
import com.jarvis.voice.TextToSpeechManager
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun HomeScreen(
    deviceContext: DeviceContext,
    speechState: SpeechState,
    sttManager: SpeechToTextManager,
    ttsManager: TextToSpeechManager,
    onQuickActionClick: (String) -> Unit
) {
    val colors = LocalJarvisColors.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Chat message history (visual only, doesn't affect pipeline logic)
    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage("JARVISH", "Hello! I'm your AI assistant. Tap the walkie-talkie below to talk, or type a message here.", false)
        )
    }
    var manualLoading by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf("") }

    // Core pipeline states — must be declared before LaunchedEffect uses them
    var llmResponse by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var lastUserText by remember { mutableStateOf("") }

    // STT to LLM to TTS Loop — SINGLE source of truth
    // Only fires on FINAL Results (not PartialResults) to prevent overlapping LLM calls.
    LaunchedEffect(speechState) {
        if (speechState is SpeechState.Results) {
            val userText = speechState.text
            if (userText.isNotBlank() && !isProcessing) {
                lastUserText = userText
                isProcessing = true
                chatMessages.add(ChatMessage("You", userText, true))
                try {
                    val response = LlmClient.generateContent(context, userText)
                    llmResponse = response
                    chatMessages.add(ChatMessage("JARVISH", response, false))
                    ttsManager.speak(response)
                } catch (e: Exception) {
                    val err = "Error: ${e.message}"
                    llmResponse = err
                    chatMessages.add(ChatMessage("JARVISH", err, false))
                } finally {
                    isProcessing = false
                    sttManager.resetState()
                }
            }
        } else if (speechState is SpeechState.Listening) {
            llmResponse = null
            lastUserText = ""
            ttsManager.stopSpeaking()
        }
    }

    // Animation for the dials/waves
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val dialAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dialAngle"
    )

    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // 1. Central Optimal Dial
        Box(
            modifier = Modifier
                .size(240.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colors.accent.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 20.dp.toPx()

                // 1. Faded outermost ring
                drawCircle(
                    color = colors.accent.copy(alpha = 0.05f),
                    radius = radius + 20.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx())
                )
                
                // 2. Dashed technological ring
                drawCircle(
                    color = colors.secondaryGlow.copy(alpha = 0.3f),
                    radius = radius + 10.dp.toPx(),
                    style = Stroke(
                        width = 2.dp.toPx(), 
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 20f), dialAngle)
                    )
                )

                // 3. Main heavy background ring
                drawCircle(
                    color = colors.accent.copy(alpha = 0.1f),
                    radius = radius,
                    style = Stroke(width = 8.dp.toPx())
                )

                // 4. Primary glowing sweep arc
                drawArc(
                    color = colors.accent,
                    startAngle = dialAngle,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )

                // 5. Inner secondary fast arc
                val innerRadius = radius - 15.dp.toPx()
                drawArc(
                    color = colors.secondaryGlow,
                    startAngle = -dialAngle * 1.5f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                    size = Size(innerRadius * 2, innerRadius * 2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "100%",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.onSurface,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "OPTIMAL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent.copy(alpha = textAlpha),
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "SYSTEM SECURE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceVariant
                )
            }
        }

        // 2. CPU & RAM Monitor Widgets (Side by side)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CPU Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceGlass)
                    .border(1.dp, colors.surfaceBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CPU", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        Text("18%", fontSize = 16.sp, fontWeight = FontWeight.Black, color = colors.accent)
                    }

                    // Drawing simulated active wave graph
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        val path = Path()
                        val width = size.width
                        val height = size.height
                        val midY = height / 2f
                        val amplitude = height * 0.25f

                        path.moveTo(0f, midY)
                        for (x in 0..width.toInt() step 5) {
                            val relativeX = x / width
                            val y = midY + amplitude * sin(relativeX * 4 * Math.PI.toFloat() + phase)
                            path.lineTo(x.toFloat(), y)
                        }

                        drawPath(
                            path = path,
                            color = colors.accent,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Text("Temp: 39°C", fontSize = 10.sp, color = colors.onSurfaceVariant)
                }
            }

            // RAM Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceGlass)
                    .border(1.dp, colors.surfaceBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("RAM", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        Text("62%", fontSize = 16.sp, fontWeight = FontWeight.Black, color = colors.secondaryGlow)
                    }

                    // Drawing simulated active wave graph (different frequency/color)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        val path = Path()
                        val width = size.width
                        val height = size.height
                        val midY = height / 2f
                        val amplitude = height * 0.2f

                        path.moveTo(0f, midY)
                        for (x in 0..width.toInt() step 5) {
                            val relativeX = x / width
                            val y = midY + amplitude * sin(relativeX * 6 * Math.PI.toFloat() - phase * 0.8f)
                            path.lineTo(x.toFloat(), y)
                        }

                        drawPath(
                            path = path,
                            color = colors.secondaryGlow,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Text("Avail: 2.8 GB", fontSize = 10.sp, color = colors.onSurfaceVariant)
                }
            }
        }

        // 3. Storage & Battery Linear Indicators
        Card(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surfaceGlass)
                .border(1.dp, colors.surfaceBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Storage Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Storage Usage", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        Text("128 GB / 256 GB (50%)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                    }
                    LinearProgressIndicator(
                        progress = { 0.5f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = colors.accent,
                        trackColor = colors.accent.copy(alpha = 0.15f)
                    )
                }

                Divider(color = colors.onSurfaceVariant.copy(alpha = 0.1f))

                // Battery Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Battery Status", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        val pctText = if (deviceContext.batteryPct > 0) "${deviceContext.batteryPct}%" else "84%"
                        val chargeText = if (deviceContext.isCharging) " (Charging)" else " (Discharging)"
                        Text("$pctText$chargeText", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                    }
                    val batteryRatio = if (deviceContext.batteryPct > 0) deviceContext.batteryPct / 100f else 0.84f
                    LinearProgressIndicator(
                        progress = { batteryRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (deviceContext.isCharging) Color.Green else colors.secondaryGlow,
                        trackColor = colors.secondaryGlow.copy(alpha = 0.15f)
                    )
                }
            }
        }

        // 4. Quick Actions Layout
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "QUICK ACTIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurfaceVariant,
                letterSpacing = 1.5.sp
            )

            val quickActions = listOf(
                QuickActionItem("Junk Cleaner", "Trash & temp logs file scan", colors.accent),
                QuickActionItem("Memory Boost", "Kill background apps", colors.secondaryGlow),
                QuickActionItem("Battery Saver", "Manage standby configuration", colors.secondaryGlow),
                QuickActionItem("Full Scan", "Security integrity audit", colors.accent)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(quickActions) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceGlass)
                            .border(1.dp, colors.surfaceBorder, RoundedCornerShape(12.dp))
                            .clickable { onQuickActionClick(item.title) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                item.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = item.color
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                item.desc,
                                fontSize = 9.sp,
                                color = colors.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 5. Chatbot Conversation Area
        Card(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surfaceGlass)
                .border(1.dp, colors.surfaceBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "💬 CHATBOT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                    letterSpacing = 1.5.sp
                )

                Divider(color = colors.accent.copy(alpha = 0.2f))

                // Chat messages display — uses the top-level chatMessages list
                // (NO inner declaration here — that would shadow the outer one)

                // Chat scrollable area — NO inner LaunchedEffect, just display the list
                val chatScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp, max = 200.dp)
                        .verticalScroll(chatScroll),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (chatMessages.isEmpty()) {
                        Text(
                            "No messages yet. Tap the walkie-talkie below to talk!",
                            fontSize = 11.sp,
                            color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        chatMessages.forEach { msg ->
                            ChatBubble(msg, colors)
                        }
                    }
                }

                // Manual text input + send button — uses own local state, does NOT touch isProcessing/llmResponse
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualText,
                        onValueChange = { manualText = it },
                        placeholder = { Text("Type a message...", fontSize = 11.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.surfaceBorder
                        )
                    )
                    Button(
                        onClick = {
                            val text = manualText.trim()
                            if (text.isBlank() || manualLoading) return@Button
                            chatMessages.add(ChatMessage("You", text, true))
                            manualText = ""
                            manualLoading = true
                            coroutineScope.launch {
                                try {
                                    val response = com.jarvis.brain.LlmClient.generateContent(context, text)
                                    chatMessages.add(ChatMessage("JARVISH", response, false))
                                    ttsManager.speak(response)
                                } catch (e: Exception) {
                                    chatMessages.add(ChatMessage("JARVISH", "Error: ${e.message}", false))
                                } finally {
                                    manualLoading = false
                                }
                            }
                        },
                        enabled = !manualLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                    ) {
                        Text(if (manualLoading) "..." else "Send", fontSize = 11.sp, color = Color.Black)
                    }
                }
            }
        }

        // 6. Walkie Talkie PTT Section
        Card(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surfaceGlass)
                .border(1.dp, colors.surfaceBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top row: Radio info + signal bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TX / RX indicator
                    val txRxColor = when {
                        speechState is SpeechState.Listening || speechState is SpeechState.PartialResults ->
                            Color(0xFFFF1E27) // Red = transmitting (you're talking)
                        isProcessing -> Color(0xFFFFD600) // Yellow = receiving (AI thinking)
                        llmResponse != null -> Color(0xFF00E676) // Green = received response
                        else -> Color(0xFFB0BEC5) // Gray = idle
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(txRxColor)
                        )
                        Text(
                            text = when {
                                speechState is SpeechState.Listening || speechState is SpeechState.PartialResults -> "TX"
                                isProcessing -> "RX WAIT"
                                llmResponse != null -> "RX OK"
                                else -> "STANDBY"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = txRxColor,
                            letterSpacing = 1.sp
                        )
                    }

                    // Signal strength bars
                    val signalBars = when {
                        speechState is SpeechState.Error -> 0
                        speechState is SpeechState.Listening || speechState is SpeechState.PartialResults -> 4
                        isProcessing -> 3
                        llmResponse != null -> 4
                        else -> 2
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                        for (i in 1..4) {
                            val barHeight = (i * 4).dp
                            val barColor = if (i <= signalBars) colors.accent else colors.surfaceVariant
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(barHeight)
                                    .background(barColor, RoundedCornerShape(1.dp))
                            )
                        }
                    }
                }

                // Big PTT Button — like a real radio
                val isPttActive = speechState is SpeechState.Listening || speechState is SpeechState.PartialResults
                val pttPulse by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (isPttActive) 1.15f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pttPulse"
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(pttPulse)
                        .clip(CircleShape)
                        .background(
                            if (isPttActive) colors.accent else colors.surfaceVariant.copy(alpha = 0.6f)
                        )
                        .border(
                            width = if (isPttActive) 4.dp else 2.dp,
                            color = if (isPttActive) colors.secondaryGlow else colors.surfaceBorder,
                            shape = CircleShape
                        )
                        .clickable {
                            coroutineScope.launch {
                                if (isPttActive) {
                                    sttManager.stopListening()
                                } else {
                                    sttManager.startListening()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📻",
                            fontSize = 36.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isPttActive) "TALKING" else "PTT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isPttActive) Color.Black else colors.onSurfaceVariant,
                            letterSpacing = 2.sp
                        )
                    }
                }

                // Status text below the PTT button
                Text(
                    text = when {
                        speechState is SpeechState.PartialResults -> "🎙️ ${speechState.text}"
                        speechState is SpeechState.Listening -> "🎙️ Hold to talk..."
                        speechState is SpeechState.Results -> "✅ Sent: \"${speechState.text}\""
                        speechState is SpeechState.Error -> "❌ ${speechState.errorMsg}"
                        isProcessing -> "🤖 JARVISH is thinking..."
                        llmResponse != null -> "✅ Response received"
                        else -> "👆 Tap to start talking"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        speechState is SpeechState.Error -> Color(0xFFFF1E27)
                        isPttActive -> colors.accent
                        else -> colors.onSurfaceVariant
                    },
                    maxLines = 2
                )

                // Frequency display (radio style)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.background.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("FREQ:", fontSize = 9.sp, color = colors.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text("145.500", fontSize = 9.sp, color = colors.accent, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("|", fontSize = 9.sp, color = colors.onSurfaceVariant)
                    Text("CH:", fontSize = 9.sp, color = colors.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text("01", fontSize = 9.sp, color = colors.accent, fontWeight = FontWeight.Black)
                    Text("|", fontSize = 9.sp, color = colors.onSurfaceVariant)
                    Text("PWR:", fontSize = 9.sp, color = colors.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text("5W", fontSize = 9.sp, color = colors.accent, fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

data class ChatMessage(
    val sender: String,
    val text: String,
    val isUser: Boolean
)

@Composable
fun ChatBubble(message: ChatMessage, colors: com.jarvis.ui.theme.JarvisColors) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isUser) colors.accent.copy(alpha = 0.85f) else colors.surfaceVariant.copy(alpha = 0.4f)
    val textColor = if (message.isUser) Color.Black else colors.onSurface
    val senderColor = if (message.isUser) colors.accent else colors.secondaryGlow

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.isUser) {
            // AI avatar
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start) {
            Text(
                text = message.sender,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = senderColor,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(
                        topStart = if (message.isUser) 12.dp else 4.dp,
                        topEnd = if (message.isUser) 4.dp else 12.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 12.dp
                    ))
                    .background(bubbleColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 11.sp,
                    color = textColor,
                    lineHeight = 16.sp
                )
            }
        }

        if (message.isUser) {
            Spacer(modifier = Modifier.width(6.dp))
            // User avatar
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.secondaryGlow.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("👤", fontSize = 14.sp)
            }
        }
    }
}

data class QuickActionItem(
    val title: String,
    val desc: String,
    val color: Color
)
