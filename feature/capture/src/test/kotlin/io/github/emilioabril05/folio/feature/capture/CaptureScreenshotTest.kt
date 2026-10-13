package io.github.emilioabril05.folio.feature.capture

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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capturas de referencia de la pantalla de captura en claro, oscuro y con la letra al 200 %. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es-w360dp-h640dp-xhdpi")
class CaptureScreenshotTest {
    private enum class Variant(
        val darkTheme: Boolean,
        val fontScale: Float,
    ) {
        Claro(darkTheme = false, fontScale = 1f),
        Oscuro(darkTheme = true, fontScale = 1f),
        Fuente200(darkTheme = false, fontScale = 2f),
    }

    @Test
    fun listo() = captureAll("captura_listo", CaptureUiState.Idle)

    @Test
    fun guardado() = captureAll("captura_guardado", CaptureUiState.Saved(pageCount = 3))

    @Test
    fun `sin escáner`() = captureAll("captura_sin_escaner", CaptureUiState.Failed(CaptureFailure.ScannerUnavailable))

    private fun captureAll(
        name: String,
        state: CaptureUiState,
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
                            CaptureScreen(uiState = state, onScan = {}, onUseCamera = {}, onImport = {})
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val SCREENSHOT_DIR = "src/test/screenshots"
        const val CHANGE_THRESHOLD = 0.01F
        val OPTIONS =
            RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(changeThreshold = CHANGE_THRESHOLD),
            )
    }
}
