package io.github.emilioabril05.folio.core.domain

import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.Page
import io.github.emilioabril05.folio.core.model.PageId
import java.time.Clock
import javax.inject.Inject

/** Gira una página 90° en sentido horario. La foto original no se toca. */
class RotatePage
    @Inject
    constructor(
        private val documents: DocumentRepository,
    ) {
        /** Gira [page] un paso a partir de su giro actual. */
        suspend operator fun invoke(page: Page): EditResult =
            documents.updatePageRotation(page.id, page.rotation.clockwise()).toEditResult()
    }

/** Cambia el orden de las páginas de un documento. */
class ReorderPages
    @Inject
    constructor(
        private val documents: DocumentRepository,
    ) {
        /** Deja las páginas de [documentId] en el orden de [pageIds]. */
        suspend operator fun invoke(
            documentId: DocumentId,
            pageIds: List<PageId>,
        ): EditResult = documents.reorderPages(documentId, pageIds).toEditResult()
    }

/** Quita una página de un documento y borra su foto original. */
class DeletePage
    @Inject
    constructor(
        private val documents: DocumentRepository,
        private val pageStore: PageStore,
    ) {
        /** Quita la página [pageId] del documento [documentId]. */
        suspend operator fun invoke(
            documentId: DocumentId,
            pageId: PageId,
        ): EditResult {
            val result = documents.deletePage(pageId).toEditResult()
            if (result == EditResult.Done) pageStore.deleteOriginal(documentId, pageId)
            return result
        }
    }

/**
 * Agrega páginas recién escaneadas al final de un documento existente. Como al crear un documento, cada
 * original se guarda en disco antes de registrarlo; si algo falla, se borran solo los originales nuevos.
 */
class AddPagesToDocument
    @Inject
    constructor(
        private val pageStore: PageStore,
        private val documents: DocumentRepository,
        private val ids: IdProvider,
        private val clock: Clock,
    ) {
        /** Agrega las páginas de [sources] al final de [documentId], en ese orden. */
        suspend operator fun invoke(
            documentId: DocumentId,
            sources: List<PageSource>,
        ): CreateDocumentResult {
            if (sources.isEmpty()) return CreateDocumentResult.NoPages

            val pages = sources.indices.map { position -> Page(ids.newPageId(), position) }
            val saved = mutableListOf<PageId>()
            val result =
                pages
                    .zip(sources)
                    .fold(Result.success(Unit)) { previous, (page, source) ->
                        if (previous.isFailure) {
                            previous
                        } else {
                            pageStore.saveOriginal(documentId, page.id, source).onSuccess { saved += page.id }
                        }
                    }.let { if (it.isSuccess) documents.addPages(documentId, pages, clock.instant()) else it }

            return result.fold(
                onSuccess = { CreateDocumentResult.Created(documentId, pages.map { it.id }) },
                onFailure = { failure ->
                    saved.forEach { pageStore.deleteOriginal(documentId, it) }
                    CreateDocumentResult.Failed((failure as? StorageFailure)?.error ?: StorageError.Unknown)
                },
            )
        }
    }
