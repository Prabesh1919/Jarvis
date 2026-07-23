package com.jarvis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.jarvis.context.ContextEngine
import com.jarvis.safety.*
import com.jarvis.ui.screens.*
import com.jarvis.ui.theme.*
import com.jarvis.voice.*

class MainActivity : ComponentActivity() {

    private lateinit var sttManager: SpeechToTextManager
    private lateinit var ttsManager: TextToSpeechManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PermissionManager.register(this)

        sttManager = SpeechToTextManager(this)
        ttsManager = TextToSpeechManager(this)

        setContent {
            var currentTheme by remember { mutableStateOf(AppThemeType.HACKER_CYBER) }
            var isDarkTheme by remember { mutableStateOf(true) }

            val jarvisColors = getColorsForTheme(currentTheme, isDarkTheme)

            CompositionLocalProvider(LocalJarvisColors provides jarvisColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = jarvisColors.background
                ) {
                    JarvisAppContent(
                        sttManager = sttManager,
                        ttsManager = ttsManager,
                        currentTheme = currentTheme,
                        isDark = isDarkTheme,
                        onThemeChange = { currentTheme = it },
                        onDarkToggle = { isDarkTheme = it },
                        onOpenSettings = { PermissionManager.openAppSettings() },
                        onRequestPermission = { PermissionManager.requestPermission(it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisAppContent(
    sttManager: SpeechToTextManager,
    ttsManager: TextToSpeechManager,
    currentTheme: AppThemeType,
    isDark: Boolean,
    onThemeChange: (AppThemeType) -> Unit,
    onDarkToggle: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onRequestPermission: (PermissionType) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalJarvisColors.current

    // Navigation state: 0 -> Terminal, 1 -> Vault & Settings
    var selectedScreen by remember { mutableIntStateOf(0) }
    var currentSubscreen by remember { mutableStateOf<String?>(null) }

    val deviceContext by ContextEngine.deviceContext.collectAsState()
    val isAutomationAllowed by SafetyLayer.isAutomationAllowed.collectAsState()
    val speechState by sttManager.speechState.collectAsState()

    var showRationaleDialog by remember { mutableStateOf(false) }
    var activePermissionType by remember { mutableStateOf(PermissionType.MICROPHONE) }
    var isPermDeniedPermanent by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // Floating 2-Screen Segmented Switcher Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceGlass)
                    .border(1.dp, colors.surfaceBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "JARVISH AI",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        color = colors.accent
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Terminal Tab Button
                        Button(
                            onClick = { selectedScreen = 0; currentSubscreen = null },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedScreen == 0) colors.accent else Color.Transparent
                            )
                        ) {
                            Text(
                                "TERMINAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (selectedScreen == 0) Color.Black else colors.onSurface
                            )
                        }

                        // Vault & Settings Tab Button
                        Button(
                            onClick = { selectedScreen = 1; currentSubscreen = null },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedScreen == 1) colors.accent else Color.Transparent
                            )
                        ) {
                            Text(
                                "VAULT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (selectedScreen == 1) Color.Black else colors.onSurface
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colors.background)
        ) {
            if (currentSubscreen != null) {
                when (currentSubscreen) {
                    "OptimizerScreen" -> OptimizerScreen(onBack = { currentSubscreen = null })
                    "PerformanceScreen" -> PerformanceScreen(onBack = { currentSubscreen = null })
                    "SecurityScreen" -> SecurityScreen(onBack = { currentSubscreen = null })
                    "AppLockScreen" -> AppLockScreen(onBack = { currentSubscreen = null })
                    "BatterySaverScreen" -> BatterySaverScreen(onBack = { currentSubscreen = null })
                    "GameBoosterScreen" -> GameBoosterScreen(onBack = { currentSubscreen = null })
                    "RagSearchScreen" -> RagSearchScreen(onBack = { currentSubscreen = null })
                    "AutomationScreen" -> AutomationScreen(
                        isAutomationAllowed = isAutomationAllowed,
                        onBack = { currentSubscreen = null }
                    )
                }
            } else {
                when (selectedScreen) {
                    0 -> JarvisMainTerminalScreen(
                        deviceContext = deviceContext,
                        speechState = speechState,
                        sttManager = sttManager,
                        ttsManager = ttsManager,
                        onOpenVaultSettings = { selectedScreen = 1 },
                        onQuickActionClick = { action ->
                            when (action) {
                                "Query Docs" -> currentSubscreen = "RagSearchScreen"
                                "Launch App" -> currentSubscreen = "AutomationScreen"
                                "System Status" -> currentSubscreen = "PerformanceScreen"
                                "Offline GGUF" -> selectedScreen = 1
                            }
                        }
                    )
                    1 -> JarvisVaultSettingsScreen(
                        currentTheme = currentTheme,
                        isDark = isDark,
                        onThemeChange = onThemeChange,
                        onDarkToggle = onDarkToggle,
                        onKeyConfigChanged = {},
                        onBackToTerminal = { selectedScreen = 0 }
                    )
                }
            }
        }
    }

    if (showRationaleDialog) {
        PermissionRationaleDialog(
            showDialog = showRationaleDialog,
            permissionType = activePermissionType,
            isPermanentlyDenied = isPermDeniedPermanent,
            onDismiss = { showRationaleDialog = false },
            onConfirm = {
                showRationaleDialog = false
                onRequestPermission(activePermissionType)
            },
            onGoToSettings = {
                showRationaleDialog = false
                onOpenSettings()
            }
        )
    }
}
