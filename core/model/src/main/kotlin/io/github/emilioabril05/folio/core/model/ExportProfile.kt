package io.github.emilioabril05.folio.core.model

/**
 * Calidad del PDF (plan §2.3, regla 4). [maxSidePx] es el lado mayor de una hoja A4 a [dpi] puntos por pulgada.
 */
enum class ExportProfile(
    val dpi: Int,
    val maxSidePx: Int,
    val jpegQuality: Int,
) {
    /** Para WhatsApp y correo: 150 ppp, JPEG 60 y grises si la hoja es solo texto. */
    Light(dpi = 150, maxSidePx = 1754, jpegQuality = 60),

    /** Por defecto: 200 ppp y JPEG 75. */
    Balanced(dpi = 200, maxSidePx = 2339, jpegQuality = 75),

    /** Para archivar o imprimir: 300 ppp y JPEG 90. */
    High(dpi = 300, maxSidePx = 3508, jpegQuality = 90),
}
