package io.github.emilioabril05.folio.core.domain

import io.github.emilioabril05.folio.core.model.CropQuad
import io.github.emilioabril05.folio.core.model.Document
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.NormalizedPoint
import io.github.emilioabril05.folio.core.model.Page
import io.github.emilioabril05.folio.core.model.PageId
import io.github.emilioabril05.folio.core.testing.FakeDocumentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class UpdatePageCropTest {
    private val pageId = PageId("page-1")
    private val tilted =
        CropQuad(
            topLeft = NormalizedPoint(0.1f, 0.1f),
            topRight = NormalizedPoint(0.9f, 0.05f),
            bottomRight = NormalizedPoint(0.95f, 0.9f),
            bottomLeft = NormalizedPoint(0.05f, 0.95f),
        )

    @Test
    fun `guarda un recorte válido`() =
        runTest {
            val repository = FakeDocumentRepository()
            repository.create(
                Document(DocumentId("doc-1"), "Escaneo", Instant.EPOCH, Instant.EPOCH, listOf(Page(pageId, 0))),
            )

            val result = UpdatePageCrop(repository)(pageId, tilted)

            assertEquals(UpdateCropResult.Saved, result)
            assertEquals(tilted, repository.crops[pageId])
        }

    @Test
    fun `rechaza un recorte inválido sin guardarlo`() =
        runTest {
            val repository = FakeDocumentRepository()
            val crossed = tilted.copy(topLeft = tilted.topRight, topRight = tilted.topLeft)

            val result = UpdatePageCrop(repository)(pageId, crossed)

            assertEquals(UpdateCropResult.Invalid(InvalidCrop.NotConvex), result)
            assertTrue(repository.crops.isEmpty())
        }

    @Test
    fun `informa si no se pudo guardar`() =
        runTest {
            val repository = FakeDocumentRepository(failWith = StorageError.Unknown)

            val result = UpdatePageCrop(repository)(pageId, tilted)

            assertEquals(UpdateCropResult.Failed(StorageError.Unknown), result)
        }
}
