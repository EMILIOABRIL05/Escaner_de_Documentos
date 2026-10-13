package io.github.emilioabril05.folio.feature.capture

import kotlinx.serialization.Serializable

/** Destino de navegación de la captura. Con [documentId], las páginas se agregan a ese documento. */
@Serializable
data class CaptureDestination(
    val documentId: String? = null,
)
