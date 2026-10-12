package io.github.emilioabril05.folio.core.domain

import io.github.emilioabril05.folio.core.model.Document
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.Page
import java.time.Clock
import javax.inject.Inject

/** Resultado de crear un documento a partir de un escaneo. */
sealed interface CreateDocumentResult {
    /** El documento quedó guardado con [pageCount] páginas. */
    data class Created(
        val documentId: DocumentId,
        val pageCount: Int,
    ) : CreateDocumentResult

    /** El escaneo no trajo ninguna página. */
    data object NoPages : CreateDocumentResult

    /** No se pudo guardar; no queda nada a medias en el teléfono. */
    data class Failed(
        val error: StorageError,
    ) : CreateDocumentResult
}

/**
 * Convierte las imágenes de un escaneo en un documento nuevo.
 *
 * Cada original se guarda en disco antes de registrar el documento, para que ninguna página se pierda
 * si el sistema cierra la app. Si algo falla, se borra lo ya guardado.
 */
class CreateDocumentFromScan
    @Inject
    constructor(
        private val pageStore: PageStore,
        private val documents: DocumentRepository,
        private val ids: IdProvider,
        private val titles: DocumentTitleFormatter,
        private val clock: Clock,
    ) {
        /** Crea el documento con las páginas de [sources], en ese orden. */
        suspend operator fun invoke(sources: List<PageSource>): CreateDocumentResult {
            if (sources.isEmpty()) return CreateDocumentResult.NoPages

            val documentId = ids.newDocumentId()
            val pages = sources.indices.map { position -> Page(ids.newPageId(), position) }
            val now = clock.instant()
            val document = Document(documentId, titles.format(now), createdAt = now, updatedAt = now, pages = pages)

            val saved =
                pages.zip(sources).fold(Result.success(Unit)) { previous, (page, source) ->
                    previous.andThen { pageStore.saveOriginal(documentId, page.id, source) }
                }
            val created = saved.andThen { documents.create(document) }

            return created.fold(
                onSuccess = { CreateDocumentResult.Created(documentId, pages.size) },
                onFailure = { failure ->
                    pageStore.deleteDocument(documentId)
                    CreateDocumentResult.Failed((failure as? StorageFailure)?.error ?: StorageError.Unknown)
                },
            )
        }

        private inline fun Result<Unit>.andThen(next: () -> Result<Unit>): Result<Unit> =
            if (isSuccess) next() else this
    }
