plugins {
    alias(libs.plugins.folio.android.feature)
}

android {
    namespace = "io.github.emilioabril05.folio.feature.capture"
}

dependencies {
    implementation(projects.engine.api)
}
