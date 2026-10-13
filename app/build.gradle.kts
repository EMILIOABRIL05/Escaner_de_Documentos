plugins {
    alias(libs.plugins.folio.android.application)
    alias(libs.plugins.folio.hilt)
}

android {
    namespace = "io.github.emilioabril05.folio"

    defaultConfig {
        applicationId = "io.github.emilioabril05.apunt"
        versionCode = 1
        versionName = "0.0.1"
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.storage)
    implementation(projects.core.designsystem)

    implementation(projects.feature.capture)
    implementation(projects.feature.review)
    implementation(projects.feature.library)
    implementation(projects.feature.export)
    implementation(projects.feature.settings)

    implementation(projects.engine.api)
    implementation(projects.engine.mlkit)
    implementation(projects.pdf)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    testImplementation(projects.core.testing)
    testImplementation(libs.konsist)
}
