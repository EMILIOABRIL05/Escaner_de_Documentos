package io.github.emilioabril05.folio.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.emilioabril05.folio.core.domain.CreateDocumentFromScan
import io.github.emilioabril05.folio.core.domain.CreateDocumentResult
import io.github.emilioabril05.folio.core.domain.PageSource
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.engine.api.ScanError
import io.github.emilioabril05.folio.engine.api.ScanOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
}

/** Recibe el resultado del escáner y guarda las páginas como un documento nuevo. */
@HiltViewModel
class CaptureViewModel
    @Inject
    constructor(
        private val createDocument: CreateDocumentFromScan,
    ) : ViewModel() {
        private val state = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)

        /** Estado actual de la pantalla. */
        val uiState: StateFlow<CaptureUiState> = state.asStateFlow()

        /** Punto de entrada único de los eventos de la pantalla. */
        fun onEvent(event: CaptureEvent) {
            when (event) {
                is CaptureEvent.ScanFinished -> onScanFinished(event.outcome)
                is CaptureEvent.ScanFailed -> state.value = CaptureUiState.Failed(event.reason.toFailure())
                CaptureEvent.MessageDismissed -> state.value = CaptureUiState.Idle
            }
        }

        private fun onScanFinished(outcome: ScanOutcome) {
            when (outcome) {
                is ScanOutcome.Success -> save(outcome)
                ScanOutcome.Cancelled -> state.value = CaptureUiState.Idle
                is ScanOutcome.Failed -> state.value = CaptureUiState.Failed(outcome.reason.toFailure())
            }
        }

        private fun save(outcome: ScanOutcome.Success) {
            state.value = CaptureUiState.Saving(outcome.pages.size)
            viewModelScope.launch {
                val result = createDocument(outcome.pages.map { PageSource(it.toString()) })
                state.value =
                    when (result) {
                        is CreateDocumentResult.Created -> CaptureUiState.Saved(result.pageCount)
                        CreateDocumentResult.NoPages -> CaptureUiState.Idle
                        is CreateDocumentResult.Failed -> CaptureUiState.Failed(result.error.toFailure())
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
