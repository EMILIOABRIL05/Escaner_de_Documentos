plugins {
    alias(libs.plugins.folio.android.library)
}

android {
    namespace = "io.github.emilioabril05.folio.engine.mlkit"
}

dependencies {
    implementation(projects.engine.api)
}
