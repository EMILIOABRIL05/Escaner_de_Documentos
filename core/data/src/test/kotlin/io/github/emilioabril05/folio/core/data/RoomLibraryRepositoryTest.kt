package io.github.emilioabril05.folio.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.emilioabril05.folio.core.database.FolioDatabase
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.domain.StorageFailure
import io.github.emilioabril05.folio.core.model.Document
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.Page
import io.github.emilioabril05.folio.core.model.PageId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class RoomLibraryRepositoryTest {
    private lateinit var database: FolioDatabase
    private lateinit var documents: RoomDocumentRepository
    private lateinit var library: RoomLibraryRepository

    @Before
    fun crearBaseDeDatosEnMemoria() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, FolioDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        documents = RoomDocumentRepository(database.documentDao())
        library = RoomLibraryRepository(database.libraryDao())
    }

    @After
    fun cerrarBaseDeDatos() {
        database.close()
    }

    @Test
    fun `lista los documentos del más reciente al más viejo con su primera página y su cuenta`() =
        runTest {
            documents.create(document("a", "Física", 1, pages = 2))
            documents.create(document("b", "Química", 2, pages = 1))

            val list = library.observeDocuments("", trashed = false).first()

            assertEquals(listOf("b", "a"), list.map { it.id.value })
            assertEquals(listOf(1, 2), list.map { it.pageCount })
            assertEquals(PageId("a-0"), list.last().firstPage?.id)
        }

    @Test
    fun `busca por título sin mezclar la papelera`() =
        runTest {
            documents.create(document("a", "Física", 1))
            documents.create(document("b", "Física II", 2))
            documents.create(document("c", "Química", 3))
            library.setTrashed(DocumentId("b"), Instant.ofEpochMilli(10))

            assertEquals(listOf("a"), library.observeDocuments("sic", trashed = false).first().map { it.id.value })
            assertEquals(listOf("b"), library.observeDocuments("", trashed = true).first().map { it.id.value })
        }

    @Test
    fun `renombra, restaura y encuentra los documentos vencidos de la papelera`() =
        runTest {
            documents.create(document("a", "Física", 1))
            library.rename(DocumentId("a"), "Fís 2", Instant.ofEpochMilli(5))
            library.setTrashed(DocumentId("a"), Instant.ofEpochMilli(10))

            assertEquals(listOf(DocumentId("a")), library.trashedBefore(Instant.ofEpochMilli(11)))
            assertTrue(library.trashedBefore(Instant.ofEpochMilli(10)).isEmpty())
            library.setTrashed(DocumentId("a"), null)

            val summary = library.observeDocuments("Fís 2", trashed = false).first().single()
            assertNull(summary.trashedAt)
            assertEquals(Instant.ofEpochMilli(5), summary.updatedAt)
        }

    @Test
    fun `un documento en la papelera se puede observar y borrar para siempre`() =
        runTest {
            documents.create(document("a", "Física", 1))
            library.setTrashed(DocumentId("a"), Instant.ofEpochMilli(10))

            assertNotNull(documents.observeDocument(DocumentId("a")).first())
            assertTrue(library.deleteForever(DocumentId("a")).isSuccess)
            assertNull(documents.observeDocument(DocumentId("a")).first())
            assertNull(database.documentDao().findPage("a-0"))
        }

    @Test
    fun `informa error desconocido al renombrar un documento que no existe`() =
        runTest {
            val result = library.rename(DocumentId("nada"), "X", Instant.EPOCH)

            assertEquals(StorageError.Unknown, (result.exceptionOrNull() as StorageFailure).error)
        }

    private fun document(
        id: String,
        title: String,
        updatedMillis: Long,
        pages: Int = 1,
    ) = Document(
        id = DocumentId(id),
        title = title,
        createdAt = Instant.ofEpochMilli(0),
        updatedAt = Instant.ofEpochMilli(updatedMillis),
        pages = (0 until pages).map { Page(PageId("$id-$it"), it) },
    )
}
