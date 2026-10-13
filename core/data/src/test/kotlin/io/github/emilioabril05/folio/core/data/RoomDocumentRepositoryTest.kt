package io.github.emilioabril05.folio.core.data

import android.content.Context
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import io.github.emilioabril05.folio.core.database.DocumentDao
import io.github.emilioabril05.folio.core.database.DocumentEntity
import io.github.emilioabril05.folio.core.database.DocumentWithPages
import io.github.emilioabril05.folio.core.database.FolioDatabase
import io.github.emilioabril05.folio.core.database.PageCropUpdate
import io.github.emilioabril05.folio.core.database.PageEntity
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.domain.StorageFailure
import io.github.emilioabril05.folio.core.model.CropQuad
import io.github.emilioabril05.folio.core.model.Document
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.NormalizedPoint
import io.github.emilioabril05.folio.core.model.Page
import io.github.emilioabril05.folio.core.model.PageFilter
import io.github.emilioabril05.folio.core.model.PageId
import io.github.emilioabril05.folio.core.model.Rotation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class RoomDocumentRepositoryTest {
    private lateinit var database: FolioDatabase

    @Before
    fun crearBaseDeDatosEnMemoria() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, FolioDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun cerrarBaseDeDatos() {
        database.close()
    }

    @Test
    fun `crea el documento y guarda sus páginas en orden`() =
        runTest {
            val repository = RoomDocumentRepository(database.documentDao())

            val result = repository.create(document())

            val saved = database.documentDao().findDocumentWithPages("doc-1")
            assertTrue(result.isSuccess)
            assertEquals("Escaneo", saved?.document?.title)
            assertEquals(CREATED.toEpochMilli(), saved?.document?.createdAtMillis)
            assertEquals(UPDATED.toEpochMilli(), saved?.document?.updatedAtMillis)
            assertEquals(listOf("p-0", "p-1", "p-2"), saved?.pages?.map { it.id })
        }

    @Test
    fun `informa espacio insuficiente cuando la base de datos está llena`() =
        runTest {
            val repository = RoomDocumentRepository(FailingDocumentDao(SQLiteFullException("full")))

            val result = repository.create(document())

            assertEquals(StorageError.NotEnoughSpace, (result.exceptionOrNull() as StorageFailure).error)
        }

    @Test
    fun `informa error desconocido ante otro fallo de sqlite`() =
        runTest {
            val repository = RoomDocumentRepository(FailingDocumentDao(SQLiteException("boom")))

            val result = repository.create(document())

            assertEquals(StorageError.Unknown, (result.exceptionOrNull() as StorageFailure).error)
        }

    @Test
    fun `guarda el recorte de cada página al crear el documento`() =
        runTest {
            val repository = RoomDocumentRepository(database.documentDao())
            val withCrop = document(pages = listOf(Page(PageId("p-0"), 0, CROP)))

            repository.create(withCrop)

            assertEquals(CROP, repository.findPage(PageId("p-0")).getOrNull()?.crop)
        }

    @Test
    fun `encuentra una página con su posición y la foto completa por defecto`() =
        runTest {
            val repository = RoomDocumentRepository(database.documentDao())
            repository.create(document())

            val page = repository.findPage(PageId("p-1")).getOrNull()

            assertEquals(Page(PageId("p-1"), 1, CropQuad.Full), page)
        }

    @Test
    fun `devuelve nulo al buscar una página que no existe`() =
        runTest {
            val repository = RoomDocumentRepository(database.documentDao())

            val result = repository.findPage(PageId("no-existe"))

            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }

    @Test
    fun `actualiza el recorte de una página`() =
        runTest {
            val repository = RoomDocumentRepository(database.documentDao())
            repository.create(document())

            val result = repository.updatePageCrop(PageId("p-1"), CROP)

            assertTrue(result.isSuccess)
            assertEquals(CROP, repository.findPage(PageId("p-1")).getOrNull()?.crop)
            assertEquals(CropQuad.Full, repository.findPage(PageId("p-0")).getOrNull()?.crop)
        }

    @Test
    fun `informa error desconocido al recortar una página que no existe`() =
        runTest {
            val repository = RoomDocumentRepository(database.documentDao())

            val result = repository.updatePageCrop(PageId("no-existe"), CROP)

            assertEquals(StorageError.Unknown, (result.exceptionOrNull() as StorageFailure).error)
        }

    @Test
    fun `informa espacio insuficiente al guardar un recorte con la base llena`() =
        runTest {
            val repository = RoomDocumentRepository(FailingDocumentDao(SQLiteFullException("full")))

            val result = repository.updatePageCrop(PageId("p-1"), CROP)

            assertEquals(StorageError.NotEnoughSpace, (result.exceptionOrNull() as StorageFailure).error)
        }

    @Test
    fun `informa error desconocido ante un fallo de sqlite al leer o recortar`() =
        runTest {
            val repository = RoomDocumentRepository(FailingDocumentDao(SQLiteException("boom")))

            val found = repository.findPage(PageId("p-1"))
            val updated = repository.updatePageCrop(PageId("p-1"), CROP)

            assertEquals(StorageError.Unknown, (found.exceptionOrNull() as StorageFailure).error)
            assertEquals(StorageError.Unknown, (updated.exceptionOrNull() as StorageFailure).error)
        }

    @Test
    fun `guarda el giro de cada página al crear el documento`() =
        runTest {
            val repository = RoomDocumentRepository(database.documentDao())
            repository.create(document(pages = listOf(Page(PageId("p-0"), 0, rotation = Rotation.Half))))

            assertEquals(Rotation.Half, repository.findPage(PageId("p-0")).getOrNull()?.rotation)
        }

    @Test
    fun `guarda el filtro de varias páginas y deja el resto con el filtro por defecto`() =
        runTest {
            val repository = RoomDocumentRepository(database.documentDao())
            repository.create(document())

            val result = repository.updatePageFilter(listOf(PageId("p-0"), PageId("p-2")), PageFilter.Grayscale)

            val pages = repository.observeDocument(DocumentId("doc-1")).first()?.pages
            assertTrue(result.isSuccess)
            assertEquals(
                listOf(PageFilter.Grayscale, PageFilter.Document, PageFilter.Grayscale),
                pages?.map { it.filter },
            )
        }

    @Test
    fun `informa error desconocido al filtrar páginas que no existen`() =
        runTest {
            val repository = RoomDocumentRepository(database.documentDao())

            val result = repository.updatePageFilter(listOf(PageId("no-existe")), PageFilter.Original)

            assertEquals(StorageError.Unknown, (result.exceptionOrNull() as StorageFailure).error)
        }

    @Test
    fun `observa el documento con sus páginas en orden`() =
        runTest {
            val repository = RoomDocumentRepository(database.documentDao())
            repository.create(document())

            val observed = repository.observeDocument(DocumentId("doc-1")).first()

            assertEquals(document(), observed)
        }

    @Test
    fun `observar un documento que no existe emite nulo`() =
        runTest {
            val repository = RoomDocumentRepository(database.documentDao())

            assertNull(repository.observeDocument(DocumentId("fantasma")).first())
        }

    @Test
    fun `observar el documento emite de nuevo tras cambiar el giro de una página`() =
        runTest {
            val repository = RoomDocumentRepository(database.documentDao())
            repository.create(document())

            repository.observeDocument(DocumentId("doc-1")).test {
                assertEquals(Rotation.None, awaitItem()?.pages?.first()?.rotation)

                repository.updatePageRotation(PageId("p-0"), Rotation.Quarter)

                assertEquals(Rotation.Quarter, awaitItem()?.pages?.first()?.rotation)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `informa error desconocido al girar una página que no existe`() =
        runTest {
            val repository = RoomDocumentRepository(database.documentDao())

            val result = repository.updatePageRotation(PageId("no-existe"), Rotation.Half)

            assertEquals(StorageError.Unknown, (result.exceptionOrNull() as StorageFailure).error)
        }

    @Test
    fun `reordena las páginas y sus posiciones pasan a ser 0, 1, 2`() =
        runTest {
            val repository = RoomDocumentRepository(database.documentDao())
            repository.create(document())

            val result =
                repository.reorderPages(
                    DocumentId("doc-1"),
                    listOf(PageId("p-2"), PageId("p-0"), PageId("p-1")),
                )

            val pages = repository.observeDocument(DocumentId("doc-1")).first()?.pages
            assertTrue(result.isSuccess)
            assertEquals(listOf("p-2", "p-0", "p-1"), pages?.map { it.id.value })
            assertEquals(listOf(0, 1, 2), pages?.map { it.position })
        }

    @Test
    fun `borrar una página sube las siguientes una posición`() =
        runTest {
            val repository = RoomDocumentRepository(database.documentDao())
            repository.create(document())

            val result = repository.deletePage(PageId("p-0"))

            val pages = repository.observeDocument(DocumentId("doc-1")).first()?.pages
            assertTrue(result.isSuccess)
            assertEquals(listOf("p-1", "p-2"), pages?.map { it.id.value })
            assertEquals(listOf(0, 1), pages?.map { it.position })
        }

    @Test
    fun `informa error desconocido al borrar una página que no existe`() =
        runTest {
            val repository = RoomDocumentRepository(database.documentDao())

            val result = repository.deletePage(PageId("no-existe"))

            assertEquals(StorageError.Unknown, (result.exceptionOrNull() as StorageFailure).error)
        }

    @Test
    fun `agrega páginas al final en su orden y actualiza la fecha del documento`() =
        runTest {
            val repository = RoomDocumentRepository(database.documentDao())
            repository.create(document())
            val added = listOf(Page(PageId("n-0"), 0, CROP), Page(PageId("n-1"), 1, rotation = Rotation.Half))

            val result = repository.addPages(DocumentId("doc-1"), added, LATER)

            val saved = repository.observeDocument(DocumentId("doc-1")).first()
            assertTrue(result.isSuccess)
            assertEquals(listOf("p-0", "p-1", "p-2", "n-0", "n-1"), saved?.pages?.map { it.id.value })
            assertEquals(listOf(0, 1, 2, 3, 4), saved?.pages?.map { it.position })
            assertEquals(CROP, saved?.pages?.get(3)?.crop)
            assertEquals(Rotation.Half, saved?.pages?.get(4)?.rotation)
            assertEquals(LATER, saved?.updatedAt)
        }

    @Test
    fun `informa error desconocido ante un fallo de sqlite al editar páginas`() =
        runTest {
            val repository = RoomDocumentRepository(FailingDocumentDao(SQLiteException("boom")))

            val results =
                listOf(
                    repository.updatePageRotation(PageId("p-1"), Rotation.Half),
                    repository.reorderPages(DocumentId("doc-1"), listOf(PageId("p-1"))),
                    repository.deletePage(PageId("p-1")),
                    repository.addPages(DocumentId("doc-1"), listOf(Page(PageId("n-0"), 0)), LATER),
                )

            results.forEach { assertEquals(StorageError.Unknown, (it.exceptionOrNull() as StorageFailure).error) }
        }

    @Test
    fun `los identificadores generados son distintos`() {
        val ids = UuidIdProvider()

        assertTrue(ids.newDocumentId() != ids.newDocumentId())
        assertTrue(ids.newPageId() != ids.newPageId())
    }

    private fun document(pages: List<Page> = (0..2).map { Page(PageId("p-$it"), it) }) =
        Document(
            id = DocumentId("doc-1"),
            title = "Escaneo",
            createdAt = CREATED,
            updatedAt = UPDATED,
            pages = pages,
        )

    private class FailingDocumentDao(
        private val failure: SQLiteException,
    ) : DocumentDao {
        override suspend fun insertDocument(document: DocumentEntity) = throw failure

        override suspend fun insertPages(pages: List<PageEntity>) = throw failure

        override suspend fun insertDocumentWithPages(
            document: DocumentEntity,
            pages: List<PageEntity>,
        ) = throw failure

        override suspend fun findDocument(documentId: String): DocumentEntity? = throw failure

        override suspend fun findPagesInOrder(documentId: String): List<PageEntity> = throw failure

        override suspend fun findDocumentWithPages(documentId: String): DocumentWithPages? = throw failure

        override suspend fun findPage(pageId: String): PageEntity? = throw failure

        override suspend fun updatePageCrop(update: PageCropUpdate): Int = throw failure

        override suspend fun updatePageRotation(
            pageId: String,
            rotationDegrees: Int,
        ): Int = throw failure

        override fun observeDocument(documentId: String): Flow<DocumentEntity?> = flow { throw failure }

        override fun observePagesInOrder(documentId: String): Flow<List<PageEntity>> = flow { throw failure }

        override suspend fun updatePagePosition(
            documentId: String,
            pageId: String,
            position: Int,
        ) = throw failure

        override suspend fun reorderPages(
            documentId: String,
            pageIds: List<String>,
        ) = throw failure

        override suspend fun updatePagesFilter(
            pageIds: List<String>,
            filter: String,
        ): Int = throw failure

        override suspend fun deletePage(pageId: String): Int = throw failure

        override suspend fun shiftPagesUp(
            documentId: String,
            position: Int,
        ) = throw failure

        override suspend fun deletePageAndShift(pageId: String): Int = throw failure

        override suspend fun findLastPosition(documentId: String): Int? = throw failure

        override suspend fun updateDocumentUpdatedAt(
            documentId: String,
            updatedAtMillis: Long,
        ): Int = throw failure

        override suspend fun appendPages(
            documentId: String,
            pages: List<PageEntity>,
            updatedAtMillis: Long,
        ): Int = throw failure

        override suspend fun count(): Int = throw failure
    }

    private companion object {
        val CREATED: Instant = Instant.ofEpochMilli(1_000L)
        val UPDATED: Instant = Instant.ofEpochMilli(2_000L)
        val LATER: Instant = Instant.ofEpochMilli(9_000L)
        val CROP =
            CropQuad(
                topLeft = NormalizedPoint(0.1f, 0.2f),
                topRight = NormalizedPoint(0.9f, 0.1f),
                bottomRight = NormalizedPoint(0.8f, 0.9f),
                bottomLeft = NormalizedPoint(0.2f, 0.8f),
            )
    }
}
