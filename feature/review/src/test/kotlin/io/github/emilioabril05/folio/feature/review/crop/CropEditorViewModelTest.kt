package io.github.emilioabril05.folio.feature.review.crop

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.domain.UpdatePageCrop
import io.github.emilioabril05.folio.core.model.Corner
import io.github.emilioabril05.folio.core.model.CropQuad
import io.github.emilioabril05.folio.core.model.Document
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.NormalizedPoint
import io.github.emilioabril05.folio.core.model.Page
import io.github.emilioabril05.folio.core.model.PageId
import io.github.emilioabril05.folio.core.testing.FakeDocumentRepository
import io.github.emilioabril05.folio.core.testing.FakePageRenderer
import io.github.emilioabril05.folio.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class CropEditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val pageId = PageId("page-1")
    private val savedCrop =
        CropQuad(
            topLeft = NormalizedPoint(0.1f, 0.1f),
            topRight = NormalizedPoint(0.9f, 0.1f),
            bottomRight = NormalizedPoint(0.9f, 0.9f),
            bottomLeft = NormalizedPoint(0.1f, 0.9f),
        )
    private val repository = FakeDocumentRepository()

    private suspend fun viewModel(renderer: FakePageRenderer = FakePageRenderer()): CropEditorViewModel {
        repository.create(
            Document(
                id = DocumentId("doc-1"),
                title = "Escaneo",
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
                pages = listOf(Page(pageId, position = 0, crop = savedCrop)),
            ),
        )
        val route = SavedStateHandle(mapOf("documentId" to "doc-1", "pageId" to "page-1"))
        return CropEditorViewModel(route, repository, renderer, UpdatePageCrop(repository))
    }

    private val CropEditorUiState.crop get() = (this as CropEditorUiState.Editing).crop

    @Test
    fun `abre la foto con el recorte guardado de la página`() =
        runTest {
            val state = viewModel().uiState.value as CropEditorUiState.Editing

            assertEquals(savedCrop, state.crop)
            assertEquals(300, state.photo.width)
            assertFalse(state.canUndo)
        }

    @Test
    fun `mover una esquina cambia el recorte y se puede deshacer`() =
        runTest {
            val viewModel = viewModel()
            val moved = NormalizedPoint(0.2f, 0.15f)

            viewModel.onEvent(CropEditorEvent.MoveCorner(Corner.TopLeft, moved))

            assertEquals(moved, viewModel.uiState.value.crop.topLeft)
            assertTrue((viewModel.uiState.value as CropEditorUiState.Editing).canUndo)
        }

    @Test
    fun `un movimiento que cruza las esquinas se ignora`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onEvent(CropEditorEvent.MoveCorner(Corner.TopLeft, NormalizedPoint(0.95f, 0.95f)))

            assertEquals(savedCrop, viewModel.uiState.value.crop)
        }

    @Test
    fun `una esquina arrastrada fuera de la foto queda en el borde`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onEvent(CropEditorEvent.MoveCorner(Corner.TopLeft, NormalizedPoint(-0.3f, -0.2f)))

            assertEquals(NormalizedPoint(0f, 0f), viewModel.uiState.value.crop.topLeft)
        }

    @Test
    fun `un arrastre completo se deshace de una vez`() =
        runTest {
            val viewModel = viewModel()
            listOf(0.12f, 0.15f, 0.2f).forEach { x ->
                viewModel.onEvent(CropEditorEvent.DragCorner(Corner.TopLeft, NormalizedPoint(x, x)))
            }
            viewModel.onEvent(CropEditorEvent.DragFinished)

            viewModel.onEvent(CropEditorEvent.Undo)

            assertEquals(savedCrop, viewModel.uiState.value.crop)
            assertFalse((viewModel.uiState.value as CropEditorUiState.Editing).canUndo)
        }

    @Test
    fun `deshacer y rehacer recorren los cambios`() =
        runTest {
            val viewModel = viewModel()
            val moved = NormalizedPoint(0.2f, 0.2f)
            viewModel.onEvent(CropEditorEvent.MoveCorner(Corner.TopLeft, moved))

            viewModel.onEvent(CropEditorEvent.Undo)
            assertEquals(savedCrop, viewModel.uiState.value.crop)

            viewModel.onEvent(CropEditorEvent.Redo)
            assertEquals(moved, viewModel.uiState.value.crop.topLeft)
        }

    @Test
    fun `restablecer vuelve a la foto completa y se puede deshacer`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onEvent(CropEditorEvent.Reset)
            assertEquals(CropQuad.Full, viewModel.uiState.value.crop)

            viewModel.onEvent(CropEditorEvent.Undo)
            assertEquals(savedCrop, viewModel.uiState.value.crop)
        }

    @Test
    fun `hecho guarda el recorte y avisa que terminó`() =
        runTest {
            val viewModel = viewModel()
            val moved = NormalizedPoint(0.2f, 0.2f)
            viewModel.onEvent(CropEditorEvent.MoveCorner(Corner.TopLeft, moved))

            viewModel.effects.test {
                viewModel.onEvent(CropEditorEvent.Done)
                assertEquals(CropEditorEffect.Finished, awaitItem())
            }
            assertEquals(moved, repository.crops.getValue(pageId).topLeft)
        }

    @Test
    fun `si la foto no se puede leer lo explica`() =
        runTest {
            val viewModel = viewModel(FakePageRenderer(failWith = StorageError.SourceUnreadable))

            assertEquals(CropEditorUiState.Failed, viewModel.uiState.value)
        }
}
