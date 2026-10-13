package io.github.emilioabril05.folio.core.domain

import io.github.emilioabril05.folio.core.model.CropQuad
import io.github.emilioabril05.folio.core.model.Document
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.Page
import io.github.emilioabril05.folio.core.model.PageId
import java.time.Instant

/** Origen de una página recién escaneada, opaco para el dominio (por ejemplo, una URI `content://`). */
@JvmInline
value class PageSource(
    val value: String,
)

/** Por qué no se pudo guardar algo en el teléfono. */
sealed interface StorageError {
    /** No queda espacio libre en el almacenamiento. */
    data object NotEnoughSpace : StorageError

    /** La imagen de origen ya no se puede leer. */
    data object SourceUnreadable : StorageError

    /** Cualquier otro fallo de disco o de base de datos. */
    data object Unknown : StorageError
}

/** Fallo de almacenamiento transportado dentro de un [Result]. */
class StorageFailure(
    val error: StorageError,
) : Exception(error.toString())

/** Guarda y borra las fotos originales de las páginas. Nunca modifica un original ya guardado. */
interface PageStore {
    /** Copia [source] como original de la página [pageId] del documento [documentId]. */
    suspend fun saveOriginal(
        documentId: DocumentId,
        pageId: PageId,
        source: PageSource,
    ): Result<Unit>

    /** Borra todos los archivos del documento [documentId]. */
    suspend fun deleteDocument(documentId: DocumentId): Result<Unit>
}

/** Metadatos de los documentos y de sus páginas. */
interface DocumentRepository {
    /** Registra un documento nuevo con sus páginas. */
    suspend fun create(document: Document): Result<Unit>

    /** La página [pageId], o `null` si no existe. */
    suspend fun findPage(pageId: PageId): Result<Page?>

    /** Guarda el recorte [crop] de la página [pageId] sin tocar su foto original. */
    suspend fun updatePageCrop(
        pageId: PageId,
        crop: CropQuad,
    ): Result<Unit>
}

/** Crea identificadores únicos. Inyectable para tener pruebas deterministas. */
interface IdProvider {
    /** Un identificador de documento nuevo. */
    fun newDocumentId(): DocumentId

    /** Un identificador de página nuevo. */
    fun newPageId(): PageId
}

/** Nombre automático de un documento, por ejemplo "Escaneo 2026-10-06 22.30". */
fun interface DocumentTitleFormatter {
    /** El nombre por defecto para un documento creado en [createdAt]. */
    fun format(createdAt: Instant): String
}

/** Imagen codificada en JPEG, lista para mostrarse. */
class EncodedImage(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
)

/** Dibuja las páginas a partir de su foto original, sin modificarla. */
interface PageRenderer {
    /** La foto original completa, reducida para que su lado mayor no pase de [maxSidePx]. */
    suspend fun original(
        documentId: DocumentId,
        pageId: PageId,
        maxSidePx: Int,
    ): Result<EncodedImage>

    /** La hoja recortada con [crop] y con la perspectiva corregida, con su lado mayor de [maxSidePx] como máximo. */
    suspend fun corrected(
        documentId: DocumentId,
        pageId: PageId,
        crop: CropQuad,
        maxSidePx: Int,
    ): Result<EncodedImage>
}

/** Lugar donde la cámara del sistema escribe una foto nueva (una URI `content://` propia de la app). */
fun interface PhotoCaptureTarget {
    /** Un destino nuevo y vacío para una foto. */
    fun newPhotoTarget(): PageSource
}
