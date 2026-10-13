plugins {
    alias(libs.plugins.folio.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.emilioabril05.folio.feature.library"
}

dependencies {
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
}
