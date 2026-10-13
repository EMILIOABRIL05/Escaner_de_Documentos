import io.github.emilioabril05.folio.buildlogic.libs
import io.github.emilioabril05.folio.buildlogic.library
import io.github.emilioabril05.folio.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Módulo de pantalla: Compose + Hilt, ve el dominio y el sistema de diseño, nunca otro feature. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("folio-android-compose-library"))
            pluginManager.apply(libs.pluginId("folio-hilt"))

            dependencies {
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:designsystem"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            }
        }
    }
}
