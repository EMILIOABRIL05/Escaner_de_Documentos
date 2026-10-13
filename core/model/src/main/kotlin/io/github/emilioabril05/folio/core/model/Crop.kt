package io.github.emilioabril05.folio.core.model

/** Punto dentro de la foto original, en fracciones de su ancho ([x]) y alto ([y]), de 0 a 1. */
data class NormalizedPoint(
    val x: Float,
    val y: Float,
)

/** Esquina del recorte. */
enum class Corner { TopLeft, TopRight, BottomRight, BottomLeft }

/** Recorte de una página: las 4 esquinas de la hoja dentro de la foto original, que nunca se modifica. */
data class CropQuad(
    val topLeft: NormalizedPoint,
    val topRight: NormalizedPoint,
    val bottomRight: NormalizedPoint,
    val bottomLeft: NormalizedPoint,
) {
    /** Las esquinas en sentido horario, empezando arriba a la izquierda. */
    val corners: List<NormalizedPoint> get() = listOf(topLeft, topRight, bottomRight, bottomLeft)

    /** La esquina [corner]. */
    operator fun get(corner: Corner): NormalizedPoint =
        when (corner) {
            Corner.TopLeft -> topLeft
            Corner.TopRight -> topRight
            Corner.BottomRight -> bottomRight
            Corner.BottomLeft -> bottomLeft
        }

    /** Una copia con la esquina [corner] movida a [point]. */
    fun with(
        corner: Corner,
        point: NormalizedPoint,
    ): CropQuad =
        when (corner) {
            Corner.TopLeft -> copy(topLeft = point)
            Corner.TopRight -> copy(topRight = point)
            Corner.BottomRight -> copy(bottomRight = point)
            Corner.BottomLeft -> copy(bottomLeft = point)
        }

    companion object {
        /** Sin recorte: la foto completa. */
        val Full =
            CropQuad(
                topLeft = NormalizedPoint(0f, 0f),
                topRight = NormalizedPoint(1f, 0f),
                bottomRight = NormalizedPoint(1f, 1f),
                bottomLeft = NormalizedPoint(0f, 1f),
            )
    }
}
