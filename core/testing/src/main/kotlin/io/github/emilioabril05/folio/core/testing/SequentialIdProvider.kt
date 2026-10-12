package io.github.emilioabril05.folio.core.testing

import io.github.emilioabril05.folio.core.domain.IdProvider
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.PageId

/** Identificadores predecibles: doc-1, doc-2… y page-1, page-2… */
class SequentialIdProvider : IdProvider {
    private var documents = 0
    private var pages = 0

    override fun newDocumentId(): DocumentId = DocumentId("doc-${++documents}")

    override fun newPageId(): PageId = PageId("page-${++pages}")
}
