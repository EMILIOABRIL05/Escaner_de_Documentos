package io.github.emilioabril05.folio.feature.review.crop

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.emilioabril05.folio.core.domain.DocumentRepository
import io.github.emilioabril05.folio.core.domain.EncodedImage
import io.github.emilioabril05.folio.core.domain.PageRenderer
import io.github.emilioabril05.folio.core.domain.UpdateCropResult
import io.github.emilioabril05.folio.core.domain.UpdatePageCrop
import io.github.emilioabril05.folio.core.domain.validate
import io.github.emilioabril05.folio.core.model.Corner
import io.github.emilioabril05.folio.core.model.CropQuad
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.NormalizedPoint
import io.github.emilioabril05.folio.core.model.PageId
import kotlinx.coroutines.Job
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

// Lado mayor de las imágenes del editor: suficiente para una pantalla de teléfono sin saturar la memoria.
private const val PHOTO_SIDE_PX = 1600
private const val PREVIEW_SIDE_PX = 512
private const val MIN_COORDINATE = 0f
private const val MAX_COORDINATE = 1f

/** Destino de navegación del editor de recorte de una página. */
@Serializable
data class CropEditorDestination(
    val documentId: String,
    val pageId: String,
)

/** Estado del editor de recorte. */
sealed interface CropEditorUiState {
    /** Cargando la foto. */
    data object Loading : CropEditorUiState

    /** Editando el recorte de [photo]. */
    data class Editing(
        val photo: EncodedImage,
        val crop: CropQuad,
        val preview: EncodedImage? = null,
        val canUndo: Boolean = false,
        val canRedo: Boolean = false,
        val saving: Boolean = false,
        val saveFailed: Boolean = false,
    ) : CropEditorUiState

    /** La foto no se pudo abrir. */
    data object Failed : CropEditorUiState
}

/** Lo que el usuario hace en el editor. */
sealed interface CropEditorEvent {
    /** Mueve una esquina de una vez (por ejemplo, con una acción de TalkBack). Se puede deshacer. */
    data class MoveCorner(
        val corner: Corner,
        val point: NormalizedPoint,
    ) : CropEditorEvent

    /** La esquina sigue al dedo durante un arrastre. */
    data class DragCorner(
        val corner: Corner,
        val point: NormalizedPoint,
    ) : CropEditorEvent

    /** Terminó el arrastre: todo el gesto se deshace de una vez. */
    data object DragFinished : CropEditorEvent

    /** Deshace el último cambio. */
    data object Undo : CropEditorEvent

    /** Rehace el último cambio deshecho. */
    data object Redo : CropEditorEvent

    /** Vuelve a la foto completa. */
    data object Reset : CropEditorEvent

    /** Guarda el recorte y cierra el editor. */
    data object Done : CropEditorEvent
}

/** Efectos de una sola vez. */
sealed interface CropEditorEffect {
    /** El recorte quedó guardado; el editor se puede cerrar. */
    data object Finished : CropEditorEffect
}

/** Edita el recorte de una página sin tocar su foto original, con deshacer y rehacer. */
@HiltViewModel
class CropEditorViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val documents: DocumentRepository,
        private val renderer: PageRenderer,
        private val updatePageCrop: UpdatePageCrop,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<CropEditorDestination>()
        private val documentId = DocumentId(route.documentId)
        private val pageId = PageId(route.pageId)

        private val state = MutableStateFlow<CropEditorUiState>(CropEditorUiState.Loading)
        private val effectChannel = Channel<CropEditorEffect>(Channel.BUFFERED)
        private val history = CropHistory()
        private var previewJob: Job? = null

        /** Estado actual del editor. */
        val uiState: StateFlow<CropEditorUiState> = state.asStateFlow()

        /** Efectos de una sola vez, como cerrar el editor. */
        val effects: Flow<CropEditorEffect> = effectChannel.receiveAsFlow()

        init {
            viewModelScope.launch { load() }
        }

        /** Punto de entrada único de los eventos del editor. */
        fun onEvent(event: CropEditorEvent) {
            val editing = state.value as? CropEditorUiState.Editing ?: return
            when (event) {
                is CropEditorEvent.MoveCorner -> {
                    moveCorner(editing, event.corner, event.point, recordHistory = true)
                }

                is CropEditorEvent.DragCorner -> {
                    history.beginDrag(editing.crop)
                    moveCorner(editing, event.corner, event.point, recordHistory = false)
                }

                CropEditorEvent.DragFinished -> {
                    if (history.endDrag(editing.crop)) publish(editing.crop)
                }

                CropEditorEvent.Undo -> {
                    history.undo(editing.crop)?.let(::publish)
                }

                CropEditorEvent.Redo -> {
                    history.redo(editing.crop)?.let(::publish)
                }

                CropEditorEvent.Reset -> {
                    applyCrop(editing, CropQuad.Full, recordHistory = true)
                }

                CropEditorEvent.Done -> {
                    save(editing)
                }
            }
        }

        private suspend fun load() {
            val page = documents.findPage(pageId).getOrNull()
            val photo = renderer.original(documentId, pageId, PHOTO_SIDE_PX).getOrNull()
            if (page == null || photo == null) {
                state.value = CropEditorUiState.Failed
                return
            }
            state.value = CropEditorUiState.Editing(photo = photo, crop = page.crop)
            refreshPreview(page.crop)
        }

        private fun moveCorner(
            editing: CropEditorUiState.Editing,
            corner: Corner,
            point: NormalizedPoint,
            recordHistory: Boolean,
        ) {
            val candidate = editing.crop.with(corner, point.insidePhoto())
            if (candidate.validate() == null) applyCrop(editing, candidate, recordHistory)
        }

        private fun applyCrop(
            editing: CropEditorUiState.Editing,
            crop: CropQuad,
            recordHistory: Boolean,
        ) {
            if (crop == editing.crop) return
            if (recordHistory) history.record(editing.crop)
            publish(crop)
        }

        private fun publish(crop: CropQuad) {
            state.update { current ->
                (current as? CropEditorUiState.Editing)?.copy(
                    crop = crop,
                    canUndo = history.canUndo,
                    canRedo = history.canRedo,
                    saveFailed = false,
                ) ?: current
            }
            if (!history.isDragging) refreshPreview(crop)
        }

        private fun refreshPreview(crop: CropQuad) {
            previewJob?.cancel()
            previewJob =
                viewModelScope.launch {
                    val preview = renderer.corrected(documentId, pageId, crop, PREVIEW_SIDE_PX).getOrNull()
                    state.update { (it as? CropEditorUiState.Editing)?.copy(preview = preview) ?: it }
                }
        }

        private fun save(editing: CropEditorUiState.Editing) {
            if (editing.saving) return
            state.value = editing.copy(saving = true, saveFailed = false)
            viewModelScope.launch {
                when (updatePageCrop(pageId, editing.crop)) {
                    UpdateCropResult.Saved -> {
                        effectChannel.send(CropEditorEffect.Finished)
                    }

                    is UpdateCropResult.Invalid, is UpdateCropResult.Failed -> {
                        state.update {
                            (it as? CropEditorUiState.Editing)?.copy(saving = false, saveFailed = true) ?: it
                        }
                    }
                }
            }
        }

        private fun NormalizedPoint.insidePhoto(): NormalizedPoint =
            NormalizedPoint(x.coerceIn(MIN_COORDINATE, MAX_COORDINATE), y.coerceIn(MIN_COORDINATE, MAX_COORDINATE))
    }
