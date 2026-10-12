package io.github.emilioabril05.folio.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DocumentDaoTest {
    private lateinit var database: FolioDatabase
    private lateinit var dao: DocumentDao

    @Before
    fun crearBaseDeDatosEnMemoria() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, FolioDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.documentDao()
    }

    @After
    fun cerrarBaseDeDatos() {
        database.close()
    }

    @Test
    fun `lee las páginas de un documento ordenadas por posición`() =
        runTest {
            dao.insertDocumentWithPages(
                document("doc-1"),
                listOf(page("p-c", "doc-1", 2), page("p-a", "doc-1", 0), page("p-b", "doc-1", 1)),
            )

            val leido = dao.findDocumentWithPages("doc-1")

            assertEquals(listOf("p-a", "p-b", "p-c"), leido?.pages?.map { it.id })
            assertEquals("Escaneo", leido?.document?.title)
        }

    @Test
    fun `devuelve nulo cuando el documento no existe`() =
        runTest {
            assertNull(dao.findDocumentWithPages("no-existe"))
        }

    @Test
    fun `cuenta los documentos guardados`() =
        runTest {
            dao.insertDocumentWithPages(document("doc-1"), listOf(page("p-1", "doc-1", 0)))
            dao.insertDocumentWithPages(document("doc-2"), listOf(page("p-2", "doc-2", 0)))

            assertEquals(2, dao.count())
        }

    @Test
    fun `no deja páginas huérfanas si falla el guardado de una página`() =
        runTest {
            val huerfana = page("p-x", "otro-doc", 0)

            runCatching { dao.insertDocumentWithPages(document("doc-1"), listOf(huerfana)) }

            assertEquals(0, dao.count())
        }

    private fun document(id: String) =
        DocumentEntity(id = id, title = "Escaneo", createdAtMillis = CREATED, updatedAtMillis = CREATED)

    private fun page(
        id: String,
        documentId: String,
        position: Int,
    ) = PageEntity(id = id, documentId = documentId, position = position)

    private companion object {
        const val CREATED = 1_000L
    }
}
