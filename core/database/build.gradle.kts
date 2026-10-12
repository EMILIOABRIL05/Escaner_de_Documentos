plugins {
    alias(libs.plugins.folio.android.library)
    alias(libs.plugins.folio.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "io.github.emilioabril05.folio.core.database"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(projects.core.model)
    api(libs.room.runtime)
    implementation(libs.kotlinx.coroutines.core)
    ksp(libs.room.compiler)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
