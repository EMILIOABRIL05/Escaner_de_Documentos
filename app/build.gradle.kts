import java.util.Properties

plugins {
    alias(libs.plugins.folio.android.application)
    alias(libs.plugins.folio.hilt)
}

// La clave de firma vive fuera del repositorio (por defecto en ~/.apunt/keystore.properties).
// Sin ella, la release se compila sin firmar, como en la CI.
val signingProperties =
    Properties().apply {
        val file =
            providers
                .gradleProperty("apunt.signing")
                .map { File(it) }
                .getOrElse(File(System.getProperty("user.home"), ".apunt/keystore.properties"))
        if (file.exists()) file.inputStream().use { load(it) }
    }

android {
    namespace = "io.github.emilioabril05.folio"

    signingConfigs {
        if (signingProperties.isNotEmpty()) {
            create("release") {
                storeFile = File(signingProperties.getProperty("storeFile"))
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    defaultConfig {
        applicationId = "io.github.emilioabril05.apunt"
        versionCode = 1
        versionName = "1.0.0"
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
