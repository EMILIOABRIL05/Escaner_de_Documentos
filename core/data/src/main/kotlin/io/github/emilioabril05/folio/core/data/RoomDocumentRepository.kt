package io.github.emilioabril05.folio.core.data

import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import io.github.emilioabril05.folio.core.database.DocumentDao
import io.github.emilioabril05.folio.core.database.DocumentEntity
import io.github.emilioabril05.folio.core.database.PageEntity
import io.github.emilioabril05.folio.core.domain.DocumentRepository
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.domain.StorageFailure
import io.github.emilioabril05.folio.core.model.Document
import javax.inject.Inject

/** Guarda los metadatos de los documentos en Room. */
class RoomDocumentRepository
    @Inject
    constructor(
        private val dao: DocumentDao,
    ) : DocumentRepository {
        override suspend fun create(document: Document): Result<Unit> =
            try {
                dao.insertDocumentWithPages(document.toEntity(), document.toPageEntities())
                Result.success(Unit)
            } catch (_: SQLiteFullException) {
                Result.failure(StorageFailure(StorageError.NotEnoughSpace))
            } catch (_: SQLiteException) {
                Result.failure(StorageFailure(StorageError.Unknown))
            }

        private fun Document.toEntity() =
            DocumentEntity(
                id = id.value,
                title = title,
                createdAtMillis = createdAt.toEpochMilli(),
                updatedAtMillis = updatedAt.toEpochMilli(),
            )

        private fun Document.toPageEntities() =
            pages.map {
                PageEntity(id = it.id.value, documentId = id.value, position = it.position)
            }
    }
