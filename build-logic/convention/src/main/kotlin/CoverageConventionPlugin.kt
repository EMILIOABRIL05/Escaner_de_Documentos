import io.github.emilioabril05.folio.buildlogic.excludeGeneratedCodeFromCoverage
import io.github.emilioabril05.folio.buildlogic.libs
import io.github.emilioabril05.folio.buildlogic.pluginId
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Cobertura de los módulos `core` medida en conjunto desde la raíz (ADR-003): las pruebas del dominio también
 * cubren las clases de datos del modelo. `core:testing` queda fuera porque contiene dobles de prueba.
 */
class CoverageConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("kover"))
            excludeGeneratedCodeFromCoverage()

            dependencies {
                COVERED_MODULES.forEach { path -> add("kover", project(path)) }
            }
            extensions.configure<KoverProjectExtension> {
                reports.verify.rule {
                    minBound(MIN_LINE_COVERAGE_PERCENT)
                }
            }
        }
    }

    private companion object {
        const val MIN_LINE_COVERAGE_PERCENT = 80
        val COVERED_MODULES =
            listOf(
                ":core:model",
                ":core:domain",
                ":core:data",
                ":core:database",
                ":core:storage",
                ":core:designsystem",
            )
    }
}
