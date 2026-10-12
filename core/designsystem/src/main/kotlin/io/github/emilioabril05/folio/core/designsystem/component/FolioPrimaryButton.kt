package io.github.emilioabril05.folio.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.emilioabril05.folio.core.designsystem.theme.FolioSpacing

private val MinHeight = 56.dp
private val ProgressSize = 24.dp
private val ProgressStroke = 2.dp

/**
 * Acción principal de una pantalla ("Escanear", "Enviar PDF"): ancho completo, al menos 56 dp de alto y
 * crece con el tamaño de letra del sistema. [supportingText] muestra el estado, por ejemplo "3 pág. · ~420 KB".
 * Mientras [loading] es `true` no responde a toques.
 */
@Composable
fun FolioPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val description = listOfNotNull(text, supportingText).joinToString(separator = ", ")
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = FolioSpacing.m, vertical = FolioSpacing.s),
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = MinHeight)
                .semantics { contentDescription = description },
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(ProgressSize),
                strokeWidth = ProgressStroke,
            )
        } else {
            ButtonLabels(text = text, supportingText = supportingText)
        }
    }
}

@Composable
private fun ButtonLabels(
    text: String,
    supportingText: String?,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
        if (supportingText != null) {
            Text(text = supportingText, style = MaterialTheme.typography.labelMedium)
        }
    }
}
