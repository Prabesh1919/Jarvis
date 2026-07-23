package com.jarvis.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.brain.LocalLlmEngine
import com.jarvis.bridge.DesktopBridgeService
import com.jarvis.config.AppConfig
import com.jarvis.ui.theme.AppThemeType
import com.jarvis.ui.theme.LocalJarvisColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisVaultSettingsScreen(
    currentTheme: AppThemeType,
    isDark: Boolean,
    onThemeChange: (AppThemeType) -> Unit,
    onDarkToggle: (Boolean) -> Unit,
    onKeyConfigChanged: () -> Unit,
    onBackToTerminal: () -> Unit
) {
    val colors = LocalJarvisColors.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var apiKeyText by remember { mutableStateOf("") }
    var desktopIpText by remember { mutableStateOf("192.168.1.100") }
    var isKeySaved by remember { mutableStateOf(AppConfig.isApiKeyConfigured(context)) }
    var isGgufAvailable by remember { mutableStateOf(LocalLlmEngine.isOfflineModelAvailable(context)) }
    var downloadStatusText by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[VAULT & SETTINGS]",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = colors.accent,
                letterSpacing = 1.sp
            )

            Button(
                onClick = onBackToTerminal,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant)
            ) {
                Text("< TERMINAL", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = colors.onSurface)
            }
        }

        // 1. Root User Privilege Card
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
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(colors.accent.copy(alpha = 0.15f))
                        .border(1.dp, colors.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🤖", fontSize = 24.sp)
                }

                Column {
                    Text(
                        "JARVISH OPERATOR",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "★ SYSTEM PRIVILEGE: ROOT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.accent
                    )
                }
            }
        }

        // 2. Keystore AES-256 API Key Vault
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "> AES-256-GCM KEYSTORE VAULT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.onSurfaceVariant
                )

                if (isKeySaved) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🔑 Gemini API Key Encrypted",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.accent
                        )
                        Button(
                            onClick = {
                                AppConfig.setGeminiApiKey(context, "")
                                isKeySaved = false
                                onKeyConfigChanged()
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant)
                        ) {
                            Text("Reset", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = colors.onSurface)
                        }
                    }
                } else {
                    Text(
                        "Missing API Key. Provide it to enable Gemini Cloud LLM:",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.error
                    )

                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        placeholder = { Text("AIzaSy...", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = colors.onSurfaceVariant.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.onSurface,
                            unfocusedTextColor = colors.onSurface,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.surfaceBorder
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (apiKeyText.isNotBlank()) {
                                AppConfig.setGeminiApiKey(context, apiKeyText)
                                isKeySaved = true
                                apiKeyText = ""
                                onKeyConfigChanged()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                    ) {
                        Text("Save key in Vault", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                    }
                }
            }
        }

        // 3. Offline GGUF Local Model Engine Manager
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "> OFFLINE LOCAL LLM ENGINE (GGUF)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (isGgufAvailable) "GGUF Engine: ACTIVE" else "GGUF Engine: NOT PROVISIONED",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isGgufAvailable) colors.accent else Color.Yellow
                    )
                    Text(
                        "Auto-switches offline prompts to local Llama 3.2 GGUF",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.onSurfaceVariant
                    )
                }

                if (downloadStatusText != null) {
                    Text(
                        downloadStatusText!!,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.accent
                    )
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isDownloading = true
                            downloadStatusText = "Provisioning local GGUF model container..."
                            LocalLlmEngine.autoProvisionDefaultModel(context)
                            val ok = LocalLlmEngine.initialize(context)
                            isGgufAvailable = ok
                            isDownloading = false
                            downloadStatusText = if (ok) "✅ Offline GGUF Model Ready (Llama-3.2-1B-Instruct)" else "❌ GGUF setup failed."
                        }
                    },
                    enabled = !isDownloading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text(
                        if (isDownloading) "PROVISIONING..." else "⚡ INITIALIZE OFFLINE GGUF MODEL",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black
                    )
                }
            }
        }

        // 4. Desktop Bridge Sync Config
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "> DESKTOP BRIDGE SYNC SETUP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.onSurfaceVariant
                )

                OutlinedTextField(
                    value = desktopIpText,
                    onValueChange = { desktopIpText = it },
                    placeholder = { Text("192.168.1.100", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.onSurface,
                        unfocusedTextColor = colors.onSurface,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.surfaceBorder
                    ),
                    singleLine = true
                )

                Button(
                    onClick = { DesktopBridgeService.connect(desktopIpText) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryGlow)
                ) {
                    Text("Connect to Desktop Bridge", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                }
            }
        }

        // 5. Theme Palette Customization
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "> THEME PALETTE CUSTOMIZATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppThemeType.values().forEach { themeOption ->
                        val isSelected = currentTheme == themeOption
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) colors.surfaceVariant else Color.Transparent)
                                .clickable { onThemeChange(themeOption) }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = themeOption.displayName,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = colors.onSurface
                            )
                            if (isSelected) {
                                Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                            }
                        }
                    }
                }
            }
        }
    }
}
