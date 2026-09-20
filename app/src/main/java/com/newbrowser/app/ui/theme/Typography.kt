package com.newbrowser.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

private val baseTypography = Typography()

/**
 * Screen titles and labels in monospace, like a worker-drone terminal readout; body text stays
 * on the default sans-serif since long-running text (page titles, urls) reads better there.
 */
val CynTypography = baseTypography.copy(
    titleLarge = baseTypography.titleLarge.copy(fontFamily = FontFamily.Monospace),
    titleMedium = baseTypography.titleMedium.copy(fontFamily = FontFamily.Monospace),
    labelLarge = baseTypography.labelLarge.copy(fontFamily = FontFamily.Monospace),
)
