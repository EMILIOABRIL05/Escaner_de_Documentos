package io.github.emilioabril05.folio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import io.github.emilioabril05.folio.core.designsystem.component.FolioEmptyState
import io.github.emilioabril05.folio.core.designsystem.theme.FolioTheme
import io.github.emilioabril05.folio.core.designsystem.R as DesignR

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FolioTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(modifier = Modifier.safeDrawingPadding(), contentAlignment = Alignment.Center) {
                        FolioEmptyState(
                            icon = painterResource(DesignR.drawable.ic_description),
                            title = stringResource(R.string.empty_title),
                            message = stringResource(R.string.empty_message),
                            actionText = stringResource(R.string.action_scan),
                            onAction = {},
                        )
                    }
                }
            }
        }
    }
}
