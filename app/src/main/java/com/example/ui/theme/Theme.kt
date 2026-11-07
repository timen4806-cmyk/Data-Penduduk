package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.repository.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = DarkNavyPrimary,
    onPrimary = DarkNavyOnPrimary,
    primaryContainer = DarkNavyPrimaryContainer,
    onPrimaryContainer = DarkNavyOnPrimaryContainer,
    secondary = DarkEmeraldSecondary,
    onSecondary = DarkEmeraldOnSecondary,
    secondaryContainer = DarkEmeraldSecondaryContainer,
    onSecondaryContainer = DarkEmeraldOnSecondaryContainer,
    tertiary = DarkAmberTertiary,
    onTertiary = DarkAmberOnTertiary,
    tertiaryContainer = DarkAmberTertiaryContainer,
    onTertiaryContainer = DarkAmberOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = CivicNavyPrimary,
    onPrimary = CivicNavyOnPrimary,
    primaryContainer = CivicNavyPrimaryContainer,
    onPrimaryContainer = CivicNavyOnPrimaryContainer,
    secondary = CivicEmeraldSecondary,
    onSecondary = CivicEmeraldOnSecondary,
    secondaryContainer = CivicEmeraldSecondaryContainer,
    onSecondaryContainer = CivicEmeraldOnSecondaryContainer,
    tertiary = CivicAmberTertiary,
    onTertiary = CivicAmberOnTertiary,
    tertiaryContainer = CivicAmberTertiaryContainer,
    onTertiaryContainer = CivicAmberOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
