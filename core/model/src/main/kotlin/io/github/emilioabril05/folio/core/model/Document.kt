package io.github.emilioabril05.folio.core.model

import java.time.Instant

/** Identificador estable de un documento. */
@JvmInline
value class DocumentId(
    val value: String,
)

/** Identificador estable de una página. */
@JvmInline
value class PageId(
    val value: String,
)

private const val QUARTER_DEGREES = 90
private const val THREE_QUARTER_TURNS = 3

/** Giro de una página, en pasos de 90° en sentido horario. */
enum class Rotation(
    val degrees: Int,
) {
    None(0),
    Quarter(QUARTER_DEGREES),
    Half(QUARTER_DEGREES * 2),
    ThreeQuarters(QUARTER_DEGREES * THREE_QUARTER_TURNS),
    ;

    /** El giro siguiente, 90° más en sentido horario. */
    fun clockwise(): Rotation = entries[(ordinal + 1) % entries.size]
}

/** Filtro de una página (plan §7.1, paso 5). Se aplica al dibujar; la foto original no cambia. */
enum class PageFilter {
    /** La foto tal cual. */
    Original,

    /** Blanco y negro adaptativo: fondo blanco y texto negro, sin sombras. Es el filtro por defecto. */
    Document,

    /** Escala de grises con contraste realzado. */
    Grayscale,

    /** Color con más saturación y contraste. */
    EnhancedColor,
}

/** Una hoja del documento. Su foto original vive en disco y nunca se modifica; recorte y giro son parámetros. */
data class Page(
    val id: PageId,
    val position: Int,
    val crop: CropQuad = CropQuad.Full,
    val rotation: Rotation = Rotation.None,
    val filter: PageFilter = PageFilter.Document,
)

/** Una pila de hojas escaneadas, en orden. */
data class Document(
    val id: DocumentId,
    val title: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val pages: List<Page>,
)
