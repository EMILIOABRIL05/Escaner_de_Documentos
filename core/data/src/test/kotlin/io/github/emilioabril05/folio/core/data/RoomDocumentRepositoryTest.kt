package io.github.emilioabril05.folio.core.data

import android.content.Context
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import io.github.emilioabril05.folio.core.model.PageId
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

        override suspend fun count(): Int = throw failure
    }

    private companion object {
        val CREATED: Instant = Instant.ofEpochMilli(1_000L)
        val UPDATED: Instant = Instant.ofEpochMilli(2_000L)
        val CROP =
            CropQuad(
                topLeft = NormalizedPoint(0.1f, 0.2f),
                topRight = NormalizedPoint(0.9f, 0.1f),
                bottomRight = NormalizedPoint(0.8f, 0.9f),
                bottomLeft = NormalizedPoint(0.2f, 0.8f),
            )
    }
}
