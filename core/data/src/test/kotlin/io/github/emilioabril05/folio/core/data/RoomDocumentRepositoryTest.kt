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
import io.github.emilioabril05.folio.core.database.PageEntity
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.domain.StorageFailure
import io.github.emilioabril05.folio.core.model.Document
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.Page
import io.github.emilioabril05.folio.core.model.PageId
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun `los identificadores generados son distintos`() {
        val ids = UuidIdProvider()

        assertTrue(ids.newDocumentId() != ids.newDocumentId())
        assertTrue(ids.newPageId() != ids.newPageId())
    }

    private fun document() =
        Document(
            id = DocumentId("doc-1"),
            title = "Escaneo",
            createdAt = CREATED,
            updatedAt = UPDATED,
            pages = (0..2).map { Page(PageId("p-$it"), it) },
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

        override suspend fun count(): Int = throw failure
    }

    private companion object {
        val CREATED: Instant = Instant.ofEpochMilli(1_000L)
        val UPDATED: Instant = Instant.ofEpochMilli(2_000L)
    }
}
