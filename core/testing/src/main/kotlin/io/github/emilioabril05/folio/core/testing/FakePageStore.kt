package io.github.emilioabril05.folio.core.testing

import io.github.emilioabril05.folio.core.domain.PageSource
import io.github.emilioabril05.folio.core.domain.PageStore
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.domain.StorageFailure
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.PageId

/** Almacén de páginas en memoria. Con [failWith], falla el guardado número [failOnSave] (desde 1). */
class FakePageStore(
    private val failWith: StorageError? = null,
    private val failOnSave: Int = 1,
) : PageStore {
    private val savedOriginals = mutableMapOf<PageId, Pair<DocumentId, PageSource>>()
    private var saves = 0

    /** Originales guardados y aún no borrados. */
    val originals: Map<PageId, Pair<DocumentId, PageSource>> get() = savedOriginals.toMap()

    override suspend fun saveOriginal(
        documentId: DocumentId,
        pageId: PageId,
        source: PageSource,
    ): Result<Unit> {
        saves++
        if (failWith != null && saves == failOnSave) return Result.failure(StorageFailure(failWith))
        savedOriginals[pageId] = documentId to source
        return Result.success(Unit)
    }

    override suspend fun deleteDocument(documentId: DocumentId): Result<Unit> {
        savedOriginals.entries.removeAll { it.value.first == documentId }
        return Result.success(Unit)
    }

    override suspend fun deleteOriginal(
        documentId: DocumentId,
        pageId: PageId,
    ): Result<Unit> {
        savedOriginals.remove(pageId)
        return Result.success(Unit)
    }
}
