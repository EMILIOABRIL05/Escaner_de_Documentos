package io.github.emilioabril05.folio

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    @Config(qualifiers = "es")
    fun `muestra el estado vacío con la acción Escanear`() {
        composeRule.onNodeWithText("Escanea tu primera hoja").assertIsDisplayed()
        composeRule.onNode(hasText("Escanear") and hasClickAction()).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en")
    fun `en inglés muestra el estado vacío traducido`() {
        composeRule.onNodeWithText("Scan your first page").assertIsDisplayed()
        composeRule.onNode(hasText("Scan") and hasClickAction()).assertIsDisplayed()
    }
}
