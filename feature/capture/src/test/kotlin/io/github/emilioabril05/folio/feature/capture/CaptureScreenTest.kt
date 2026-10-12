package io.github.emilioabril05.folio.feature.capture

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.emilioabril05.folio.core.designsystem.theme.FolioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es")
class CaptureScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var scans = 0

    private fun show(state: CaptureUiState) {
        composeRule.setContent {
            FolioTheme { CaptureScreen(uiState = state, onScan = { scans++ }) }
        }
    }

    @Test
    fun `listo muestra el consejo y la acción Escanear`() {
        show(CaptureUiState.Idle)

        composeRule.onNodeWithText("Escanea tu primera hoja").assertIsDisplayed()
        composeRule.onNodeWithText("Pon la hoja sobre un fondo oscuro y con buena luz.").assertIsDisplayed()
        composeRule.onNode(hasText("Escanear") and hasClickAction()).performClick()
        assertEquals(1, scans)
    }

    @Test
    fun `guardando muestra cuántas páginas y no ofrece acciones`() {
        show(CaptureUiState.Saving(pageCount = 3))

        composeRule.onNodeWithText("Guardando 3 páginas…").assertIsDisplayed()
        composeRule.onNode(hasClickAction()).assertDoesNotExist()
    }

    @Test
    fun `guardado confirma las páginas y permite seguir escaneando`() {
        show(CaptureUiState.Saved(pageCount = 1))

        composeRule.onNodeWithText("1 página guardada").assertIsDisplayed()
        composeRule.onNode(hasText("Escanear otra vez") and hasClickAction()).performClick()
        assertEquals(1, scans)
    }

    @Test
    fun `sin escáner explica la causa y la solución y permite reintentar`() {
        show(CaptureUiState.Failed(CaptureFailure.ScannerUnavailable))

        composeRule.onNodeWithText("Este teléfono no tiene el escáner").assertIsDisplayed()
        composeRule.onNodeWithText("Actualiza Google Play services y vuelve a intentarlo.").assertIsDisplayed()
        composeRule.onNode(hasText("Reintentar") and hasClickAction()).performClick()
        assertEquals(1, scans)
    }

    @Test
    fun `sin espacio explica cómo resolverlo`() {
        show(CaptureUiState.Failed(CaptureFailure.NotEnoughSpace))

        composeRule.onNodeWithText("No hay espacio para guardar").assertIsDisplayed()
        composeRule.onNodeWithText("Libera espacio en el teléfono y vuelve a escanear.").assertIsDisplayed()
    }

    @Test
    fun `los cambios de estado se anuncian a TalkBack`() {
        show(CaptureUiState.Saved(pageCount = 3))

        composeRule
            .onNode(
                hasText("3 páginas guardadas") and
                    SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
            ).assertExists()
    }
}
