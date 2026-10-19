package com.example.pocketexpense.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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

@Composable
fun PocketExpenseTheme(
    darkTheme: Boolean = true, // Default to premium dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}