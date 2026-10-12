plugins {
    alias(libs.plugins.folio.jvm.library)
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)
    api(libs.javax.inject)

    testImplementation(projects.core.testing)
}
