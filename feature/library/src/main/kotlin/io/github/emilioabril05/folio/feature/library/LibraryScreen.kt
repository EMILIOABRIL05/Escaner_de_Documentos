package io.github.emilioabril05.folio.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.emilioabril05.folio.core.designsystem.component.FolioPrimaryButton
import io.github.emilioabril05.folio.core.designsystem.theme.FolioSpacing
import io.github.emilioabril05.folio.core.domain.TRASH_RETENTION_DAYS
import io.github.emilioabril05.folio.core.designsystem.R as DesignR

/** Biblioteca o papelera sin estado. */
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    actions: LibraryScreenActions,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    var renaming by remember { mutableStateOf<LibraryItem?>(null) }
    var deleting by remember { mutableStateOf<LibraryItem?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LibraryContent(
            uiState = uiState,
            onEvent = onEvent,
            actions = actions,
            onRename = { renaming = it },
            onDeleteForever = { deleting = it },
        )
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    renaming?.let { item ->
        RenameDialog(
            current = item.summary.title,
            onConfirm = { title ->
                onEvent(LibraryEvent.Rename(item.summary.id, title))
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { item ->
        DeleteForeverDialog(
            title = item.summary.title,
            onConfirm = {
                onEvent(LibraryEvent.DeleteForever(item.summary.id))
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun LibraryContent(
    uiState: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    actions: LibraryScreenActions,
    onRename: (LibraryItem) -> Unit,
    onDeleteForever: (LibraryItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Header(uiState = uiState, onEvent = onEvent, onOpenTrash = actions.onOpenTrash, onBack = actions.onBack)
        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.items.isEmpty() -> {
                    EmptyMessage(uiState)
                }

                else -> {
                    DocumentList(
                        uiState = uiState,
                        onEvent = onEvent,
                        onRename = onRename,
                        onDeleteForever = onDeleteForever,
                    )
                }
            }
        }
        if (!uiState.isTrash) {
            FolioPrimaryButton(
                text = stringResource(R.string.library_scan),
                onClick = actions.onScan,
                modifier = Modifier.padding(FolioSpacing.m),
            )
        }
    }
}

@Composable
private fun Header(
    uiState: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    onOpenTrash: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = FolioSpacing.m)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (uiState.isTrash) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.library_back))
                }
            }
            Text(
                text = stringResource(if (uiState.isTrash) R.string.library_trash_title else R.string.library_title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f).padding(vertical = FolioSpacing.s).semantics { heading() },
            )
            if (!uiState.isTrash) {
                IconButton(onClick = onOpenTrash) {
                    Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.library_open_trash))
                }
            }
        }
        if (uiState.isTrash) {
            Text(
                text = stringResource(R.string.library_trash_hint, TRASH_RETENTION_DAYS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { onEvent(LibraryEvent.QueryChanged(it)) },
                placeholder = { Text(stringResource(R.string.library_search)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EmptyMessage(uiState: LibraryUiState) {
    val message =
        when {
            uiState.isTrash -> R.string.library_trash_empty
            uiState.query.isNotBlank() -> R.string.library_no_results
            else -> R.string.library_empty
        }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(FolioSpacing.l),
    ) {
        Icon(
            painterResource(DesignR.drawable.ic_description),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Text(stringResource(message), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun RenameDialog(
    current: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_rename)) },
        text = { OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = { onConfirm(title) }, enabled = title.isNotBlank()) {
                Text(stringResource(R.string.library_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.library_cancel)) } },
    )
}

@Composable
private fun DeleteForeverDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_delete_forever_title)) },
        text = { Text(stringResource(R.string.library_delete_forever_message, title)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.library_delete_forever)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.library_cancel)) } },
    )
}
