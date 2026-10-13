package io.github.emilioabril05.folio.feature.review.crop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.emilioabril05.folio.core.model.Corner
import io.github.emilioabril05.folio.core.model.CropQuad
import io.github.emilioabril05.folio.core.model.NormalizedPoint
import io.github.emilioabril05.folio.feature.review.R
import kotlin.math.roundToInt

private val HandleTouchSize = 48.dp
private val HandleDotSize = 24.dp
private val HandleBorder = 2.dp
private val OutlineWidth = 2.dp
private val LoupeRadius = 56.dp
private val LoupeGap = 72.dp

// Aumento de la lupa y paso de cada acción de TalkBack (2 % de la foto).
private const val LOUPE_ZOOM = 2.5f
private const val ACCESSIBILITY_STEP = 0.02f
private const val HALF = 2f

/** Rectángulo donde se dibuja la foto dentro del lienzo, ajustada sin deformarse. */
private class PhotoFrame(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    fun toScreen(point: NormalizedPoint): Offset = Offset(left + point.x * width, top + point.y * height)

    fun toPhoto(offset: Offset): NormalizedPoint = NormalizedPoint((offset.x - left) / width, (offset.y - top) / height)
}

/** Foto con el recorte encima: 4 esquinas arrastrables, con acciones de TalkBack y una lupa al arrastrar. */
@Composable
internal fun CropCanvas(
    photo: ImageBitmap,
    crop: CropQuad,
    onEvent: (CropEditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val frame =
            with(density) { fitPhoto(photo, Size(maxWidth.toPx(), maxHeight.toPx())) }
        var dragging by remember { mutableStateOf<Corner?>(null) }
        val accent = MaterialTheme.colorScheme.primary

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawImage(
                image = photo,
                dstOffset = IntOffset(frame.left.roundToInt(), frame.top.roundToInt()),
                dstSize = IntSize(frame.width.roundToInt(), frame.height.roundToInt()),
            )
            drawOutline(crop, frame, accent)
            dragging?.let { corner -> drawLoupe(photo, crop[corner], frame.toScreen(crop[corner]), accent) }
        }
        Corner.entries.forEach { corner ->
            CornerHandle(
                corner = corner,
                crop = crop,
                frame = frame,
                onDragging = { dragging = it },
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun CornerHandle(
    corner: Corner,
    crop: CropQuad,
    frame: PhotoFrame,
    onDragging: (Corner?) -> Unit,
    onEvent: (CropEditorEvent) -> Unit,
) {
    val currentCrop by rememberUpdatedState(crop)
    val currentFrame by rememberUpdatedState(frame)
    val position = frame.toScreen(crop[corner])
    val halfTouch = with(LocalDensity.current) { HandleTouchSize.toPx() / HALF }
    val name = stringResource(corner.nameRes())
    val actions = accessibilityMoves(corner, crop, onEvent)

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .offset { IntOffset((position.x - halfTouch).roundToInt(), (position.y - halfTouch).roundToInt()) }
                .size(HandleTouchSize)
                .semantics {
                    contentDescription = name
                    customActions = actions
                }.pointerInput(corner) {
                    detectDragGestures(
                        onDragStart = { onDragging(corner) },
                        onDragEnd = {
                            onDragging(null)
                            onEvent(CropEditorEvent.DragFinished)
                        },
                        onDragCancel = {
                            onDragging(null)
                            onEvent(CropEditorEvent.DragFinished)
                        },
                    ) { change, delta ->
                        change.consume()
                        val current = currentFrame.toScreen(currentCrop[corner])
                        onEvent(CropEditorEvent.DragCorner(corner, currentFrame.toPhoto(current + delta)))
                    }
                },
    ) {
        Box(
            modifier =
                Modifier
                    .size(HandleDotSize)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(HandleBorder, MaterialTheme.colorScheme.onPrimary, CircleShape),
        )
    }
}

@Composable
private fun accessibilityMoves(
    corner: Corner,
    crop: CropQuad,
    onEvent: (CropEditorEvent) -> Unit,
): List<CustomAccessibilityAction> {
    val point = crop[corner]

    fun move(
        label: String,
        dx: Float,
        dy: Float,
    ) = CustomAccessibilityAction(label) {
        onEvent(CropEditorEvent.MoveCorner(corner, NormalizedPoint(point.x + dx, point.y + dy)))
        true
    }
    return listOf(
        move(stringResource(R.string.crop_move_up), 0f, -ACCESSIBILITY_STEP),
        move(stringResource(R.string.crop_move_down), 0f, ACCESSIBILITY_STEP),
        move(stringResource(R.string.crop_move_left), -ACCESSIBILITY_STEP, 0f),
        move(stringResource(R.string.crop_move_right), ACCESSIBILITY_STEP, 0f),
    )
}

private fun Corner.nameRes(): Int =
    when (this) {
        Corner.TopLeft -> R.string.crop_corner_top_left
        Corner.TopRight -> R.string.crop_corner_top_right
        Corner.BottomRight -> R.string.crop_corner_bottom_right
        Corner.BottomLeft -> R.string.crop_corner_bottom_left
    }

private fun fitPhoto(
    photo: ImageBitmap,
    area: Size,
): PhotoFrame {
    val scale = minOf(area.width / photo.width, area.height / photo.height)
    val width = photo.width * scale
    val height = photo.height * scale
    return PhotoFrame(left = (area.width - width) / HALF, top = (area.height - height) / HALF, width, height)
}

private fun DrawScope.drawOutline(
    crop: CropQuad,
    frame: PhotoFrame,
    color: Color,
) {
    val path =
        Path().apply {
            crop.corners.map(frame::toScreen).forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
            close()
        }
    drawPath(path, color, style = Stroke(width = OutlineWidth.toPx()))
}

// La lupa muestra la zona bajo el dedo ampliada, por encima de él para que el dedo no la tape.
private fun DrawScope.drawLoupe(
    photo: ImageBitmap,
    corner: NormalizedPoint,
    finger: Offset,
    color: Color,
) {
    val radius = LoupeRadius.toPx()
    val center =
        Offset(
            x = finger.x.coerceIn(radius, size.width - radius),
            y = (finger.y - LoupeGap.toPx()).coerceAtLeast(radius),
        )
    val sourceSide = (radius * HALF / LOUPE_ZOOM).roundToInt().coerceAtLeast(1)
    val sourceX = (corner.x * photo.width - sourceSide / HALF).roundToInt().coerceIn(0, photo.width - sourceSide)
    val sourceY = (corner.y * photo.height - sourceSide / HALF).roundToInt().coerceIn(0, photo.height - sourceSide)
    val circle =
        Path().apply {
            addOval(
                androidx.compose.ui.geometry
                    .Rect(center, radius),
            )
        }
    clipPath(circle) {
        drawImage(
            image = photo,
            srcOffset = IntOffset(sourceX, sourceY),
            srcSize = IntSize(sourceSide, sourceSide),
            dstOffset = IntOffset((center.x - radius).roundToInt(), (center.y - radius).roundToInt()),
            dstSize = IntSize((radius * HALF).roundToInt(), (radius * HALF).roundToInt()),
        )
    }
    drawCircle(color, radius, center, style = Stroke(width = OutlineWidth.toPx()))
    drawCircle(color, OutlineWidth.toPx() * HALF, center)
}
