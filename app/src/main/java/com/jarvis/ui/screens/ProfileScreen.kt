package com.jarvis.ui.screens

import android.content.Context
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
import com.jarvis.config.AppConfig
import com.jarvis.ui.theme.AppThemeType
import com.jarvis.ui.theme.LocalJarvisColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentTheme: AppThemeType,
    isDark: Boolean,
    onThemeChange: (AppThemeType) -> Unit,
    onDarkToggle: (Boolean) -> Unit,
    onKeyConfigChanged: () -> Unit
) {
    val colors = LocalJarvisColors.current
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var apiKeyText by remember { mutableStateOf("") }
    var isKeySaved by remember { mutableStateOf(AppConfig.isApiKeyConfigured(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "[SYSTEM CONFIGURATION & KEYS]",
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
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
                Text(
                    "> THEME SELECTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark Theme Mode", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = colors.onSurface)
                    Switch(
                        checked = isDark,
                        onCheckedChange = onDarkToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = colors.accent,
                            uncheckedThumbColor = colors.onSurfaceVariant,
                            uncheckedTrackColor = colors.surfaceVariant
                        )
                    )
                }

                HorizontalDivider(color = colors.surfaceBorder.copy(alpha = 0.3f))

                Text("Theme Color Accent", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = colors.onSurface)

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
                    "> SECURE VAULT KEYS",
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
                            "🔑 Gemini Key Configured",
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
                        "Missing API Key. Provide it to enable Gemini processing:",
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
    }
}
