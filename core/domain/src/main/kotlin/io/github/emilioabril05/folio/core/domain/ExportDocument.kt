package io.github.emilioabril05.folio.core.domain

import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.ExportProfile
import kotlinx.coroutines.flow.first
import javax.inject.Inject

// Margen del cálculo de peso (plan §7.3): la primera página no siempre es la más pesada.
private const val SIZE_ESTIMATE_MARGIN = 1.1

/** Resultado de exportar un documento. */
sealed interface ExportResult {
    /** El PDF está listo. */
    data class Ready(
        val pdf: ExportedPdf,
    ) : ExportResult

    /** El documento no existe o no tiene páginas. */
    data object NothingToExport : ExportResult

    /** No se pudo escribir el PDF. */
    data class Failed(
        val error: StorageError,
    ) : ExportResult
}

/** Arma el PDF de un documento con un perfil de calidad, página por página. El PDF es derivado (plan §2.3). */
class ExportDocument
    @Inject
    constructor(
        private val documents: DocumentRepository,
        private val renderer: PageRenderer,
        private val writer: PdfWriter,
    ) {
        /** Exporta [documentId] con [profile]. */
        suspend operator fun invoke(
            documentId: DocumentId,
            profile: ExportProfile,
        ): ExportResult {
            val document = documents.observeDocument(documentId).first()
            if (document == null || document.pages.isEmpty()) return ExportResult.NothingToExport
            val pages = document.pages.sortedBy { it.position }
            return writer
                .write(documentId, pdfFileName(document.title), pages.size, profile.dpi) { index ->
                    renderer.export(documentId, pages[index], profile)
                }.fold(
                    onSuccess = { ExportResult.Ready(it) },
                    onFailure = { ExportResult.Failed((it as? StorageFailure)?.error ?: StorageError.Unknown) },
                )
        }
    }

/** Calcula el peso aproximado del PDF antes de exportarlo (plan §7.3, RF-08). */
class EstimatePdfSize
    @Inject
    constructor(
        private val documents: DocumentRepository,
        private val renderer: PageRenderer,
    ) {
        /** Peso estimado en bytes de [documentId] con [profile], o `null` si no se puede calcular. */
        suspend operator fun invoke(
            documentId: DocumentId,
            profile: ExportProfile,
        ): Long? {
            val pages =
                documents
                    .observeDocument(documentId)
                    .first()
                    ?.pages
                    ?.sortedBy { it.position }
            val first = pages?.firstOrNull()
            val firstPageBytes =
                first?.let {
                    renderer
                        .export(documentId, it, profile)
                        .getOrNull()
                        ?.bytes
                        ?.size
                }
            return if (pages == null || firstPageBytes == null) {
                null
            } else {
                (firstPageBytes * pages.size * SIZE_ESTIMATE_MARGIN).toLong()
            }
        }
    }

// Nombre de archivo legible y seguro (plan §9.1): "Escaneo 2026-10-13 12.35" → "Escaneo-2026-10-13-12-35.pdf".
internal fun pdfFileName(title: String): String {
    val safe =
        title
            .trim()
            .replace(Regex("""[^\p{L}\p{N}]+"""), "-")
            .trim('-')
            .ifEmpty { "Folio" }
    return "$safe.pdf"
}
