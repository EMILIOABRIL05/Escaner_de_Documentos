package io.github.emilioabril05.folio.core.storage.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.test.core.app.ApplicationProvider
import io.github.emilioabril05.folio.core.domain.EncodedImage
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.domain.StorageFailure
import io.github.emilioabril05.folio.core.model.CropQuad
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.NormalizedPoint
import io.github.emilioabril05.folio.core.model.PageId
import io.github.emilioabril05.folio.core.storage.originalFile
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BitmapPageRendererTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val renderer = BitmapPageRenderer(context, UnconfinedTestDispatcher())
    private val documentId = DocumentId("doc-1")
    private val pageId = PageId("page-1")

    // Hoja inclinada dentro de una foto de 400 × 400.
    private val sheet =
        CropQuad(
            topLeft = NormalizedPoint(0.2f, 0.1f),
            topRight = NormalizedPoint(0.8f, 0.2f),
            bottomRight = NormalizedPoint(0.75f, 0.9f),
            bottomLeft = NormalizedPoint(0.15f, 0.8f),
        )

    private fun saveOriginal(
        width: Int,
        height: Int,
        drawSheet: Boolean = false,
    ) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        if (drawSheet) {
            val path = Path()
            sheet.corners.forEachIndexed { index, point ->
                val x = point.x * width
                val y = point.y * height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            canvas.drawPath(path, Paint().apply { color = Color.BLUE })
        }
        val file = originalFile(context, documentId, pageId)
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, LOSSLESS_ENOUGH, it) }
    }

    private fun EncodedImage.decode(): Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    private fun Int.isBlue(): Boolean = Color.blue(this) > STRONG && Color.red(this) < WEAK && Color.green(this) < WEAK

    @Test
    fun `la foto original se reduce sin deformarse a su lado máximo`() =
        runTest {
            saveOriginal(width = 400, height = 200)

            val image = renderer.original(documentId, pageId, maxSidePx = 100).getOrThrow()

            assertEquals(100, image.width)
            assertEquals(50, image.height)
            assertEquals(100, image.decode().width)
        }

    @Test
    fun `sin recorte la página corregida es la foto completa`() =
        runTest {
            saveOriginal(width = 400, height = 200)

            val image = renderer.corrected(documentId, pageId, CropQuad.Full, maxSidePx = 400).getOrThrow()

            assertEquals(400, image.width)
            assertEquals(200, image.height)
        }

    @Test
    fun `endereza una hoja inclinada y la deja ocupando toda la imagen`() =
        runTest {
            saveOriginal(width = 400, height = 400, drawSheet = true)

            val page = renderer.corrected(documentId, pageId, sheet, maxSidePx = 400).getOrThrow().decode()

            val inset = INSET_FRACTION
            val samples =
                listOf(
                    page.getPixel((page.width * inset).toInt(), (page.height * inset).toInt()),
                    page.getPixel((page.width * (1 - inset)).toInt(), (page.height * inset).toInt()),
                    page.getPixel((page.width * (1 - inset)).toInt(), (page.height * (1 - inset)).toInt()),
                    page.getPixel((page.width * inset).toInt(), (page.height * (1 - inset)).toInt()),
                    page.getPixel(page.width / 2, page.height / 2),
                )
            assertTrue("muestras ${samples.map { Integer.toHexString(it) }}", samples.all { it.isBlue() })
        }

    @Test
    fun `si falta la foto original lo informa como ilegible`() =
        runTest {
            val result = renderer.corrected(documentId, PageId("no-existe"), CropQuad.Full, maxSidePx = 400)

            assertEquals(StorageError.SourceUnreadable, (result.exceptionOrNull() as StorageFailure).error)
        }

    private companion object {
        const val LOSSLESS_ENOUGH = 100
        const val STRONG = 180
        const val WEAK = 90
        const val INSET_FRACTION = 0.08f
    }
}
