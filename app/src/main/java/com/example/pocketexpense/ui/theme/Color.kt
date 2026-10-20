package com.example.pocketexpense.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// Dark Theme Palette Tokens
// ==========================================
val DarkBackground = Color(0xFF090D16)
val DarkSurface = Color(0xFF121824)
val DarkSurfaceElevated = Color(0xFF1A2232)
val DarkSurfaceHighlight = Color(0xFF222C40)
val DarkBorder = Color(0xFF26334A)
val DarkBorderSubtle = Color(0x1FFFFFFF)

// Brand & Financial Accents
val MintGreen = Color(0xFF00E6A1)
val MintGreenDim = Color(0xFF00B37D)
val MintGreenContainer = Color(0xFF0C2B20)
val MintGreenGlow = Color(0x2E00E6A1)

val ElectricPurple = Color(0xFF7C5CFC)
val ElectricPurpleContainer = Color(0xFF221A44)
val ElectricPurpleGlow = Color(0x2E7C5CFC)

val SkyBlue = Color(0xFF38BDF8)
val SkyBlueContainer = Color(0xFF0C2738)

val CoralRed = Color(0xFFFF5252)
val CoralRedContainer = Color(0xFF331616)

val AmberGold = Color(0xFFFFB800)
val AmberGoldContainer = Color(0xFF332406)
val AmberWarning = AmberGold

// Typography & Content Colors (Dark)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF64748B)

// ==========================================
// Light Theme Palette Tokens (Crisp Modern Fintech)
// ==========================================
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF1F5F9)
val LightSurfaceHighlight = Color(0xFFE2E8F0)
val LightBorder = Color(0xFFE2E8F0)
val LightBorderSubtle = Color(0x12000000)

val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightTextTertiary = Color(0xFF94A3B8)

val LightMintGreen = Color(0xFF00B37D)
val LightMintGreenContainer = Color(0xFFD1FAE5)

val LightElectricPurple = Color(0xFF6D28D9)
val LightElectricPurpleContainer = Color(0xFFEDE9FE)

// ==========================================
// Category Colors (Refined & Vibrant for both themes)
// ==========================================
val CategoryFood = Color(0xFFFF6B6B)
val CategoryTravel = Color(0xFF20BF6B)
val CategoryGroceries = Color(0xFF26DE81)
val CategoryShopping = Color(0xFFFA8231)
val CategoryBills = Color(0xFF4B7BEC)
val CategoryEducation = Color(0xFFA55EEA)
val CategoryHealth = Color(0xFFFC5C65)
val CategoryUncategorized = Color(0xFF778CA3)

// Luxury Gradients
val HeroCardBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF1B253A),
        Color(0xFF111726),
        Color(0xFF0D121F)
    )
)

val GlowAccentBrush = Brush.linearGradient(
    colors = listOf(
        ElectricPurple.copy(alpha = 0.22f),
        MintGreen.copy(alpha = 0.12f),
        DarkSurface
    )
)

// ==========================================
// Dynamic Unified AppTheme Palette Interface
// ==========================================
@Immutable
data class PocketExpenseColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHighlight: Color,
    val border: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val heroCardBrush: Brush,
    val glowBrush: Brush
)

val LocalPocketExpenseColors = staticCompositionLocalOf {
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
}

object AppTheme {
    val colors: PocketExpenseColors
        @Composable
        get() = LocalPocketExpenseColors.current
}