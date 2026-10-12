package io.github.emilioabril05.folio.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val CardShadow = 1.dp

/** Tarjeta agrupada (estilo lista de ajustes de iOS): esquina de 16 dp, sombra suave, fondo de superficie. */
@Composable
fun FolioCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = CardShadow,
    ) {
        Column(content = content)
    }
}
