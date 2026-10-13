package io.github.emilioabril05.folio.core.data

import io.github.emilioabril05.folio.core.database.CropColumns
import io.github.emilioabril05.folio.core.database.DocumentEntity
import io.github.emilioabril05.folio.core.database.PageEntity
import io.github.emilioabril05.folio.core.database.PointColumns
import io.github.emilioabril05.folio.core.model.CropQuad
import io.github.emilioabril05.folio.core.model.Document
import io.github.emilioabril05.folio.core.model.NormalizedPoint
import io.github.emilioabril05.folio.core.model.Page
import io.github.emilioabril05.folio.core.model.PageId

internal fun Document.toEntity() =
    DocumentEntity(
        id = id.value,
        title = title,
        createdAtMillis = createdAt.toEpochMilli(),
        updatedAtMillis = updatedAt.toEpochMilli(),
    )

internal fun Document.toPageEntities() =
    pages.map {
        PageEntity(id = it.id.value, documentId = id.value, position = it.position, crop = it.crop.toColumns())
    }

internal fun PageEntity.toPage() = Page(id = PageId(id), position = position, crop = crop.toCropQuad())

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
