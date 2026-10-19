package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryOrange,
    onPrimary = Color.White,
    primaryContainer = DarkCharcoalContainer,
    onPrimaryContainer = PrimaryOrange,
    secondary = LightOrangeHighlight,
    onSecondary = Color.Black,
    tertiary = PrimaryOrange,
    background = DarkCharcoalBackground,
    onBackground = TextPrimaryLight,
    surface = DarkCharcoalSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DarkCharcoalCard,
    onSurfaceVariant = TextSecondaryLight,
    outline = DarkLightShadowColor,
    error = StatusError
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = Color.White,
    primaryContainer = SoftOrangeContainer,
    onPrimaryContainer = DeepOrangePressed,
    secondary = DeepOrangePressed,
    onSecondary = Color.White,
    tertiary = PrimaryOrange,
    background = IvoryBackground,
    onBackground = TextPrimaryDark,
    surface = NeumorphicWhiteSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = NeumorphicCardSurface,
    onSurfaceVariant = TextSecondaryMuted,
    outline = DarkShadowColor,
    error = StatusError
)

@Composable
fun MaxAiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
