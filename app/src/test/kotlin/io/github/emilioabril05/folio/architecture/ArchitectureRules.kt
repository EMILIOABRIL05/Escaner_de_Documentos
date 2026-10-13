package io.github.emilioabril05.folio.architecture

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration

private const val BASE_PACKAGE = "io.github.emilioabril05.folio"
private const val FEATURE_PACKAGE = "$BASE_PACKAGE.feature."
private val PURE_PACKAGES = listOf("$BASE_PACKAGE.core.model", "$BASE_PACKAGE.core.domain")
private val ANDROID_PREFIXES = listOf("android.", "androidx.")
private val IMPLEMENTATION_PACKAGES =
    listOf("core.data", "core.database", "core.storage", "engine", "pdf").map { "$BASE_PACKAGE.$it" }

/** Imports de un feature hacia otro feature, como `"archivo -> import"`. */
fun KoScope.featureCrossImports(): List<String> =
    files.flatMap { file ->
        val ownFeature = file.packageName().featureName() ?: return@flatMap emptyList()
        file
            .importNames()
            .filter { import -> import.featureName().let { it != null && it != ownFeature } }
            .map { file.violation(it) }
    }

/** Imports de Android dentro de `core.model` o `core.domain`. */
fun KoScope.androidImportsInPureModules(): List<String> =
    files
        .filter { file -> PURE_PACKAGES.any { file.packageName().isInside(it) } }
        .flatMap { file ->
            file
                .importNames()
                .filter { import -> ANDROID_PREFIXES.any { import.startsWith(it) } }
                .map { file.violation(it) }
        }

/** Imports del dominio hacia las capas que lo implementan. */
fun KoScope.domainImportsOfImplementations(): List<String> =
    files
        .filter { file -> file.packageName().isInside("$BASE_PACKAGE.core.domain") }
        .flatMap { file ->
            file
                .importNames()
                .filter { import -> IMPLEMENTATION_PACKAGES.any { import.isInside(it) } }
                .map { file.violation(it) }
        }

/** Clases `*ViewModel` declaradas fuera de los módulos feature. */
fun KoScope.viewModelsOutsideFeatures(): List<String> =
    classes()
        .filter { it.name.endsWith("ViewModel") }
        .filterNot {
            it.packagee
                ?.name
                .orEmpty()
                .startsWith(FEATURE_PACKAGE)
        }.map { "${it.path} -> ${it.name}" }

private fun KoFileDeclaration.packageName(): String = packagee?.name.orEmpty()

private fun KoFileDeclaration.importNames(): List<String> = imports.map { it.name }

private fun KoFileDeclaration.violation(import: String): String = "$path -> $import"

private fun String.featureName(): String? =
    takeIf { it.startsWith(FEATURE_PACKAGE) }?.removePrefix(FEATURE_PACKAGE)?.substringBefore('.')

private fun String.isInside(packageName: String): Boolean = this == packageName || startsWith("$packageName.")
