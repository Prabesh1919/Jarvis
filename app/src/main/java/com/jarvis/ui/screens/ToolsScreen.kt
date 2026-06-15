package com.jarvis.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "SYSTEM UTILITY TOOLS",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = colors.accent,
            letterSpacing = 1.5.sp
        )

        // 1. System Tools Category
        ToolCategorySection(
            title = "SYSTEM PERFORMANCE",
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
            title = "SECURITY & PRIVACY",
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
            title = "INTELLIGENT UTILITIES",
            tools = listOf(
                ToolItem("Game Booster", "🎮", "GameBoosterScreen"),
                ToolItem("On-Device RAG", "📁", "RagSearchScreen"),
                ToolItem("Automation Tasks", "🤖", "AutomationScreen")
            ),
            onToolSelect = onToolSelect
        )
        
        Spacer(modifier = Modifier.height(20.dp))
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

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onSurfaceVariant,
            letterSpacing = 1.sp
        )

        // Display as 2x2 cards manually to avoid inner scroll conflict in verticalScroll
        val rows = tools.chunked(2)
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (tool in row) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(76.dp)
                            .clickable { onToolSelect(tool.destination) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(tool.icon, fontSize = 24.sp)
                            Column {
                                Text(
                                    tool.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface
                                )
                                Text(
                                    "Tap to optimize",
                                    fontSize = 9.sp,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                // Placeholder to complete Row if odd count
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
