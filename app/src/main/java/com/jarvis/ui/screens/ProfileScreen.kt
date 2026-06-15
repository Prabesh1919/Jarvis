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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    // API Key states
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
            text = "USER PROFILE & CONFIGURATION",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = colors.accent,
            letterSpacing = 1.5.sp
        )

        // 1. User Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(colors.accent.copy(alpha = 0.15f))
                        .border(2.dp, colors.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🤖", fontSize = 28.sp)
                }

                Column {
                    Text(
                        "JARVISH USER",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "★ Premium System Operator",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                }
            }
        }

        // 2. Styling Themes Block
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "INTERFACE CUSTOMIZATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                // Dark/Light switch row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark Theme Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                    Switch(
                        checked = isDark,
                        onCheckedChange = onDarkToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.accent,
                            uncheckedThumbColor = colors.onSurfaceVariant,
                            uncheckedTrackColor = colors.surfaceVariant
                        )
                    )
                }

                Divider(color = colors.onSurfaceVariant.copy(alpha = 0.08f))

                // Theme color selection
                Text("Theme Color Accent", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeType.values().forEach { themeOption ->
                        val isSelected = currentTheme == themeOption
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.surfaceVariant else Color.Transparent)
                                .clickable { onThemeChange(themeOption) }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = themeOption.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = colors.onSurface
                            )
                            if (isSelected) {
                                Text("✓", fontSize = 14.sp, fontWeight = FontWeight.Black, color = colors.accent)
                            }
                        }
                    }
                }
            }
        }

        // 3. Vault & Gemini Secret Config
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "LLM & BRAIN SECRETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                if (isKeySaved) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🔑 Gemini API Key configured",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Green
                        )
                        Button(
                            onClick = {
                                AppConfig.setGeminiApiKey(context, "")
                                isKeySaved = false
                                onKeyConfigChanged()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant)
                        ) {
                            Text("Reset", color = colors.onSurface)
                        }
                    }
                } else {
                    Text(
                        "Missing API Key. Provide it to enable AI processing:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )

                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        placeholder = { Text("AIzaSy...", color = colors.onSurfaceVariant.copy(alpha = 0.5f)) },
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
                            if (apiKeyText.isNotBlank()) {
                                AppConfig.setGeminiApiKey(context, apiKeyText)
                                isKeySaved = true
                                apiKeyText = ""
                                onKeyConfigChanged()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                    ) {
                        Text("Save key in Vault", color = Color.White)
                    }
                }
            }
        }

        // 4. Options
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileOptionRow("Account Settings")
                Divider(color = colors.onSurfaceVariant.copy(alpha = 0.08f))
                ProfileOptionRow("Privacy Policy")
                Divider(color = colors.onSurfaceVariant.copy(alpha = 0.08f))
                ProfileOptionRow("Help & Support")
                Divider(color = colors.onSurfaceVariant.copy(alpha = 0.08f))
                ProfileOptionRow("About Jarvish")
            }
        }
    }
}

@Composable
fun ProfileOptionRow(title: String) {
    val colors = LocalJarvisColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* action */ }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
        Text("➔", fontSize = 14.sp, color = colors.onSurfaceVariant)
    }
}
