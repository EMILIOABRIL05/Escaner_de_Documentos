package io.github.emilioabril05.folio.core.storage.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import io.github.emilioabril05.folio.core.model.PageFilter
import kotlin.math.max

private const val GRAYSCALE_CONTRAST = 1.25f
private const val ENHANCED_SATURATION = 1.3f
private const val ENHANCED_CONTRAST = 1.15f
private const val CHANNEL_MID = 128f

// El fondo se estima con una copia 16 veces más chica (dos pasos de 1/4): sin texto, solo luz y sombras.
private const val BACKGROUND_STEP = 4
private const val BACKGROUND_STEPS = 2
private const val MIN_BACKGROUND_SIDE_PX = 1

// Un píxel más claro que esta fracción del fondo es papel; el resto es tinta.
private const val PAPER_THRESHOLD = 0.85f
private const val MIN_BACKGROUND_LEVEL = 1
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val BYTE_MASK = 0xFF
private const val OPAQUE = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
private const val BLACK = OPAQUE

// Pesos de luminancia (Rec. 601).
private const val LUMA_RED = 0.299f
private const val LUMA_GREEN = 0.587f
private const val LUMA_BLUE = 0.114f

/** Devuelve el bitmap con [filter] aplicado y libera el recibido si se creó uno nuevo. */
internal fun Bitmap.applyFilter(filter: PageFilter): Bitmap =
    when (filter) {
        PageFilter.Original -> this
        PageFilter.Grayscale -> recolored(grayscaleMatrix())
        PageFilter.EnhancedColor -> recolored(enhancedMatrix())
        PageFilter.Document -> normalizedBlackAndWhite()
    }

private fun contrastMatrix(contrast: Float) =
    ColorMatrix(
        floatArrayOf(
            contrast,
            0f,
            0f,
            0f,
            CHANNEL_MID * (1 - contrast),
            0f,
            contrast,
            0f,
            0f,
            CHANNEL_MID * (1 - contrast),
            0f,
            0f,
            contrast,
            0f,
            CHANNEL_MID * (1 - contrast),
            0f,
            0f,
            0f,
            1f,
            0f,
        ),
    )

private fun grayscaleMatrix() =
    ColorMatrix().apply {
        setSaturation(0f)
        postConcat(contrastMatrix(GRAYSCALE_CONTRAST))
    }

private fun enhancedMatrix() =
    ColorMatrix().apply {
        setSaturation(ENHANCED_SATURATION)
        postConcat(contrastMatrix(ENHANCED_CONTRAST))
    }

private fun Bitmap.recolored(matrix: ColorMatrix): Bitmap {
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
    Canvas(result).drawBitmap(this, 0f, 0f, paint)
    recycle()
    return result
}

// Gris dividido por el fondo estimado: quita sombras y deja papel blanco y tinta negra.
private fun Bitmap.normalizedBlackAndWhite(): Bitmap {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    val background = blurredBackground()
    val result = IntArray(pixels.size)
    for (index in pixels.indices) {
        val gray = pixels[index].luma()
        val paper = max(background[index].luma(), MIN_BACKGROUND_LEVEL.toFloat())
        result[index] = if (gray / paper >= PAPER_THRESHOLD) WHITE else BLACK
    }
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    output.setPixels(result, 0, width, 0, 0, width, height)
    recycle()
    return output
}

private fun Bitmap.blurredBackground(): IntArray {
    var small = this
    repeat(BACKGROUND_STEPS) {
        val next =
            Bitmap.createScaledBitmap(
                small,
                max(small.width / BACKGROUND_STEP, MIN_BACKGROUND_SIDE_PX),
                max(small.height / BACKGROUND_STEP, MIN_BACKGROUND_SIDE_PX),
                true,
            )
        if (small !== this) small.recycle()
        small = next
    }
    val enlarged = Bitmap.createScaledBitmap(small, width, height, true)
    if (small !== this && small !== enlarged) small.recycle()
    val background = IntArray(width * height)
    enlarged.getPixels(background, 0, width, 0, 0, width, height)
    if (enlarged !== this) enlarged.recycle()
    return background
}

private fun Int.luma(): Float =
    LUMA_RED * ((this shr RED_SHIFT) and BYTE_MASK) +
        LUMA_GREEN * ((this shr GREEN_SHIFT) and BYTE_MASK) +
        LUMA_BLUE * (this and BYTE_MASK)
