package io.github.emilioabril05.folio.engine.api

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.IntentSenderRequest

/** Opciones de un escaneo. */
data class ScanOptions(
    /** Máximo de páginas por escaneo; `null` es sin límite. */
    val pageLimit: Int? = null,
)

/** Por qué no se pudo escanear. */
sealed interface ScanError {
    /** El escáner no existe en este teléfono (sin Google Play services o con poca memoria). */
    data object Unavailable : ScanError

    /** El escáner falló por otro motivo. */
    data object Unknown : ScanError
}

/** Preparación del escáner antes de abrirlo. */
sealed interface ScanLaunch {
    /** Listo para abrirse con `ActivityResultContracts.StartIntentSenderForResult`. */
    data class Ready(
        val request: IntentSenderRequest,
    ) : ScanLaunch

    /** No se puede abrir el escáner. */
    data class Failed(
        val reason: ScanError,
    ) : ScanLaunch
}

/** Resultado de un escaneo. */
sealed interface ScanOutcome {
    /** Páginas escaneadas, en orden, como imágenes JPEG legibles por la app. */
    data class Success(
        val pages: List<Uri>,
    ) : ScanOutcome

    /** El usuario cerró el escáner sin terminar. */
    data object Cancelled : ScanOutcome

    /** El escáner devolvió un error. */
    data class Failed(
        val reason: ScanError,
    ) : ScanOutcome
}

/**
 * Motor de escaneo intercambiable (ML Kit hoy, OpenCV en v2).
 *
 * El escaneo se hace en dos pasos porque algunos motores necesitan preparar su pantalla de forma asíncrona:
 * [prepare] obtiene lo necesario para abrirlo y [parseResult] interpreta lo que devuelve.
 */
interface DocumentScanner {
    /** Prepara el escáner para abrirlo desde [activity]. */
    suspend fun prepare(
        activity: Activity,
        options: ScanOptions = ScanOptions(),
    ): ScanLaunch

    /** Interpreta el resultado que el escáner entregó a la actividad. */
    fun parseResult(
        resultCode: Int,
        data: Intent?,
    ): ScanOutcome
}
