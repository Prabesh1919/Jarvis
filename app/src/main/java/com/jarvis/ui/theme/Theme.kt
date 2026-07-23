package com.jarvis.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

enum class AppThemeType(val displayName: String) {
    MARK_XXXIX_CYAN("Mark XXXIX Arc Cyan"),
    HACKER_CYBER("Hacker Cyber"),
    RED_MATRIX("Red Matrix"),
    BLUE_OCEAN("Blue Ocean"),
    GREEN_TECH("Green Tech"),
    PURPLE_VOID("Purple Void"),
    ORANGE_HEAT("Orange Heat")
}

data class JarvisColors(
    val accent: Color,
    val secondaryGlow: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val isDark: Boolean,
    val cardGradStart: Color,
    val cardGradEnd: Color,
    val surfaceGlass: Color,
    val surfaceBorder: Color
)

val LocalJarvisColors = staticCompositionLocalOf {
    getColorsForTheme(AppThemeType.MARK_XXXIX_CYAN, isDark = true)
}

fun getColorsForTheme(theme: AppThemeType, isDark: Boolean): JarvisColors {
    return if (isDark) {
        when (theme) {
            AppThemeType.MARK_XXXIX_CYAN -> JarvisColors(
                accent = Color(0xFF00D4FF),
                secondaryGlow = Color(0xFFFF6B00),
                background = Color(0xFF00060A),
                surface = Color(0xFF010D14),
                surfaceVariant = Color(0xFF010F18),
                onBackground = Color(0xFF8FFCFF),
                onSurface = Color(0xFF8FFCFF),
                onSurfaceVariant = Color(0xFF3A8A9A),
                isDark = true,
                cardGradStart = Color(0xFF011520),
                cardGradEnd = Color(0xFF00060A),
                surfaceGlass = Color(0xFF010D14).copy(alpha = 0.65f),
                surfaceBorder = Color(0xFF0D3347)
            )
            AppThemeType.HACKER_CYBER -> JarvisColors(
                accent = Color(0xFF00FF41),
                secondaryGlow = Color(0xFF008000),
                background = Color(0xFF020904),
                surface = Color(0xFF06140B),
                surfaceVariant = Color(0xFF0A2012),
                onBackground = Color(0xFF00FF41),
                onSurface = Color(0xFF00FF41),
                onSurfaceVariant = Color(0xFF00B32D),
                isDark = true,
                cardGradStart = Color(0xFF03190C),
                cardGradEnd = Color(0xFF010A05),
                surfaceGlass = Color(0xFF06140B).copy(alpha = 0.35f),
                surfaceBorder = Color(0xFF00FF41).copy(alpha = 0.45f)
            )
            AppThemeType.RED_MATRIX -> JarvisColors(
                accent = Color(0xFFFF1E27),
                secondaryGlow = Color(0xFFFF5252),
                background = Color(0xFF080808),
                surface = Color(0xFF141414),
                surfaceVariant = Color(0xFF221112),
                onBackground = Color(0xFFECEFF1),
                onSurface = Color(0xFFFFFFFF),
                onSurfaceVariant = Color(0xFFB0BEC5),
                isDark = true,
                cardGradStart = Color(0xFF1C0A0B),
                cardGradEnd = Color(0xFF0C0203),
                surfaceGlass = Color(0xFF141414).copy(alpha = 0.2f),
                surfaceBorder = Color(0xFFFFFFFF).copy(alpha = 0.2f)
            )
            AppThemeType.BLUE_OCEAN -> JarvisColors(
                accent = Color(0xFF00E5FF),
                secondaryGlow = Color(0xFF2979FF),
                background = Color(0xFF050B14),
                surface = Color(0xFF0D1726),
                surfaceVariant = Color(0xFF112239),
                onBackground = Color(0xFFECEFF1),
                onSurface = Color(0xFFFFFFFF),
                onSurfaceVariant = Color(0xFF90A4AE),
                isDark = true,
                cardGradStart = Color(0xFF0A192F),
                cardGradEnd = Color(0xFF050E1A),
                surfaceGlass = Color(0xFF0D1726).copy(alpha = 0.25f),
                surfaceBorder = Color(0xFF00E5FF).copy(alpha = 0.2f)
            )
            AppThemeType.GREEN_TECH -> JarvisColors(
                accent = Color(0xFF00E676),
                secondaryGlow = Color(0xFFB9F6CA),
                background = Color(0xFF050C08),
                surface = Color(0xFF0E1A12),
                surfaceVariant = Color(0xFF13281B),
                onBackground = Color(0xFFECEFF1),
                onSurface = Color(0xFFFFFFFF),
                onSurfaceVariant = Color(0xFF99B2A2),
                isDark = true,
                cardGradStart = Color(0xFF092013),
                cardGradEnd = Color(0xFF040F09),
                surfaceGlass = Color(0xFF0E1A12).copy(alpha = 0.2f),
                surfaceBorder = Color(0xFF00E676).copy(alpha = 0.2f)
            )
            AppThemeType.PURPLE_VOID -> JarvisColors(
                accent = Color(0xFFD500F9),
                secondaryGlow = Color(0xFFF50057),
                background = Color(0xFF09040D),
                surface = Color(0xFF150D22),
                surfaceVariant = Color(0xFF220E33),
                onBackground = Color(0xFFECEFF1),
                onSurface = Color(0xFFFFFFFF),
                onSurfaceVariant = Color(0xFFB090C4),
                isDark = true,
                cardGradStart = Color(0xFF1A0A2D),
                cardGradEnd = Color(0xFF090214),
                surfaceGlass = Color(0xFF150D22).copy(alpha = 0.25f),
                surfaceBorder = Color(0xFFD500F9).copy(alpha = 0.2f)
            )
            AppThemeType.ORANGE_HEAT -> JarvisColors(
                accent = Color(0xFFFF6D00),
                secondaryGlow = Color(0xFFFFD600),
                background = Color(0xFF0F0803),
                surface = Color(0xFF1C1109),
                surfaceVariant = Color(0xFF2B180C),
                onBackground = Color(0xFFECEFF1),
                onSurface = Color(0xFFFFFFFF),
                onSurfaceVariant = Color(0xFFC7A287),
                isDark = true,
                cardGradStart = Color(0xFF261205),
                cardGradEnd = Color(0xFF110601),
                surfaceGlass = Color(0xFF1C1109).copy(alpha = 0.2f),
                surfaceBorder = Color(0xFFFFFFB7).copy(alpha = 0.2f)
            )
        }
    } else {
        // Light Mode equivalents (futuristic glossy, but light background)
        when (theme) {
            AppThemeType.HACKER_CYBER -> JarvisColors(
                accent = Color(0xFF00C853),
                secondaryGlow = Color(0xFF69F0AE),
                background = Color(0xFFE8F5E9),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFC8E6C9),
                onBackground = Color(0xFF1B5E20),
                onSurface = Color(0xFF004D40),
                onSurfaceVariant = Color(0xFF2E7D32),
                isDark = false,
                cardGradStart = Color(0xFFC8E6C9),
                cardGradEnd = Color(0xFFE8F5E9),
                surfaceGlass = Color(0xFFFFFFFF).copy(alpha = 0.6f),
                surfaceBorder = Color(0xFF00C853).copy(alpha = 0.25f)
            )
            AppThemeType.RED_MATRIX -> JarvisColors(
                accent = Color(0xFFD32F2F),
                secondaryGlow = Color(0xFFFF5252),
                background = Color(0xFFFAFAFA),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFFFEBEE),
                onBackground = Color(0xFF212121),
                onSurface = Color(0xFF1A1A1A),
                onSurfaceVariant = Color(0xFF5D4037),
                isDark = false,
                cardGradStart = Color(0xFFFFEBEE),
                cardGradEnd = Color(0xFFFAFAFA),
                surfaceGlass = Color(0xFFFFFFFF).copy(alpha = 0.5f),
                surfaceBorder = Color(0xFF000000).copy(alpha = 0.1f)
            )
            AppThemeType.BLUE_OCEAN -> JarvisColors(
                accent = Color(0xFF1976D2),
                secondaryGlow = Color(0xFF00BCD4),
                background = Color(0xFFF4F7FB),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFE3F2FD),
                onBackground = Color(0xFF212121),
                onSurface = Color(0xFF1A1A1A),
                onSurfaceVariant = Color(0xFF37474F),
                isDark = false,
                cardGradStart = Color(0xFFE3F2FD),
                cardGradEnd = Color(0xFFF4F7FB),
                surfaceGlass = Color(0xFFFFFFFF).copy(alpha = 0.5f),
                surfaceBorder = Color(0xFF1976D2).copy(alpha = 0.1f)
            )
            AppThemeType.GREEN_TECH -> JarvisColors(
                accent = Color(0xFF388E3C),
                secondaryGlow = Color(0xFF81C784),
                background = Color(0xFFF1F8E9),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFE8F5E9),
                onBackground = Color(0xFF212121),
                onSurface = Color(0xFF1A1A1A),
                onSurfaceVariant = Color(0xFF2E7D32),
                isDark = false,
                cardGradStart = Color(0xFFE8F5E9),
                cardGradEnd = Color(0xFFF1F8E9),
                surfaceGlass = Color(0xFFFFFFFF).copy(alpha = 0.5f),
                surfaceBorder = Color(0xFF388E3C).copy(alpha = 0.1f)
            )
            AppThemeType.PURPLE_VOID -> JarvisColors(
                accent = Color(0xFF7B1FA2),
                secondaryGlow = Color(0xFFE040FB),
                background = Color(0xFFFAF4FC),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFF3E5F5),
                onBackground = Color(0xFF212121),
                onSurface = Color(0xFF1A1A1A),
                onSurfaceVariant = Color(0xFF6A1B9A),
                isDark = false,
                cardGradStart = Color(0xFFF3E5F5),
                cardGradEnd = Color(0xFFFAF4FC),
                surfaceGlass = Color(0xFFFFFFFF).copy(alpha = 0.5f),
                surfaceBorder = Color(0xFF7B1FA2).copy(alpha = 0.1f)
            )
            AppThemeType.ORANGE_HEAT -> JarvisColors(
                accent = Color(0xFFE65100),
                secondaryGlow = Color(0xFFFFB74D),
                background = Color(0xFFFFF8E1),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFFFF3E0),
                onBackground = Color(0xFF212121),
                onSurface = Color(0xFF1A1A1A),
                onSurfaceVariant = Color(0xFFE65100),
                isDark = false,
                cardGradStart = Color(0xFFFFF3E0),
                cardGradEnd = Color(0xFFFFF8E1),
                surfaceGlass = Color(0xFFFFFFFF).copy(alpha = 0.5f),
                surfaceBorder = Color(0xFFE65100).copy(alpha = 0.1f)
            )
        }
    }
}
