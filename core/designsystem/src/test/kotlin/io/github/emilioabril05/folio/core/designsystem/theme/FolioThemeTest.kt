package io.github.emilioabril05.folio.core.designsystem.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Method

@RunWith(AndroidJUnit4::class)
class FolioThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun reducedMotionWith(animatorScale: Float): Boolean {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, animatorScale)
        var reducedMotion: Boolean? = null
        composeRule.setContent {
            FolioTheme { reducedMotion = LocalReducedMotion.current }
        }
        composeRule.waitForIdle()
        return checkNotNull(reducedMotion)
    }

    @Test
    fun `con quitar animaciones activo el tema reduce el movimiento`() {
        assertEquals(true, reducedMotionWith(animatorScale = 0f))
    }

    @Test
    fun `con animaciones normales el tema no reduce el movimiento`() {
        assertEquals(false, reducedMotionWith(animatorScale = 1f))
    }

    private fun rolesOutsidePalette(darkTheme: Boolean): Map<String, Color> {
        lateinit var scheme: ColorScheme
        composeRule.setContent {
            FolioTheme(darkTheme = darkTheme) { scheme = MaterialTheme.colorScheme }
        }
        composeRule.waitForIdle()
        val palette = if (darkTheme) FolioColors.Dark else FolioColors.Light
        return scheme.roles().filterValues { it !in palette.allColors() }
    }

    @Test
    fun `en claro ningún rol de color sale de la paleta`() {
        assertEquals(emptyMap<String, Color>(), rolesOutsidePalette(darkTheme = false))
    }

    @Test
    fun `en oscuro ningún rol de color sale de la paleta`() {
        assertEquals(emptyMap<String, Color>(), rolesOutsidePalette(darkTheme = true))
    }

    @Test
    fun `el modo oscuro usa la paleta oscura`() {
        var background = FolioColors.Light.background
        composeRule.setContent {
            FolioTheme(darkTheme = true) { background = MaterialTheme.colorScheme.background }
        }
        composeRule.waitForIdle()

        assertEquals(FolioColors.Dark.background, background)
    }

    // Roles de error y el velo de los diálogos son semánticos y no forman parte del acento.
    private fun ColorScheme.roles(): Map<String, Color> =
        ColorScheme::class.java.methods
            .filter { it.isColorGetter() }
            .map { it.name.removePrefix("get").substringBefore('-') to Color((it.invoke(this) as Long).toULong()) }
            .filterNot { (name, _) -> name.contains("Error") || name.startsWith("Error") || name == "Scrim" }
            .toMap()

    private fun FolioPalette.allColors(): Set<Color> =
        FolioPalette::class.java.methods
            .filter { it.isColorGetter() }
            .map { Color((it.invoke(this) as Long).toULong()) }
            .toSet()

    // Color es una clase de valor: su getter compilado devuelve un long.
    private fun Method.isColorGetter(): Boolean =
        parameterCount == 0 && returnType == Long::class.javaPrimitiveType && name.startsWith("get")
}
