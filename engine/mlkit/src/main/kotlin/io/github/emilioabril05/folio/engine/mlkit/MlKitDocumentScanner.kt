package io.github.emilioabril05.folio.engine.mlkit

import android.app.Activity
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import io.github.emilioabril05.folio.engine.api.DocumentScanner
import io.github.emilioabril05.folio.engine.api.ScanError
import io.github.emilioabril05.folio.engine.api.ScanLaunch
import io.github.emilioabril05.folio.engine.api.ScanOptions
import io.github.emilioabril05.folio.engine.api.ScanOutcome
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Escáner de ML Kit: detección de bordes, recorte, filtros y disparo automático en el teléfono.
 * Usa la cámara de Google Play services, por eso la app no pide permiso de cámara.
 */
class MlKitDocumentScanner
    @Inject
    constructor() : DocumentScanner {
        override suspend fun prepare(
            activity: Activity,
            options: ScanOptions,
        ): ScanLaunch =
            try {
                val intentSender = GmsDocumentScanning.getClient(options.toMlKit()).getStartScanIntent(activity).await()
                ScanLaunch.Ready(IntentSenderRequest.Builder(intentSender).build())
            } catch (unavailable: MlKitException) {
                ScanLaunch.Failed(unavailable.toScanError())
            }

        override fun parseResult(
            resultCode: Int,
            data: Intent?,
        ): ScanOutcome {
            if (resultCode != Activity.RESULT_OK) return ScanOutcome.Cancelled
            val pages = GmsDocumentScanningResult.fromActivityResultIntent(data)?.pages.orEmpty()
            return if (pages.isEmpty()) {
                ScanOutcome.Failed(ScanError.Unknown)
            } else {
                ScanOutcome.Success(pages.map { it.imageUri })
            }
        }

        private fun ScanOptions.toMlKit(): GmsDocumentScannerOptions =
            GmsDocumentScannerOptions
                .Builder()
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setGalleryImportAllowed(false)
                .apply { pageLimit?.let(::setPageLimit) }
                .build()

        private fun MlKitException.toScanError(): ScanError =
            if (errorCode == MlKitException.UNAVAILABLE) ScanError.Unavailable else ScanError.Unknown
    }
