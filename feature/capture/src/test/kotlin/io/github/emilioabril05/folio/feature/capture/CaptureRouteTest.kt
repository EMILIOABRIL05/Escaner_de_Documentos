package io.github.emilioabril05.folio.feature.capture

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.emilioabril05.folio.core.designsystem.theme.FolioTheme
import io.github.emilioabril05.folio.core.domain.CreateDocumentFromScan
import io.github.emilioabril05.folio.core.domain.DocumentTitleFormatter
import io.github.emilioabril05.folio.core.domain.PageSource
import io.github.emilioabril05.folio.core.testing.FakeDocumentRepository
import io.github.emilioabril05.folio.core.testing.FakePageStore
import io.github.emilioabril05.folio.core.testing.SequentialIdProvider
import io.github.emilioabril05.folio.engine.api.DocumentScanner
import io.github.emilioabril05.folio.engine.api.ScanError
import io.github.emilioabril05.folio.engine.api.ScanLaunch
import io.github.emilioabril05.folio.engine.api.ScanOptions
import io.github.emilioabril05.folio.engine.api.ScanOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Clock

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es")
class CaptureRouteTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val unavailableScanner =
        object : DocumentScanner {
            override suspend fun prepare(
                activity: Activity,
                options: ScanOptions,
            ): ScanLaunch = ScanLaunch.Failed(ScanError.Unavailable)

            override fun parseResult(
                resultCode: Int,
                data: Intent?,
            ): ScanOutcome = ScanOutcome.Cancelled
        }

    @Test
    fun `si el escáner no se puede abrir la pantalla lo explica`() {
        val viewModel =
            CaptureViewModel(
                CreateDocumentFromScan(
                    FakePageStore(),
                    FakeDocumentRepository(),
                    SequentialIdProvider(),
                    DocumentTitleFormatter { it.toString() },
                    Clock.systemUTC(),
                ),
            )
        composeRule.setContent {
            FolioTheme {
                CaptureRoute(
                    scanner = unavailableScanner,
                    photoTarget = { PageSource("content://folio.files/captures/foto.jpg") },
                    onEditCrop = { _, _ -> },
                    viewModel = viewModel,
                )
            }
        }

        composeRule.onNode(hasText("Escanear") and hasClickAction()).performClick()

        composeRule.waitUntil { viewModel.uiState.value is CaptureUiState.Failed }
        composeRule.onNodeWithText("Este teléfono no tiene el escáner").assertExists()
    }
}
