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

/** Una hoja del documento. Su foto original vive en disco y nunca se modifica. */
data class Page(
    val id: PageId,
    val position: Int,
)

/** Una pila de hojas escaneadas, en orden. */
data class Document(
    val id: DocumentId,
    val title: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val pages: List<Page>,
)
