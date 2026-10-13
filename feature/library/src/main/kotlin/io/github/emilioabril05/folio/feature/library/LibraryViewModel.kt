package io.github.emilioabril05.folio.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.emilioabril05.folio.core.domain.DeleteForever
import io.github.emilioabril05.folio.core.domain.DocumentSummary
import io.github.emilioabril05.folio.core.domain.DuplicateDocument
import io.github.emilioabril05.folio.core.domain.EncodedImage
import io.github.emilioabril05.folio.core.domain.LibraryRepository
import io.github.emilioabril05.folio.core.domain.MoveToTrash
import io.github.emilioabril05.folio.core.domain.PageRenderer
import io.github.emilioabril05.folio.core.domain.RenameDocument
import io.github.emilioabril05.folio.core.model.DocumentId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

// Lado mayor de las miniaturas de la lista (56 dp a densidad alta, con margen).
private const val THUMBNAIL_SIDE_PX = 240

/** Destino de navegación de la biblioteca; con [trash], muestra la papelera. */
@Serializable
data class LibraryDestination(
    val trash: Boolean = false,
)

/** Un documento en la lista, con la miniatura de su primera página (o `null` mientras se dibuja). */
data class LibraryItem(
    val summary: DocumentSummary,
    val thumbnail: EncodedImage?,
)

/** Estado de la biblioteca. */
data class LibraryUiState(
    val isTrash: Boolean = false,
    val query: String = "",
    val items: List<LibraryItem> = emptyList(),
    val loading: Boolean = true,
    val lastTrashed: DocumentId? = null,
    val editFailed: Boolean = false,
)

/** Lo que el usuario hace en la biblioteca. */
sealed interface LibraryEvent {
    /** Cambia el texto de búsqueda. */
    data class QueryChanged(
        val query: String,
    ) : LibraryEvent

    /** Abre un documento. */
    data class Open(
        val documentId: DocumentId,
    ) : LibraryEvent

    /** Cambia el nombre de un documento. */
    data class Rename(
        val documentId: DocumentId,
        val title: String,
    ) : LibraryEvent

    /** Duplica un documento con el título [title]. */
    data class Duplicate(
        val documentId: DocumentId,
        val title: String,
    ) : LibraryEvent

    /** Manda un documento a la papelera. */
    data class Trash(
        val documentId: DocumentId,
    ) : LibraryEvent

    /** Saca un documento de la papelera. */
    data class Restore(
        val documentId: DocumentId,
    ) : LibraryEvent

    /** El aviso de "Deshacer" se cerró. */
    data object UndoDismissed : LibraryEvent

    /** Borra un documento para siempre (solo desde la papelera). */
    data class DeleteForever(
        val documentId: DocumentId,
    ) : LibraryEvent
}

/** Efectos de una sola vez. */
sealed interface LibraryEffect {
    /** Abrir la revisión de un documento. */
    data class OpenDocument(
        val documentId: DocumentId,
    ) : LibraryEffect
}

/** Casos de uso que cambian un documento de la biblioteca. */
class LibraryEdits
    @Inject
    constructor(
        val renameDocument: RenameDocument,
        val duplicateDocument: DuplicateDocument,
        val moveToTrash: MoveToTrash,
        val deleteForever: DeleteForever,
    )

/** Biblioteca y papelera: lista con búsqueda por título, renombrar, duplicar, papelera de 30 días. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val library: LibraryRepository,
        private val renderer: PageRenderer,
        private val edits: LibraryEdits,
    ) : ViewModel() {
        private val isTrash = savedStateHandle.toRoute<LibraryDestination>().trash
        private val query = MutableStateFlow("")
        private val state = MutableStateFlow(LibraryUiState(isTrash = isTrash))
        private val effectChannel = Channel<LibraryEffect>(Channel.BUFFERED)
        private val thumbnails = mutableMapOf<DocumentSummary, EncodedImage>()

        /** Estado actual. */
        val uiState: StateFlow<LibraryUiState> = state.asStateFlow()

        /** Efectos de una sola vez. */
        val effects: Flow<LibraryEffect> = effectChannel.receiveAsFlow()

        init {
            viewModelScope.launch { edits.deleteForever.purgeExpired() }
            viewModelScope.launch {
                query.flatMapLatest { library.observeDocuments(it, isTrash) }.collect { summaries ->
                    state.update { current ->
                        current.copy(loading = false, items = summaries.map { LibraryItem(it, thumbnails[it]) })
                    }
                    summaries.forEach { renderThumbnail(it) }
                }
            }
        }

        /** Punto de entrada único de los eventos. */
        fun onEvent(event: LibraryEvent) {
            when (event) {
                is LibraryEvent.QueryChanged -> {
                    state.update { it.copy(query = event.query) }
                    query.value = event.query.trim()
                }

                is LibraryEvent.Open -> {
                    send(LibraryEffect.OpenDocument(event.documentId))
                }

                is LibraryEvent.Rename -> {
                    edit { edits.renameDocument(event.documentId, event.title) }
                }

                is LibraryEvent.Duplicate -> {
                    edit { edits.duplicateDocument(event.documentId, event.title) }
                }

                is LibraryEvent.Trash -> {
                    state.update { it.copy(lastTrashed = event.documentId) }
                    edit { edits.moveToTrash(event.documentId) }
                }

                is LibraryEvent.Restore -> {
                    state.update { it.copy(lastTrashed = null) }
                    edit { edits.moveToTrash.restore(event.documentId) }
                }

                LibraryEvent.UndoDismissed -> {
                    state.update { it.copy(lastTrashed = null) }
                }

                is LibraryEvent.DeleteForever -> {
                    edit { edits.deleteForever(event.documentId) }
                }
            }
        }

        private fun edit(action: suspend () -> io.github.emilioabril05.folio.core.domain.EditResult) {
            viewModelScope.launch {
                val failed = action() is io.github.emilioabril05.folio.core.domain.EditResult.Failed
                state.update { it.copy(editFailed = failed) }
            }
        }

        private fun renderThumbnail(summary: DocumentSummary) {
            val page = summary.firstPage ?: return
            if (summary in thumbnails) return
            viewModelScope.launch {
                renderer.page(summary.id, page, THUMBNAIL_SIDE_PX).onSuccess { image ->
                    thumbnails[summary] = image
                    state.update { current ->
                        current.copy(
                            items = current.items.map { if (it.summary == summary) it.copy(thumbnail = image) else it },
                        )
                    }
                }
            }
        }

        private fun send(effect: LibraryEffect) {
            viewModelScope.launch { effectChannel.send(effect) }
        }
    }
