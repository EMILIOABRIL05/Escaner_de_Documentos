package io.github.emilioabril05.folio.feature.library

/** Acciones de navegación de la biblioteca: escanear, abrir la papelera y volver. */
class LibraryScreenActions(
    val onScan: () -> Unit,
    val onOpenTrash: () -> Unit,
    val onBack: () -> Unit,
)
