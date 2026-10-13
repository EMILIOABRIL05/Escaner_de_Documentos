package io.github.emilioabril05.folio.core.testing

import io.github.emilioabril05.folio.core.domain.DocumentRepository
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.domain.StorageFailure
import io.github.emilioabril05.folio.core.model.CropQuad
import io.github.emilioabril05.folio.core.model.Document
import io.github.emilioabril05.folio.core.model.Page
import io.github.emilioabril05.folio.core.model.PageId

/** Repositorio en memoria. Con [failWith], cada creación falla con ese error. */
class FakeDocumentRepository(
    private val failWith: StorageError? = null,
    private val onCreate: (Document) -> Unit = {},
) : DocumentRepository {
    private val stored = mutableListOf<Document>()
    private val savedCrops = mutableMapOf<PageId, CropQuad>()

    /** Documentos creados. */
    val documents: List<Document> get() = stored.toList()

    /** Recortes guardados por página. */
    val crops: Map<PageId, CropQuad> get() = savedCrops.toMap()

    override suspend fun create(document: Document): Result<Unit> {
        onCreate(document)
        if (failWith != null) return Result.failure(StorageFailure(failWith))
        stored += document
        return Result.success(Unit)
    }

    override suspend fun findPage(pageId: PageId): Result<Page?> {
        if (failWith != null) return Result.failure(StorageFailure(failWith))
        val page = stored.flatMap { it.pages }.firstOrNull { it.id == pageId }
        return Result.success(page?.copy(crop = savedCrops[pageId] ?: page.crop))
    }

    override suspend fun updatePageCrop(
        pageId: PageId,
        crop: CropQuad,
    ): Result<Unit> {
        if (failWith != null) return Result.failure(StorageFailure(failWith))
        savedCrops[pageId] = crop
        return Result.success(Unit)
    }
}
