package io.github.emilioabril05.folio.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {
    private val palettes = mapOf("claro" to FolioColors.Light, "oscuro" to FolioColors.Dark)

    @Test
    fun `blanco sobre negro da 21 a 1`() {
        assertEquals(MAX_CONTRAST, contrastRatio(Color.White, Color.Black), TOLERANCE)
    }

    @Test
    fun `texto sobre fondo cumple 4,5 a 1 en claro y oscuro`() {
        palettes.forEach { (name, palette) ->
            val pairs =
                mapOf(
                    "texto/fondo" to (palette.text to palette.background),
                    "texto/superficie" to (palette.text to palette.surface),
                    "secundario/fondo" to (palette.textSecondary to palette.background),
                    "secundario/superficie" to (palette.textSecondary to palette.surface),
                    "texto/superficie elevada" to (palette.text to palette.surfaceHigh),
                    "secundario/superficie elevada" to (palette.textSecondary to palette.surfaceHigh),
                    "fondo/texto (superficie inversa)" to (palette.background to palette.text),
                    "sobre acento/acento" to (palette.onAccent to palette.accent),
                    "acento/superficie" to (palette.accent to palette.surface),
                )
            pairs.forEach { (pair, colors) ->
                val ratio = contrastRatio(colors.first, colors.second)
                assertTrue("$name $pair da $ratio", ratio >= TEXT_MIN_CONTRAST)
            }
        }
    }

    @Test
    fun `acento sobre superficie cumple 3 a 1 para íconos`() {
        palettes.forEach { (name, palette) ->
            val ratio = contrastRatio(palette.accent, palette.surface)
            assertTrue("$name da $ratio", ratio >= ICON_MIN_CONTRAST)
        }
    }

    @Test
    fun `el contorno de los controles cumple 3 a 1 sobre superficie y fondo`() {
        palettes.forEach { (name, palette) ->
            listOf(palette.surface, palette.background).forEach { background ->
                val ratio = contrastRatio(palette.outline, background)
                assertTrue("$name da $ratio", ratio >= ICON_MIN_CONTRAST)
            }
        }
    }

    private companion object {
        const val MAX_CONTRAST = 21.0
        const val TOLERANCE = 0.01
        const val TEXT_MIN_CONTRAST = 4.5
        const val ICON_MIN_CONTRAST = 3.0
    }
}
