package io.github.emilioabril05.folio.feature.capture

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.emilioabril05.folio.engine.api.DocumentScanner
import io.github.emilioabril05.folio.engine.api.ScanLaunch
import kotlinx.coroutines.launch

/** Conecta el escáner con la pantalla de captura. */
@Composable
fun CaptureRoute(
    scanner: DocumentScanner,
    modifier: Modifier = Modifier,
    viewModel: CaptureViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            viewModel.onEvent(CaptureEvent.ScanFinished(scanner.parseResult(result.resultCode, result.data)))
        }

    CaptureScreen(
        uiState = uiState,
        onScan = {
            if (activity != null) {
                scope.launch {
                    when (val launch = scanner.prepare(activity)) {
                        is ScanLaunch.Ready -> launcher.launch(launch.request)
                        is ScanLaunch.Failed -> viewModel.onEvent(CaptureEvent.ScanFailed(launch.reason))
                    }
                }
            }
        },
        modifier = modifier,
    )
}
