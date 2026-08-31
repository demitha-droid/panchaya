package org.jethavanarama.dhammasermons.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceCard: Color,
    val surfaceHighlight: Color,
    val border: Color,
    val borderSubtle: Color,
    val primary: Color,
    val secondary: Color,
    val saffronGold: Color,
    val saffronLight: Color,
    val saffronDark: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val sageLotus: Color,
    val accentPahuraRed: Color,
    val accentOfflineGreen: Color,
    val isDark: Boolean
)

// Kyoto Paper & Terroir Minimal (Light Warm Clean)
val KyotoAppColors = AppColors(
    background = Color(0xFFF5F1E8),       // Oatmeal Sandstone Paper
    surface = Color(0xFFEBE5D8),          // Linen Sandstone Surface
    surfaceCard = Color(0xFFFCFAF6),      // Pure Ivory Washi Card
    surfaceHighlight = Color(0xFFEADFD3), // Warm Terracotta Tint Surface
    border = Color(0xFFDDD6C9),           // Delicate Sandstone Hairline Border
    borderSubtle = Color(0xFFE8E2D8),     // Soft Paper Edge
    primary = Color(0xFFC86A32),          // Monastic Terracotta Ochre
    secondary = Color(0xFFD97706),        // Warm Temple Amber
    saffronGold = Color(0xFFD97706),      // Warm Temple Amber
    saffronLight = Color(0xFFF59E0B),     // Golden Saffron
    saffronDark = Color(0xFF9A3412),      // Deep Earth Clay
    textPrimary = Color(0xFF1A1815),      // Deep Sumi Calligraphy Ink
    textSecondary = Color(0xFF605A52),    // Warm Stone Muted
    textMuted = Color(0xFF8A847A),        // Soft Sandstone Ash
    sageLotus = Color(0xFF7A8B7B),        // Kyoto Garden Moss
    accentPahuraRed = Color(0xFFC2410C),  // Japanese Vermilion
    accentOfflineGreen = Color(0xFF5A8B63), // Natural Olive Sage
    isDark = false
)

// Sacred Obsidian & Liquid Amber (Dark OLED Void + Molten Gold)
val ObsidianAppColors = AppColors(
    background = Color(0xFF080705),       // True OLED pitch black
    surface = Color(0xFF100E0C),          // Obsidian Glass surface
    surfaceCard = Color(0xFF16130F),      // Translucent dark acrylic card
    surfaceHighlight = Color(0xFF221A11), // Glowing Amber card surface
    border = Color(0x33F59E0B),           // Luminous Amber hairline border (20% alpha)
    borderSubtle = Color(0x1AFDFBF7),     // Subtle ivory hairline border (10% alpha)
    primary = Color(0xFFF59E0B),          // Molten Liquid Amber
    secondary = Color(0xFFFBBF24),        // Radiant Sun Gold
    saffronGold = Color(0xFFFBBF24),      // Radiant Sun Gold
    saffronLight = Color(0xFFFDE68A),     // Luminous Warm Amber Glow
    saffronDark = Color(0xFFB45309),      // Deep Monastery Ochre
    textPrimary = Color(0xFFFDFBF7),      // Luminous Parchment Ivory
    textSecondary = Color(0xFFD1C7B7),    // Warm Silk Grey
    textMuted = Color(0xFF8C8275),        // Soft Temple Ash
    sageLotus = Color(0xFF94B49F),        // Serene Lotus Sage
    accentPahuraRed = Color(0xFFEF4444),  // Radiant Crimson
    accentOfflineGreen = Color(0xFF10B981), // Emerald Offline Indicator
    isDark = true
)

val LocalAppColors = staticCompositionLocalOf { KyotoAppColors }

val BackgroundDark: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.background

val SurfaceDark: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.surface

val SurfaceCard: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.surfaceCard

val SurfaceHighlight: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.surfaceHighlight

val BorderDark: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.border

val BorderSubtle: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.borderSubtle

val SaffronPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.primary

val SaffronGold: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.saffronGold

val SaffronLight: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.saffronLight

val SaffronDark: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.saffronDark

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.textPrimary

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.textSecondary

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.textMuted

val SageLotus: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.sageLotus

val AccentPahuraRed: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.accentPahuraRed

val AccentJethavanaramaGold: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.primary

val AccentOfflineGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.accentOfflineGreen
