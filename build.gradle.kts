plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.roborazzi) apply false
}

// Cobertura de los módulos core medida en conjunto: las pruebas del dominio también cubren el modelo.
// core:testing queda fuera porque contiene dobles de prueba.
dependencies {
    kover(projects.core.model)
    kover(projects.core.domain)
    kover(projects.core.data)
    kover(projects.core.database)
    kover(projects.core.storage)
    kover(projects.core.designsystem)
}

kover {
    reports {
        verify {
            rule {
                minBound(80)
            }
        }
    }
}
