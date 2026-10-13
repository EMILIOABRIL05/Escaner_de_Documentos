package io.github.emilioabril05.folio.feature.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.emilioabril05.folio.core.domain.ExportedPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PDF_MIME = "application/pdf"
private val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

/** Conecta "Enviar PDF" con su ViewModel, la hoja de compartir, WhatsApp y el guardado en el teléfono. */
@Composable
fun ExportRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val whatsApp = remember { WHATSAPP_PACKAGES.firstOrNull { context.isInstalled(it) } }
    var pendingSave by remember { mutableStateOf<ExportedPdf?>(null) }
    val chooserTitle = stringResource(R.string.export_share)

    val saveLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PDF_MIME)) { target ->
            val pdf = pendingSave
            pendingSave = null
            if (target != null && pdf != null) {
                scope.launch { context.copy(Uri.parse(pdf.shareUri), target) }
            }
        }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ExportEffect.Deliver -> {
                    when (effect.target) {
                        ExportTarget.Share -> {
                            context.startActivity(Intent.createChooser(effect.pdf.shareIntent(), chooserTitle))
                        }

                        ExportTarget.WhatsApp -> {
                            context.startActivity(effect.pdf.shareIntent().setPackage(whatsApp))
                        }

                        ExportTarget.Save -> {
                            pendingSave = effect.pdf
                            saveLauncher.launch(effect.pdf.fileName)
                        }
                    }
                }
            }
        }
    }

    ExportScreen(
        uiState = uiState,
        whatsAppAvailable = whatsApp != null,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
    )
}

private fun ExportedPdf.shareIntent(): Intent {
    val uri = Uri.parse(shareUri)
    return Intent(Intent.ACTION_SEND)
        .setType(PDF_MIME)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .apply { clipData = ClipData.newRawUri(fileName, uri) }
}

private fun Context.isInstalled(packageName: String): Boolean =
    packageManager.getLaunchIntentForPackage(packageName) != null

private suspend fun Context.copy(
    from: Uri,
    to: Uri,
) = withContext(Dispatchers.IO) {
    contentResolver.openInputStream(from)?.use { input ->
        contentResolver.openOutputStream(to)?.use { output -> input.copyTo(output) }
    }
}
