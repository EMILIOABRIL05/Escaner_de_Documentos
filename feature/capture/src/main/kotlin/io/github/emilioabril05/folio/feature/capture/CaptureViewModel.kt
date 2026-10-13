package io.github.emilioabril05.folio.feature.capture

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.emilioabril05.folio.core.domain.AddPagesToDocument
import io.github.emilioabril05.folio.core.domain.CreateDocumentFromScan
import io.github.emilioabril05.folio.core.domain.CreateDocumentResult
import io.github.emilioabril05.folio.core.domain.PageSource
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.PageId
import io.github.emilioabril05.folio.engine.api.ScanError
import io.github.emilioabril05.folio.engine.api.ScanOutcome
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Por qué falló la captura, en términos que la pantalla sabe explicar. */
enum class CaptureFailure { ScannerUnavailable, NotEnoughSpace, UnreadablePage, Unknown }

/** Estado de la pantalla de captura. */
sealed interface CaptureUiState {
    /** Lista para escanear. */
    data object Idle : CaptureUiState

    /** Guardando las páginas recién escaneadas. */
    data class Saving(
        val pageCount: Int,
    ) : CaptureUiState

    /** Las páginas quedaron guardadas en el teléfono. */
    data class Saved(
        val pageCount: Int,
    ) : CaptureUiState

    /** Algo falló; la pantalla muestra causa y solución. */
    data class Failed(
        val failure: CaptureFailure,
    ) : CaptureUiState
}

/** Lo que el usuario o el escáner le comunican a la pantalla. */
sealed interface CaptureEvent {
    /** El escáner devolvió un resultado. */
    data class ScanFinished(
        val outcome: ScanOutcome,
    ) : CaptureEvent

    /** El escáner no se pudo abrir. */
    data class ScanFailed(
        val reason: ScanError,
    ) : CaptureEvent

    /** El usuario descartó el mensaje de error. */
    data object MessageDismissed : CaptureEvent

    /** La cámara del sistema guardó una foto en [photo] (alternativa sin escáner). */
    data class PhotoTaken(
        val photo: Uri,
    ) : CaptureEvent

    /** El usuario eligió [photos] en el selector de fotos del sistema, en ese orden. */
    data class PhotosImported(
        val photos: List<Uri>,
    ) : CaptureEvent
}

/** Efectos de una sola vez. */
sealed interface CaptureEffect {
    /** Abrir la revisión del documento y, encima, el recorte de la página recién guardada. */
    data class EditCrop(
        val documentId: DocumentId,
        val pageId: PageId,
    ) : CaptureEffect

    /** Abrir la revisión del documento recién creado. */
    data class OpenReview(
        val documentId: DocumentId,
    ) : CaptureEffect

    /** Las páginas se agregaron al documento que se estaba revisando: volver a él. */
    data object PagesAdded : CaptureEffect
}

// Clave del argumento de navegación con el documento al que se agregan páginas (ver CaptureDestination).
internal const val TARGET_DOCUMENT_KEY = "documentId"

/** Recibe el resultado del escáner y guarda las páginas en un documento nuevo o en uno existente. */
@HiltViewModel
class CaptureViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val createDocument: CreateDocumentFromScan,
        private val addPages: AddPagesToDocument,
    ) : ViewModel() {
        // Con un documento de destino, las páginas se agregan a él en lugar de crear uno nuevo.
        private val targetDocument =
            savedStateHandle.get<String>(TARGET_DOCUMENT_KEY)?.let(::DocumentId)

        private val state = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)

        private val effectChannel = Channel<CaptureEffect>(Channel.BUFFERED)

        /** Estado actual de la pantalla. */
        val uiState: StateFlow<CaptureUiState> = state.asStateFlow()

        /** Efectos de una sola vez, como abrir el recorte. */
        val effects: Flow<CaptureEffect> = effectChannel.receiveAsFlow()

        /** Punto de entrada único de los eventos de la pantalla. */
        fun onEvent(event: CaptureEvent) {
            when (event) {
                is CaptureEvent.ScanFinished -> {
                    onScanFinished(event.outcome)
                }

                is CaptureEvent.ScanFailed -> {
                    state.value = CaptureUiState.Failed(event.reason.toFailure())
                }

                CaptureEvent.MessageDismissed -> {
                    state.value = CaptureUiState.Idle
                }

                is CaptureEvent.PhotoTaken -> {
                    savePhotos(listOf(event.photo))
                }

                is CaptureEvent.PhotosImported -> {
                    savePhotos(event.photos)
                }
            }
        }

        private fun onScanFinished(outcome: ScanOutcome) {
            when (outcome) {
                is ScanOutcome.Success -> save(outcome.pages)
                ScanOutcome.Cancelled -> state.value = CaptureUiState.Idle
                is ScanOutcome.Failed -> state.value = CaptureUiState.Failed(outcome.reason.toFailure())
            }
        }

        // Una sola foto casi nunca viene recortada: se abre también el editor.
        private fun savePhotos(photos: List<Uri>) {
            if (photos.isEmpty()) return
            save(photos)
        }

        private fun save(pages: List<Uri>) {
            state.value = CaptureUiState.Saving(pages.size)
            viewModelScope.launch {
                val sources = pages.map { PageSource(it.toString()) }
                val target = targetDocument
                val result = if (target == null) createDocument(sources) else addPages(target, sources)
                when (result) {
                    is CreateDocumentResult.Created -> {
                        state.value = CaptureUiState.Saved(result.pageCount)
                        effectChannel.send(nextStep(result, isNewDocument = target == null))
                    }

                    CreateDocumentResult.NoPages -> {
                        state.value = CaptureUiState.Idle
                    }

                    is CreateDocumentResult.Failed -> {
                        state.value = CaptureUiState.Failed(result.error.toFailure())
                    }
                }
            }
        }

        private fun nextStep(
            created: CreateDocumentResult.Created,
            isNewDocument: Boolean,
        ): CaptureEffect =
            when {
                !isNewDocument -> CaptureEffect.PagesAdded
                created.pageCount == 1 -> CaptureEffect.EditCrop(created.documentId, created.pageIds.first())
                else -> CaptureEffect.OpenReview(created.documentId)
            }

        private fun ScanError.toFailure(): CaptureFailure =
            when (this) {
                ScanError.Unavailable -> CaptureFailure.ScannerUnavailable
                ScanError.Unknown -> CaptureFailure.Unknown
            }

        private fun StorageError.toFailure(): CaptureFailure =
            when (this) {
                StorageError.NotEnoughSpace -> CaptureFailure.NotEnoughSpace
                StorageError.SourceUnreadable -> CaptureFailure.UnreadablePage
                StorageError.Unknown -> CaptureFailure.Unknown
            }
    }
