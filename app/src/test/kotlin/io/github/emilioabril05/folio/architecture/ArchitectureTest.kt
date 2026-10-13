package io.github.emilioabril05.folio.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import org.junit.Assert.assertEquals
import org.junit.Test

class ArchitectureTest {
    private val projectScope: KoScope =
        Konsist.scopeFromProject().slice { file -> FIXTURES_DIR !in file.path }

    @Test
    fun `ningún feature importa otro feature`() {
        assertEquals(emptyList<String>(), projectScope.featureCrossImports())
    }

    @Test
    fun `model y domain no dependen de Android`() {
        assertEquals(emptyList<String>(), projectScope.androidImportsInPureModules())
    }

    @Test
    fun `domain no conoce las implementaciones`() {
        assertEquals(emptyList<String>(), projectScope.domainImportsOfImplementations())
    }

    @Test
    fun `los ViewModel viven en los módulos feature`() {
        assertEquals(emptyList<String>(), projectScope.viewModelsOutsideFeatures())
    }

    @Test
    fun `la regla de aislamiento detecta una violación sembrada`() {
        val fixtureScope = Konsist.scopeFromDirectory("app/src/test/resources/$FIXTURES_DIR")

        assertEquals(1, fixtureScope.featureCrossImports().size)
    }

    private companion object {
        const val FIXTURES_DIR = "konsist-fixtures"
    }
}
