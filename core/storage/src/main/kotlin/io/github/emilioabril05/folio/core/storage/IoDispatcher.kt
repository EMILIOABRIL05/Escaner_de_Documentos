package io.github.emilioabril05.folio.core.storage

import javax.inject.Qualifier

/** Marca el dispatcher para trabajo de entrada y salida de disco. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
