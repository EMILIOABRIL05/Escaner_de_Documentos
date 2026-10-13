package io.github.emilioabril05.folio.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Acceso a los documentos y, por herencia de [PageDao], a sus páginas. */
@Dao
interface DocumentDao :
    PageDao,
    PageAppearanceDao {
    @Insert
    suspend fun insertDocument(document: DocumentEntity)

    /** Guarda el documento y todas sus páginas en una sola transacción. */
    @Transaction
    suspend fun insertDocumentWithPages(
        document: DocumentEntity,
        pages: List<PageEntity>,
    ) {
        insertDocument(document)
        insertPages(pages)
    }

    @Query("SELECT * FROM documents WHERE id = :documentId")
    suspend fun findDocument(documentId: String): DocumentEntity?

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY position")
    suspend fun findPagesInOrder(documentId: String): List<PageEntity>

    /** El documento con sus páginas ordenadas por posición, o nulo si no existe. */
    @Transaction
    suspend fun findDocumentWithPages(documentId: String): DocumentWithPages? =
        findDocument(documentId)?.let { DocumentWithPages(it, findPagesInOrder(documentId)) }

    /** El documento, o nulo si no existe; emite de nuevo con cada cambio. */
    @Query("SELECT * FROM documents WHERE id = :documentId")
    fun observeDocument(documentId: String): Flow<DocumentEntity?>

    @Query("UPDATE documents SET updatedAtMillis = :updatedAtMillis WHERE id = :documentId")
    suspend fun updateDocumentUpdatedAt(
        documentId: String,
        updatedAtMillis: Long,
    ): Int

    /**
     * Agrega [pages] después de la última página del documento, en su orden, y actualiza la fecha de cambio.
     * Devuelve cuántos documentos actualizó (0 si no existe).
     */
    @Transaction
    suspend fun appendPages(
        documentId: String,
        pages: List<PageEntity>,
        updatedAtMillis: Long,
    ): Int {
        val firstPosition = (findLastPosition(documentId) ?: -1) + 1
        insertPages(pages.mapIndexed { index, page -> page.copy(position = firstPosition + index) })
        return updateDocumentUpdatedAt(documentId, updatedAtMillis)
    }

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun count(): Int
}
