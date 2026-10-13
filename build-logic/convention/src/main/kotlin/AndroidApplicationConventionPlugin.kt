import com.android.build.api.dsl.ApplicationExtension
import io.github.emilioabril05.folio.buildlogic.configureAndroidUnitTests
import io.github.emilioabril05.folio.buildlogic.configureCompose
import io.github.emilioabril05.folio.buildlogic.configureKotlinAndroid
import io.github.emilioabril05.folio.buildlogic.libs
import io.github.emilioabril05.folio.buildlogic.pluginId
import io.github.emilioabril05.folio.buildlogic.versionInt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** App de Android con Compose, R8 en release y pruebas locales con Robolectric. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("android-application"))
            pluginManager.apply(libs.pluginId("folio-quality"))

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                configureCompose(this)
                defaultConfig.targetSdk = libs.versionInt("targetSdk")
                buildTypes.getByName("release") {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro",
                    )
                }
            }
            configureAndroidUnitTests()
        }
    }
}
