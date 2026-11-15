package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PrimeDarkColorScheme = darkColorScheme(
    primary = PrimeBlue,
    onPrimary = PrimeTextPrimary,
    primaryContainer = PrimeNavySurface,
    onPrimaryContainer = PrimeBlueLight,
    secondary = PrimeRed,
    onSecondary = PrimeTextPrimary,
    secondaryContainer = PrimeRedDark,
    onSecondaryContainer = PrimeRedLight,
    tertiary = PrimePurple,
    onTertiary = PrimeTextPrimary,
    background = PrimeDarkBackground,
    onBackground = PrimeTextPrimary,
    surface = PrimeCardBackground,
    onSurface = PrimeTextPrimary,
    surfaceVariant = PrimeNavySurface,
    onSurfaceVariant = PrimeTextSecondary,
    outline = PrimeBorder,
    outlineVariant = PrimeSurfaceHover
)

private val PrimeLightColorScheme = lightColorScheme(
    primary = PrimeBlue,
    onPrimary = PrimeTextPrimary,
    primaryContainer = PrimeBlueLight,
    onPrimaryContainer = PrimeDarkBackground,
    secondary = PrimeRed,
    onSecondary = PrimeTextPrimary,
    background = PrimeDarkBackground, // Gaming apps are predominantly dark
    onBackground = PrimeTextPrimary,
    surface = PrimeCardBackground,
    onSurface = PrimeTextPrimary,
    surfaceVariant = PrimeNavySurface,
    onSurfaceVariant = PrimeTextSecondary,
    outline = PrimeBorder
)

@Composable
fun PrimeEsportTheme(
    darkTheme: Boolean = true, // Default to true for gaming aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PrimeDarkColorScheme else PrimeLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Keep MyApplicationTheme for backward compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    PrimeEsportTheme(darkTheme = true, content = content)
}
