package io.github.emilioabril05.folio.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureCompose(commonExtension: CommonExtension) {
    pluginManager.apply(libs.pluginId("compose-compiler"))
    commonExtension.buildFeatures.compose = true

    dependencies {
        val bom = platform(libs.library("androidx-compose-bom"))
        add("implementation", bom)
        add("implementation", libs.library("androidx-compose-ui"))
        add("implementation", libs.library("androidx-compose-material3"))
        add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
        add("debugImplementation", libs.library("androidx-compose-ui-tooling"))

        add("testImplementation", bom)
        add("testImplementation", libs.library("androidx-compose-ui-test-junit4"))
        add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
    }
}

internal fun Project.configureAndroidUnitTests() {
    dependencies {
        add("testImplementation", libs.library("junit4"))
        add("testImplementation", libs.library("robolectric"))
        add("testImplementation", libs.library("androidx-test-ext-junit"))
    }
}
