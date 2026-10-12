plugins {
    alias(libs.plugins.folio.android.library)
}

android {
    namespace = "io.github.emilioabril05.folio.pdf"
}

dependencies {
    implementation(projects.core.domain)
}
