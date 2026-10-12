package io.github.emilioabril05.folio.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

private val JAVA_VERSION = JavaVersion.VERSION_17
private val JVM_TARGET = JvmTarget.JVM_17

// Robolectric con SDK 36 accede a jdk.internal.access, que el JDK 21 no exporta por defecto.
private const val ROBOLECTRIC_JDK_ACCESS = "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED"

internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = libs.versionInt("compileSdk")
        defaultConfig.minSdk = libs.versionInt("minSdk")
        compileOptions.sourceCompatibility = JAVA_VERSION
        compileOptions.targetCompatibility = JAVA_VERSION
        testOptions.unitTests.isIncludeAndroidResources = true
        // Hilt genera fuentes de prueba aunque el módulo aún no tenga pruebas propias;
        // solo esos módulos toleran no descubrir pruebas.
        val hasHandwrittenTests = file("src/test").exists()
        testOptions.unitTests.all { test ->
            test.jvmArgs(ROBOLECTRIC_JDK_ACCESS)
            test.failOnNoDiscoveredTests.set(hasHandwrittenTests)
        }
    }
    configureKotlinCompilation()
}

internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JAVA_VERSION
        targetCompatibility = JAVA_VERSION
    }
    configureKotlinCompilation()
}

private fun Project.configureKotlinCompilation() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(JVM_TARGET)
    }
}
