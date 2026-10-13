package io.github.emilioabril05.folio.feature.export

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.emilioabril05.folio.core.domain.EstimatePdfSize
import io.github.emilioabril05.folio.core.domain.ExportDocument
import io.github.emilioabril05.folio.core.domain.ExportResult
import io.github.emilioabril05.folio.core.domain.ExportedPdf
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.ExportProfile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

/** Destino de navegación de "Enviar PDF". */
@Serializable
data class ExportDestination(
    val documentId: String,
)

/** A dónde va el PDF. */
enum class ExportTarget { WhatsApp, Share, Save }

/** Por qué no se pudo armar el PDF. */
enum class ExportFailure { NotEnoughSpace, Empty, Unknown }

/** Estado de "Enviar PDF". */
data class ExportUiState(
    val profile: ExportProfile = ExportProfile.Balanced,
    val estimates: Map<ExportProfile, Long> = emptyMap(),
    val exporting: Boolean = false,
    val failure: ExportFailure? = null,
)

/** Lo que el usuario hace en "Enviar PDF". */
sealed interface ExportEvent {
    /** Elige la calidad del PDF. */
    data class SelectProfile(
        val profile: ExportProfile,
    ) : ExportEvent

    /** Arma el PDF y lo envía a [target]. */
    data class Send(
        val target: ExportTarget,
    ) : ExportEvent
}

/** Efectos de una sola vez. */
sealed interface ExportEffect {
    /** El PDF está listo para entregarse a [target]. */
    data class Deliver(
        val pdf: ExportedPdf,
        val target: ExportTarget,
    ) : ExportEffect
}

/** Arma el PDF con el perfil elegido y muestra el peso estimado de cada perfil (RF-06, RF-08). */
@HiltViewModel
class ExportViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val exportDocument: ExportDocument,
        private val estimatePdfSize: EstimatePdfSize,
    ) : ViewModel() {
        private val documentId = DocumentId(savedStateHandle.toRoute<ExportDestination>().documentId)
        private val state = MutableStateFlow(ExportUiState())
        private val effectChannel = Channel<ExportEffect>(Channel.BUFFERED)

        /** Estado actual. */
        val uiState: StateFlow<ExportUiState> = state.asStateFlow()

        /** Efectos de una sola vez. */
        val effects: Flow<ExportEffect> = effectChannel.receiveAsFlow()

        init {
            viewModelScope.launch {
                ExportProfile.entries.forEach { profile ->
                    estimatePdfSize(documentId, profile)?.let { bytes ->
                        state.update { it.copy(estimates = it.estimates + (profile to bytes)) }
                    }
                }
            }
        }

        /** Punto de entrada único de los eventos. */
        fun onEvent(event: ExportEvent) {
            when (event) {
                is ExportEvent.SelectProfile -> state.update { it.copy(profile = event.profile, failure = null) }
                is ExportEvent.Send -> send(event.target)
            }
        }

        private fun send(target: ExportTarget) {
            if (state.value.exporting) return
            state.update { it.copy(exporting = true, failure = null) }
            viewModelScope.launch {
                val result = exportDocument(documentId, state.value.profile)
                state.update { it.copy(exporting = false, failure = result.toFailure()) }
                if (result is ExportResult.Ready) effectChannel.send(ExportEffect.Deliver(result.pdf, target))
            }
        }

        private fun ExportResult.toFailure(): ExportFailure? =
            when (this) {
                is ExportResult.Ready -> {
                    null
                }

                ExportResult.NothingToExport -> {
                    ExportFailure.Empty
                }

                is ExportResult.Failed -> {
                    if (error == StorageError.NotEnoughSpace) ExportFailure.NotEnoughSpace else ExportFailure.Unknown
                }
            }
    }
