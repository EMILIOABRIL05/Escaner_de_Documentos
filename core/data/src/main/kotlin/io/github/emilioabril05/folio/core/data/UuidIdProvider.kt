package io.github.emilioabril05.folio.core.data

import io.github.emilioabril05.folio.core.domain.IdProvider
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.PageId
import java.util.UUID
import javax.inject.Inject

/** Genera identificadores aleatorios con UUID. */
class UuidIdProvider
    @Inject
    constructor() : IdProvider {
        override fun newDocumentId(): DocumentId = DocumentId(UUID.randomUUID().toString())

        override fun newPageId(): PageId = PageId(UUID.randomUUID().toString())
    }
