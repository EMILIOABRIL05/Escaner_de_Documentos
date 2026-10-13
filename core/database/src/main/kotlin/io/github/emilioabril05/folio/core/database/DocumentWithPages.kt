package io.github.emilioabril05.folio.core.database

/** Un documento junto con sus páginas ordenadas por posición. */
data class DocumentWithPages(
    val document: DocumentEntity,
    val pages: List<PageEntity>,
)
