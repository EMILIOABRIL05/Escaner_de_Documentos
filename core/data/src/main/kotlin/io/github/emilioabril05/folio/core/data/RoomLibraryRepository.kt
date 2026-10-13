package io.github.emilioabril05.folio.core.data

import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import io.github.emilioabril05.folio.core.database.DocumentEntity
import io.github.emilioabril05.folio.core.database.LibraryDao
import io.github.emilioabril05.folio.core.database.PageEntity
import io.github.emilioabril05.folio.core.domain.DocumentSummary
import io.github.emilioabril05.folio.core.domain.LibraryRepository
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.domain.StorageFailure
import io.github.emilioabril05.folio.core.model.DocumentId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Lista y administra los documentos de la biblioteca en Room. */
class RoomLibraryRepository
    @Inject
    constructor(
        private val dao: LibraryDao,
    ) : LibraryRepository {
        override fun observeDocuments(
            query: String,
            trashed: Boolean,
        ): Flow<List<DocumentSummary>> =
            combine(
                dao.observeDocuments(query, trashed),
                dao.observeFirstPages(),
                dao.observePageCounts(),
            ) { documents, firstPages, counts ->
                val firstPageByDocument = firstPages.associateBy(PageEntity::documentId)
                val countByDocument = counts.associate { it.documentId to it.pageCount }
                documents.map { it.toSummary(firstPageByDocument[it.id], countByDocument[it.id] ?: 0) }
            }

        override suspend fun rename(
            documentId: DocumentId,
            title: String,
            updatedAt: Instant,
        ): Result<Unit> = guarded { requireUpdated(dao.rename(documentId.value, title, updatedAt.toEpochMilli())) }

        override suspend fun setTrashed(
            documentId: DocumentId,
            trashedAt: Instant?,
        ): Result<Unit> = guarded { requireUpdated(dao.setTrashedAt(documentId.value, trashedAt?.toEpochMilli())) }

        override suspend fun deleteForever(documentId: DocumentId): Result<Unit> =
            guarded { requireUpdated(dao.deleteDocument(documentId.value)) }

        override suspend fun trashedBefore(before: Instant): List<DocumentId> =
            try {
                dao.findTrashedBefore(before.toEpochMilli()).map(::DocumentId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: SQLiteException) {
                emptyList()
            }

        private fun DocumentEntity.toSummary(
            firstPage: PageEntity?,
            pageCount: Int,
        ) = DocumentSummary(
            id = DocumentId(id),
            title = title,
            updatedAt = Instant.ofEpochMilli(updatedAtMillis),
            pageCount = pageCount,
            firstPage = firstPage?.toPage(),
            trashedAt = trashedAtMillis?.let(Instant::ofEpochMilli),
        )

        private fun requireUpdated(rows: Int) {
            if (rows == 0) throw DocumentNotFoundException()
        }

        private suspend fun <T> guarded(block: suspend () -> T): Result<T> =
            try {
                Result.success(block())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: SQLiteFullException) {
                Result.failure(StorageFailure(StorageError.NotEnoughSpace))
            } catch (_: SQLiteException) {
                Result.failure(StorageFailure(StorageError.Unknown))
            } catch (_: DocumentNotFoundException) {
                Result.failure(StorageFailure(StorageError.Unknown))
            }

        private class DocumentNotFoundException : Exception()
    }
