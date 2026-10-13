package io.github.emilioabril05.folio.feature.review.crop

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.emilioabril05.folio.core.designsystem.theme.FolioTheme
import io.github.emilioabril05.folio.core.domain.EncodedImage
import io.github.emilioabril05.folio.core.model.Corner
import io.github.emilioabril05.folio.core.model.CropQuad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es")
class CropEditorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<CropEditorEvent>()

    private fun show(state: CropEditorUiState) {
        composeRule.setContent {
            FolioTheme { CropEditorScreen(uiState = state, onEvent = { events += it }, onBack = {}) }
        }
    }

    private fun editing(
        canUndo: Boolean = false,
        saveFailed: Boolean = false,
    ) = CropEditorUiState.Editing(photo = testPhoto(), crop = CropQuad.Full, canUndo = canUndo, saveFailed = saveFailed)

    @Test
    fun `las 4 esquinas tienen nombre y objetivo táctil de 48 dp`() {
        show(editing())

        listOf("superior izquierda", "superior derecha", "inferior derecha", "inferior izquierda").forEach { name ->
            composeRule
                .onNodeWithContentDescription("Esquina $name")
                .assertWidthIsAtLeast(MIN_TOUCH_TARGET)
                .assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        }
    }

    @Test
    fun `TalkBack puede mover una esquina sin arrastrar`() {
        show(editing())

        val node = composeRule.onNodeWithContentDescription("Esquina inferior derecha")
        val action =
            node
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
                .single { it.label == "Mover hacia la izquierda" }
        composeRule.runOnIdle { action.action() }

        val move = events.single() as CropEditorEvent.MoveCorner
        assertEquals(Corner.BottomRight, move.corner)
        assertTrue("x=${move.point.x}", move.point.x < 1f)
        assertEquals(1f, move.point.y)
    }

    @Test
    fun `sin cambios no se puede deshacer`() {
        show(editing(canUndo = false))

        composeRule.onNodeWithContentDescription("Deshacer").assertIsNotEnabled()
    }

    @Test
    fun `hecho guarda el recorte`() {
        show(editing())

        composeRule.onNode(hasText("Hecho") and hasClickAction()).performClick()

        assertEquals(listOf(CropEditorEvent.Done), events)
    }

    @Test
    fun `restablecer vuelve a la foto completa`() {
        show(editing())

        composeRule.onNode(hasText("Restablecer") and hasClickAction()).performClick()

        assertEquals(listOf(CropEditorEvent.Reset), events)
    }

    @Test
    fun `si no se pudo guardar lo explica`() {
        show(editing(saveFailed = true))

        composeRule.onNodeWithText("No se pudo guardar el recorte. Vuelve a intentarlo.").assertExists()
    }

    @Test
    fun `si la foto no se puede abrir explica qué hacer`() {
        show(CropEditorUiState.Failed)

        composeRule.onNodeWithText("No se pudo abrir la foto").assertExists()
        composeRule.onNode(hasText("Volver") and hasClickAction()).assertExists()
        composeRule.onNode(hasContentDescription("Deshacer")).assertDoesNotExist()
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions)).assertDoesNotExist()
    }

    private companion object {
        val MIN_TOUCH_TARGET = 48.dp
        private const val PHOTO_WIDTH = 300
        private const val PHOTO_HEIGHT = 400
        private const val JPEG_QUALITY = 90

        fun testPhoto(): EncodedImage {
            val bitmap = Bitmap.createBitmap(PHOTO_WIDTH, PHOTO_HEIGHT, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.LTGRAY)
            val bytes =
                ByteArrayOutputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)
                    it.toByteArray()
                }
            return EncodedImage(bytes, PHOTO_WIDTH, PHOTO_HEIGHT)
        }
    }
}
