package io.github.emilioabril05.folio.core.designsystem.component

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.emilioabril05.folio.core.designsystem.theme.FolioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FolioPrimaryButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var clicks = 0

    private fun setButton(
        enabled: Boolean = true,
        loading: Boolean = false,
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                FolioTheme {
                    FolioPrimaryButton(
                        text = SEND,
                        onClick = { clicks++ },
                        supportingText = WEIGHT,
                        enabled = enabled,
                        loading = loading,
                    )
                }
            }
        }
    }

    @Test
    fun `el botón principal mide al menos 56 dp de alto`() {
        setButton()

        composeRule.onNode(hasClickAction()).assertHeightIsAtLeast(MIN_HEIGHT)
    }

    @Test
    fun `el botón anuncia texto y peso juntos a TalkBack`() {
        setButton()

        composeRule
            .onNode(hasContentDescription(SEND, substring = true) and hasContentDescription(WEIGHT, substring = true))
            .assertExists()
    }

    @Test
    fun `el botón deshabilitado no es clicable`() {
        setButton(enabled = false)

        composeRule.onNode(hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun `el botón cargando no dispara la acción`() {
        setButton(loading = true)

        composeRule.onNode(hasClickAction()).performClick()

        assertEquals(0, clicks)
    }

    @Test
    fun `con fuente al 200 % el botón crece y no corta el texto`() {
        setButton(fontScale = DOUBLE_FONT_SCALE)

        val bounds = composeRule.onNode(hasClickAction()).getUnclippedBoundsInRoot()
        val height = bounds.bottom - bounds.top
        assertTrue("alto $height", height > MIN_HEIGHT)
        listOf(SEND, WEIGHT).forEach { text ->
            val layouts = mutableListOf<TextLayoutResult>()
            composeRule
                .onNodeWithText(text, useUnmergedTree = true)
                .fetchSemanticsNode()
                .config[SemanticsActions.GetTextLayoutResult]
                .action
                ?.invoke(layouts)
            val layout = layouts.single()
            val linesFit = (0 until layout.lineCount).all { layout.getLineRight(it) <= layout.size.width }
            val ellipsized = (0 until layout.lineCount).any { layout.isLineEllipsized(it) }
            assertTrue("$text cortado a lo ancho", linesFit)
            assertFalse("$text cortado a lo alto", layout.didOverflowHeight)
            assertFalse("$text con puntos suspensivos", ellipsized)
        }
    }

    private companion object {
        const val SEND = "Enviar PDF"
        const val WEIGHT = "3 pág. · ~420 KB"
        const val DOUBLE_FONT_SCALE = 2f
        val MIN_HEIGHT = 56.dp
    }
}
