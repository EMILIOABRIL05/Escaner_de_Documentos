plugins {
    alias(libs.plugins.folio.android.library)
    alias(libs.plugins.folio.hilt)
}

android {
    namespace = "io.github.emilioabril05.folio.engine.mlkit"
}

dependencies {
    implementation(projects.engine.api)
    implementation(libs.mlkit.document.scanner)
    implementation(libs.kotlinx.coroutines.play.services)
}
