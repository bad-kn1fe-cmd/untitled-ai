package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class AppThemeOption(
    val id: String,
    val name: String,
    val isDark: Boolean,
    val previewBg: Color,
    val previewSurface: Color,
    val previewAccent: Color
)

val AvailableThemes = listOf(
    AppThemeOption("dark", "Тёмная (Классика)", true, Color(0xFF141416), Color(0xFF1E1E22), Color(0xFFD0BCFF)),
    AppThemeOption("cyberpunk", "Киберпанк Неон 2077", true, Color(0xFF070B14), Color(0xFF0F172A), Color(0xFF00F0FF)),
    AppThemeOption("sakura", "Сакура Градиент", true, Color(0xFF1A0B14), Color(0xFF281220), Color(0xFFF472B6)),
    AppThemeOption("ocean", "Океанская Бездна", true, Color(0xFF05111A), Color(0xFF0C1F2E), Color(0xFF38BDF8)),
    AppThemeOption("sunset", "Тёплый Закат", true, Color(0xFF1A0C10), Color(0xFF2B141C), Color(0xFFFB923C)),
    AppThemeOption("amber", "Имперское Золото", true, Color(0xFF140F08), Color(0xFF241A0E), Color(0xFFF59E0B)),
    AppThemeOption("purple", "Фиолетовый Неон", true, Color(0xFF0C0617), Color(0xFF180D2E), Color(0xFFC084FC)),
    AppThemeOption("oled", "OLED Чистый Чёрный", true, Color(0xFF000000), Color(0xFF0C0C0E), Color(0xFF818CF8)),
    AppThemeOption("gruvbox", "Gruvbox (Ретро)", true, Color(0xFF1D2021), Color(0xFF282828), Color(0xFFFABD2F)),
    AppThemeOption("nord", "Nord (Северная)", true, Color(0xFF242933), Color(0xFF2E3440), Color(0xFF88C0D0)),
    AppThemeOption("forest", "Изумрудный Лес", true, Color(0xFF081410), Color(0xFF10221A), Color(0xFF34D399)),
    AppThemeOption("crimson", "Винный Бархат", true, Color(0xFF15070A), Color(0xFF240E14), Color(0xFFFB7185)),
    AppThemeOption("light", "Светлая", false, Color(0xFFF8F9FA), Color(0xFFFFFFFF), Color(0xFF6366F1))
)

@Composable
fun AiRoleplayTheme(
    themeMode: String = "dark",
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Color = SophisticatedLavender,
    fontFamily: androidx.compose.ui.text.font.FontFamily = androidx.compose.ui.text.font.FontFamily.Default,
    content: @Composable () -> Unit
) {
    val effectiveMode = when (themeMode) {
        "system" -> if (darkTheme) "dark" else "light"
        else -> themeMode
    }

    val colorScheme: ColorScheme = when (effectiveMode) {
        "cyberpunk" -> darkColorScheme(
            primary = accentColor,
            onPrimary = Color(0xFF070B14),
            primaryContainer = Color(0xFF034A6E),
            onPrimaryContainer = Color(0xFFE0F2FE),
            secondary = Color(0xFF00F0FF),
            onSecondary = Color(0xFF070B14),
            secondaryContainer = Color(0xFF0C4A6E),
            onSecondaryContainer = Color(0xFFBAE6FD),
            tertiary = Color(0xFFFF007F),
            onTertiary = Color.White,
            background = Color(0xFF070B14),
            onBackground = Color(0xFFE2E8F0),
            surface = Color(0xFF0F172A),
            onSurface = Color(0xFFF1F5F9),
            surfaceVariant = Color(0xFF1E293B),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF334155),
            outlineVariant = Color(0xFF1E293B),
            error = Color(0xFFFF0055),
            onError = Color.White
        )
        "sakura" -> darkColorScheme(
            primary = accentColor,
            onPrimary = Color(0xFF1A0B14),
            primaryContainer = Color(0xFF4C1D38),
            onPrimaryContainer = Color(0xFFFCE7F3),
            secondary = Color(0xFFF472B6),
            onSecondary = Color(0xFF1A0B14),
            secondaryContainer = Color(0xFF500724),
            onSecondaryContainer = Color(0xFFFBCFE8),
            tertiary = Color(0xFFFBBF24),
            onTertiary = Color(0xFF1A0B14),
            background = Color(0xFF1A0B14),
            onBackground = Color(0xFFFCE7F3),
            surface = Color(0xFF281220),
            onSurface = Color(0xFFFCE7F3),
            surfaceVariant = Color(0xFF3B1C30),
            onSurfaceVariant = Color(0xFFF9A8D4),
            outline = Color(0xFF701A75),
            outlineVariant = Color(0xFF4A044E),
            error = Color(0xFFFB7185),
            onError = Color(0xFF1A0B14)
        )
        "ocean" -> darkColorScheme(
            primary = accentColor,
            onPrimary = Color(0xFF05111A),
            primaryContainer = Color(0xFF075985),
            onPrimaryContainer = Color(0xFFE0F2FE),
            secondary = Color(0xFF38BDF8),
            onSecondary = Color(0xFF05111A),
            secondaryContainer = Color(0xFF0369A1),
            onSecondaryContainer = Color(0xFFBAE6FD),
            tertiary = Color(0xFF2DD4BF),
            onTertiary = Color(0xFF05111A),
            background = Color(0xFF05111A),
            onBackground = Color(0xFFE0F2FE),
            surface = Color(0xFF0C1F2E),
            onSurface = Color(0xFFF0F9FF),
            surfaceVariant = Color(0xFF132D42),
            onSurfaceVariant = Color(0xFF7DD3FC),
            outline = Color(0xFF0284C7),
            outlineVariant = Color(0xFF0369A1),
            error = Color(0xFFF87171),
            onError = Color(0xFF05111A)
        )
        "sunset" -> darkColorScheme(
            primary = accentColor,
            onPrimary = Color(0xFF1A0C10),
            primaryContainer = Color(0xFF6B21A8),
            onPrimaryContainer = Color(0xFFFAE8FF),
            secondary = Color(0xFFFB923C),
            onSecondary = Color(0xFF1A0C10),
            secondaryContainer = Color(0xFF7C2D12),
            onSecondaryContainer = Color(0xFFFFEDD5),
            tertiary = Color(0xFFF43F5E),
            onTertiary = Color.White,
            background = Color(0xFF1A0C10),
            onBackground = Color(0xFFFFF1F2),
            surface = Color(0xFF2B141C),
            onSurface = Color(0xFFFFF1F2),
            surfaceVariant = Color(0xFF3D1D28),
            onSurfaceVariant = Color(0xFFFDA4AF),
            outline = Color(0xFF9F1239),
            outlineVariant = Color(0xFF4C0519),
            error = Color(0xFFEF4444),
            onError = Color.White
        )
        "amber" -> darkColorScheme(
            primary = accentColor,
            onPrimary = Color(0xFF140F08),
            primaryContainer = Color(0xFF78350F),
            onPrimaryContainer = Color(0xFFFEF3C7),
            secondary = Color(0xFFF59E0B),
            onSecondary = Color(0xFF140F08),
            secondaryContainer = Color(0xFF451A03),
            onSecondaryContainer = Color(0xFFFDE68A),
            tertiary = Color(0xFFD97706),
            onTertiary = Color(0xFF140F08),
            background = Color(0xFF140F08),
            onBackground = Color(0xFFFEF3C7),
            surface = Color(0xFF241A0E),
            onSurface = Color(0xFFFEF3C7),
            surfaceVariant = Color(0xFF362816),
            onSurfaceVariant = Color(0xFFFCD34D),
            outline = Color(0xFFB45309),
            outlineVariant = Color(0xFF78350F),
            error = Color(0xFFF87171),
            onError = Color(0xFF140F08)
        )
        "gruvbox" -> darkColorScheme(
            primary = accentColor,
            onPrimary = Color(0xFF1D2021),
            primaryContainer = Color(0xFF3C3836),
            onPrimaryContainer = Color(0xFFEBDBB2),
            secondary = Color(0xFFFABD2F),
            onSecondary = Color(0xFF1D2021),
            secondaryContainer = Color(0xFF504945),
            onSecondaryContainer = Color(0xFFEBDBB2),
            tertiary = Color(0xFFFE8019),
            onTertiary = Color(0xFF1D2021),
            background = Color(0xFF1D2021),
            onBackground = Color(0xFFEBDBB2),
            surface = Color(0xFF282828),
            onSurface = Color(0xFFEBDBB2),
            surfaceVariant = Color(0xFF3C3836),
            onSurfaceVariant = Color(0xFFA89984),
            outline = Color(0xFF504945),
            outlineVariant = Color(0xFF3C3836),
            error = Color(0xFFFB4934),
            onError = Color(0xFF1D2021)
        )
        "purple" -> darkColorScheme(
            primary = accentColor,
            onPrimary = Color(0xFF0C0617),
            primaryContainer = Color(0xFF2C1654),
            onPrimaryContainer = Color(0xFFF3E8FF),
            secondary = Color(0xFFC084FC),
            onSecondary = Color(0xFF0C0617),
            secondaryContainer = Color(0xFF3B1E70),
            onSecondaryContainer = Color(0xFFF3E8FF),
            tertiary = Color(0xFFE879F9),
            onTertiary = Color(0xFF0C0617),
            background = Color(0xFF0C0617),
            onBackground = Color(0xFFF3E8FF),
            surface = Color(0xFF180D2E),
            onSurface = Color(0xFFF3E8FF),
            surfaceVariant = Color(0xFF261548),
            onSurfaceVariant = Color(0xFFD8B4FE),
            outline = Color(0xFF4C288A),
            outlineVariant = Color(0xFF321A5E),
            error = ErrorRed,
            onError = Color(0xFF0C0617)
        )
        "oled" -> darkColorScheme(
            primary = accentColor,
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF1E1B4B),
            onPrimaryContainer = Color(0xFFE0E7FF),
            secondary = Color(0xFFA1A1AA),
            onSecondary = Color.Black,
            secondaryContainer = Color(0xFF27272A),
            onSecondaryContainer = Color(0xFFF4F4F5),
            tertiary = Color(0xFF38BDF8),
            onTertiary = Color.Black,
            background = Color.Black,
            onBackground = Color(0xFFF4F4F5),
            surface = Color(0xFF0C0C0E),
            onSurface = Color(0xFFF4F4F5),
            surfaceVariant = Color(0xFF17171B),
            onSurfaceVariant = Color(0xFFA1A1AA),
            outline = Color(0xFF27272A),
            outlineVariant = Color(0xFF18181B),
            error = ErrorRed,
            onError = Color.Black
        )
        "nord" -> darkColorScheme(
            primary = accentColor,
            onPrimary = Color(0xFF242933),
            primaryContainer = Color(0xFF3B4252),
            onPrimaryContainer = Color(0xFFECEFF4),
            secondary = Color(0xFF88C0D0),
            onSecondary = Color(0xFF242933),
            secondaryContainer = Color(0xFF434C5E),
            onSecondaryContainer = Color(0xFFECEFF4),
            tertiary = Color(0xFF81A1C1),
            onTertiary = Color(0xFF242933),
            background = Color(0xFF242933),
            onBackground = Color(0xFFECEFF4),
            surface = Color(0xFF2E3440),
            onSurface = Color(0xFFECEFF4),
            surfaceVariant = Color(0xFF3B4252),
            onSurfaceVariant = Color(0xFFD8DEE9),
            outline = Color(0xFF4C566A),
            outlineVariant = Color(0xFF3B4252),
            error = Color(0xFFBF616A),
            onError = Color(0xFF242933)
        )
        "forest" -> darkColorScheme(
            primary = accentColor,
            onPrimary = Color(0xFF081410),
            primaryContainer = Color(0xFF1A3D2F),
            onPrimaryContainer = Color(0xFFE6F5EE),
            secondary = Color(0xFF34D399),
            onSecondary = Color(0xFF081410),
            secondaryContainer = Color(0xFF1C4535),
            onSecondaryContainer = Color(0xFFE6F5EE),
            tertiary = Color(0xFF6EE7B7),
            onTertiary = Color(0xFF081410),
            background = Color(0xFF081410),
            onBackground = Color(0xFFE6F5EE),
            surface = Color(0xFF10221A),
            onSurface = Color(0xFFE6F5EE),
            surfaceVariant = Color(0xFF1A3327),
            onSurfaceVariant = Color(0xFFA7D7C1),
            outline = Color(0xFF2B5441),
            outlineVariant = Color(0xFF1C3A2D),
            error = ErrorRed,
            onError = Color(0xFF081410)
        )
        "crimson" -> darkColorScheme(
            primary = accentColor,
            onPrimary = Color(0xFF15070A),
            primaryContainer = Color(0xFF4A1824),
            onPrimaryContainer = Color(0xFFFFE6EB),
            secondary = Color(0xFFFB7185),
            onSecondary = Color(0xFF15070A),
            secondaryContainer = Color(0xFF521B27),
            onSecondaryContainer = Color(0xFFFFE6EB),
            tertiary = Color(0xFFFDA4AF),
            onTertiary = Color(0xFF15070A),
            background = Color(0xFF15070A),
            onBackground = Color(0xFFFFE6EB),
            surface = Color(0xFF240E14),
            onSurface = Color(0xFFFFE6EB),
            surfaceVariant = Color(0xFF36141D),
            onSurfaceVariant = Color(0xFFF4A7B7),
            outline = Color(0xFF5E2432),
            outlineVariant = Color(0xFF3D1620),
            error = ErrorRed,
            onError = Color(0xFF15070A)
        )
        "light" -> lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.18f),
            onPrimaryContainer = Color(0xFF1E1B4B),
            secondary = Color(0xFF4F46E5),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE0E7FF),
            onSecondaryContainer = Color(0xFF1E1B4B),
            tertiary = Color(0xFF9333EA),
            onTertiary = Color.White,
            background = Color(0xFFF8F9FA),
            onBackground = Color(0xFF181A20),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF181A20),
            surfaceVariant = Color(0xFFECEEF2),
            onSurfaceVariant = Color(0xFF494C56),
            outline = Color(0xFFCBD0D9),
            outlineVariant = Color(0xFFE2E4E9),
            error = ErrorRed,
            onError = Color.White
        )
        else -> darkColorScheme(
            primary = accentColor,
            onPrimary = SophisticatedDarkBackground,
            primaryContainer = SophisticatedPurpleContainer,
            onPrimaryContainer = SophisticatedPurpleContainerOn,
            secondary = SophisticatedMauve,
            onSecondary = Color(0xFF332D41),
            secondaryContainer = Color(0xFF4A4458),
            onSecondaryContainer = SophisticatedHighlight,
            tertiary = SophisticatedRose,
            onTertiary = Color(0xFF492532),
            tertiaryContainer = Color(0xFF633B48),
            onTertiaryContainer = Color(0xFFFFD8E4),
            background = SophisticatedDarkBackground,
            onBackground = TextPrimaryDark,
            surface = SophisticatedDarkSurface,
            onSurface = TextPrimaryDark,
            surfaceVariant = SophisticatedDarkSurfaceVariant,
            onSurfaceVariant = TextSecondaryDark,
            outline = SophisticatedDarkBorder,
            outlineVariant = SophisticatedDarkBorder.copy(alpha = 0.5f),
            error = ErrorRed,
            onError = Color(0xFF601410)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = createAppTypography(fontFamily),
        content = content
    )
}
