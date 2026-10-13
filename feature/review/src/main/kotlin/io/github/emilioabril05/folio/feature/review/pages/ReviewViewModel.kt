package io.github.emilioabril05.folio.feature.review.pages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.emilioabril05.folio.core.domain.ApplyFilter
import io.github.emilioabril05.folio.core.domain.DeletePage
import io.github.emilioabril05.folio.core.domain.DocumentRepository
import io.github.emilioabril05.folio.core.domain.EditResult
import io.github.emilioabril05.folio.core.domain.EncodedImage
import io.github.emilioabril05.folio.core.domain.PageRenderer
import io.github.emilioabril05.folio.core.domain.ReorderPages
import io.github.emilioabril05.folio.core.domain.RotatePage
import io.github.emilioabril05.folio.core.model.Document
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.Page
import io.github.emilioabril05.folio.core.model.PageFilter
import io.github.emilioabril05.folio.core.model.PageId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

// Lado mayor de las miniaturas: nítidas en una cuadrícula de 2 columnas sin gastar memoria.
private const val THUMBNAIL_SIDE_PX = 480

/** Destino de navegación de la revisión de un documento. */
@Serializable
data class ReviewDestination(
    val documentId: String,
)

/** Una página en la cuadrícula, con su miniatura ya recortada y girada (o `null` mientras se dibuja). */
data class ReviewPage(
    val page: Page,
    val thumbnail: EncodedImage?,
)

/** Estado de la revisión. */
sealed interface ReviewUiState {
    /** Cargando el documento. */
    data object Loading : ReviewUiState

    /** El documento con sus páginas. */
    data class Ready(
        val title: String,
        val pages: List<ReviewPage>,
        val canUndoDelete: Boolean = false,
        val editFailed: Boolean = false,
    ) : ReviewUiState {
        /** El filtro común a todas las páginas, o `null` si son distintos. */
        val commonFilter: PageFilter? get() = pages.map { it.page.filter }.distinct().singleOrNull()
    }

    /** El documento ya no existe. */
    data object Missing : ReviewUiState
}

/** Lo que el usuario hace en la revisión. */
sealed interface ReviewEvent {
    /** Gira la página 90° en sentido horario. */
    data class Rotate(
        val pageId: PageId,
    ) : ReviewEvent

    /** Mueve la página de [fromIndex] a [toIndex] (arrastrando o con TalkBack). */
    data class Move(
        val fromIndex: Int,
        val toIndex: Int,
    ) : ReviewEvent

    /** Quita la página; se puede deshacer hasta que se confirme. */
    data class Delete(
        val pageId: PageId,
    ) : ReviewEvent

    /** Recupera la página recién quitada. */
    data object UndoDelete : ReviewEvent

    /** El aviso de "Deshacer" se cerró: la página se borra de verdad. */
    data object DeleteConfirmed : ReviewEvent

    /** Abre el recorte de la página. */
    data class OpenPage(
        val pageId: PageId,
    ) : ReviewEvent

    /** Escanea más páginas para este documento. */
    data object AddPages : ReviewEvent

    /** Aplica [filter] a todas las páginas. */
    data class ApplyFilterToAll(
        val filter: PageFilter,
    ) : ReviewEvent

    /** Abre "Enviar PDF". */
    data object Export : ReviewEvent
}

/** Efectos de una sola vez. */
sealed interface ReviewEffect {
    /** Abrir el editor de recorte. */
    data class EditCrop(
        val documentId: DocumentId,
        val pageId: PageId,
    ) : ReviewEffect

    /** Abrir el escáner para agregar páginas a [documentId]. */
    data class AddPages(
        val documentId: DocumentId,
    ) : ReviewEffect

    /** Abrir "Enviar PDF" de [documentId]. */
    data class Export(
        val documentId: DocumentId,
    ) : ReviewEffect
}

/** Revisión de un documento: rotar, reordenar, quitar y agregar páginas. */
@HiltViewModel
class ReviewViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val documents: DocumentRepository,
        private val renderer: PageRenderer,
        private val rotatePage: RotatePage,
        private val reorderPages: ReorderPages,
        private val deletePage: DeletePage,
        private val applyFilter: ApplyFilter,
    ) : ViewModel() {
        private val documentId = DocumentId(savedStateHandle.toRoute<ReviewDestination>().documentId)
        private val state = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
        private val effectChannel = Channel<ReviewEffect>(Channel.BUFFERED)
        private val thumbnails = mutableMapOf<Page, EncodedImage>()
        private var latest: Document? = null
        private var order: List<PageId>? = null
        private var pendingDeletion: PageId? = null
        private var editFailed = false

        /** Estado actual de la revisión. */
        val uiState: StateFlow<ReviewUiState> = state.asStateFlow()

        /** Efectos de una sola vez. */
        val effects: Flow<ReviewEffect> = effectChannel.receiveAsFlow()

        init {
            viewModelScope.launch {
                documents.observeDocument(documentId).collect { document ->
                    latest = document
                    order = null
                    publish()
                    if (document == null) return@collect
                    document?.pages?.forEach { renderThumbnail(it) }
                }
            }
        }

        /** Punto de entrada único de los eventos. */
        fun onEvent(event: ReviewEvent) {
            when (event) {
                is ReviewEvent.Rotate -> edit { pageOf(event.pageId)?.let { rotatePage(it) } }
                is ReviewEvent.Move -> move(event.fromIndex, event.toIndex)
                is ReviewEvent.Delete -> delete(event.pageId)
                ReviewEvent.UndoDelete -> undoDelete()
                ReviewEvent.DeleteConfirmed -> confirmDelete()
                is ReviewEvent.OpenPage -> send(ReviewEffect.EditCrop(documentId, event.pageId))
                ReviewEvent.AddPages -> send(ReviewEffect.AddPages(documentId))
                is ReviewEvent.ApplyFilterToAll -> edit { applyFilter(visiblePages().map { it.id }, event.filter) }
                ReviewEvent.Export -> send(ReviewEffect.Export(documentId))
            }
        }

        private fun move(
            fromIndex: Int,
            toIndex: Int,
        ) {
            val visible = visiblePages().map { it.id }.toMutableList()
            if (fromIndex !in visible.indices || toIndex !in visible.indices || fromIndex == toIndex) return
            visible.add(toIndex, visible.removeAt(fromIndex))
            val hidden = listOfNotNull(pendingDeletion)
            order = visible + hidden
            publish()
            edit { reorderPages(documentId, visible + hidden) }
        }

        private fun delete(pageId: PageId) {
            confirmDelete()
            pendingDeletion = pageId
            publish()
        }

        private fun undoDelete() {
            pendingDeletion = null
            publish()
        }

        private fun confirmDelete() {
            val pageId = pendingDeletion ?: return
            pendingDeletion = null
            edit { deletePage(documentId, pageId) }
            publish()
        }

        private fun edit(action: suspend () -> Any?) {
            viewModelScope.launch {
                val result = action()
                editFailed = result is EditResult.Failed
                publish()
            }
        }

        private fun renderThumbnail(page: Page) {
            if (page in thumbnails) return
            viewModelScope.launch {
                renderer.page(documentId, page, THUMBNAIL_SIDE_PX).onSuccess {
                    thumbnails[page] = it
                    publish()
                }
            }
        }

        private fun pageOf(pageId: PageId): Page? = latest?.pages?.firstOrNull { it.id == pageId }

        private fun visiblePages(): List<Page> {
            val pages = latest?.pages.orEmpty()
            val ordered = order?.let { ids -> ids.mapNotNull { id -> pages.firstOrNull { it.id == id } } } ?: pages
            return ordered.filter { it.id != pendingDeletion }
        }

        private fun publish() {
            val document = latest
            state.value =
                if (document == null) {
                    ReviewUiState.Missing
                } else {
                    ReviewUiState.Ready(
                        title = document.title,
                        pages = visiblePages().map { ReviewPage(it, thumbnails[it]) },
                        canUndoDelete = pendingDeletion != null,
                        editFailed = editFailed,
                    )
                }
        }

        private fun send(effect: ReviewEffect) {
            viewModelScope.launch { effectChannel.send(effect) }
        }
    }
