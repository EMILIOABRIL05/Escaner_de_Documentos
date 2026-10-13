package io.github.emilioabril05.folio.core.domain

import io.github.emilioabril05.folio.core.model.CropQuad
import io.github.emilioabril05.folio.core.model.NormalizedPoint
import io.github.emilioabril05.folio.core.model.PageId
import javax.inject.Inject
import kotlin.math.abs

// Un recorte menor al 1 % de la foto casi siempre es un toque accidental.
private const val MIN_AREA_FRACTION = 0.01f
private const val MIN_COORDINATE = 0f
private const val MAX_COORDINATE = 1f
private const val HALF = 0.5f

/** Por qué un recorte no se puede aplicar. */
enum class InvalidCrop {
    /** Alguna esquina queda fuera de la foto. */
    OutOfBounds,

    /** Las esquinas se cruzan o forman un ángulo hacia adentro. */
    NotConvex,

    /** El recorte es demasiado pequeño para ser una hoja. */
    TooSmall,
}

/** Por qué no se puede aplicar este recorte, o `null` si es válido. */
fun CropQuad.validate(): InvalidCrop? =
    when {
        corners.any { !it.isInsidePhoto() } -> InvalidCrop.OutOfBounds
        !isConvexClockwise() -> InvalidCrop.NotConvex
        area() < MIN_AREA_FRACTION -> InvalidCrop.TooSmall
        else -> null
    }

private fun NormalizedPoint.isInsidePhoto(): Boolean =
    x in MIN_COORDINATE..MAX_COORDINATE && y in MIN_COORDINATE..MAX_COORDINATE

// Con el eje y hacia abajo, recorrer TL → TR → BR → BL gira siempre a la derecha:
// todos los productos cruz son positivos.
private fun CropQuad.isConvexClockwise(): Boolean =
    corners.indices.all { index ->
        val current = corners[index]
        val next = corners[(index + 1) % corners.size]
        val afterNext = corners[(index + 2) % corners.size]
        cross(current, next, afterNext) > 0f
    }

private fun cross(
    a: NormalizedPoint,
    b: NormalizedPoint,
    c: NormalizedPoint,
): Float = (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)

// Fórmula del área de Gauss (shoelace).
private fun CropQuad.area(): Float =
    abs(
        corners.indices
            .sumOf { index ->
                val current = corners[index]
                val next = corners[(index + 1) % corners.size]
                (current.x * next.y - next.x * current.y).toDouble()
            }.toFloat(),
    ) * HALF

/** Resultado de cambiar el recorte de una página. */
sealed interface UpdateCropResult {
    /** El recorte quedó guardado. */
    data object Saved : UpdateCropResult

    /** El recorte no es válido y no se guardó. */
    data class Invalid(
        val reason: InvalidCrop,
    ) : UpdateCropResult

    /** No se pudo guardar. */
    data class Failed(
        val error: StorageError,
    ) : UpdateCropResult
}

/** Guarda el recorte de una página como parámetro; la foto original no se toca (edición no destructiva). */
class UpdatePageCrop
    @Inject
    constructor(
        private val documents: DocumentRepository,
    ) {
        /** Valida y guarda [crop] para la página [pageId]. */
        suspend operator fun invoke(
            pageId: PageId,
            crop: CropQuad,
        ): UpdateCropResult {
            crop.validate()?.let { return UpdateCropResult.Invalid(it) }
            return documents.updatePageCrop(pageId, crop).fold(
                onSuccess = { UpdateCropResult.Saved },
                onFailure = { UpdateCropResult.Failed((it as? StorageFailure)?.error ?: StorageError.Unknown) },
            )
        }
    }
