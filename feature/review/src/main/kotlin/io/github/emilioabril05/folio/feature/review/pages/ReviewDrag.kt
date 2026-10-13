package io.github.emilioabril05.folio.feature.review.pages

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import io.github.emilioabril05.folio.core.model.PageId

private const val DRAGGED_SCALE = 1.05f
private const val DRAGGED_Z = 1f

// Clave de la celda "Agregar páginas" en la cuadrícula; no es un destino al reordenar.
internal const val ADD_CELL_KEY = "add"

// Qué página se está arrastrando y cuánto se ha movido.
internal class PageDragState {
    var dragging by mutableStateOf<PageId?>(null)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    fun start(pageId: PageId) {
        dragging = pageId
        offset = Offset.Zero
    }

    fun moveBy(amount: Offset) {
        offset += amount
    }

    fun reset() {
        dragging = null
        offset = Offset.Zero
    }
}

internal fun Modifier.dragVisual(
    drag: PageDragState,
    pageId: PageId,
): Modifier {
    val isDragged = drag.dragging == pageId
    return zIndex(if (isDragged) DRAGGED_Z else 0f).graphicsLayer {
        if (isDragged) {
            translationX = drag.offset.x
            translationY = drag.offset.y
            scaleX = DRAGGED_SCALE
            scaleY = DRAGGED_SCALE
        }
    }
}

internal fun Modifier.dragToReorder(
    drag: PageDragState,
    pageId: PageId,
    haptics: HapticFeedback,
    onFinish: () -> Unit,
): Modifier =
    pointerInput(pageId) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                drag.start(pageId)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onDragEnd = onFinish,
            onDragCancel = drag::reset,
        ) { change, amount ->
            change.consume()
            drag.moveBy(amount)
        }
    }

// La página cae en la celda que queda bajo el centro de la celda arrastrada.
internal fun LazyGridState.targetIndex(
    from: Int,
    offset: Offset,
): Int? {
    val dragged = layoutInfo.visibleItemsInfo.firstOrNull { it.index == from } ?: return null
    val center =
        Offset(
            dragged.offset.x + dragged.size.width / 2f + offset.x,
            dragged.offset.y + dragged.size.height / 2f + offset.y,
        )
    return layoutInfo.visibleItemsInfo
        .firstOrNull { info ->
            info.key != ADD_CELL_KEY &&
                center.x in info.offset.x.toFloat()..(info.offset.x + info.size.width).toFloat() &&
                center.y in info.offset.y.toFloat()..(info.offset.y + info.size.height).toFloat()
        }?.index
}
