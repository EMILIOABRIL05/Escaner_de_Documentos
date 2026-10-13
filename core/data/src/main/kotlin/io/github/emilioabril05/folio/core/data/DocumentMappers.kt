package io.github.emilioabril05.folio.core.data

import io.github.emilioabril05.folio.core.database.DocumentEntity
import io.github.emilioabril05.folio.core.database.PageEntity
import io.github.emilioabril05.folio.core.model.Document
import io.github.emilioabril05.folio.core.model.DocumentId
import java.time.Instant

internal fun DocumentEntity.toDocument(pages: List<PageEntity>) =
    Document(
        id = DocumentId(id),
        title = title,
        createdAt = Instant.ofEpochMilli(createdAtMillis),
        updatedAt = Instant.ofEpochMilli(updatedAtMillis),
        pages = pages.map { it.toPage() },
    )
