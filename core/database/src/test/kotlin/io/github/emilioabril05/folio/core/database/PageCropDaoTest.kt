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
class PageCropDaoTest {
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
    fun `encuentra una página con la foto completa por defecto`() =
        runTest {
            insertarDocumentoConPagina()

            val pagina = dao.findPage("p-1")

            assertEquals("doc-1", pagina?.documentId)
            assertEquals(CropColumns.Full, pagina?.crop)
        }

    @Test
    fun `devuelve nulo cuando la página no existe`() =
        runTest {
            assertNull(dao.findPage("no-existe"))
        }

    @Test
    fun `actualiza el recorte de una página y deja intacto el resto`() =
        runTest {
            insertarDocumentoConPagina()

            val actualizadas = dao.updatePageCrop(PageCropUpdate("p-1", RECORTE))

            val pagina = dao.findPage("p-1")
            assertEquals(1, actualizadas)
            assertEquals(RECORTE, pagina?.crop)
            assertEquals(POSICION, pagina?.position)
        }

    @Test
    fun `no actualiza nada cuando la página no existe`() =
        runTest {
            assertEquals(0, dao.updatePageCrop(PageCropUpdate("no-existe", RECORTE)))
        }

    private suspend fun insertarDocumentoConPagina() {
        dao.insertDocumentWithPages(
            DocumentEntity(id = "doc-1", title = "Escaneo", createdAtMillis = CREATED, updatedAtMillis = CREATED),
            listOf(PageEntity(id = "p-1", documentId = "doc-1", position = POSICION)),
        )
    }

    private companion object {
        const val CREATED = 1_000L
        const val POSICION = 3
        val RECORTE =
            CropColumns(
                topLeft = PointColumns(0.1f, 0.2f),
                topRight = PointColumns(0.9f, 0.1f),
                bottomRight = PointColumns(0.8f, 0.9f),
                bottomLeft = PointColumns(0.2f, 0.8f),
            )
    }
}
