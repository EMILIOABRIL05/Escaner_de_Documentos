package io.github.emilioabril05.folio.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PageEditsDaoTest {
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
    fun `una página nueva no está girada`() =
        runTest {
            insertarDocumentoConPaginas(1)

            assertEquals(0, dao.findPage("p-0")?.rotationDegrees)
        }

    @Test
    fun `guarda el giro de una página y deja intacto el resto`() =
        runTest {
            insertarDocumentoConPaginas(2)

            val cambiadas = dao.updatePageRotation("p-1", ROTACION)

            assertEquals(1, cambiadas)
            assertEquals(ROTACION, dao.findPage("p-1")?.rotationDegrees)
            assertEquals(0, dao.findPage("p-0")?.rotationDegrees)
            assertEquals(1, dao.findPage("p-1")?.position)
        }

    @Test
    fun `guarda el filtro de varias páginas a la vez`() =
        runTest {
            insertarDocumentoConPaginas(3)

            val cambiadas = dao.updatePagesFilter(listOf("p-0", "p-2"), "Grayscale")

            assertEquals(2, cambiadas)
            assertEquals("Grayscale", dao.findPage("p-0")?.filter)
            assertEquals("Document", dao.findPage("p-1")?.filter)
            assertEquals("Grayscale", dao.findPage("p-2")?.filter)
        }

    @Test
    fun `observa el documento y sus páginas en orden`() =
        runTest {
            insertarDocumentoConPaginas(3)

            assertEquals("doc-1", dao.observeDocument("doc-1").first()?.id)
            assertEquals(listOf("p-0", "p-1", "p-2"), dao.observePagesInOrder("doc-1").first().map { it.id })
        }

    @Test
    fun `observar un documento que no existe emite nulo`() =
        runTest {
            assertNull(dao.observeDocument("fantasma").first())
        }

    @Test
    fun `reordena las páginas con posiciones consecutivas`() =
        runTest {
            insertarDocumentoConPaginas(3)

            dao.reorderPages("doc-1", listOf("p-2", "p-0", "p-1"))

            assertEquals(listOf("p-2", "p-0", "p-1"), dao.findPagesInOrder("doc-1").map { it.id })
            assertEquals(listOf(0, 1, 2), dao.findPagesInOrder("doc-1").map { it.position })
        }

    @Test
    fun `borra una página y las siguientes suben una posición`() =
        runTest {
            insertarDocumentoConPaginas(4)

            val borradas = dao.deletePageAndShift("p-1")

            assertEquals(1, borradas)
            assertEquals(listOf("p-0", "p-2", "p-3"), dao.findPagesInOrder("doc-1").map { it.id })
            assertEquals(listOf(0, 1, 2), dao.findPagesInOrder("doc-1").map { it.position })
        }

    @Test
    fun `borrar una página que no existe no cambia nada`() =
        runTest {
            insertarDocumentoConPaginas(2)

            assertEquals(0, dao.deletePageAndShift("fantasma"))
            assertEquals(listOf(0, 1), dao.findPagesInOrder("doc-1").map { it.position })
        }

    @Test
    fun `agrega páginas después de la última y actualiza la fecha del documento`() =
        runTest {
            insertarDocumentoConPaginas(2)
            val nuevas = listOf(pagina("n-0", 0), pagina("n-1", 1))

            dao.appendPages("doc-1", nuevas, UPDATED)

            assertEquals(listOf("p-0", "p-1", "n-0", "n-1"), dao.findPagesInOrder("doc-1").map { it.id })
            assertEquals(listOf(0, 1, 2, 3), dao.findPagesInOrder("doc-1").map { it.position })
            assertEquals(UPDATED, dao.findDocument("doc-1")?.updatedAtMillis)
        }

    @Test
    fun `agrega páginas a un documento sin páginas desde la posición cero`() =
        runTest {
            insertarDocumentoConPaginas(0)

            dao.appendPages("doc-1", listOf(pagina("n-0", 7)), UPDATED)

            assertEquals(listOf(0), dao.findPagesInOrder("doc-1").map { it.position })
        }

    private fun pagina(
        id: String,
        position: Int,
    ) = PageEntity(id = id, documentId = "doc-1", position = position)

    private suspend fun insertarDocumentoConPaginas(count: Int) {
        dao.insertDocumentWithPages(
            DocumentEntity(id = "doc-1", title = "Escaneo", createdAtMillis = CREATED, updatedAtMillis = CREATED),
            (0 until count).map { pagina("p-$it", it) },
        )
    }

    private companion object {
        const val CREATED = 1_000L
        const val UPDATED = 5_000L
        const val ROTACION = 90
    }
}
