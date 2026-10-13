package io.github.emilioabril05.folio.core.database

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Cantidad de páginas de un documento. */
data class PageCount(
    val documentId: String,
    val pageCount: Int,
)

/** Consultas y cambios de la biblioteca: lista, búsqueda, renombrar y papelera. */
@Dao
interface LibraryDao {
    /**
     * Documentos activos (o en la papelera si [trashed]) cuyo título contiene [query],
     * del más reciente al más viejo.
     */
    @Query(
        "SELECT * FROM documents WHERE title LIKE '%' || :query || '%' " +
            "AND ((:trashed AND trashedAtMillis IS NOT NULL) OR (NOT :trashed AND trashedAtMillis IS NULL)) " +
            "ORDER BY updatedAtMillis DESC",
    )
    fun observeDocuments(
        query: String,
        trashed: Boolean,
    ): Flow<List<DocumentEntity>>

    /** La primera página (posición 0) de cada documento. */
    @Query("SELECT * FROM pages WHERE position = 0")
    fun observeFirstPages(): Flow<List<PageEntity>>

    /** Cuántas páginas tiene cada documento. */
    @Query("SELECT documentId, COUNT(*) AS pageCount FROM pages GROUP BY documentId")
    fun observePageCounts(): Flow<List<PageCount>>

    @Query("UPDATE documents SET title = :title, updatedAtMillis = :updatedAtMillis WHERE id = :documentId")
    suspend fun rename(
        documentId: String,
        title: String,
        updatedAtMillis: Long,
    ): Int

    @Query("UPDATE documents SET trashedAtMillis = :trashedAtMillis WHERE id = :documentId")
    suspend fun setTrashedAt(
        documentId: String,
        trashedAtMillis: Long?,
    ): Int

    @Query("DELETE FROM documents WHERE id = :documentId")
    suspend fun deleteDocument(documentId: String): Int

    @Query("SELECT id FROM documents WHERE trashedAtMillis < :beforeMillis")
    suspend fun findTrashedBefore(beforeMillis: Long): List<String>
}
