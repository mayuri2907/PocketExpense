package com.example.pocketexpense.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.pocketexpense.util.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = MintGreen,
    onPrimary = DarkBackground,
    primaryContainer = MintGreenContainer,
    onPrimaryContainer = MintGreen,

    secondary = ElectricPurple,
    onSecondary = TextPrimary,
    secondaryContainer = ElectricPurpleContainer,
    onSecondaryContainer = ElectricPurple,

    tertiary = AmberGold,
    onTertiary = DarkBackground,
    tertiaryContainer = AmberGoldContainer,
    onTertiaryContainer = AmberGold,

    error = CoralRed,
    onError = TextPrimary,
    errorContainer = CoralRedContainer,
    onErrorContainer = CoralRed,

    background = DarkBackground,
    onBackground = TextPrimary,

    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,

    outline = DarkBorder,
    outlineVariant = DarkSurfaceHighlight
)

private val LightColorScheme = lightColorScheme(
    primary = LightMintGreen,
    onPrimary = Color.White,
    primaryContainer = LightMintGreenContainer,
    onPrimaryContainer = Color(0xFF065F46),

    secondary = LightElectricPurple,
    onSecondary = Color.White,
    secondaryContainer = LightElectricPurpleContainer,
    onSecondaryContainer = Color(0xFF4C1D95),

    tertiary = AmberGold,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF92400E),

    error = CoralRed,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),

    background = LightBackground,
    onBackground = LightTextPrimary,

    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,

    outline = LightBorder,
    outlineVariant = LightSurfaceHighlight
)

@Composable
fun PocketExpenseTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemDark
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val pocketColors = if (isDark) {
        PocketExpenseColors(
            isDark = true,
            background = DarkBackground,
            surface = DarkSurface,
            surfaceElevated = DarkSurfaceElevated,
            surfaceHighlight = DarkSurfaceHighlight,
            border = DarkBorder,
            borderSubtle = DarkBorderSubtle,
            textPrimary = TextPrimary,
            textSecondary = TextSecondary,
            textTertiary = TextTertiary,
            primary = MintGreen,
            primaryContainer = MintGreenContainer,
            secondary = ElectricPurple,
            secondaryContainer = ElectricPurpleContainer,
            heroCardBrush = HeroCardBrush,
            glowBrush = GlowAccentBrush
        )
    } else {
        PocketExpenseColors(
            isDark = false,
            background = LightBackground,
            surface = LightSurface,
            surfaceElevated = LightSurfaceElevated,
            surfaceHighlight = LightSurfaceHighlight,
            border = LightBorder,
            borderSubtle = LightBorderSubtle,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            textTertiary = LightTextTertiary,
            primary = LightMintGreen,
            primaryContainer = LightMintGreenContainer,
            secondary = LightElectricPurple,
            secondaryContainer = LightElectricPurpleContainer,
            heroCardBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0F172A),
                    Color(0xFF1E293B),
                    Color(0xFF334155)
                )
            ),
            glowBrush = Brush.linearGradient(
                colors = listOf(
                    LightMintGreen.copy(alpha = 0.15f),
                    LightElectricPurple.copy(alpha = 0.1f),
                    LightSurface
                )
            )
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalPocketExpenseColors provides pocketColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}