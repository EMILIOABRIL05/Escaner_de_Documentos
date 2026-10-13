package io.github.emilioabril05.folio.feature.review.crop

import io.github.emilioabril05.folio.core.model.CropQuad

/**
 * Historial de deshacer y rehacer del recorte. Un arrastre completo cuenta como un solo cambio:
 * se marca su inicio con [beginDrag] y se registra al terminar con [endDrag].
 */
internal class CropHistory {
    private val undoStack = ArrayDeque<CropQuad>()
    private val redoStack = ArrayDeque<CropQuad>()
    private var cropBeforeDrag: CropQuad? = null

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val isDragging: Boolean get() = cropBeforeDrag != null

    /** Registra que el recorte deja de ser [previous]. */
    fun record(previous: CropQuad) {
        undoStack.addLast(previous)
        redoStack.clear()
    }

    /** Marca el recorte al empezar un arrastre; las llamadas siguientes del mismo arrastre no cambian nada. */
    fun beginDrag(current: CropQuad) {
        if (cropBeforeDrag == null) cropBeforeDrag = current
    }

    /** Cierra el arrastre; si el recorte cambió, lo registra como un solo cambio. Devuelve si hubo cambio. */
    fun endDrag(current: CropQuad): Boolean {
        val start = cropBeforeDrag
        cropBeforeDrag = null
        val changed = start != null && start != current
        if (changed && start != null) record(start)
        return changed
    }

    /** El recorte anterior a [current], o `null` si no hay nada que deshacer. */
    fun undo(current: CropQuad): CropQuad? = step(from = undoStack, to = redoStack, current = current)

    /** El recorte deshecho más reciente, o `null` si no hay nada que rehacer. */
    fun redo(current: CropQuad): CropQuad? = step(from = redoStack, to = undoStack, current = current)

    private fun step(
        from: ArrayDeque<CropQuad>,
        to: ArrayDeque<CropQuad>,
        current: CropQuad,
    ): CropQuad? {
        val target = from.removeLastOrNull() ?: return null
        to.addLast(current)
        return target
    }
}
