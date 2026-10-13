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
import io.github.emilioabril05.folio.core.model.Page
import io.github.emilioabril05.folio.core.model.PageFilter
import io.github.emilioabril05.folio.core.model.PageId
import io.github.emilioabril05.folio.core.model.Rotation
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

    // Foto blanca con el cuadrante de arriba a la izquierda en rojo.
    private fun saveRedTopLeftQuadrant(
        width: Int,
        height: Int,
    ) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawRect(0f, 0f, width / 2f, height / 2f, Paint().apply { color = Color.RED })
        val file = originalFile(context, documentId, pageId)
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, LOSSLESS_ENOUGH, it) }
    }

    private fun Int.isRed(): Boolean = Color.red(this) > STRONG && Color.blue(this) < WEAK && Color.green(this) < WEAK

    private fun Int.isWhite(): Boolean =
        Color.red(this) > STRONG && Color.blue(this) > STRONG && Color.green(this) > STRONG

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
    fun `la página sin giro conserva el tamaño de la foto`() =
        runTest {
            saveRedTopLeftQuadrant(width = 400, height = 200)

            val image =
                renderer
                    .page(
                        documentId,
                        Page(pageId, 0, filter = PageFilter.Original),
                        maxSidePx = 400,
                    ).getOrThrow()

            assertEquals(400, image.width)
            assertEquals(200, image.height)
            assertTrue(image.decode().getPixel(QUADRANT_SAMPLE, QUADRANT_SAMPLE).isRed())
        }

    @Test
    fun `girar 90 grados intercambia ancho y alto y lleva la esquina roja arriba a la derecha`() =
        runTest {
            saveRedTopLeftQuadrant(width = 400, height = 200)

            val image =
                renderer
                    .page(
                        documentId,
                        Page(pageId, 0, rotation = Rotation.Quarter, filter = PageFilter.Original),
                        maxSidePx = 400,
                    ).getOrThrow()

            val decoded = image.decode()
            assertEquals(200, image.width)
            assertEquals(400, image.height)
            assertTrue(decoded.getPixel(decoded.width - QUADRANT_SAMPLE, QUADRANT_SAMPLE).isRed())
            assertTrue(decoded.getPixel(QUADRANT_SAMPLE, QUADRANT_SAMPLE).isWhite())
        }

    @Test
    fun `girar 180 grados mantiene el tamaño y lleva la esquina roja abajo a la derecha`() =
        runTest {
            saveRedTopLeftQuadrant(width = 400, height = 200)

            val image =
                renderer
                    .page(
                        documentId,
                        Page(pageId, 0, rotation = Rotation.Half, filter = PageFilter.Original),
                        maxSidePx = 400,
                    ).getOrThrow()

            val decoded = image.decode()
            assertEquals(400, image.width)
            assertEquals(200, image.height)
            assertTrue(decoded.getPixel(decoded.width - QUADRANT_SAMPLE, decoded.height - QUADRANT_SAMPLE).isRed())
        }

    @Test
    fun `girar 270 grados intercambia ancho y alto y lleva la esquina roja abajo a la izquierda`() =
        runTest {
            saveRedTopLeftQuadrant(width = 400, height = 200)

            val image =
                renderer
                    .page(
                        documentId,
                        Page(pageId, 0, rotation = Rotation.ThreeQuarters, filter = PageFilter.Original),
                        maxSidePx = 400,
                    ).getOrThrow()

            val decoded = image.decode()
            assertEquals(200, image.width)
            assertEquals(400, image.height)
            assertTrue(decoded.getPixel(QUADRANT_SAMPLE, decoded.height - QUADRANT_SAMPLE).isRed())
        }

    @Test
    fun `el filtro de documento deja el papel blanco y la tinta negra`() =
        runTest {
            saveRedTopLeftQuadrant(width = 400, height = 200)

            val decoded =
                renderer
                    .page(
                        documentId,
                        Page(pageId, 0, filter = PageFilter.Document),
                        maxSidePx = 400,
                    ).getOrThrow()
                    .decode()

            assertTrue(decoded.getPixel(decoded.width - QUADRANT_SAMPLE, decoded.height - QUADRANT_SAMPLE).isWhite())
        }

    @Test
    fun `el filtro de grises quita el color`() =
        runTest {
            saveRedTopLeftQuadrant(width = 400, height = 200)

            val decoded =
                renderer
                    .page(
                        documentId,
                        Page(pageId, 0, filter = PageFilter.Grayscale),
                        maxSidePx = 400,
                    ).getOrThrow()
                    .decode()

            val pixel = decoded.getPixel(QUADRANT_SAMPLE, QUADRANT_SAMPLE)
            assertTrue(Math.abs(Color.red(pixel) - Color.blue(pixel)) < WEAK)
        }

    @Test
    fun `la página aplica el recorte antes del giro`() =
        runTest {
            saveOriginal(width = 400, height = 400, drawSheet = true)

            val image =
                renderer
                    .page(
                        documentId,
                        Page(pageId, 0, sheet, Rotation.Quarter, PageFilter.Original),
                        maxSidePx = 400,
                    ).getOrThrow()

            val decoded = image.decode()
            assertTrue(decoded.getPixel(decoded.width / 2, decoded.height / 2).isBlue())
        }

    @Test
    fun `si falta la foto original de la página lo informa como ilegible`() =
        runTest {
            val result = renderer.page(documentId, Page(PageId("no-existe"), 0), maxSidePx = 400)

            assertEquals(StorageError.SourceUnreadable, (result.exceptionOrNull() as StorageFailure).error)
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
        const val QUADRANT_SAMPLE = 20
    }
}
