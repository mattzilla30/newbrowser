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

private val LightColors = lightColorScheme(
    primary = BrowserBlue,
    secondary = BrowserTeal,
)

private val DarkColors = darkColorScheme(
    primary = BrowserBlueDark,
    secondary = BrowserTealDark,
)

@Composable
fun NewBrowserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    themeColorIndex: Int = -1,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
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
        content = content,
    )
}
