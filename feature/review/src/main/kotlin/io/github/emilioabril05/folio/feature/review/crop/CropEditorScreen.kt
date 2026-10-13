package io.github.emilioabril05.folio.feature.review.crop

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.emilioabril05.folio.core.designsystem.component.FolioPrimaryButton
import io.github.emilioabril05.folio.core.designsystem.theme.FolioSpacing
import io.github.emilioabril05.folio.core.domain.EncodedImage
import io.github.emilioabril05.folio.feature.review.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val PreviewSize = 56.dp
private val PreviewCorner = 8.dp

/** Editor de recorte sin estado: muestra [uiState] y comunica cada acción con [onEvent]. */
@Composable
fun CropEditorScreen(
    uiState: CropEditorUiState,
    onEvent: (CropEditorEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        CropEditorUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }

        CropEditorUiState.Failed -> {
            OpenFailed(onBack = onBack, modifier = modifier)
        }

        is CropEditorUiState.Editing -> {
            Editing(state = uiState, onEvent = onEvent, modifier = modifier)
        }
    }
}

@Composable
private fun Editing(
    state: CropEditorUiState.Editing,
    onEvent: (CropEditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val photo by rememberDecoded(state.photo)
    val preview by rememberDecoded(state.preview)
    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { onEvent(CropEditorEvent.Undo) }, enabled = state.canUndo) {
                Icon(painterResource(R.drawable.ic_undo), contentDescription = stringResource(R.string.crop_undo))
            }
            IconButton(onClick = { onEvent(CropEditorEvent.Redo) }, enabled = state.canRedo) {
                Icon(painterResource(R.drawable.ic_redo), contentDescription = stringResource(R.string.crop_redo))
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(FolioSpacing.l)) {
            photo?.let {
                CropCanvas(
                    photo = it,
                    crop = state.crop,
                    onEvent = onEvent,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (state.saveFailed) {
            Text(
                text = stringResource(R.string.crop_save_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FolioSpacing.m)
                        .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(FolioSpacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FolioSpacing.s),
        ) {
            preview?.let {
                Image(
                    bitmap = it,
                    contentDescription = stringResource(R.string.crop_preview),
                    modifier = Modifier.size(PreviewSize).clip(RoundedCornerShape(PreviewCorner)),
                )
            }
            TextButton(onClick = { onEvent(CropEditorEvent.Reset) }) { Text(stringResource(R.string.crop_reset)) }
            FolioPrimaryButton(
                text = stringResource(R.string.crop_done),
                onClick = { onEvent(CropEditorEvent.Done) },
                loading = state.saving,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OpenFailed(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(FolioSpacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FolioSpacing.s, Alignment.CenterVertically),
        ) {
            Text(
                text = stringResource(R.string.crop_open_failed_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.crop_open_failed_solution),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        FolioPrimaryButton(
            text = stringResource(R.string.crop_back),
            onClick = onBack,
            modifier = Modifier.padding(FolioSpacing.m),
        )
    }
}

// Decodifica el JPEG fuera del hilo principal.
@Composable
private fun rememberDecoded(image: EncodedImage?) =
    produceState<ImageBitmap?>(initialValue = null, image) {
        value =
            image?.let {
                withContext(Dispatchers.Default) {
                    BitmapFactory.decodeByteArray(it.bytes, 0, it.bytes.size)?.asImageBitmap()
                }
            }
    }
