plugins {
    alias(libs.plugins.folio.android.library)
    alias(libs.plugins.folio.hilt)
}

android {
    namespace = "io.github.emilioabril05.folio.core.data"
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.database)
    implementation(projects.core.storage)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
