plugins {
    alias(libs.plugins.folio.android.compose.library)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "io.github.emilioabril05.folio.core.designsystem"
}

roborazzi {
    outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
}

dependencies {
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
}
