package io.github.emilioabril05.folio.core.data

import io.github.emilioabril05.folio.core.database.CropColumns
import io.github.emilioabril05.folio.core.database.DocumentEntity
import io.github.emilioabril05.folio.core.database.PageEntity
import io.github.emilioabril05.folio.core.database.PointColumns
import io.github.emilioabril05.folio.core.model.CropQuad
import io.github.emilioabril05.folio.core.model.Document
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.NormalizedPoint
import io.github.emilioabril05.folio.core.model.Page
import io.github.emilioabril05.folio.core.model.PageFilter
import io.github.emilioabril05.folio.core.model.PageId
import io.github.emilioabril05.folio.core.model.Rotation
import java.time.Instant

internal fun Document.toEntity() =
    DocumentEntity(
        id = id.value,
        title = title,
        createdAtMillis = createdAt.toEpochMilli(),
        updatedAtMillis = updatedAt.toEpochMilli(),
    )

internal fun Document.toPageEntities() = pages.map { it.toEntity(id.value) }

internal fun Page.toEntity(documentId: String) =
    PageEntity(
        id = id.value,
        documentId = documentId,
        position = position,
        crop = crop.toColumns(),
        rotationDegrees = rotation.degrees,
        filter = filter.name,
    )

internal fun PageEntity.toPage() =
    Page(
        id = PageId(id),
        position = position,
        crop = crop.toCropQuad(),
        rotation = rotationDegrees.toRotation(),
        filter = filter.toPageFilter(),
    )

// Un nombre de filtro desconocido se trata como el filtro por defecto.
private fun String.toPageFilter() = PageFilter.entries.firstOrNull { it.name == this } ?: PageFilter.Document

// Un valor guardado que no sea múltiplo de 90° se trata como sin giro.
private fun Int.toRotation() = Rotation.entries.firstOrNull { it.degrees == this } ?: Rotation.None

internal fun CropQuad.toColumns() =
    CropColumns(
        topLeft = topLeft.toColumns(),
        topRight = topRight.toColumns(),
        bottomRight = bottomRight.toColumns(),
        bottomLeft = bottomLeft.toColumns(),
    )

private fun CropColumns.toCropQuad() =
    CropQuad(
        topLeft = topLeft.toPoint(),
        topRight = topRight.toPoint(),
        bottomRight = bottomRight.toPoint(),
        bottomLeft = bottomLeft.toPoint(),
    )

private fun NormalizedPoint.toColumns() = PointColumns(x, y)

private fun PointColumns.toPoint() = NormalizedPoint(x, y)
