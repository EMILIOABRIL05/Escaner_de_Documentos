package io.github.emilioabril05.folio.architecture

import com.lemonappdev.konsist.api.container.KoScope

private const val NOT_NULL_ASSERTION = "!!"

// Comentarios, textos y nombres entre comillas invertidas, en ese orden de prioridad.
private val NON_CODE =
    Regex(
        """/\*[\s\S]*?\*/|//[^\n]*|""" + "\"\"\"" + """[\s\S]*?""" + "\"\"\"" +
            """|"(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n])*'|`[^`\n]*`""",
    )

/** Usos del operador `!!`, como `"archivo:línea"`. Ignora comentarios, textos y nombres entre comillas invertidas. */
fun KoScope.notNullAssertions(): List<String> =
    files.flatMap { file ->
        file.text
            .codeOnly()
            .lines()
            .withIndex()
            .filter { (_, line) -> NOT_NULL_ASSERTION in line }
            .map { (index, _) -> "${file.path}:${index + 1}" }
    }

// Sustituye lo que no es código por saltos de línea equivalentes, para conservar los números de línea.
private fun String.codeOnly(): String = replace(NON_CODE) { match -> "\n".repeat(match.value.count { it == '\n' }) }
