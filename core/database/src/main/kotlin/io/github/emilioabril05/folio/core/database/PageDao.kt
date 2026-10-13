package io.github.emilioabril05.folio.core.database

import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Consultas y cambios sobre las páginas; [DocumentDao] las hereda. */
interface PageDao {
    @Insert
    suspend fun insertPages(pages: List<PageEntity>)

    @Query("SELECT * FROM pages WHERE id = :pageId")
    suspend fun findPage(pageId: String): PageEntity?

    /** Las páginas del documento ordenadas por posición; emite de nuevo con cada cambio. */
    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY position")
    fun observePagesInOrder(documentId: String): Flow<List<PageEntity>>

    @Query("UPDATE pages SET position = :position WHERE id = :pageId AND documentId = :documentId")
    suspend fun updatePagePosition(
        documentId: String,
        pageId: String,
        position: Int,
    )

    /** Pone las posiciones 0, 1, 2… a las páginas en el orden de [pageIds], en una sola transacción. */
    @Transaction
    suspend fun reorderPages(
        documentId: String,
        pageIds: List<String>,
    ) {
        pageIds.forEachIndexed { index, pageId -> updatePagePosition(documentId, pageId, index) }
    }

    @Query("DELETE FROM pages WHERE id = :pageId")
    suspend fun deletePage(pageId: String): Int

    @Query("UPDATE pages SET position = position - 1 WHERE documentId = :documentId AND position > :position")
    suspend fun shiftPagesUp(
        documentId: String,
        position: Int,
    )

    /** Borra la página y sube una posición a las siguientes; devuelve cuántas filas borró. */
    @Transaction
    suspend fun deletePageAndShift(pageId: String): Int {
        val page = findPage(pageId) ?: return 0
        val deleted = deletePage(pageId)
        shiftPagesUp(page.documentId, page.position)
        return deleted
    }

    @Query("SELECT MAX(position) FROM pages WHERE documentId = :documentId")
    suspend fun findLastPosition(documentId: String): Int?
}
