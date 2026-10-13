package io.github.emilioabril05.folio.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

// Margen que WCAG suma a la luminancia de ambos colores.
private const val LUMINANCE_OFFSET = 0.05

/** Contraste WCAG 2.2 entre dos colores, de 1:1 a 21:1. El orden de los colores no importa. */
fun contrastRatio(
    foreground: Color,
    background: Color,
): Double {
    val first = foreground.luminance().toDouble()
    val second = background.luminance().toDouble()
    return (max(first, second) + LUMINANCE_OFFSET) / (min(first, second) + LUMINANCE_OFFSET)
}
