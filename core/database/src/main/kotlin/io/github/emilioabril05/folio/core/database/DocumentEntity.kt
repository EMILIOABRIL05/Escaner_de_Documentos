package io.github.emilioabril05.folio.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Fila de un documento escaneado. */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val trashedAtMillis: Long? = null,
)
