package io.github.emilioabril05.folio.engine.mlkit

import android.app.Activity
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.emilioabril05.folio.engine.api.ScanError
import io.github.emilioabril05.folio.engine.api.ScanOutcome
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MlKitDocumentScannerTest {
    private val scanner = MlKitDocumentScanner()

    @Test
    fun `cerrar el escáner sin terminar es una cancelación`() {
        assertEquals(ScanOutcome.Cancelled, scanner.parseResult(Activity.RESULT_CANCELED, null))
    }

    @Test
    fun `un resultado correcto sin páginas es un error`() {
        assertEquals(ScanOutcome.Failed(ScanError.Unknown), scanner.parseResult(Activity.RESULT_OK, Intent()))
    }
}
