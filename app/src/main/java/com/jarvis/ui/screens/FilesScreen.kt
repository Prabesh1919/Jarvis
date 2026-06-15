package com.jarvis.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ui.theme.LocalJarvisColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen() {
    val colors = LocalJarvisColors.current
    val scrollState = rememberScrollState()

    var searchQuery by remember { mutableStateOf("") }

    val fileCategories = listOf(
        FileCategory("Documents", "📄", "128 Files", colors.accent),
        FileCategory("Downloads", "📥", "64 Files", colors.secondaryGlow),
        FileCategory("Images", "🖼️", "1,245 Files", colors.secondaryGlow),
        FileCategory("Videos", "🎥", "320 Files", colors.accent),
        FileCategory("Music", "🎵", "213 Files", colors.accent),
        FileCategory("Archives", "📦", "79 Files", colors.secondaryGlow),
        FileCategory("Apps", "📱", "48 Files", colors.secondaryGlow),
        FileCategory("Others", "📂", "58 Files", colors.accent)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "FILE MANAGER",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = colors.accent,
            letterSpacing = 1.5.sp
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search files...", color = colors.onSurfaceVariant.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.onSurface,
                unfocusedTextColor = colors.onSurface,
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.onSurfaceVariant.copy(alpha = 0.3f)
            ),
            singleLine = true
        )

        // Storage Usage Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "INTERNAL STORAGE STATUS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("128 GB / 256 GB Used", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                    Text("50%", fontSize = 16.sp, fontWeight = FontWeight.Black, color = colors.accent)
                }
                LinearProgressIndicator(
                    progress = { 0.5f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = colors.accent,
                    trackColor = colors.accent.copy(alpha = 0.15f)
                )
            }
        }

        // File Categories Grid
        Text(
            "FILE CATEGORIES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onSurfaceVariant,
            letterSpacing = 1.sp
        )

        val rows = fileCategories.chunked(2)
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (cat in row) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .clickable { /* open category action */ },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat.emoji, fontSize = 22.sp)
                            Column {
                                Text(
                                    cat.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface
                                )
                                Text(
                                    cat.count,
                                    fontSize = 9.sp,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

data class FileCategory(
    val name: String,
    val emoji: String,
    val count: String,
    val tint: Color
)
