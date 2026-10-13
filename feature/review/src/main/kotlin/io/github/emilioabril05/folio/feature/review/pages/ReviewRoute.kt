package io.github.emilioabril05.folio.feature.review.pages

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
import io.github.emilioabril05.folio.core.model.PageId
import io.github.emilioabril05.folio.feature.review.R

/** Conecta la revisión con su ViewModel, el aviso de "Deshacer" y la navegación. */
@Composable
fun ReviewRoute(
    onEditCrop: (DocumentId, PageId) -> Unit,
    onAddPages: (DocumentId) -> Unit,
    onExport: (DocumentId) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnEditCrop by rememberUpdatedState(onEditCrop)
    val currentOnAddPages by rememberUpdatedState(onAddPages)
    val currentOnExport by rememberUpdatedState(onExport)
    val deletedMessage = stringResource(R.string.review_page_deleted)
    val undoLabel = stringResource(R.string.review_undo)
    val canUndo = (uiState as? ReviewUiState.Ready)?.canUndoDelete == true

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReviewEffect.EditCrop -> currentOnEditCrop(effect.documentId, effect.pageId)
                is ReviewEffect.AddPages -> currentOnAddPages(effect.documentId)
                is ReviewEffect.Export -> currentOnExport(effect.documentId)
            }
        }
    }
    LaunchedEffect(canUndo) {
        if (!canUndo) return@LaunchedEffect
        val result =
            snackbarHostState.showSnackbar(
                message = deletedMessage,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Long,
            )
        viewModel.onEvent(
            if (result == SnackbarResult.ActionPerformed) ReviewEvent.UndoDelete else ReviewEvent.DeleteConfirmed,
        )
    }

    ReviewScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = {
            viewModel.onEvent(ReviewEvent.DeleteConfirmed)
            onBack()
        },
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}
