package io.github.emilioabril05.folio.core.testing

import io.github.emilioabril05.folio.core.domain.DocumentRepository
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.domain.StorageFailure
import io.github.emilioabril05.folio.core.model.Document

/** Repositorio en memoria. Con [failWith], cada creación falla con ese error. */
class FakeDocumentRepository(
    private val failWith: StorageError? = null,
    private val onCreate: (Document) -> Unit = {},
) : DocumentRepository {
    private val stored = mutableListOf<Document>()

    /** Documentos creados. */
    val documents: List<Document> get() = stored.toList()

    override suspend fun create(document: Document): Result<Unit> {
        onCreate(document)
        if (failWith != null) return Result.failure(StorageFailure(failWith))
        stored += document
        return Result.success(Unit)
    }
}
