package com.ramesh.sms.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.ramesh.sms.data.util.AppLanguage
import com.ramesh.sms.data.util.AppPreferences

private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    surfaceDim = md_theme_light_surfaceDim,
    surfaceBright = md_theme_light_surfaceBright,
    surfaceContainerLowest = md_theme_light_surfaceContainerLowest,
    surfaceContainerLow = md_theme_light_surfaceContainerLow,
    surfaceContainer = md_theme_light_surfaceContainer,
    surfaceContainerHigh = md_theme_light_surfaceContainerHigh,
    surfaceContainerHighest = md_theme_light_surfaceContainerHighest,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant
)

private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    surfaceDim = md_theme_dark_surfaceDim,
    surfaceBright = md_theme_dark_surfaceBright,
    surfaceContainerLowest = md_theme_dark_surfaceContainerLowest,
    surfaceContainerLow = md_theme_dark_surfaceContainerLow,
    surfaceContainer = md_theme_dark_surfaceContainer,
    surfaceContainerHigh = md_theme_dark_surfaceContainerHigh,
    surfaceContainerHighest = md_theme_dark_surfaceContainerHighest,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant
)

/** Applies the chosen accent palette on top of Ramesh's base scheme. */
private fun ColorScheme.withAccent(palette: AccentPalette, dark: Boolean): ColorScheme {
    return if (dark) {
        copy(
            primary = palette.darkPrimary,
            onPrimary = palette.darkOnPrimary,
            primaryContainer = palette.darkPrimaryContainer,
            onPrimaryContainer = palette.darkOnPrimaryContainer,
            secondary = lerp(palette.darkPrimary, Color(0xFFB8C2C6), 0.6f),
            onSecondary = Color(0xFF152024),
            secondaryContainer = lerp(palette.darkPrimaryContainer, Color(0xFF2E383B), 0.55f),
            onSecondaryContainer = lerp(palette.darkPrimaryContainer, Color.White, 0.78f),
            tertiary = lerp(palette.darkPrimary, Color(0xFFC9C3D6), 0.65f),
            onTertiary = Color(0xFF1B1A23),
            tertiaryContainer = lerp(palette.darkPrimaryContainer, Color(0xFF35313D), 0.65f),
            onTertiaryContainer = lerp(palette.darkPrimaryContainer, Color.White, 0.82f)
        )
    } else {
        copy(
            primary = palette.lightPrimary,
            onPrimary = palette.lightOnPrimary,
            primaryContainer = palette.lightPrimaryContainer,
            onPrimaryContainer = palette.lightOnPrimaryContainer,
            secondary = lerp(palette.lightPrimary, Color(0xFF4A5658), 0.62f),
            onSecondary = Color.White,
            secondaryContainer = lerp(palette.lightPrimaryContainer, Color(0xFFE3E9EA), 0.5f),
            onSecondaryContainer = lerp(palette.lightPrimary, Color(0xFF0A1A1C), 0.62f),
            tertiary = lerp(palette.lightPrimary, Color(0xFF5B5A66), 0.62f),
            onTertiary = Color.White,
            tertiaryContainer = lerp(palette.lightPrimaryContainer, Color(0xFFEAE7F0), 0.6f),
            onTertiaryContainer = lerp(palette.lightPrimary, Color(0xFF151220), 0.72f)
        )
    }
}

/** The "Black" theme: true black surfaces for AMOLED screens. */
private fun ColorScheme.asBlack(): ColorScheme = copy(
    background = Color(0xFF000000),
    onBackground = Color(0xFFE8E8E8),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = Color(0xFF2A2E31),
    onSurfaceVariant = Color(0xFFC4C8CB),
    surfaceDim = Color(0xFF000000),
    surfaceBright = Color(0xFF24272A),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF070809),
    surfaceContainer = Color(0xFF0D0F10),
    surfaceContainerHigh = Color(0xFF16181A),
    surfaceContainerHighest = Color(0xFF202325),
    outline = Color(0xFF7C8487),
    outlineVariant = Color(0xFF2C3033)
)

@Composable
fun RameshTheme(
    appearance: Appearance = AppearanceStore.current,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appearance.themeMode) {
        ThemeMode.AUTO -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.BLACK -> true
    }

    val context = LocalContext.current
    val baseColors = when {
        appearance.accent == AccentColor.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val palette = appearance.accent.palette
    val accentColors = if (palette != null) baseColors.withAccent(palette, darkTheme) else baseColors
    val colorScheme = if (appearance.themeMode == ThemeMode.BLACK) accentColors.asBlack() else accentColors

    val typography = remember(appearance.fontFamily) { typographyFor(appearance.fontFamily) }

    // The chosen language decides the layout direction explicitly. Relying on the activity
    // configuration alone left Persian builds laid out left-to-right in some cases, which pushed
    // names, SIM badges and dates to the same side of a row.
    val language = remember(context) { AppPreferences(context.applicationContext).appLanguage }
    val systemDirection = LocalLayoutDirection.current
    val direction = when (language) {
        AppLanguage.PERSIAN -> LayoutDirection.Rtl
        AppLanguage.ENGLISH -> LayoutDirection.Ltr
        AppLanguage.SYSTEM -> systemDirection
    }

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = Shapes,
            typography = typography,
            content = content
        )
    }
}
