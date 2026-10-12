import com.android.build.api.dsl.LibraryExtension
import io.github.emilioabril05.folio.buildlogic.configureAndroidUnitTests
import io.github.emilioabril05.folio.buildlogic.configureKotlinAndroid
import io.github.emilioabril05.folio.buildlogic.libs
import io.github.emilioabril05.folio.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Librería de Android sin interfaz. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("android-library"))

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
            }
            configureAndroidUnitTests()
        }
    }
}
