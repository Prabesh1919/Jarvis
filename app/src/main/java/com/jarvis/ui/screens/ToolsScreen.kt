package com.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ui.theme.LocalJarvisColors

@Composable
fun ToolsScreen(
    onToolSelect: (String) -> Unit
) {
    val colors = LocalJarvisColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "[SYSTEM TOOL REGISTRY]",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = colors.accent,
            letterSpacing = 1.sp
        )

        // 1. System Tools Category
        ToolCategorySection(
            title = "> PERFORMANCE ENGINE",
            tools = listOf(
                ToolItem("Junk Cleaner", "🛠️", "OptimizerScreen"),
                ToolItem("Memory Boost", "🚀", "PerformanceScreen"),
                ToolItem("CPU Cooler", "❄️", "PerformanceScreen"),
                ToolItem("Battery Saver", "🔋", "BatterySaverScreen")
            ),
            onToolSelect = onToolSelect
        )

        // 2. Security Tools Category
        ToolCategorySection(
            title = "> SECURITY & PRIVACY VAULT",
            tools = listOf(
                ToolItem("Virus Scan", "🛡️", "SecurityScreen"),
                ToolItem("App Lock", "🔒", "AppLockScreen"),
                ToolItem("Wi-Fi Security", "📶", "SecurityScreen"),
                ToolItem("Privacy Guard", "👤", "SecurityScreen")
            ),
            onToolSelect = onToolSelect
        )

        // 3. Utility Tools Category
        ToolCategorySection(
            title = "> INTELLIGENT AGENT UTILITIES",
            tools = listOf(
                ToolItem("Game Booster", "🎮", "GameBoosterScreen"),
                ToolItem("Local RAG Search", "📁", "RagSearchScreen"),
                ToolItem("Automation Agent", "🤖", "AutomationScreen")
            ),
            onToolSelect = onToolSelect
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

data class ToolItem(
    val name: String,
    val icon: String,
    val destination: String
)

@Composable
fun ToolCategorySection(
    title: String,
    tools: List<ToolItem>,
    onToolSelect: (String) -> Unit
) {
    val colors = LocalJarvisColors.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = colors.onSurfaceVariant
        )

        val rows = tools.chunked(2)
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (tool in row) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceGlass)
                            .border(1.dp, colors.surfaceBorder, RoundedCornerShape(10.dp))
                            .clickable { onToolSelect(tool.destination) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tool.icon, fontSize = 20.sp)
                            Column {
                                Text(
                                    tool.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.onSurface
                                )
                                Text(
                                    "[ EXECUTE ]",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.accent
                                )
                            }
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
