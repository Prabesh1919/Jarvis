package com.jarvis.ui.screens

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Context
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
import com.jarvis.brain.LocalLlmEngine
import com.jarvis.config.AppConfig
import com.jarvis.context.DeviceContext
import com.jarvis.safety.SafetyLayer
import com.jarvis.ui.theme.JarvisColors
import com.jarvis.ui.theme.LocalJarvisColors
import com.jarvis.voice.SpeechState
import com.jarvis.voice.SpeechToTextManager
import com.jarvis.voice.TextToSpeechManager
import com.jarvis.voice.TtsState
import kotlinx.coroutines.launch

fun isNetworkConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val net = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(net) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Composable
fun JarvisMainTerminalScreen(
    deviceContext: DeviceContext,
    speechState: SpeechState,
    sttManager: SpeechToTextManager,
    ttsManager: TextToSpeechManager,
    onOpenVaultSettings: () -> Unit,
    onQuickActionClick: (String) -> Unit
) {
    val colors = LocalJarvisColors.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Automatic Offline Network & Model Switcher State
    var isOnline by remember { mutableStateOf(isNetworkConnected(context)) }
    
    // Periodically re-check network state
    LaunchedEffect(Unit) {
        while (true) {
            isOnline = isNetworkConnected(context)
            kotlinx.coroutines.delay(3000)
        }
    }

    val activeModelName = remember(isOnline) {
        if (isOnline) AppConfig.getLlmModel(context) else "LOCAL LLAMA 3.2 1B (OFFLINE GGUF)"
    }

    // Chat message history
    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage("JARVISH", "[SYSTEM_READY] Gateway: $activeModelName. Type or speak below.", false, modelName = activeModelName)
        )
    }
    var manualLoading by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf("") }

    val ttsState by ttsManager.ttsState.collectAsState()
    var isProcessing by remember { mutableStateOf(false) }
    var lastResponseText by remember { mutableStateOf<String?>(null) }
    val isSafetyAllowed by SafetyLayer.isAutomationAllowed.collectAsState()

    // Auto-scroll to bottom on new message
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // STT -> LLM -> TTS Pipeline with Automatic Offline Fallback
    LaunchedEffect(speechState) {
        if (speechState is SpeechState.Results) {
            val userText = speechState.text
            if (userText.isNotBlank() && !isProcessing) {
                isProcessing = true
                lastResponseText = null
                chatMessages.add(ChatMessage("You", userText, true))
                try {
                    val response = if (isOnline) {
                        LlmClient.generateContent(context, userText)
                    } else {
                        LocalLlmEngine.generate(context, userText)
                    }
                    chatMessages.add(ChatMessage("JARVISH", response, false, modelName = activeModelName))
                    lastResponseText = response
                    ttsManager.speak(response)
                } catch (e: Exception) {
                    chatMessages.add(ChatMessage("JARVISH", "[ERROR] ${e.message}", false, modelName = activeModelName))
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Status Header & Automatic Model Indicator Badge
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
                            .background(if (isOnline) colors.accent else Color.Yellow)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (isOnline) "[ONLINE]" else "[OFFLINE]",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isOnline) colors.accent else Color.Yellow
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "[MODEL: ${activeModelName.uppercase()}]",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.secondaryGlow
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Safety Guard Toggle Button
                    IconButton(
                        onClick = { SafetyLayer.setAutomationAllowed(!isSafetyAllowed) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text(if (isSafetyAllowed) "🛡️" else "🛑", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Vault Settings Gear Button
                    IconButton(
                        onClick = onOpenVaultSettings,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("⚙️", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Full-Screen Chat Terminal Feed
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

        // 3. Quick Action Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val chips = listOf("Query Docs", "Launch App", "System Status", "Offline GGUF")
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

        // 4. Command Prompt & Microphone Dock
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
                                val response = if (isOnline) {
                                    LlmClient.generateContent(context, text)
                                } else {
                                    LocalLlmEngine.generate(context, text)
                                }
                                chatMessages.add(ChatMessage("JARVISH", response, false, modelName = activeModelName))
                                lastResponseText = response
                                ttsManager.speak(response)
                            } catch (e: Exception) {
                                chatMessages.add(ChatMessage("JARVISH", "[ERROR] ${e.message}", false, modelName = activeModelName))
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
