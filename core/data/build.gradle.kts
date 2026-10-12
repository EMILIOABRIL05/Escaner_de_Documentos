plugins {
    alias(libs.plugins.folio.android.library)
}

android {
    namespace = "io.github.emilioabril05.folio.core.data"
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.database)
    implementation(projects.core.storage)
}
