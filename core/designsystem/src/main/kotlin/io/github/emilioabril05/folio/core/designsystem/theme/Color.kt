package io.github.emilioabril05.folio.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Colores de Folio para un modo (claro u oscuro). Un solo color de acento. */
@Immutable
data class FolioPalette(
    val background: Color,
    val surface: Color,
    val text: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
)

/** Paletas de Folio. Cada par texto/fondo cumple WCAG 2.2 AA (ver `ContrastTest`). */
object FolioColors {
    val Light =
        FolioPalette(
            background = Color(0xFFF5F5F7),
            surface = Color(0xFFFFFFFF),
            text = Color(0xFF111827),
            textSecondary = Color(0xFF4B5563),
            accent = Color(0xFF2563EB),
            onAccent = Color(0xFFFFFFFF),
        )

    // #2563EB sobre negro da 4,06:1; en oscuro el acento se aclara para llegar a 4,5:1.
    val Dark =
        FolioPalette(
            background = Color(0xFF000000),
            surface = Color(0xFF1C1C1E),
            text = Color(0xFFF3F4F6),
            textSecondary = Color(0xFF9CA3AF),
            accent = Color(0xFF60A5FA),
            onAccent = Color(0xFF0B1220),
        )
}
