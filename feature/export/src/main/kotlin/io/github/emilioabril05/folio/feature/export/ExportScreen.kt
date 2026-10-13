package io.github.emilioabril05.folio.feature.export

import android.text.format.Formatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.emilioabril05.folio.core.designsystem.component.FolioPrimaryButton
import io.github.emilioabril05.folio.core.designsystem.theme.FolioSpacing
import io.github.emilioabril05.folio.core.model.ExportProfile

private val MinTouch = 56.dp
private val SelectedBorder = 2.dp
private val IdleBorder = 1.dp

/** "Enviar PDF" sin estado: calidad con su peso estimado y los destinos. */
@Composable
fun ExportScreen(
    uiState: ExportUiState,
    whatsAppAvailable: Boolean,
    onEvent: (ExportEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.export_close))
            }
            Text(
                text = stringResource(R.string.export_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(FolioSpacing.m)
                    .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(FolioSpacing.s),
        ) {
            Text(text = stringResource(R.string.export_quality), style = MaterialTheme.typography.labelLarge)
            ExportProfile.entries.forEach { profile ->
                ProfileOption(
                    profile = profile,
                    estimate = uiState.estimates[profile],
                    selected = profile == uiState.profile,
                    onSelect = { onEvent(ExportEvent.SelectProfile(profile)) },
                )
            }
            uiState.failure?.let { failure ->
                Text(
                    text = stringResource(failure.messageRes()),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
        Column(
            modifier = Modifier.padding(FolioSpacing.m),
            verticalArrangement = Arrangement.spacedBy(FolioSpacing.s),
        ) {
            OutlinedButton(
                onClick = { onEvent(ExportEvent.Send(ExportTarget.Save)) },
                enabled = !uiState.exporting,
                modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch),
            ) { Text(stringResource(R.string.export_save)) }
            if (whatsAppAvailable) {
                OutlinedButton(
                    onClick = { onEvent(ExportEvent.Send(ExportTarget.WhatsApp)) },
                    enabled = !uiState.exporting,
                    modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch),
                ) { Text(stringResource(R.string.export_whatsapp)) }
            }
            FolioPrimaryButton(
                text = stringResource(R.string.export_share),
                supportingText = uiState.estimates[uiState.profile]?.let { estimateText(it) },
                onClick = { onEvent(ExportEvent.Send(ExportTarget.Share)) },
                loading = uiState.exporting,
            )
        }
    }
}

@Composable
private fun ProfileOption(
    profile: ExportProfile,
    estimate: Long?,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        border =
            BorderStroke(
                if (selected) SelectedBorder else IdleBorder,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            ),
        color = MaterialTheme.colorScheme.surface,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouch)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(FolioSpacing.m),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(profile.nameRes()), style = MaterialTheme.typography.labelLarge)
                Text(
                    stringResource(profile.hintRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = estimate?.let { estimateText(it) } ?: stringResource(R.string.export_estimating),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun estimateText(bytes: Long): String =
    stringResource(R.string.export_estimate, Formatter.formatShortFileSize(LocalContext.current, bytes))

private fun ExportProfile.nameRes(): Int =
    when (this) {
        ExportProfile.Light -> R.string.export_profile_light
        ExportProfile.Balanced -> R.string.export_profile_balanced
        ExportProfile.High -> R.string.export_profile_high
    }

private fun ExportProfile.hintRes(): Int =
    when (this) {
        ExportProfile.Light -> R.string.export_profile_light_hint
        ExportProfile.Balanced -> R.string.export_profile_balanced_hint
        ExportProfile.High -> R.string.export_profile_high_hint
    }

private fun ExportFailure.messageRes(): Int =
    when (this) {
        ExportFailure.NotEnoughSpace -> R.string.export_error_space
        ExportFailure.Empty -> R.string.export_error_empty
        ExportFailure.Unknown -> R.string.export_error_unknown
    }
