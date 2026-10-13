package io.github.emilioabril05.folio.core.storage

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import io.github.emilioabril05.folio.core.domain.PageSource
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.domain.StorageFailure
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.PageId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException
import java.io.InputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FilePageStoreTest {
    private lateinit var context: Context
    private lateinit var store: FilePageStore
    private val documentId = DocumentId("doc-1")
    private val pageId = PageId("page-1")

    @Before
    fun prepararAlmacen() {
        context = ApplicationProvider.getApplicationContext()
        store = FilePageStore(context, UnconfinedTestDispatcher())
    }

    private fun original() = File(context.filesDir, "documents/doc-1/original/page-1.jpg")

    private fun sourceFile(bytes: ByteArray): PageSource {
        val file = File.createTempFile("origen", ".jpg", context.cacheDir)
        file.writeBytes(bytes)
        return PageSource(file.toURI().toString())
    }

    @Test
    fun `copia los bytes del origen al almacenamiento privado`() =
        runTest {
            val bytes = byteArrayOf(1, 2, 3, 4, 5)

            val result = store.saveOriginal(documentId, pageId, sourceFile(bytes))

            assertTrue(result.isSuccess)
            assertArrayEquals(bytes, original().readBytes())
        }

    @Test
    fun `informa origen ilegible y no deja archivo parcial`() =
        runTest {
            val missing = PageSource(File(context.cacheDir, "no-existe.jpg").toURI().toString())

            val result = store.saveOriginal(documentId, pageId, missing)

            assertEquals(StorageError.SourceUnreadable, (result.exceptionOrNull() as StorageFailure).error)
            assertFalse(original().exists())
        }

    @Test
    fun `copia el original de una página a otro documento`() =
        runTest {
            val bytes = byteArrayOf(7, 8, 9)
            store.saveOriginal(documentId, pageId, sourceFile(bytes))

            val result = store.copyOriginal(documentId, pageId, DocumentId("doc-2"), PageId("page-2"))

            assertTrue(result.isSuccess)
            assertArrayEquals(bytes, File(context.filesDir, "documents/doc-2/original/page-2.jpg").readBytes())
            assertArrayEquals(bytes, original().readBytes())
        }

    @Test
    fun `copiar un original que no existe informa origen ilegible`() =
        runTest {
            val result = store.copyOriginal(documentId, pageId, DocumentId("doc-2"), PageId("page-2"))

            assertEquals(StorageError.SourceUnreadable, (result.exceptionOrNull() as StorageFailure).error)
        }

    @Test
    fun `borra la carpeta del documento`() =
        runTest {
            store.saveOriginal(documentId, pageId, sourceFile(byteArrayOf(9)))

            val result = store.deleteDocument(documentId)

            assertTrue(result.isSuccess)
            assertFalse(File(context.filesDir, "documents/doc-1").exists())
        }

    @Test
    fun `borra solo el original de la página indicada`() =
        runTest {
            val otherPage = PageId("page-2")
            store.saveOriginal(documentId, pageId, sourceFile(byteArrayOf(1)))
            store.saveOriginal(documentId, otherPage, sourceFile(byteArrayOf(2)))

            val result = store.deleteOriginal(documentId, pageId)

            assertTrue(result.isSuccess)
            assertFalse(original().exists())
            assertTrue(File(context.filesDir, "documents/doc-1/original/page-2.jpg").exists())
        }

    @Test
    fun `borrar un original inexistente no falla`() =
        runTest {
            assertTrue(store.deleteOriginal(documentId, PageId("fantasma")).isSuccess)
        }

    @Test
    fun `borrar un documento inexistente no falla`() =
        runTest {
            assertTrue(store.deleteDocument(DocumentId("fantasma")).isSuccess)
        }

    @Test
    fun `si la copia falla a mitad de camino borra el archivo a medias`() {
        val brokenSource =
            object : InputStream() {
                private var delivered = 0

                override fun read(): Int =
                    if (delivered++ <
                        PARTIAL_BYTES
                    ) {
                        1
                    } else {
                        throw IOException("Lectura interrumpida")
                    }
            }

        val result = store.copyOrCleanUp(brokenSource, original())

        assertEquals(StorageError.Unknown, (result.exceptionOrNull() as StorageFailure).error)
        assertFalse(original().exists())
    }

    @Test
    fun `un origen sin permiso de lectura se informa como ilegible`() =
        runTest {
            Robolectric.buildContentProvider(DeniedProvider::class.java).create(DENIED_AUTHORITY)

            val result = store.saveOriginal(documentId, pageId, PageSource("content://$DENIED_AUTHORITY/hoja.jpg"))

            assertEquals(StorageError.SourceUnreadable, (result.exceptionOrNull() as StorageFailure).error)
            assertFalse(original().exists())
        }

    @Test
    fun `traduce un error de disco lleno a espacio insuficiente`() {
        val error = IOException("write failed: ENOSPC (No space left on device)")

        assertEquals(StorageError.NotEnoughSpace, error.toStorageError())
    }

    @Test
    fun `traduce otro error de disco a desconocido`() {
        assertEquals(StorageError.Unknown, IOException("boom").toStorageError())
    }

    private companion object {
        const val DENIED_AUTHORITY = "folio.prueba.denegado"
        const val PARTIAL_BYTES = 3
    }
}

/** Proveedor que niega la lectura, como una app que revoca el permiso de una URI compartida. */
class DeniedProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun openFile(
        uri: Uri,
        mode: String,
    ): ParcelFileDescriptor = throw SecurityException("Permiso denegado")

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
