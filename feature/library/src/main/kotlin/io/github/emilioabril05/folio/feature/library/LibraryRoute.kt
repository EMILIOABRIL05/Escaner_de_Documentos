package io.github.emilioabril05.folio.feature.library

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.emilioabril05.folio.core.model.DocumentId

/** Conecta la biblioteca (o la papelera) con su ViewModel y la navegación. */
@Composable
fun LibraryRoute(
    onOpenDocument: (DocumentId) -> Unit,
    onScan: () -> Unit,
    onOpenTrash: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnOpen by rememberUpdatedState(onOpenDocument)
    val trashedMessage = stringResource(R.string.library_trashed)
    val undoLabel = stringResource(R.string.library_undo)
    val errorMessage = stringResource(R.string.library_edit_failed)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LibraryEffect.OpenDocument -> currentOnOpen(effect.documentId)
            }
        }
    }
    LaunchedEffect(uiState.lastTrashed) {
        val trashed = uiState.lastTrashed ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(trashedMessage, undoLabel, duration = SnackbarDuration.Long)
        viewModel.onEvent(
            if (result == SnackbarResult.ActionPerformed) LibraryEvent.Restore(trashed) else LibraryEvent.UndoDismissed,
        )
    }
    LaunchedEffect(uiState.editFailed) {
        if (uiState.editFailed) snackbarHostState.showSnackbar(errorMessage)
    }

    LibraryScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        actions = LibraryScreenActions(onScan = onScan, onOpenTrash = onOpenTrash, onBack = onBack),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}
