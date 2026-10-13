package io.github.emilioabril05.folio.core.domain

import io.github.emilioabril05.folio.core.model.Document
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.Page
import io.github.emilioabril05.folio.core.model.PageId
import io.github.emilioabril05.folio.core.model.Rotation
import io.github.emilioabril05.folio.core.testing.FakeDocumentRepository
import io.github.emilioabril05.folio.core.testing.FakePageStore
import io.github.emilioabril05.folio.core.testing.SequentialIdProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class PageEditsTest {
    private val documentId = DocumentId("doc-1")
    private val now = Instant.parse("2026-10-13T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val pages = listOf(Page(PageId("a"), 0), Page(PageId("b"), 1), Page(PageId("c"), 2))
    private val repository = FakeDocumentRepository()
    private val store = FakePageStore()

    private suspend fun givenDocument() {
        repository.create(Document(documentId, "Escaneo", Instant.EPOCH, Instant.EPOCH, pages))
        pages.forEach { store.saveOriginal(documentId, it.id, PageSource("content://${it.id.value}")) }
    }

    private suspend fun currentPages() =
        repository
            .observeDocument(documentId)
            .first()
            ?.pages
            .orEmpty()

    @Test
    fun `rotar gira la página 90 grados en sentido horario`() =
        runTest {
            givenDocument()

            val result = RotatePage(repository)(pages[1])

            assertEquals(EditResult.Done, result)
            assertEquals(Rotation.Quarter, currentPages()[1].rotation)
        }

    @Test
    fun `cuatro giros dejan la página como estaba`() {
        assertEquals(
            Rotation.None,
            Rotation.None
                .clockwise()
                .clockwise()
                .clockwise()
                .clockwise(),
        )
    }

    @Test
    fun `reordenar guarda el nuevo orden de las páginas`() =
        runTest {
            givenDocument()

            ReorderPages(repository)(documentId, listOf(PageId("c"), PageId("a"), PageId("b")))

            assertEquals(listOf("c", "a", "b"), currentPages().map { it.id.value })
            assertEquals(listOf(0, 1, 2), currentPages().map { it.position })
        }

    @Test
    fun `eliminar quita la página y borra su foto original`() =
        runTest {
            givenDocument()

            val result = DeletePage(repository, store)(documentId, PageId("b"))

            assertEquals(EditResult.Done, result)
            assertEquals(listOf("a", "c"), currentPages().map { it.id.value })
            assertTrue(PageId("b") !in store.originals)
        }

    @Test
    fun `añadir guarda los originales y agrega las páginas al final`() =
        runTest {
            givenDocument()
            val add = AddPagesToDocument(store, repository, SequentialIdProvider(), clock)

            val result = add(documentId, listOf(PageSource("content://d"), PageSource("content://e")))

            assertEquals(CreateDocumentResult.Created(documentId, listOf(PageId("page-1"), PageId("page-2"))), result)
            assertEquals(listOf("a", "b", "c", "page-1", "page-2"), currentPages().map { it.id.value })
            assertTrue(PageId("page-1") in store.originals)
        }

    @Test
    fun `si no se pueden añadir las páginas borra solo los originales nuevos`() =
        runTest {
            givenDocument()
            // Guardados 1 a 3: las páginas existentes. El 5.º es la segunda página nueva.
            val failing = FakePageStore(failWith = StorageError.NotEnoughSpace, failOnSave = 5)
            pages.forEach { failing.saveOriginal(documentId, it.id, PageSource("content://${it.id.value}")) }
            val add = AddPagesToDocument(failing, repository, SequentialIdProvider(), clock)

            val result = add(documentId, listOf(PageSource("content://d"), PageSource("content://e")))

            assertEquals(CreateDocumentResult.Failed(StorageError.NotEnoughSpace), result)
            assertEquals(setOf(PageId("a"), PageId("b"), PageId("c")), failing.originals.keys)
            assertEquals(3, currentPages().size)
        }
}
