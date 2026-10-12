package io.github.emilioabril05.folio.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf

/** Duraciones de movimiento de Folio. Se anulan si el sistema tiene "Quitar animaciones" activo. */
object FolioMotion {
    const val SHORT_MILLIS = 200
    const val MEDIUM_MILLIS = 300

    /** Duración efectiva de una animación: cero si el usuario pidió quitar animaciones. */
    fun durationMillis(
        base: Int,
        reducedMotion: Boolean,
    ): Int = if (reducedMotion) 0 else base
}

/** `true` cuando el sistema tiene "Quitar animaciones" activo. Lo provee `FolioTheme`. */
val LocalReducedMotion = staticCompositionLocalOf { false }
