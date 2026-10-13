package io.github.emilioabril05.folio.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

/** Acceso a los documentos y a sus páginas. */
@Dao
interface DocumentDao {
    @Insert
    suspend fun insertDocument(document: DocumentEntity)

    @Insert
    suspend fun insertPages(pages: List<PageEntity>)

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

    @Query("SELECT * FROM pages WHERE id = :pageId")
    suspend fun findPage(pageId: String): PageEntity?

    /** Guarda solo el recorte de la página; devuelve cuántas filas cambió (0 si no existe). */
    @Update(entity = PageEntity::class)
    suspend fun updatePageCrop(update: PageCropUpdate): Int

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun count(): Int
}
