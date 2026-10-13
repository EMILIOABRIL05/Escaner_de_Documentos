package io.github.emilioabril05.folio.feature.capture

import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.emilioabril05.folio.core.domain.PhotoCaptureTarget
import io.github.emilioabril05.folio.core.model.DocumentId
import io.github.emilioabril05.folio.core.model.PageId
import io.github.emilioabril05.folio.engine.api.DocumentScanner
import io.github.emilioabril05.folio.engine.api.ScanLaunch
import kotlinx.coroutines.launch

// Tope del selector de fotos por importación; suficiente para una prueba o un trámite largo.
private const val MAX_IMPORTED_PHOTOS = 50

/**
 * Conecta el escáner y la cámara del sistema con la pantalla de captura.
 * Cuando hay que recortar a mano una foto recién tomada, llama a [onEditCrop].
 */
@Composable
fun CaptureRoute(
    scanner: DocumentScanner,
    photoTarget: PhotoCaptureTarget,
    onEditCrop: (DocumentId, PageId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CaptureViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val currentOnEditCrop by rememberUpdatedState(onEditCrop)
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }

    val scanLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            viewModel.onEvent(CaptureEvent.ScanFinished(scanner.parseResult(result.resultCode, result.data)))
        }
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            val photo = pendingPhoto
            pendingPhoto = null
            if (saved && photo != null) viewModel.onEvent(CaptureEvent.PhotoTaken(Uri.parse(photo)))
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(MAX_IMPORTED_PHOTOS),
        ) { photos ->
            viewModel.onEvent(CaptureEvent.PhotosImported(photos))
        }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CaptureEffect.EditCrop -> currentOnEditCrop(effect.documentId, effect.pageId)
            }
        }
    }

    CaptureScreen(
        uiState = uiState,
        onScan = {
            if (activity != null) {
                scope.launch {
                    when (val launch = scanner.prepare(activity)) {
                        is ScanLaunch.Ready -> scanLauncher.launch(launch.request)
                        is ScanLaunch.Failed -> viewModel.onEvent(CaptureEvent.ScanFailed(launch.reason))
                    }
                }
            }
        },
        onUseCamera = {
            val target = photoTarget.newPhotoTarget().value
            pendingPhoto = target
            cameraLauncher.launch(Uri.parse(target))
        },
        onImport = {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        modifier = modifier,
    )
}
