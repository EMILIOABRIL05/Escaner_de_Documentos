import io.github.emilioabril05.folio.buildlogic.configureKotlinJvm
import io.github.emilioabril05.folio.buildlogic.libs
import io.github.emilioabril05.folio.buildlogic.library
import io.github.emilioabril05.folio.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Módulo Kotlin puro, sin Android.
 *
 * Registra `testDebugUnitTest` como alias de `test` para que la puerta de calidad,
 * que invoca `testDebugUnitTest` en todos los módulos, también ejecute sus pruebas.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("kotlin-jvm"))
            pluginManager.apply(libs.pluginId("folio-quality"))
            configureKotlinJvm()

            dependencies {
                add("testImplementation", libs.library("junit4"))
            }

            tasks.register(UNIT_TEST_ALIAS) {
                group = "verification"
                description = "Alias de test para la puerta de calidad."
                dependsOn("test")
            }
        }
    }

    private companion object {
        const val UNIT_TEST_ALIAS = "testDebugUnitTest"
    }
}
