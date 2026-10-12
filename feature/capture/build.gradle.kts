plugins {
    alias(libs.plugins.folio.android.feature)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "io.github.emilioabril05.folio.feature.capture"
}

roborazzi {
    outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
}

dependencies {
    implementation(projects.engine.api)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    testImplementation(projects.core.testing)
    testImplementation(libs.turbine)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
}
