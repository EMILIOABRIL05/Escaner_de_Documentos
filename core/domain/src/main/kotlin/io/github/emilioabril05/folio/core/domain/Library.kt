package io.github.emilioabril05.folio.core.domain

import io.github.emilioabril05.folio.core.model.Document
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/** Días que un documento pasa en la papelera antes de borrarse para siempre (plan §2.3, regla 6). */
const val TRASH_RETENTION_DAYS = 30L

/** Resumen de un documento para la biblioteca. */
data class DocumentSummary(
    val id: DocumentId,
    val title: String,
    val updatedAt: Instant,
    val pageCount: Int,
    val firstPage: Page?,
    val trashedAt: Instant?,
)

/** Biblioteca de documentos: listar, buscar, renombrar, duplicar, papelera. */
interface LibraryRepository {
    /**
     * Documentos activos (o en la papelera si [trashed]) cuyo título contiene [query],
     * del más reciente al más viejo.
     */
    fun observeDocuments(
        query: String,
        trashed: Boolean,
    ): Flow<List<DocumentSummary>>

    /** Cambia el título de [documentId]. */
    suspend fun rename(
        documentId: DocumentId,
        title: String,
        updatedAt: Instant,
    ): Result<Unit>

    /** Manda [documentId] a la papelera en [trashedAt], o lo saca si es `null`. */
    suspend fun setTrashed(
        documentId: DocumentId,
        trashedAt: Instant?,
    ): Result<Unit>

    /** Borra el registro de [documentId] y sus páginas. */
    suspend fun deleteForever(documentId: DocumentId): Result<Unit>

    /** Documentos que entraron a la papelera antes de [before]. */
    suspend fun trashedBefore(before: Instant): List<DocumentId>
}

/** Cambia el nombre de un documento; un nombre vacío se ignora. */
class RenameDocument
    @Inject
    constructor(
        private val library: LibraryRepository,
        private val clock: Clock,
    ) {
        /** Renombra [documentId] a [title]. */
        suspend operator fun invoke(
            documentId: DocumentId,
            title: String,
        ): EditResult {
            val clean = title.trim()
            if (clean.isEmpty()) return EditResult.Done
            return library.rename(documentId, clean, clock.instant()).toEditResult()
        }
    }

/** Duplica un documento con sus páginas y sus parámetros de edición; los originales se copian. */
class DuplicateDocument
    @Inject
    constructor(
        private val documents: DocumentRepository,
        private val pageStore: PageStore,
        private val ids: IdProvider,
        private val clock: Clock,
    ) {
        /** Crea una copia de [source] titulada [title]. */
        suspend operator fun invoke(
            source: DocumentId,
            title: String,
        ): EditResult {
            val original = documents.observeDocument(source).first() ?: return EditResult.Failed(StorageError.Unknown)
            val copyId = ids.newDocumentId()
            val pages = original.pages.map { it to it.copy(id = ids.newPageId()) }
            val copied =
                pages.fold(Result.success(Unit)) { previous, (from, to) ->
                    if (previous.isFailure) previous else pageStore.copyOriginal(source, from.id, copyId, to.id)
                }
            val now = clock.instant()
            val result = copied.andThen { documents.create(Document(copyId, title, now, now, pages.map { it.second })) }
            if (result.isFailure) pageStore.deleteDocument(copyId)
            return result.toEditResult()
        }

        private inline fun Result<Unit>.andThen(next: () -> Result<Unit>): Result<Unit> =
            if (isSuccess) next() else this
    }

/** Manda un documento a la papelera, o lo restaura. */
class MoveToTrash
    @Inject
    constructor(
        private val library: LibraryRepository,
        private val clock: Clock,
    ) {
        /** Envía [documentId] a la papelera. */
        suspend operator fun invoke(documentId: DocumentId): EditResult =
            library.setTrashed(documentId, clock.instant()).toEditResult()

        /** Saca [documentId] de la papelera. */
        suspend fun restore(documentId: DocumentId): EditResult = library.setTrashed(documentId, null).toEditResult()
    }

/** Borra para siempre: registro, originales, caché y PDFs del documento (plan §12.3). */
class DeleteForever
    @Inject
    constructor(
        private val library: LibraryRepository,
        private val pageStore: PageStore,
        private val clock: Clock,
    ) {
        /** Borra [documentId] para siempre. */
        suspend operator fun invoke(documentId: DocumentId): EditResult {
            val result = library.deleteForever(documentId)
            if (result.isSuccess) pageStore.deleteDocument(documentId)
            return result.toEditResult()
        }

        /** Borra los documentos que llevan más de [TRASH_RETENTION_DAYS] días en la papelera. */
        suspend fun purgeExpired() {
            val limit = clock.instant().minus(Duration.ofDays(TRASH_RETENTION_DAYS))
            library.trashedBefore(limit).forEach { invoke(it) }
        }
    }
