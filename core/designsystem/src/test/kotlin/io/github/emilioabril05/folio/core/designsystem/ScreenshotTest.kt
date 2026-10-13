package io.github.emilioabril05.folio.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.emilioabril05.folio.core.designsystem.component.FolioCard
import io.github.emilioabril05.folio.core.designsystem.component.FolioEmptyState
import io.github.emilioabril05.folio.core.designsystem.component.FolioPrimaryButton
import io.github.emilioabril05.folio.core.designsystem.theme.FolioSpacing
import io.github.emilioabril05.folio.core.designsystem.theme.FolioTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capturas de referencia de cada componente en claro, oscuro y con la letra al 200 %. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class ScreenshotTest {
    private enum class Variant(
        val darkTheme: Boolean,
        val fontScale: Float,
    ) {
        Claro(darkTheme = false, fontScale = 1f),
        Oscuro(darkTheme = true, fontScale = 1f),
        Fuente200(darkTheme = false, fontScale = 2f),
    }

    @Test
    fun `botón principal`() =
        captureAll("boton_principal") {
            FolioPrimaryButton(text = "Enviar PDF", onClick = {}, supportingText = "3 pág. · ~420 KB")
        }

    @Test
    fun tarjeta() =
        captureAll("tarjeta") {
            FolioCard {
                Text(text = "Perfil por defecto", modifier = Modifier.padding(FolioSpacing.m))
                Text(text = "Equilibrado", modifier = Modifier.padding(FolioSpacing.m))
            }
        }

    @Test
    fun `estado vacío`() =
        captureAll("estado_vacio") {
            FolioEmptyState(
                icon = painterResource(R.drawable.ic_description),
                title = "Escanea tu primera hoja",
                message = "Tus documentos aparecerán aquí.",
                actionText = "Escanear",
                onAction = {},
            )
        }

    private fun captureAll(
        name: String,
        content: @Composable () -> Unit,
    ) {
        Variant.entries.forEach { variant ->
            captureRoboImage(
                filePath = "$SCREENSHOT_DIR/${name}_${variant.name.lowercase()}.png",
                roborazziOptions = OPTIONS,
            ) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, variant.fontScale)) {
                    FolioTheme(darkTheme = variant.darkTheme) {
                        Box(
                            modifier =
                                Modifier
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(FolioSpacing.m),
                        ) {
                            content()
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
