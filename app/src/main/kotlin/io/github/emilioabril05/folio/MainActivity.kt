package io.github.emilioabril05.folio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import io.github.emilioabril05.folio.core.designsystem.theme.FolioTheme
import io.github.emilioabril05.folio.core.domain.PhotoCaptureTarget
import io.github.emilioabril05.folio.engine.api.DocumentScanner
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var scanner: DocumentScanner

    @Inject
    lateinit var photoTarget: PhotoCaptureTarget

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FolioTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    FolioNavHost(
                        scanner = scanner,
                        photoTarget = photoTarget,
                        modifier = Modifier.safeDrawingPadding(),
                    )
                }
            }
        }
    }
}
