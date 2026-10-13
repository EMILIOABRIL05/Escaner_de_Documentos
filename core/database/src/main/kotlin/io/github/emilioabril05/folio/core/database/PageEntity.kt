package io.github.emilioabril05.folio.core.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Un punto del recorte, en fracciones del ancho y del alto de la foto original. */
data class PointColumns(
    @ColumnInfo(name = "X") val x: Float,
    @ColumnInfo(name = "Y") val y: Float,
)

/** Las 4 esquinas del recorte de una página: columnas `cropTopLeftX`, `cropTopLeftY`, etc. */
data class CropColumns(
    @Embedded(prefix = "cropTopLeft") val topLeft: PointColumns,
    @Embedded(prefix = "cropTopRight") val topRight: PointColumns,
    @Embedded(prefix = "cropBottomRight") val bottomRight: PointColumns,
    @Embedded(prefix = "cropBottomLeft") val bottomLeft: PointColumns,
) {
    companion object {
        /** Sin recorte: la foto completa. */
        val Full =
            CropColumns(
                topLeft = PointColumns(0f, 0f),
                topRight = PointColumns(1f, 0f),
                bottomRight = PointColumns(1f, 1f),
                bottomLeft = PointColumns(0f, 1f),
            )
    }
}

/** Fila de una página; se borra junto con su documento. */
@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("documentId")],
)
data class PageEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val position: Int,
    @Embedded val crop: CropColumns = CropColumns.Full,
)

/** Solo el recorte de una página, para actualizarlo sin tocar el resto de la fila. */
data class PageCropUpdate(
    val id: String,
    @Embedded val crop: CropColumns,
)
