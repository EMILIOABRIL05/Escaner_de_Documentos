package io.github.emilioabril05.folio.feature.review

// Un comentario con !! no cuenta.
class UsesNotNullAssertion(private val title: String?) {
    val label = "texto con !! no cuenta"

    fun length(): Int = title!!.length
}
