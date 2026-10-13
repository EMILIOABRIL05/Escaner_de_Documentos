package io.github.emilioabril05.folio.core.storage

import android.content.Context
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.PageId
import java.io.File

private const val DOCUMENTS_DIR = "documents"
private const val ORIGINALS_DIR = "original"
private const val ORIGINAL_EXTENSION = ".jpg"

/** Carpeta privada con todos los archivos del documento [documentId] (plan §6.4). */
internal fun documentDirectory(
    context: Context,
    documentId: DocumentId,
): File = File(File(context.filesDir, DOCUMENTS_DIR), documentId.value)

/** Foto original de la página [pageId]: `files/documents/{docId}/original/{pageId}.jpg`. Nunca se modifica. */
internal fun originalFile(
    context: Context,
    documentId: DocumentId,
    pageId: PageId,
): File = File(File(documentDirectory(context, documentId), ORIGINALS_DIR), pageId.value + ORIGINAL_EXTENSION)
