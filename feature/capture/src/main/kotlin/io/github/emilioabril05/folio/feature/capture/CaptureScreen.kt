package io.github.emilioabril05.folio.feature.capture

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.emilioabril05.folio.core.designsystem.component.FolioPrimaryButton
import io.github.emilioabril05.folio.core.designsystem.theme.FolioSpacing
import io.github.emilioabril05.folio.core.designsystem.R as DesignR

private val IllustrationSize = 64.dp

/**
 * Pantalla de captura sin estado: muestra [uiState], llama a [onScan] para abrir el escáner y a [onUseCamera]
 * para usar la cámara del sistema cuando el escáner no existe en el teléfono.
 */
@Composable
fun CaptureScreen(
    uiState: CaptureUiState,
    onScan: () -> Unit,
    onUseCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scannerMissing = uiState == CaptureUiState.Failed(CaptureFailure.ScannerUnavailable)
    val content = uiState.content()
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            StateMessage(content = content, saving = uiState is CaptureUiState.Saving)
        }
        if (scannerMissing && content.action != null) {
            TextButton(
                onClick = onScan,
                modifier = Modifier.fillMaxWidth().padding(horizontal = FolioSpacing.m),
            ) { Text(content.action) }
            FolioPrimaryButton(
                text = stringResource(R.string.capture_action_use_camera),
                onClick = onUseCamera,
                modifier = Modifier.padding(FolioSpacing.m),
            )
        } else if (content.action != null) {
            FolioPrimaryButton(
                text = content.action,
                onClick = onScan,
                modifier = Modifier.padding(FolioSpacing.m),
            )
        }
    }
}

@Composable
private fun StateMessage(
    content: StateContent,
    saving: Boolean,
) {
    Column(
        modifier = Modifier.padding(FolioSpacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FolioSpacing.s),
    ) {
        if (saving) {
            CircularProgressIndicator(modifier = Modifier.size(IllustrationSize))
        } else {
            Image(
                painter = painterResource(content.icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(content.iconTint),
                modifier = Modifier.size(IllustrationSize),
            )
        }
        Text(
            text = content.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.semantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                },
        )
        if (content.message != null) {
            Text(
                text = content.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private class StateContent(
    val icon: Int,
    val iconTint: Color,
    val title: String,
    val message: String?,
    val action: String?,
)

@Composable
private fun CaptureUiState.content(): StateContent {
    val accent = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    return when (this) {
        CaptureUiState.Idle -> {
            StateContent(
                icon = DesignR.drawable.ic_description,
                iconTint = accent,
                title = stringResource(R.string.capture_idle_title),
                message = stringResource(R.string.capture_idle_tip),
                action = stringResource(R.string.capture_action_scan),
            )
        }

        is CaptureUiState.Saving -> {
            StateContent(
                icon = DesignR.drawable.ic_description,
                iconTint = accent,
                title = pluralStringResource(R.plurals.capture_saving_title, pageCount, pageCount),
                message = null,
                action = null,
            )
        }

        is CaptureUiState.Saved -> {
            StateContent(
                icon = R.drawable.ic_check_circle,
                iconTint = accent,
                title = pluralStringResource(R.plurals.capture_saved_title, pageCount, pageCount),
                message = stringResource(R.string.capture_saved_message),
                action = stringResource(R.string.capture_action_scan_again),
            )
        }

        is CaptureUiState.Failed -> {
            StateContent(
                icon = R.drawable.ic_error,
                iconTint = error,
                title = stringResource(failure.titleRes()),
                message = stringResource(failure.solutionRes()),
                action = stringResource(R.string.capture_action_retry),
            )
        }
    }
}

private fun CaptureFailure.titleRes(): Int =
    when (this) {
        CaptureFailure.ScannerUnavailable -> R.string.capture_error_unavailable_title
        CaptureFailure.NotEnoughSpace -> R.string.capture_error_space_title
        CaptureFailure.UnreadablePage -> R.string.capture_error_unreadable_title
        CaptureFailure.Unknown -> R.string.capture_error_unknown_title
    }

private fun CaptureFailure.solutionRes(): Int =
    when (this) {
        CaptureFailure.ScannerUnavailable -> R.string.capture_error_unavailable_solution
        CaptureFailure.NotEnoughSpace -> R.string.capture_error_space_solution
        CaptureFailure.UnreadablePage -> R.string.capture_error_unreadable_solution
        CaptureFailure.Unknown -> R.string.capture_error_unknown_solution
    }
