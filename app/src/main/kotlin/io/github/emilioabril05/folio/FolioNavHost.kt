package io.github.emilioabril05.folio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.emilioabril05.folio.core.domain.PhotoCaptureTarget
import io.github.emilioabril05.folio.engine.api.DocumentScanner
import io.github.emilioabril05.folio.feature.capture.CaptureDestination
import io.github.emilioabril05.folio.feature.capture.CaptureRoute
import io.github.emilioabril05.folio.feature.review.crop.CropEditorDestination
import io.github.emilioabril05.folio.feature.review.crop.CropEditorRoute

/** Navegación entre pantallas. Solo la app conoce todos los features y los conecta (ADR-004). */
@Composable
fun FolioNavHost(
    scanner: DocumentScanner,
    photoTarget: PhotoCaptureTarget,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = CaptureDestination, modifier = modifier) {
        composable<CaptureDestination> {
            CaptureRoute(
                scanner = scanner,
                photoTarget = photoTarget,
                onEditCrop = { documentId, pageId ->
                    navController.navigate(CropEditorDestination(documentId.value, pageId.value))
                },
            )
        }
        composable<CropEditorDestination> {
            CropEditorRoute(onFinished = { navController.popBackStack() })
        }
    }
}
