package com.jarvis.safety

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A Jetpack Compose dialog that presents permission rationale to the user
 * prior to triggering the Android system request prompt.
 */
@Composable
fun PermissionRationaleDialog(
    showDialog: Boolean,
    permissionType: PermissionType,
    isPermanentlyDenied: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onGoToSettings: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    if (showDialog) {
        Dialog(
            onDismissRequest = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDismiss()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Modern styled icon header
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (permissionType) {
                                PermissionType.MICROPHONE -> "🎙️"
                                PermissionType.NOTIFICATIONS -> "🔔"
                                PermissionType.USAGE_STATS -> "📊"
                            },
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }

                    Text(
                        text = when (permissionType) {
                            PermissionType.MICROPHONE -> "Microphone Access Required"
                            PermissionType.NOTIFICATIONS -> "Notification Settings Required"
                            PermissionType.USAGE_STATS -> "Usage Details Access Required"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = when (permissionType) {
                            PermissionType.MICROPHONE -> "This assistant requires microphone access to capture voice commands and perform tasks. Audio is strictly processed on-device for security."
                            PermissionType.NOTIFICATIONS -> "Allows the assistant to keep you informed of task completions and request critical authorizations when running automation."
                            PermissionType.USAGE_STATS -> "Required to identify active apps. This enables contextual optimizations and triggers actions inside app screens."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Not Now")
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isPermanentlyDenied) {
                                    onGoToSettings()
                                } else {
                                    onConfirm()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPermanentlyDenied) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = if (isPermanentlyDenied) "Open Settings" else "Grant Access"
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable deep-linking button for graceful degradation when permission is permanently denied.
 */
@Composable
fun SettingsDeepLinkButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("⚙️", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Configure in App Settings",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
