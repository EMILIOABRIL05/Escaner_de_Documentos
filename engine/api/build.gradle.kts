plugins {
    alias(libs.plugins.folio.android.library)
}

android {
    namespace = "io.github.emilioabril05.folio.engine.api"
}

dependencies {
    api(projects.core.model)
}
