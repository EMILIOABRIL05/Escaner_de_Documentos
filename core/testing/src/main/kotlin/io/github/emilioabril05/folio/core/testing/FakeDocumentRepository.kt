package io.github.emilioabril05.folio.core.testing

import io.github.emilioabril05.folio.core.domain.DocumentRepository
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.domain.StorageFailure
import io.github.emilioabril05.folio.core.model.CropQuad
import io.github.emilioabril05.folio.core.model.Document
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.Page
import io.github.emilioabril05.folio.core.model.PageFilter
import io.github.emilioabril05.folio.core.model.PageId
import io.github.emilioabril05.folio.core.model.Rotation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

/** Repositorio en memoria. Con [failWith], cada escritura falla con ese error. */
class FakeDocumentRepository(
    private val failWith: StorageError? = null,
    private val onCreate: (Document) -> Unit = {},
) : DocumentRepository {
    private val stored = MutableStateFlow<List<Document>>(emptyList())

    /** Documentos guardados, con sus cambios. */
    val documents: List<Document> get() = stored.value

    /** Recortes guardados por página. */
    val crops: Map<PageId, CropQuad>
        get() =
            stored.value
                .flatMap { it.pages }
                .filter { it.crop != CropQuad.Full }
                .associate { it.id to it.crop }

    override suspend fun create(document: Document): Result<Unit> {
        onCreate(document)
        return write { it + document }
    }

    override suspend fun findPage(pageId: PageId): Result<Page?> {
        if (failWith != null) return Result.failure(StorageFailure(failWith))
        return Result.success(stored.value.flatMap { it.pages }.firstOrNull { it.id == pageId })
    }

    override suspend fun updatePageCrop(
        pageId: PageId,
        crop: CropQuad,
    ): Result<Unit> = write { it.mapPage(pageId) { page -> page.copy(crop = crop) } }

    override fun observeDocument(documentId: DocumentId): Flow<Document?> =
        stored.map { documents -> documents.firstOrNull { it.id == documentId } }

    override suspend fun updatePageRotation(
        pageId: PageId,
        rotation: Rotation,
    ): Result<Unit> = write { it.mapPage(pageId) { page -> page.copy(rotation = rotation) } }

    override suspend fun updatePageFilter(
        pageIds: List<PageId>,
        filter: PageFilter,
    ): Result<Unit> =
        write { documents ->
            pageIds.fold(documents) { acc, id -> acc.mapPage(id) { it.copy(filter = filter) } }
        }

    override suspend fun reorderPages(
        documentId: DocumentId,
        pageIds: List<PageId>,
    ): Result<Unit> =
        write {
            it.mapDocument(documentId) { document ->
                val byId = document.pages.associateBy { page -> page.id }
                document.copy(pages = pageIds.mapNotNull { id -> byId[id] }.renumbered())
            }
        }

    override suspend fun deletePage(pageId: PageId): Result<Unit> =
        write { documents ->
            documents.map { document -> document.copy(pages = document.pages.filter { it.id != pageId }.renumbered()) }
        }

    override suspend fun addPages(
        documentId: DocumentId,
        pages: List<Page>,
        updatedAt: Instant,
    ): Result<Unit> =
        write {
            it.mapDocument(documentId) { document ->
                document.copy(pages = (document.pages + pages).renumbered(), updatedAt = updatedAt)
            }
        }

    private fun write(change: (List<Document>) -> List<Document>): Result<Unit> {
        if (failWith != null) return Result.failure(StorageFailure(failWith))
        stored.update(change)
        return Result.success(Unit)
    }
}

private fun List<Page>.renumbered(): List<Page> = mapIndexed { index, page -> page.copy(position = index) }

private fun List<Document>.mapPage(
    pageId: PageId,
    change: (Page) -> Page,
): List<Document> =
    map { document ->
        document.copy(
            pages =
                document.pages.map {
                    if (it.id ==
                        pageId
                    ) {
                        change(it)
                    } else {
                        it
                    }
                },
        )
    }

private fun List<Document>.mapDocument(
    documentId: DocumentId,
    change: (Document) -> Document,
): List<Document> = map { if (it.id == documentId) change(it) else it }
