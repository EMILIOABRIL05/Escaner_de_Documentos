plugins {
    alias(libs.plugins.folio.android.library)
    alias(libs.plugins.folio.hilt)
}

android {
    namespace = "io.github.emilioabril05.folio.core.storage"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.domain)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
