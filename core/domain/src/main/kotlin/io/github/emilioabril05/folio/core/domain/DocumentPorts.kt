package io.github.emilioabril05.folio.core.domain

import io.github.emilioabril05.folio.core.model.CropQuad
import io.github.emilioabril05.folio.core.model.Document
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.ExportProfile
import io.github.emilioabril05.folio.core.model.Page
import io.github.emilioabril05.folio.core.model.PageFilter
import io.github.emilioabril05.folio.core.model.PageId
import io.github.emilioabril05.folio.core.model.Rotation
import kotlinx.coroutines.flow.Flow
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

    /** Copia el original de la página [fromPage] de [fromDocument] como original de [toPage] en [toDocument]. */
    suspend fun copyOriginal(
        fromDocument: DocumentId,
        fromPage: PageId,
        toDocument: DocumentId,
        toPage: PageId,
    ): Result<Unit>

    /** Borra la foto original de la página [pageId]. */
    suspend fun deleteOriginal(
        documentId: DocumentId,
        pageId: PageId,
    ): Result<Unit>
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

    /** El documento [documentId] con sus páginas en orden; emite de nuevo con cada cambio, o `null` si no existe. */
    fun observeDocument(documentId: DocumentId): Flow<Document?>

    /** Guarda el giro [rotation] de la página [pageId]. */
    suspend fun updatePageRotation(
        pageId: PageId,
        rotation: Rotation,
    ): Result<Unit>

    /** Guarda el filtro [filter] en las páginas [pageIds]. */
    suspend fun updatePageFilter(
        pageIds: List<PageId>,
        filter: PageFilter,
    ): Result<Unit>

    /** Ordena las páginas del documento como en [pageIds]; sus posiciones pasan a ser 0, 1, 2… */
    suspend fun reorderPages(
        documentId: DocumentId,
        pageIds: List<PageId>,
    ): Result<Unit>

    /** Quita la página [pageId]; las páginas siguientes suben una posición. */
    suspend fun deletePage(pageId: PageId): Result<Unit>

    /** Agrega [pages] al final del documento [documentId], en su orden, y actualiza su fecha de cambio. */
    suspend fun addPages(
        documentId: DocumentId,
        pages: List<Page>,
        updatedAt: Instant,
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

    /** La página con su recorte y su giro aplicados, con su lado mayor de [maxSidePx] como máximo. */
    suspend fun page(
        documentId: DocumentId,
        page: Page,
        maxSidePx: Int,
    ): Result<EncodedImage>

    /** La página lista para el PDF con la calidad de [profile]: recorte, giro, filtro y compresión del perfil. */
    suspend fun export(
        documentId: DocumentId,
        page: Page,
        profile: ExportProfile,
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

/** Resultado de un cambio sobre las páginas de un documento. */
sealed interface EditResult {
    /** El cambio quedó guardado. */
    data object Done : EditResult

    /** No se pudo guardar. */
    data class Failed(
        val error: StorageError,
    ) : EditResult
}

/** Convierte un [Result] de almacenamiento en un [EditResult]. */
fun Result<Unit>.toEditResult(): EditResult =
    fold(
        onSuccess = { EditResult.Done },
        onFailure = { EditResult.Failed((it as? StorageFailure)?.error ?: StorageError.Unknown) },
    )

/** PDF listo para compartir: [shareUri] es una URI `content://` con permiso de lectura temporal. */
data class ExportedPdf(
    val shareUri: String,
    val fileName: String,
    val sizeBytes: Long,
    val pageCount: Int,
)

/** Escribe un PDF página por página, sin tener más de una página en memoria (RNF-04). */
interface PdfWriter {
    /**
     * Escribe el PDF de [documentId] con el nombre [fileName], pidiendo cada una de las [pageCount] páginas a
     * [renderPage] en orden. Las imágenes van a [dpi] puntos por pulgada.
     */
    suspend fun write(
        documentId: DocumentId,
        fileName: String,
        pageCount: Int,
        dpi: Int,
        renderPage: suspend (index: Int) -> Result<EncodedImage>,
    ): Result<ExportedPdf>
}
