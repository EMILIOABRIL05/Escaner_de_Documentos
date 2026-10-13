package io.github.emilioabril05.folio.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class TypographyTest {
    @Test
    fun `todos los estilos de texto usan Inter`() {
        val styles =
            Typography::class.java.methods
                .filter { it.parameterCount == 0 && it.returnType == TextStyle::class.java }
                // Los estilos "Emphasized" son internos de Material 3 (API Expressive experimental).
                .filterNot { INTERNAL_MARKER in it.name }
                .associate { it.name.removePrefix("get") to (it.invoke(FolioTypography) as TextStyle) }

        val notInter = styles.filterValues { it.fontFamily != InterFontFamily }.keys

        assertEquals(emptySet<String>(), notInter)
    }

    private companion object {
        const val INTERNAL_MARKER = "$"
    }
}
