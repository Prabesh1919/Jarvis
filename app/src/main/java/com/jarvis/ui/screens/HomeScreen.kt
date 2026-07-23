package com.jarvis.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.brain.LlmClient
import com.jarvis.config.AppConfig
import com.jarvis.context.DeviceContext
import com.jarvis.ui.theme.JarvisColors
import com.jarvis.ui.theme.LocalJarvisColors
import com.jarvis.voice.SpeechState
import com.jarvis.voice.SpeechToTextManager
import com.jarvis.voice.TextToSpeechManager
import com.jarvis.voice.TtsState
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String,
    val text: String,
    val isUser: Boolean,
    val modelName: String = "Gemini 2.5 Flash",
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun ChatBubble(msg: ChatMessage, colors: JarvisColors) {
    val isUser = msg.isUser
    val align = if (isUser) Alignment.End else Alignment.Start
    
    val bubbleGrad = if (isUser) {
        Brush.linearGradient(
            colors = listOf(
                colors.accent.copy(alpha = 0.22f),
                colors.surfaceVariant.copy(alpha = 0.85f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                colors.cardGradStart.copy(alpha = 0.9f),
                colors.cardGradEnd.copy(alpha = 0.95f)
            )
        )
    }
    
    val borderCol = if (isUser) colors.accent.copy(alpha = 0.6f) else colors.surfaceBorder.copy(alpha = 0.8f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = align
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isUser) "▶ OPERATOR" else "⚡ JARVISH",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isUser) colors.accent else colors.secondaryGlow
            )
            if (!isUser) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = colors.accent.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.accent.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = msg.modelName.uppercase(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.accent,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 12.dp
                    )
                )
                .background(bubbleGrad)
                .border(
                    width = 1.dp,
                    color = borderCol,
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 12.dp
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = msg.text,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = colors.onSurface,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun HomeScreen(
    deviceContext: DeviceContext,
    speechState: SpeechState,
    sttManager: SpeechToTextManager,
    ttsManager: TextToSpeechManager,
    onQuickActionClick: (String) -> Unit
) {
    val colors = LocalJarvisColors.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val currentModel = remember(context) { AppConfig.getLlmModel(context) }

    // Expandable Telemetry Drawer State
    var isHudExpanded by remember { mutableStateOf(false) }

    // Chat message history
    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage("JARVISH", "[SYSTEM_READY] Active Gateway: $currentModel. Type or speak below.", false, modelName = currentModel)
        )
    }
    var manualLoading by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf("") }

    val ttsState by ttsManager.ttsState.collectAsState()
    var isProcessing by remember { mutableStateOf(false) }
    var lastResponseText by remember { mutableStateOf<String?>(null) }

    // Auto-scroll to bottom on new message
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // STT -> LLM -> TTS Pipeline
    LaunchedEffect(speechState) {
        if (speechState is SpeechState.Results) {
            val userText = speechState.text
            if (userText.isNotBlank() && !isProcessing) {
                isProcessing = true
                lastResponseText = null
                chatMessages.add(ChatMessage("You", userText, true))
                try {
                    val response = LlmClient.generateContent(context, userText)
                    chatMessages.add(ChatMessage("JARVISH", response, false, modelName = currentModel))
                    lastResponseText = response
                    ttsManager.speak(response)
                } catch (e: Exception) {
                    chatMessages.add(ChatMessage("JARVISH", "[ERROR] ${e.message}", false, modelName = currentModel))
                } finally {
                    isProcessing = false
                    sttManager.resetState()
                }
            }
        } else if (speechState is SpeechState.Listening) {
            if (ttsState is TtsState.Speaking) {
                ttsManager.stopSpeaking()
            }
        }
    }

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dialAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dialAngle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Compact Header & Active LLM Model Indicator Badge
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceGlass)
                .border(1.dp, colors.surfaceBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "[ONLINE]",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.accent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "[MODEL: ${currentModel.uppercase()}]",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.secondaryGlow
                    )
                }

                TextButton(
                    onClick = { isHudExpanded = !isHudExpanded },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (isHudExpanded) "[ ▲ HUD ]" else "[ ▼ HUD ]",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Expandable Telemetry Drawer
        AnimatedVisibility(
            visible = isHudExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(colors.accent.copy(alpha = 0.08f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2f - 15.dp.toPx()

                        drawCircle(
                            color = colors.accent.copy(alpha = 0.1f),
                            radius = radius,
                            style = Stroke(width = 4.dp.toPx())
                        )
                        drawArc(
                            color = colors.accent,
                            startAngle = dialAngle,
                            sweepAngle = 140f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "OPTIMAL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = colors.accent
                        )
                        Text(
                            "CPU: 18% | RAM: 62%",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Desktop-Style Chatbot Terminal Messages Area (Displays Active LLM Model on Each Bubble)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceGlass)
                .border(1.dp, colors.surfaceBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatMessages) { msg ->
                    ChatBubble(msg, colors)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 4. Quick-Action Terminal Command Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val chips = listOf("Query Docs", "Launch App", "System Status", "Offline Mode")
            chips.forEach { chipText ->
                SuggestionChip(
                    onClick = { onQuickActionClick(chipText) },
                    label = {
                        Text(
                            "> $chipText",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.accent
                        )
                    },
                    modifier = Modifier.height(26.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = colors.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = colors.surfaceBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 5. Desktop-Style Interactive Input Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceGlass)
                .border(1.dp, colors.surfaceBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualText,
                    onValueChange = { manualText = it },
                    placeholder = {
                        Text(
                            "Type command or ask JARVISH...",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.onSurface
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.onSurface,
                        unfocusedTextColor = colors.onSurface,
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
                                val response = LlmClient.generateContent(context, text)
                                chatMessages.add(ChatMessage("JARVISH", response, false, modelName = currentModel))
                                lastResponseText = response
                                ttsManager.speak(response)
                            } catch (e: Exception) {
                                chatMessages.add(ChatMessage("JARVISH", "[ERROR] ${e.message}", false, modelName = currentModel))
                            } finally {
                                manualLoading = false
                            }
                        }
                    },
                    enabled = !manualLoading,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text(
                        if (manualLoading) "..." else "SEND",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black
                    )
                }

                val isListening = speechState is SpeechState.Listening
                val micBg = if (isListening) colors.accent else colors.surfaceVariant

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(micBg)
                        .border(1.dp, colors.surfaceBorder, CircleShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    sttManager.startListening()
                                    tryAwaitRelease()
                                    sttManager.stopListening()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isListening) "🎙️" else "🎤",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
