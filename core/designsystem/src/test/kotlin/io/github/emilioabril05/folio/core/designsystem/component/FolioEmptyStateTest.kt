package io.github.emilioabril05.folio.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.emilioabril05.folio.core.designsystem.theme.FolioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolioEmptyStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var actions = 0

    private fun setEmptyState() {
        composeRule.setContent {
            FolioTheme {
                FolioEmptyState(
                    icon = ColorPainter(Color.Gray),
                    title = TITLE,
                    message = MESSAGE,
                    actionText = ACTION,
                    onAction = { actions++ },
                )
            }
        }
    }

    @Test
    fun `el estado vacío muestra título y mensaje`() {
        setEmptyState()

        composeRule.onNodeWithText(TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(MESSAGE).assertIsDisplayed()
    }

    @Test
    fun `el estado vacío ofrece su acción con objetivo táctil de 48 dp`() {
        setEmptyState()

        composeRule.onNode(hasClickAction()).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        composeRule.onNode(hasClickAction()).performClick()
        assertEquals(1, actions)
    }

    private companion object {
        const val TITLE = "Escanea tu primera hoja"
        const val MESSAGE = "Tus documentos aparecerán aquí."
        const val ACTION = "Escanear"
        val MIN_TOUCH_TARGET = 48.dp
    }
}
