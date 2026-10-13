package io.github.emilioabril05.folio.core.testing

import io.github.emilioabril05.folio.core.domain.EncodedImage
import io.github.emilioabril05.folio.core.domain.PageRenderer
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.domain.StorageFailure
import io.github.emilioabril05.folio.core.model.CropQuad
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.ExportProfile
import io.github.emilioabril05.folio.core.model.Page
import io.github.emilioabril05.folio.core.model.PageId

/** Renderizador que devuelve imágenes vacías del tamaño pedido. Con [failWith], toda lectura falla. */
class FakePageRenderer(
    private val failWith: StorageError? = null,
    private val photoWidth: Int = 300,
    private val photoHeight: Int = 400,
) : PageRenderer {
    private val renderedCrops = mutableListOf<CropQuad>()

    /** Recortes pedidos a [corrected], en orden. */
    val corrections: List<CropQuad> get() = renderedCrops.toList()

    override suspend fun original(
        documentId: DocumentId,
        pageId: PageId,
        maxSidePx: Int,
    ): Result<EncodedImage> = image(photoWidth, photoHeight)

    override suspend fun page(
        documentId: DocumentId,
        page: Page,
        maxSidePx: Int,
    ): Result<EncodedImage> = image(photoWidth, photoHeight)

    override suspend fun export(
        documentId: DocumentId,
        page: Page,
        profile: ExportProfile,
    ): Result<EncodedImage> = image(photoWidth, photoHeight)

    override suspend fun corrected(
        documentId: DocumentId,
        pageId: PageId,
        crop: CropQuad,
        maxSidePx: Int,
    ): Result<EncodedImage> {
        renderedCrops += crop
        return image(photoWidth, photoHeight)
    }

    private fun image(
        width: Int,
        height: Int,
    ): Result<EncodedImage> =
        if (failWith != null) {
            Result.failure(StorageFailure(failWith))
        } else {
            Result.success(EncodedImage(ByteArray(0), width, height))
        }
}
