plugins {
    alias(libs.plugins.folio.android.library)
}

android {
    namespace = "io.github.emilioabril05.folio.core.testing"
}

dependencies {
    api(projects.core.domain)
}
