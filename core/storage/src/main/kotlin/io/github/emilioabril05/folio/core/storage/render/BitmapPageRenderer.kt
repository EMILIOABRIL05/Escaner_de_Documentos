package io.github.emilioabril05.folio.core.storage.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.emilioabril05.folio.core.domain.EncodedImage
import io.github.emilioabril05.folio.core.domain.PageRenderer
import io.github.emilioabril05.folio.core.domain.StorageError
import io.github.emilioabril05.folio.core.domain.StorageFailure
import io.github.emilioabril05.folio.core.model.CropQuad
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.PageId
import io.github.emilioabril05.folio.core.storage.IoDispatcher
import io.github.emilioabril05.folio.core.storage.originalFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

// Calidad JPEG de las vistas previas: nítida en pantalla y liviana en memoria.
private const val PREVIEW_JPEG_QUALITY = 85
private const val CORNER_COUNT = 4
private const val COORDINATES_PER_POINT = 2

// Posición de cada esquina en CropQuad.corners.
private const val TOP_LEFT = 0
private const val TOP_RIGHT = 1
private const val BOTTOM_RIGHT = 2
private const val BOTTOM_LEFT = 3
private const val MIN_SIDE_PX = 1

/**
 * Dibuja páginas con `Matrix.setPolyToPoly` de Android, sin OpenCV: lleva las 4 esquinas del recorte a un
 * rectángulo y así corrige la perspectiva. Decodifica la foto reducida para no cargar más píxeles de los
 * necesarios (RNF-04) y libera cada bitmap antes de devolver el resultado.
 */
class BitmapPageRenderer
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : PageRenderer {
        override suspend fun original(
            documentId: DocumentId,
            pageId: PageId,
            maxSidePx: Int,
        ): Result<EncodedImage> = corrected(documentId, pageId, CropQuad.Full, maxSidePx)

        override suspend fun corrected(
            documentId: DocumentId,
            pageId: PageId,
            crop: CropQuad,
            maxSidePx: Int,
        ): Result<EncodedImage> =
            withContext(ioDispatcher) {
                val file = originalFile(context, documentId, pageId)
                val source = file.decodeForSide(maxSidePx)
                if (source == null) {
                    Result.failure(StorageFailure(StorageError.SourceUnreadable))
                } else {
                    val page = source.straighten(crop, maxSidePx)
                    source.recycle()
                    Result.success(page.encode().also { page.recycle() })
                }
            }

        // Muestreo en potencias de 2: el bitmap decodificado queda entre 1 y 2 veces el tamaño pedido.
        private fun File.decodeForSide(maxSidePx: Int): Bitmap? {
            if (!exists()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sampleSize = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= maxSidePx) {
                sampleSize *= 2
            }
            return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        }

        private fun Bitmap.straighten(
            crop: CropQuad,
            maxSidePx: Int,
        ): Bitmap {
            val corners = crop.corners.flatMap { listOf(it.x * width, it.y * height) }.toFloatArray()
            val sheetWidth = max(distance(corners, TOP_LEFT, TOP_RIGHT), distance(corners, BOTTOM_LEFT, BOTTOM_RIGHT))
            val sheetHeight = max(distance(corners, TOP_LEFT, BOTTOM_LEFT), distance(corners, TOP_RIGHT, BOTTOM_RIGHT))
            val scale = maxSidePx / max(sheetWidth, sheetHeight)
            val outWidth = (sheetWidth * scale).roundToInt().coerceAtLeast(MIN_SIDE_PX)
            val outHeight = (sheetHeight * scale).roundToInt().coerceAtLeast(MIN_SIDE_PX)

            val target =
                floatArrayOf(
                    0f,
                    0f,
                    outWidth.toFloat(),
                    0f,
                    outWidth.toFloat(),
                    outHeight.toFloat(),
                    0f,
                    outHeight.toFloat(),
                )
            val matrix = Matrix().apply { setPolyToPoly(corners, 0, target, 0, CORNER_COUNT) }
            val page = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
            Canvas(page).drawBitmap(this, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
            return page
        }

        // Distancia entre las esquinas número [from] y [to] de un arreglo x0, y0, x1, y1…
        private fun distance(
            corners: FloatArray,
            from: Int,
            to: Int,
        ): Float {
            val (fromX, toX) = from * COORDINATES_PER_POINT to to * COORDINATES_PER_POINT
            return hypot(corners[toX] - corners[fromX], corners[toX + 1] - corners[fromX + 1])
        }

        private fun Bitmap.encode(): EncodedImage {
            val bytes =
                ByteArrayOutputStream().use { stream ->
                    compress(Bitmap.CompressFormat.JPEG, PREVIEW_JPEG_QUALITY, stream)
                    stream.toByteArray()
                }
            return EncodedImage(bytes, width, height)
        }
    }
