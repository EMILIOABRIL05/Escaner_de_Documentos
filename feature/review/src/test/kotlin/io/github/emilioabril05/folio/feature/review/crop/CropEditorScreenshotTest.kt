package io.github.emilioabril05.folio.feature.review.crop

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.emilioabril05.folio.core.designsystem.theme.FolioTheme
import io.github.emilioabril05.folio.core.domain.EncodedImage
import io.github.emilioabril05.folio.core.model.CropQuad
import io.github.emilioabril05.folio.core.model.NormalizedPoint
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

/** Capturas de referencia del editor de recorte en claro, oscuro y con la letra al 200 %. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es-w360dp-h640dp-xhdpi")
class CropEditorScreenshotTest {
    private enum class Variant(
        val darkTheme: Boolean,
        val fontScale: Float,
    ) {
        Claro(darkTheme = false, fontScale = 1f),
        Oscuro(darkTheme = true, fontScale = 1f),
        Fuente200(darkTheme = false, fontScale = 2f),
    }

    private val tilted =
        CropQuad(
            topLeft = NormalizedPoint(0.15f, 0.12f),
            topRight = NormalizedPoint(0.85f, 0.18f),
            bottomRight = NormalizedPoint(0.8f, 0.88f),
            bottomLeft = NormalizedPoint(0.1f, 0.82f),
        )

    @Test
    fun editando() =
        captureAll("recorte_editando", CropEditorUiState.Editing(photo = photo(), crop = tilted, canUndo = true))

    @Test
    fun `no se pudo abrir`() = captureAll("recorte_error", CropEditorUiState.Failed)

    private fun captureAll(
        name: String,
        state: CropEditorUiState,
    ) {
        Variant.entries.forEach { variant ->
            captureRoboImage(
                filePath = "$SCREENSHOT_DIR/${name}_${variant.name.lowercase()}.png",
                roborazziOptions = OPTIONS,
            ) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, variant.fontScale)) {
                    FolioTheme(darkTheme = variant.darkTheme) {
                        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                            CropEditorScreen(uiState = state, onEvent = {}, onBack = {})
                        }
                    }
                }
            }
        }
    }

    // Una "foto" de una hoja blanca inclinada sobre una mesa oscura.
    private fun photo(): EncodedImage {
        val bitmap = Bitmap.createBitmap(PHOTO_WIDTH, PHOTO_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.DKGRAY)
        val sheet =
            android.graphics.Path().apply {
                tilted.corners.forEachIndexed { index, point ->
                    val x = point.x * PHOTO_WIDTH
                    val y = point.y * PHOTO_HEIGHT
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
        canvas.drawPath(sheet, Paint().apply { color = Color.WHITE })
        val bytes =
            ByteArrayOutputStream().use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)
                it.toByteArray()
            }
        return EncodedImage(bytes, PHOTO_WIDTH, PHOTO_HEIGHT)
    }

    private companion object {
        const val SCREENSHOT_DIR = "src/test/screenshots"
        const val CHANGE_THRESHOLD = 0.01F
        const val PHOTO_WIDTH = 600
        const val PHOTO_HEIGHT = 800
        const val JPEG_QUALITY = 90
        val OPTIONS =
            RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(changeThreshold = CHANGE_THRESHOLD),
            )
    }
}
