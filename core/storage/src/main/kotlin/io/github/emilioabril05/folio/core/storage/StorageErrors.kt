package io.github.emilioabril05.folio.core.storage

import io.github.emilioabril05.folio.core.domain.StorageError
import java.io.IOException

private const val NO_SPACE_CODE = "ENOSPC"
private const val NO_SPACE_TEXT = "no space left"

/** Traduce un fallo de disco al error del dominio, sin exponer rutas ni contenido. */
internal fun IOException.toStorageError(): StorageError {
    val text = message.orEmpty()
    val isOutOfSpace = text.contains(NO_SPACE_CODE) || text.contains(NO_SPACE_TEXT, ignoreCase = true)
    return if (isOutOfSpace) StorageError.NotEnoughSpace else StorageError.Unknown
}
