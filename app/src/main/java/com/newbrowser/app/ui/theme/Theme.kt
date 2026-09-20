package com.newbrowser.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.newbrowser.app.data.CYN_THEME_INDEX

private val LightColors = lightColorScheme(
    primary = BrowserBlue,
    secondary = BrowserTeal,
)

private val DarkColors = darkColorScheme(
    primary = BrowserBlueDark,
    secondary = BrowserTealDark,
)

/** The app's own default look: Cyn's magenta glow and worker-drone cyan on a near-black chassis. */
private val CynColorScheme = darkColorScheme(
    primary = CynPrimary,
    onPrimary = CynOnPrimary,
    primaryContainer = CynPrimaryContainer,
    onPrimaryContainer = CynOnPrimaryContainer,
    secondary = CynSecondary,
    onSecondary = CynOnSecondary,
    secondaryContainer = CynSecondaryContainer,
    onSecondaryContainer = CynOnSecondaryContainer,
    tertiary = CynTertiary,
    onTertiary = CynOnTertiary,
    tertiaryContainer = CynTertiaryContainer,
    onTertiaryContainer = CynOnTertiaryContainer,
    background = CynBackground,
    onBackground = CynOnBackground,
    surface = CynSurface,
    onSurface = CynOnSurface,
    surfaceVariant = CynSurfaceVariant,
    onSurfaceVariant = CynOnSurfaceVariant,
    outline = CynOutline,
    error = CynError,
    onError = CynOnError,
)

@Composable
fun NewBrowserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    themeColorIndex: Int = CYN_THEME_INDEX,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        themeColorIndex == CYN_THEME_INDEX -> CynColorScheme
        themeColorIndex in THEME_COLOR_PRESETS.indices -> {
            val seed = THEME_COLOR_PRESETS[themeColorIndex]
            if (darkTheme) darkColorScheme(primary = seed) else lightColorScheme(primary = seed)
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = CynShapes,
        typography = CynTypography,
        content = content,
    )
}
