package io.github.emilioabril05.folio.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.emilioabril05.folio.core.designsystem.theme.FolioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolioCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `la tarjeta muestra todo su contenido`() {
        composeRule.setContent {
            FolioTheme {
                FolioCard {
                    Text("Perfil por defecto")
                    Text("Equilibrado")
                }
            }
        }

        composeRule.onNodeWithText("Perfil por defecto").assertIsDisplayed()
        composeRule.onNodeWithText("Equilibrado").assertIsDisplayed()
    }
}
