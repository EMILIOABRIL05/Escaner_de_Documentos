package io.github.emilioabril05.folio.core.storage

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.emilioabril05.folio.core.domain.PageSource
import io.github.emilioabril05.folio.core.domain.PageStore
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.domain.StorageFailure
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.PageId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

/** Guarda los originales como archivos JPEG en el almacenamiento privado de la app. */
class FilePageStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : PageStore {
        override suspend fun saveOriginal(
            documentId: DocumentId,
            pageId: PageId,
            source: PageSource,
        ): Result<Unit> =
            withContext(ioDispatcher) {
                val target = originalFile(documentId, pageId)
                val input = openSource(source)
                if (input == null) {
                    Result.failure(StorageFailure(StorageError.SourceUnreadable))
                } else {
                    copyOrCleanUp(input, target)
                }
            }

        override suspend fun deleteDocument(documentId: DocumentId): Result<Unit> =
            withContext(ioDispatcher) {
                if (documentDirectory(documentId).deleteRecursively()) {
                    Result.success(Unit)
                } else {
                    Result.failure(StorageFailure(StorageError.Unknown))
                }
            }

        private fun openSource(source: PageSource): InputStream? =
            try {
                context.contentResolver.openInputStream(Uri.parse(source.value))
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }

        internal fun copyOrCleanUp(
            input: InputStream,
            target: File,
        ): Result<Unit> =
            try {
                target.parentFile?.mkdirs()
                input.use { stream -> target.outputStream().use { stream.copyTo(it) } }
                Result.success(Unit)
            } catch (error: IOException) {
                target.delete()
                Result.failure(StorageFailure(error.toStorageError()))
            }

        private fun documentDirectory(documentId: DocumentId) =
            File(File(context.filesDir, DOCUMENTS_DIRECTORY), documentId.value)

        private fun originalFile(
            documentId: DocumentId,
            pageId: PageId,
        ) = File(File(documentDirectory(documentId), ORIGINAL_DIRECTORY), pageId.value + ORIGINAL_EXTENSION)

        private companion object {
            const val DOCUMENTS_DIRECTORY = "documents"
            const val ORIGINAL_DIRECTORY = "original"
            const val ORIGINAL_EXTENSION = ".jpg"
        }
    }
