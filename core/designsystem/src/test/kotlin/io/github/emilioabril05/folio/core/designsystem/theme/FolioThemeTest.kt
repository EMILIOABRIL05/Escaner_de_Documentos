package io.github.emilioabril05.folio.core.designsystem.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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

    @Test
    fun `el modo oscuro usa la paleta oscura`() {
        var background = FolioColors.Light.background
        composeRule.setContent {
            FolioTheme(darkTheme = true) { background = MaterialTheme.colorScheme.background }
        }
        composeRule.waitForIdle()

        assertEquals(FolioColors.Dark.background, background)
    }
}
