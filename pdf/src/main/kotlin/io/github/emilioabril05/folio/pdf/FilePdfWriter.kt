package io.github.emilioabril05.folio.pdf

import android.content.Context
import androidx.core.content.FileProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.emilioabril05.folio.core.domain.EncodedImage
import io.github.emilioabril05.folio.core.domain.ExportedPdf
import io.github.emilioabril05.folio.core.domain.PdfWriter
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.domain.StorageFailure
import io.github.emilioabril05.folio.core.model.DocumentId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

private const val EXPORTS_DIR = "exports"
private const val FILE_PROVIDER_SUFFIX = ".files"
private val NO_SPACE = Regex("ENOSPC|No space left", RegexOption.IGNORE_CASE)

/**
 * Escribe el PDF en `files/exports/{docId}/` (plan §6.4) y lo comparte como URI `content://` de FileProvider.
 * Cada exportación reemplaza la anterior del mismo documento.
 */
class FilePdfWriter
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PdfWriter {
        override suspend fun write(
            documentId: DocumentId,
            fileName: String,
            pageCount: Int,
            dpi: Int,
            renderPage: suspend (index: Int) -> Result<EncodedImage>,
        ): Result<ExportedPdf> =
            withContext(Dispatchers.IO) {
                val folder = File(File(context.filesDir, EXPORTS_DIR), documentId.value)
                folder.deleteRecursively()
                folder.mkdirs()
                val file = File(folder, fileName)
                try {
                    file.outputStream().buffered().use { output ->
                        val pdf = JpegPdfStream(output, pageCount)
                        for (index in 0 until pageCount) {
                            val page = renderPage(index).getOrThrow()
                            pdf.addPage(page.bytes, page.width, page.height, dpi)
                        }
                        pdf.finish()
                    }
                    val uri = FileProvider.getUriForFile(context, context.packageName + FILE_PROVIDER_SUFFIX, file)
                    Result.success(ExportedPdf(uri.toString(), fileName, file.length(), pageCount))
                } catch (error: IOException) {
                    file.delete()
                    Result.failure(StorageFailure(error.toStorageError()))
                } catch (failure: StorageFailure) {
                    file.delete()
                    Result.failure(failure)
                }
            }

        private fun IOException.toStorageError(): StorageError =
            if (NO_SPACE.containsMatchIn(message.orEmpty())) StorageError.NotEnoughSpace else StorageError.Unknown
    }

/** Usa el escritor de PDF con JPEG incrustados. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PdfModule {
    @Binds
    abstract fun bindPdfWriter(writer: FilePdfWriter): PdfWriter
}
