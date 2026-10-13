package io.github.emilioabril05.folio.core.domain

import io.github.emilioabril05.folio.core.model.PageFilter
import io.github.emilioabril05.folio.core.model.PageId
import javax.inject.Inject

/** Aplica un filtro a una o varias páginas ("Aplicar a todas"). La foto original no cambia. */
class ApplyFilter
    @Inject
    constructor(
        private val documents: DocumentRepository,
    ) {
        /** Aplica [filter] a las páginas [pageIds]. */
        suspend operator fun invoke(
            pageIds: List<PageId>,
            filter: PageFilter,
        ): EditResult = documents.updatePageFilter(pageIds, filter).toEditResult()
    }
