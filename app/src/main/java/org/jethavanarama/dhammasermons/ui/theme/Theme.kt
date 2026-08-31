package org.jethavanarama.dhammasermons.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun DhammaSermonsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.LIGHT_KYOTO -> false
        ThemeMode.DARK_OBSIDIAN -> true
        ThemeMode.SYSTEM -> isSystemDark
    }

    val appColors = if (isDark) ObsidianAppColors else KyotoAppColors

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = appColors.primary,
            onPrimary = appColors.background,
            primaryContainer = appColors.surfaceHighlight,
            onPrimaryContainer = appColors.textPrimary,
            secondary = appColors.saffronGold,
            onSecondary = appColors.background,
            background = appColors.background,
            onBackground = appColors.textPrimary,
            surface = appColors.surfaceCard,
            onSurface = appColors.textPrimary,
            surfaceVariant = appColors.surface,
            onSurfaceVariant = appColors.textSecondary,
            outline = appColors.border
        )
    } else {
        lightColorScheme(
            primary = appColors.primary,
            onPrimary = Color.White,
            primaryContainer = appColors.surfaceHighlight,
            onPrimaryContainer = appColors.textPrimary,
            secondary = appColors.saffronGold,
            onSecondary = Color.White,
            background = appColors.background,
            onBackground = appColors.textPrimary,
            surface = appColors.surfaceCard,
            onSurface = appColors.textPrimary,
            surfaceVariant = appColors.surface,
            onSurfaceVariant = appColors.textSecondary,
            outline = appColors.border
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = appColors.background.toArgb()
            window.navigationBarColor = appColors.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalAppColors provides appColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
