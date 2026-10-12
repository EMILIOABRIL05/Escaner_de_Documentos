import io.github.emilioabril05.folio.buildlogic.libs
import io.github.emilioabril05.folio.buildlogic.library
import io.github.emilioabril05.folio.buildlogic.pluginId
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/**
 * Puerta de calidad de cada módulo: ktlint (formato), detekt (código limpio) y Kover (cobertura).
 *
 * La cobertura mínima solo se exige en los módulos `core`, salvo `core:testing`, que contiene dobles de prueba.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("ktlint"))
            pluginManager.apply(libs.pluginId("detekt"))
            pluginManager.apply(libs.pluginId("kover"))

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

            if (requiresCoverage()) {
                extensions.configure<KoverProjectExtension> {
                    reports.verify.rule {
                        minBound(MIN_LINE_COVERAGE_PERCENT)
                    }
                }
            }
        }
    }

    private fun Project.requiresCoverage(): Boolean = path.startsWith(":core:") && path != ":core:testing"

    private companion object {
        const val DETEKT_CONFIG = "config/detekt/detekt.yml"
        const val MIN_LINE_COVERAGE_PERCENT = 80
    }
}
