package io.github.emilioabril05.folio.core.database

import androidx.room.Query
import androidx.room.Update

/** Cambios de aspecto de las páginas (recorte, giro y filtro); [DocumentDao] los hereda. */
interface PageAppearanceDao {
    /** Guarda solo el recorte de la página; devuelve cuántas filas cambió (0 si no existe). */
    @Update(entity = PageEntity::class)
    suspend fun updatePageCrop(update: PageCropUpdate): Int

    /** Guarda solo el giro de la página; devuelve cuántas filas cambió (0 si no existe). */
    @Query("UPDATE pages SET rotationDegrees = :rotationDegrees WHERE id = :pageId")
    suspend fun updatePageRotation(
        pageId: String,
        rotationDegrees: Int,
    ): Int

    /** Guarda el filtro (por nombre) en todas las páginas de [pageIds] a la vez; devuelve cuántas filas cambió. */
    @Query("UPDATE pages SET filter = :filter WHERE id IN (:pageIds)")
    suspend fun updatePagesFilter(
        pageIds: List<String>,
        filter: String,
    ): Int
}
