package io.github.emilioabril05.folio.core.domain

import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.PageId
import io.github.emilioabril05.folio.core.testing.FakeDocumentRepository
import io.github.emilioabril05.folio.core.testing.FakePageStore
import io.github.emilioabril05.folio.core.testing.SequentialIdProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CreateDocumentFromScanTest {
    private val now = Instant.parse("2026-10-06T22:30:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val titles = DocumentTitleFormatter { "Escaneo $it" }
    private val sources = listOf(PageSource("content://1"), PageSource("content://2"), PageSource("content://3"))

    private fun useCase(
        store: FakePageStore = FakePageStore(),
        repository: FakeDocumentRepository = FakeDocumentRepository(),
    ) = CreateDocumentFromScan(store, repository, SequentialIdProvider(), titles, clock)

    @Test
    fun `crea un documento con una página por imagen y en el mismo orden`() =
        runTest {
            val repository = FakeDocumentRepository()

            val result = useCase(repository = repository)(sources)

            assertEquals(
                CreateDocumentResult.Created(
                    DocumentId("doc-1"),
                    listOf(PageId("page-1"), PageId("page-2"), PageId("page-3")),
                ),
                result,
            )
            val document = repository.documents.single()
            assertEquals(listOf(0, 1, 2), document.pages.map { it.position })
            assertEquals(listOf("page-1", "page-2", "page-3"), document.pages.map { it.id.value })
        }

    @Test
    fun `guarda cada original en disco antes de registrar el documento`() =
        runTest {
            val store = FakePageStore()
            var originalsWhenCreated = 0
            val repository = FakeDocumentRepository(onCreate = { originalsWhenCreated = store.originals.size })

            useCase(store, repository)(sources)

            assertEquals(3, originalsWhenCreated)
            assertEquals(sources, store.originals.values.map { it.second })
        }

    @Test
    fun `usa el nombre automático con la fecha de creación`() =
        runTest {
            val repository = FakeDocumentRepository()

            useCase(repository = repository)(sources)

            val document = repository.documents.single()
            assertEquals("Escaneo $now", document.title)
            assertEquals(now, document.createdAt)
            assertEquals(now, document.updatedAt)
        }

    @Test
    fun `sin páginas no crea nada`() =
        runTest {
            val repository = FakeDocumentRepository()

            val result = useCase(repository = repository)(emptyList())

            assertEquals(CreateDocumentResult.NoPages, result)
            assertTrue(repository.documents.isEmpty())
        }

    @Test
    fun `si falta espacio borra lo ya guardado y no crea el documento`() =
        runTest {
            val store = FakePageStore(failWith = StorageError.NotEnoughSpace, failOnSave = 3)
            val repository = FakeDocumentRepository()

            val result = useCase(store, repository)(sources)

            assertEquals(CreateDocumentResult.Failed(StorageError.NotEnoughSpace), result)
            assertTrue(store.originals.isEmpty())
            assertTrue(repository.documents.isEmpty())
        }

    @Test
    fun `si no se puede registrar el documento borra los originales`() =
        runTest {
            val store = FakePageStore()
            val repository = FakeDocumentRepository(failWith = StorageError.Unknown)

            val result = useCase(store, repository)(sources)

            assertEquals(CreateDocumentResult.Failed(StorageError.Unknown), result)
            assertTrue(store.originals.isEmpty())
        }
}
