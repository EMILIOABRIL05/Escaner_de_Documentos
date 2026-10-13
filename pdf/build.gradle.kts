plugins {
    alias(libs.plugins.folio.android.library)
    alias(libs.plugins.folio.hilt)
}

android {
    namespace = "io.github.emilioabril05.folio.pdf"
}

dependencies {
    implementation(projects.core.domain)
    implementation(libs.androidx.core.ktx)
}
