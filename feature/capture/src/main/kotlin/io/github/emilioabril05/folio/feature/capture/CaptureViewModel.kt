package io.github.emilioabril05.folio.feature.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
}

/** Efectos de una sola vez. */
sealed interface CaptureEffect {
    /** Abrir el editor de recorte de la página recién guardada. */
    data class EditCrop(
        val documentId: DocumentId,
        val pageId: PageId,
    ) : CaptureEffect
}

/** Recibe el resultado del escáner y guarda las páginas como un documento nuevo. */
@HiltViewModel
class CaptureViewModel
    @Inject
    constructor(
        private val createDocument: CreateDocumentFromScan,
    ) : ViewModel() {
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
                    save(listOf(event.photo)) { created ->
                        effectChannel.send(CaptureEffect.EditCrop(created.documentId, created.pageIds.first()))
                    }
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

        private fun save(
            pages: List<Uri>,
            onCreated: suspend (CreateDocumentResult.Created) -> Unit = {},
        ) {
            state.value = CaptureUiState.Saving(pages.size)
            viewModelScope.launch {
                when (val result = createDocument(pages.map { PageSource(it.toString()) })) {
                    is CreateDocumentResult.Created -> {
                        state.value = CaptureUiState.Saved(result.pageCount)
                        onCreated(result)
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
