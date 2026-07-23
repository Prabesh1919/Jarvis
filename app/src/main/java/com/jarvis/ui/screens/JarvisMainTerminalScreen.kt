package com.jarvis.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jarvis.brain.LlmClient
import com.jarvis.brain.LocalLlmEngine
import com.jarvis.config.AppConfig
import com.jarvis.context.DeviceContext
import com.jarvis.safety.PermissionManager
import com.jarvis.safety.PermissionType
import com.jarvis.safety.SafetyLayer
import com.jarvis.ui.components.ArcReactorWidget
import com.jarvis.ui.theme.JarvisColors
import com.jarvis.ui.theme.LocalJarvisColors
import com.jarvis.voice.GeminiVoiceEngine
import com.jarvis.voice.SpeechState
import com.jarvis.voice.SpeechToTextManager
import com.jarvis.voice.TextToSpeechManager
import com.jarvis.voice.TtsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Speaks response using Voice AI Models exclusively (0% Android TTS):
 * 1. ONLINE: Gemini Native Audio Engine (Charon Voice Model - same as desktop JARVIS).
 * 2. OFFLINE: Local On-Device Voice AI Synthesizer (zero downloads required).
 */
suspend fun speakWithVoiceModel(
    context: android.content.Context,
    text: String,
    ttsManager: TextToSpeechManager? = null
) {
    val usedGemini = GeminiVoiceEngine.speak(context, text)
    if (!usedGemini) {
        if (ttsManager != null && ttsManager.ttsState.value !is TtsState.Error) {
            ttsManager.speak(text)
        } else {
            com.jarvis.voice.LocalVoiceModel.speak(context, text)
        }
    }
}

fun isNetworkConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val net = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(net) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
           caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

/**
 * Smart LLM prompt executor:
 * 1. Checks if network is available. If OFFLINE -> Routes IMMEDIATELY (0ms delay) to Local Llama 3.2 GGUF engine!
 * 2. If ONLINE -> Tries configured Cloud LLM (Gemini 2.5 Flash / OpenRouter).
 * 3. On any cloud network drop -> Failover seamlessly to Local Llama 3.2 GGUF engine.
 */
suspend fun executeSmartLlmPrompt(context: Context, text: String): Pair<String, String> {
    // If device is offline, DO NOT wait for 15s cloud timeout — route IMMEDIATELY to Local GGUF!
    if (!isNetworkConnected(context)) {
        LocalLlmEngine.autoProvisionDefaultModel(context)
        try {
            val localResponse = LocalLlmEngine.generate(context, text)
            return Pair(localResponse, "LOCAL LLAMA 3.2 1B (OFFLINE GGUF)")
        } catch (ggufErr: Exception) {
            return Pair("[OFFLINE ENGINE] ${ggufErr.message}", "LOCAL LLAMA 3.2 1B (OFFLINE GGUF)")
        }
    }

    val cloudModel = AppConfig.getLlmModel(context)
    try {
        val cloudResponse = LlmClient.generateContent(context, text)
        return Pair(cloudResponse, cloudModel)
    } catch (cloudErr: Exception) {
        LocalLlmEngine.autoProvisionDefaultModel(context)
        try {
            val localResponse = LocalLlmEngine.generate(context, text)
            return Pair(localResponse, "LOCAL LLAMA 3.2 1B (OFFLINE GGUF)")
        } catch (ggufErr: Exception) {
            return Pair("[OFFLINE ERROR] ${ggufErr.message}", "LOCAL LLAMA 3.2 1B (OFFLINE GGUF)")
        }
    }
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

    var isOnline by remember { mutableStateOf(isNetworkConnected(context)) }
    var activeModelDisplay by remember {
        mutableStateOf(
            if (isOnline) AppConfig.getLlmModel(context) else "LOCAL LLAMA 3.2 1B (OFFLINE GGUF)"
        )
    }

    // Event-Driven Zero-Overhead Network & Offline Model Monitor
    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                isOnline = true
                activeModelDisplay = AppConfig.getLlmModel(context)
            }

            override fun onLost(network: android.net.Network) {
                isOnline = false
                activeModelDisplay = "LOCAL LLAMA 3.2 1B (OFFLINE GGUF)"
            }
        }

        try {
            cm?.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {}

        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            LocalLlmEngine.autoProvisionDefaultModel(context)
        }

        onDispose {
            try {
                cm?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }

    // Chat message history
    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage("JARVISH", "[SYSTEM_READY] Gateway active. Type or speak below.", false, modelName = activeModelDisplay)
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

    // Handle SpeechState events (including Errors & Results)
    LaunchedEffect(speechState) {
        when (speechState) {
            is SpeechState.Results -> {
                val userText = (speechState as SpeechState.Results).text
                if (userText.isNotBlank() && !isProcessing) {
                    isProcessing = true
                    lastResponseText = null
                    chatMessages.add(ChatMessage("You", userText, true))
                    try {
                        val (response, usedModel) = executeSmartLlmPrompt(context, userText)
                        activeModelDisplay = usedModel
                        isOnline = !usedModel.contains("OFFLINE")
                        chatMessages.add(ChatMessage("JARVISH", response, false, modelName = usedModel))
                        lastResponseText = response
                        speakWithVoiceModel(context, response, ttsManager)
                    } catch (e: Exception) {
                        chatMessages.add(ChatMessage("JARVISH", "[ERROR] ${e.message}", false, modelName = activeModelDisplay))
                    } finally {
                        isProcessing = false
                        sttManager.resetState()
                    }
                }
            }
            is SpeechState.Error -> {
                val errorText = (speechState as SpeechState.Error).errorMsg
                chatMessages.add(ChatMessage("JARVISH", "[MIC NOTICE] $errorText", false, modelName = activeModelDisplay))
                sttManager.resetState()
            }
            is SpeechState.Listening -> {
                GeminiVoiceEngine.stopPlayback()
                if (ttsState is TtsState.Speaking) {
                    ttsManager.stopSpeaking()
                }
            }
            else -> {}
        }
    }

    val infiniteAnim = rememberInfiniteTransition(label = "StatusBeacon")
    val beaconAlpha by infiniteAnim.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconAlpha"
    )

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
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(colors.cardGradStart.copy(alpha = 0.85f), colors.cardGradEnd.copy(alpha = 0.9f))
                    )
                )
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
                            .background(
                                (if (isOnline) colors.accent else Color.Yellow).copy(alpha = beaconAlpha)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isOnline) "● ONLINE" else "● OFFLINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isOnline) colors.accent else Color.Yellow
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "[MODEL: ${activeModelDisplay.uppercase()}]",
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

        // 2. Iron Man Mark-XXXIX Arc Reactor Core HUD Widget (100% Desktop Identical)
        val isListening = speechState is SpeechState.Listening
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            ArcReactorWidget(
                modifier = Modifier.size(120.dp),
                isListening = isListening,
                isSpeaking = manualLoading || isProcessing
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. Full-Screen Chat Terminal Feed
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
                items(chatMessages, key = { msg -> msg.timestamp }) { msg ->
                    ChatBubble(msg, colors)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 4. Quick Action Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val chips = listOf("Query Docs", "Launch App", "System Status")
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

        // 5. Command Prompt & Microphone Dock
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
                                val (response, usedModel) = executeSmartLlmPrompt(context, text)
                                activeModelDisplay = usedModel
                                isOnline = !usedModel.contains("OFFLINE")
                                chatMessages.add(ChatMessage("JARVISH", response, false, modelName = usedModel))
                                lastResponseText = response
                                speakWithVoiceModel(context, response, ttsManager)
                            } catch (e: Exception) {
                                chatMessages.add(ChatMessage("JARVISH", "[ERROR] ${e.message}", false, modelName = activeModelDisplay))
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

                val micBg = if (isListening) colors.accent else colors.surfaceVariant

                IconButton(
                    onClick = {
                        // Check runtime microphone permission automatically on click
                        val hasMicPerm = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasMicPerm) {
                            PermissionManager.requestPermission(PermissionType.MICROPHONE)
                            return@IconButton
                        }

                        coroutineScope.launch {
                            if (isListening) {
                                sttManager.stopListening()
                            } else {
                                // Interrupt any active voice playback immediately
                                GeminiVoiceEngine.stopPlayback()
                                com.jarvis.voice.LocalVoiceModel.stopPlayback()
                                if (ttsState is TtsState.Speaking) {
                                    ttsManager.stopSpeaking()
                                }
                                sttManager.startListening()
                            }
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(micBg)
                        .border(1.5.dp, if (isListening) colors.accent else colors.surfaceBorder, CircleShape)
                ) {
                    Text(
                        text = if (isListening) "🎙️" else "🎤",
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}
