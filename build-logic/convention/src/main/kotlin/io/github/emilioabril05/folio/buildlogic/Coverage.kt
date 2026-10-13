package io.github.emilioabril05.folio.buildlogic

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

// Código que generan Hilt (fábricas, inyectores, componentes) y Room (implementaciones de DAO y base de datos).
// Se prueba a través del código escrito a mano; medirlo solo diluye la cobertura.
private val GENERATED_CLASSES =
    arrayOf(
        "*_Factory",
        "*_Factory\$*",
        "*_Provide*Factory*",
        "*_Bind*Factory*",
        "*_MembersInjector",
        "Hilt_*",
        "*_HiltModules*",
        "*_HiltComponents*",
        "*_ComponentTreeDeps*",
        "dagger.hilt.internal.*",
        "hilt_aggregated_deps.*",
        "_io_github_emilioabril05_*",
        "*_Impl",
        "*_Impl\$*",
        // Puente que genera Kotlin para los métodos por defecto de las interfaces; el código real se ejecuta en la interfaz.
        "*\$DefaultImpls",
    )

// Módulos de Hilt: solo declaran cómo unir dependencias.
private const val HILT_MODULE_ANNOTATION = "dagger.Module"

/** Excluye del informe de cobertura el código generado por Hilt y Room. */
internal fun Project.excludeGeneratedCodeFromCoverage() {
    extensions.configure<KoverProjectExtension> {
        reports.filters.excludes {
            classes(*GENERATED_CLASSES)
            annotatedBy(HILT_MODULE_ANNOTATION)
        }
    }
}
