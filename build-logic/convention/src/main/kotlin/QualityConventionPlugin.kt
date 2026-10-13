import io.github.emilioabril05.folio.buildlogic.excludeGeneratedCodeFromCoverage
import io.github.emilioabril05.folio.buildlogic.libs
import io.github.emilioabril05.folio.buildlogic.library
import io.github.emilioabril05.folio.buildlogic.pluginId
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/**
 * Puerta de calidad de cada módulo: ktlint (formato), detekt (código limpio) y Kover (cobertura).
 *
 * La cobertura mínima de los módulos `core` se verifica en conjunto desde la raíz del proyecto.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("ktlint"))
            pluginManager.apply(libs.pluginId("detekt"))
            pluginManager.apply(libs.pluginId("kover"))
            excludeGeneratedCodeFromCoverage()

            extensions.configure<KtlintExtension> {
                version.set(libs.findVersion("ktlint").get().requiredVersion)
            }

            extensions.configure<DetektExtension> {
                buildUponDefaultConfig = true
                config.setFrom(rootProject.file(DETEKT_CONFIG))
            }
            dependencies {
                add("detektPlugins", libs.library("compose-rules-detekt"))
            }
        }
    }

    private companion object {
        const val DETEKT_CONFIG = "config/detekt/detekt.yml"
    }
}
