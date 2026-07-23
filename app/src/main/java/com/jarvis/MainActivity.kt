package com.jarvis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.config.AppConfig
import com.jarvis.context.ContextEngine
import com.jarvis.context.DeviceContext
import com.jarvis.safety.*
import com.jarvis.ui.screens.*
import com.jarvis.ui.theme.*
import com.jarvis.voice.*

class MainActivity : ComponentActivity() {

    private lateinit var sttManager: SpeechToTextManager
    private lateinit var ttsManager: TextToSpeechManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bind permission manager
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

    // Navigation and screen routing
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Home, 1: Control, 2: Tools, 3: Files, 4: Profile
    var currentSubscreen by remember { mutableStateOf<String?>(null) } // "OptimizerScreen", "PerformanceScreen", "SecurityScreen", etc.

    // System Monitor flows
    val deviceContext by ContextEngine.deviceContext.collectAsState()
    val isAutomationAllowed by SafetyLayer.isAutomationAllowed.collectAsState()

    // Permission flows
    val micPermState by PermissionManager.microphoneState.collectAsState()
    val notifPermState by PermissionManager.notificationsState.collectAsState()
    val usagePermState by PermissionManager.usageStatsState.collectAsState()

    // Speech states
    val speechState by sttManager.speechState.collectAsState()

    // Rationale dialog states
    var showRationaleDialog by remember { mutableStateOf(false) }
    var activePermissionType by remember { mutableStateOf(PermissionType.MICROPHONE) }
    var isPermDeniedPermanent by remember { mutableStateOf(false) }

    fun triggerPermissionRequest(type: PermissionType, state: PermissionState) {
        if (state is PermissionState.PermanentlyDenied) {
            activePermissionType = type
            isPermDeniedPermanent = true
            showRationaleDialog = true
        } else if (state is PermissionState.Denied) {
            activePermissionType = type
            isPermDeniedPermanent = false
            showRationaleDialog = true
        } else {
            onRequestPermission(type)
        }
    }

    Scaffold(
        topBar = {
            // Floating Glass Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.surfaceGlass)
                    .border(1.dp, colors.surfaceBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "JARVISH",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 3.sp,
                    color = colors.onSurface,
                    modifier = Modifier.align(Alignment.Center)
                )

                IconButton(
                    onClick = { onDarkToggle(!isDark) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(32.dp)
                ) {
                    Text(if (isDark) "☀️" else "🌙", fontSize = 16.sp)
                }
            }
        },
        bottomBar = {
            if (currentSubscreen == null) {
                // Floating Glass Dock
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .navigationBarsPadding() // Ensures it sits above system nav
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(colors.surfaceGlass)
                            .border(1.dp, colors.surfaceBorder, RoundedCornerShape(32.dp))
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf(
                            TabItem("Home", "🏠", 0),
                            TabItem("Control", "🎛️", 1),
                            TabItem("Tools", "🛠️", 2),
                            TabItem("Files", "📂", 3),
                            TabItem("Profile", "👤", 4)
                        )

                        tabs.forEach { tab ->
                            val isSelected = selectedTab == tab.index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) colors.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent)
                                    .clickable { selectedTab = tab.index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = tab.icon,
                                        fontSize = if (isSelected) 22.sp else 18.sp,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                    // Animated indicator dot
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) colors.accent else Color.Transparent)
                                    )
                                }
                            }
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
            // Priority Subscreen Routing
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
                // Tab Routing
                when (selectedTab) {
                    0 -> HomeScreen(
                        deviceContext = deviceContext,
                        speechState = speechState,
                        sttManager = sttManager,
                        ttsManager = ttsManager,
                        onQuickActionClick = { action ->
                            when (action) {
                                "Junk Cleaner" -> currentSubscreen = "OptimizerScreen"
                                "Memory Boost" -> currentSubscreen = "PerformanceScreen"
                                "Battery Saver" -> currentSubscreen = "BatterySaverScreen"
                                "Full Scan" -> currentSubscreen = "SecurityScreen"
                            }
                        }
                    )
                    1 -> ControlScreen()
                    2 -> ToolsScreen(onToolSelect = { dest -> currentSubscreen = dest })
                    3 -> FilesScreen()
                    4 -> ProfileScreen(
                        currentTheme = currentTheme,
                        isDark = isDark,
                        onThemeChange = onThemeChange,
                        onDarkToggle = onDarkToggle,
                        onKeyConfigChanged = {
                            // Reset key states or reload configuration
                        }
                    )
                }
            }
        }
    }

    // Modal Rationale Dialog for permissions
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

data class TabItem(val label: String, val icon: String, val index: Int)
