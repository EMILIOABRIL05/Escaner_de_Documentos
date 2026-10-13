package io.github.emilioabril05.folio.core.data

import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import io.github.emilioabril05.folio.core.database.DocumentDao
import io.github.emilioabril05.folio.core.database.PageCropUpdate
import io.github.emilioabril05.folio.core.domain.DocumentRepository
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.domain.StorageFailure
import io.github.emilioabril05.folio.core.model.CropQuad
import io.github.emilioabril05.folio.core.model.Document
import io.github.emilioabril05.folio.core.model.Page
import io.github.emilioabril05.folio.core.model.PageId
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Guarda los metadatos de los documentos en Room. */
class RoomDocumentRepository
    @Inject
    constructor(
        private val dao: DocumentDao,
    ) : DocumentRepository {
        override suspend fun create(document: Document): Result<Unit> =
            guarded { dao.insertDocumentWithPages(document.toEntity(), document.toPageEntities()) }

        override suspend fun findPage(pageId: PageId): Result<Page?> = guarded { dao.findPage(pageId.value)?.toPage() }

        override suspend fun updatePageCrop(
            pageId: PageId,
            crop: CropQuad,
        ): Result<Unit> =
            guarded {
                val updatedRows = dao.updatePageCrop(PageCropUpdate(pageId.value, crop.toColumns()))
                if (updatedRows == 0) throw PageNotFoundException()
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
            } catch (_: PageNotFoundException) {
                Result.failure(StorageFailure(StorageError.Unknown))
            }

        private class PageNotFoundException : Exception()
    }
