package io.github.emilioabril05.folio.feature.review.crop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Conecta el editor de recorte con su ViewModel. Llama a [onFinished] al guardar o al volver atrás. */
@Composable
fun CropEditorRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CropEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CropEditorEffect.Finished -> currentOnFinished()
            }
        }
    }

    CropEditorScreen(uiState = uiState, onEvent = viewModel::onEvent, onBack = onFinished, modifier = modifier)
}
