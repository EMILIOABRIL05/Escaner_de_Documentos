package io.github.emilioabril05.folio.feature.capture

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.emilioabril05.folio.core.domain.AddPagesToDocument
import io.github.emilioabril05.folio.core.domain.CreateDocumentFromScan
import io.github.emilioabril05.folio.core.domain.DocumentTitleFormatter
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.PageId
import io.github.emilioabril05.folio.core.testing.FakeDocumentRepository
import io.github.emilioabril05.folio.core.testing.FakePageStore
import io.github.emilioabril05.folio.core.testing.MainDispatcherRule
import io.github.emilioabril05.folio.core.testing.SequentialIdProvider
import io.github.emilioabril05.folio.engine.api.ScanError
import io.github.emilioabril05.folio.engine.api.ScanOutcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class CaptureViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val pages =
        listOf(Uri.parse("content://scan/1"), Uri.parse("content://scan/2"), Uri.parse("content://scan/3"))
    private val repository = FakeDocumentRepository()

    private fun viewModel(store: FakePageStore = FakePageStore()) =
        CaptureViewModel(
            SavedStateHandle(),
            CreateDocumentFromScan(
                pageStore = store,
                documents = repository,
                ids = SequentialIdProvider(),
                titles = DocumentTitleFormatter { "Escaneo $it" },
                clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
            ),
            AddPagesToDocument(store, repository, SequentialIdProvider(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)),
        )

    @Test
    fun `empieza listo para escanear`() {
        assertEquals(CaptureUiState.Idle, viewModel().uiState.value)
    }

    @Test
    fun `al terminar el escaneo guarda las páginas e informa cuántas`() =
        runTest {
            val viewModel = viewModel()

            viewModel.uiState.test {
                assertEquals(CaptureUiState.Idle, awaitItem())
                viewModel.onEvent(CaptureEvent.ScanFinished(ScanOutcome.Success(pages)))
                assertEquals(CaptureUiState.Saving(pageCount = 3), awaitItem())
                assertEquals(CaptureUiState.Saved(pageCount = 3), awaitItem())
            }
            assertEquals(
                3,
                repository.documents
                    .single()
                    .pages.size,
            )
        }

    @Test
    fun `si el usuario cancela vuelve a estar listo sin guardar nada`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onEvent(CaptureEvent.ScanFinished(ScanOutcome.Cancelled))

            assertEquals(CaptureUiState.Idle, viewModel.uiState.value)
            assertEquals(emptyList<Any>(), repository.documents)
        }

    @Test
    fun `si el escáner no existe en el teléfono lo explica`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onEvent(CaptureEvent.ScanFailed(ScanError.Unavailable))

            assertEquals(CaptureUiState.Failed(CaptureFailure.ScannerUnavailable), viewModel.uiState.value)
        }

    @Test
    fun `si falta espacio lo explica y no deja el documento a medias`() =
        runTest {
            val viewModel = viewModel(FakePageStore(failWith = StorageError.NotEnoughSpace))

            viewModel.onEvent(CaptureEvent.ScanFinished(ScanOutcome.Success(pages)))

            assertEquals(CaptureUiState.Failed(CaptureFailure.NotEnoughSpace), viewModel.uiState.value)
            assertEquals(emptyList<Any>(), repository.documents)
        }

    @Test
    fun `una foto de la cámara del sistema se guarda y abre el recorte`() =
        runTest {
            val viewModel = viewModel()
            val photo = Uri.parse("content://folio.files/captures/foto.jpg")

            viewModel.effects.test {
                viewModel.onEvent(CaptureEvent.PhotoTaken(photo))

                assertEquals(CaptureEffect.EditCrop(DocumentId("doc-1"), PageId("page-1")), awaitItem())
            }
            assertEquals(CaptureUiState.Saved(pageCount = 1), viewModel.uiState.value)
            assertEquals(
                1,
                repository.documents
                    .single()
                    .pages.size,
            )
        }

    @Test
    fun `importar varias fotos de la galería las guarda en el orden elegido`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onEvent(CaptureEvent.PhotosImported(pages))

            assertEquals(CaptureUiState.Saved(pageCount = 3), viewModel.uiState.value)
            assertEquals(
                listOf(0, 1, 2),
                repository.documents
                    .single()
                    .pages
                    .map { it.position },
            )
        }

    @Test
    fun `importar una sola foto abre el recorte`() =
        runTest {
            val viewModel = viewModel()

            viewModel.effects.test {
                viewModel.onEvent(CaptureEvent.PhotosImported(pages.take(1)))

                assertEquals(CaptureEffect.EditCrop(DocumentId("doc-1"), PageId("page-1")), awaitItem())
            }
        }

    @Test
    fun `cerrar el selector sin elegir fotos no cambia nada`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onEvent(CaptureEvent.PhotosImported(emptyList()))

            assertEquals(CaptureUiState.Idle, viewModel.uiState.value)
            assertEquals(emptyList<Any>(), repository.documents)
        }

    @Test
    fun `una sola página escaneada va directo a la revisión sin abrir el recorte`() =
        runTest {
            val viewModel = viewModel()

            viewModel.effects.test {
                viewModel.onEvent(CaptureEvent.ScanFinished(ScanOutcome.Success(pages.take(1))))

                assertEquals(CaptureEffect.OpenReview(DocumentId("doc-1")), awaitItem())
            }
        }

    @Test
    fun `al cerrar el mensaje vuelve a estar listo`() =
        runTest {
            val viewModel = viewModel()
            viewModel.onEvent(CaptureEvent.ScanFailed(ScanError.Unknown))

            viewModel.onEvent(CaptureEvent.MessageDismissed)

            assertEquals(CaptureUiState.Idle, viewModel.uiState.value)
        }
}
