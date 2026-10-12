package io.github.emilioabril05.folio.architecture

import com.lemonappdev.konsist.api.Konsist
import org.junit.Assert.assertEquals
import org.junit.Test

class CleanCodeTest {
    @Test
    fun `ningún archivo usa el operador !!`() {
        val projectScope = Konsist.scopeFromProject().slice { file -> FIXTURES_DIR !in file.path }

        assertEquals(emptyList<String>(), projectScope.notNullAssertions())
    }

    @Test
    fun `la regla de !! ignora comentarios y textos y detecta el uso real`() {
        val fixture = Konsist.scopeFromFile("app/src/test/resources/$FIXTURES_DIR/UsesNotNullAssertion.kt")

        assertEquals(1, fixture.notNullAssertions().size)
    }

    private companion object {
        const val FIXTURES_DIR = "konsist-fixtures"
    }
}
