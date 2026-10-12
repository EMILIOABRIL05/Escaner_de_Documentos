plugins {
    alias(libs.plugins.folio.android.library)
}

android {
    namespace = "io.github.emilioabril05.folio.core.storage"
}

dependencies {
    implementation(projects.core.model)
}
