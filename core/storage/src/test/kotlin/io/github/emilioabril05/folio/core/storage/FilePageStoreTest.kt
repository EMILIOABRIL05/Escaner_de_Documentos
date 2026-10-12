package io.github.emilioabril05.folio.core.storage

import android.content.Context
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
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException

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
    fun `borra la carpeta del documento`() =
        runTest {
            store.saveOriginal(documentId, pageId, sourceFile(byteArrayOf(9)))

            val result = store.deleteDocument(documentId)

            assertTrue(result.isSuccess)
            assertFalse(File(context.filesDir, "documents/doc-1").exists())
        }

    @Test
    fun `borrar un documento inexistente no falla`() =
        runTest {
            assertTrue(store.deleteDocument(DocumentId("fantasma")).isSuccess)
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
}
