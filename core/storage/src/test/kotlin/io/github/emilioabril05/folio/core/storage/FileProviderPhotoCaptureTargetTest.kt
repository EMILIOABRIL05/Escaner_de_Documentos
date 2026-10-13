package io.github.emilioabril05.folio.core.storage

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class FileProviderPhotoCaptureTargetTest {
    @get:Rule
    val cache = TemporaryFolder()

    @Test
    fun `crea un archivo jpg vacío dentro de la carpeta de capturas`() {
        val target = FileProviderPhotoCaptureTarget(cache.root) { it.name }

        val name = target.newPhotoTarget().value

        val file = File(File(cache.root, "captures"), name)
        assertTrue(file.exists())
        assertTrue(name.endsWith(".jpg"))
        assertEquals(0L, file.length())
    }

    @Test
    fun `cada llamada devuelve un destino distinto`() {
        val target = FileProviderPhotoCaptureTarget(cache.root) { it.name }

        assertNotEquals(target.newPhotoTarget(), target.newPhotoTarget())
        assertEquals(2, File(cache.root, "captures").listFiles().orEmpty().size)
    }

    // FileProvider compara rutas con "/", así que en Windows (solo el entorno de pruebas) no reconoce la raíz.
    @Test
    fun `devuelve una URI content con la autoridad propia de la app`() {
        assumeFalse(System.getProperty("os.name").orEmpty().startsWith("Windows"))
        val context = ApplicationProvider.getApplicationContext<Context>()

        val uri = Uri.parse(FileProviderPhotoCaptureTarget(context).newPhotoTarget().value)

        assertEquals("content", uri.scheme)
        assertEquals("${context.packageName}.files", uri.authority)
    }
}
