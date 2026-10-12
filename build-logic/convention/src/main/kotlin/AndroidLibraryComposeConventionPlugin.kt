import com.android.build.api.dsl.LibraryExtension
import io.github.emilioabril05.folio.buildlogic.configureCompose
import io.github.emilioabril05.folio.buildlogic.libs
import io.github.emilioabril05.folio.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Librería de Android con Jetpack Compose. */
class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("folio-android-library"))

            extensions.configure<LibraryExtension> {
                configureCompose(this)
            }
        }
    }
}
