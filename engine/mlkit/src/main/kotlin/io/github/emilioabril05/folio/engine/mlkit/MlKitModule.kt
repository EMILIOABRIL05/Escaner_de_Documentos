package io.github.emilioabril05.folio.engine.mlkit

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.emilioabril05.folio.engine.api.DocumentScanner

/** Usa ML Kit como motor de escaneo. */
@Module
@InstallIn(SingletonComponent::class)
abstract class MlKitModule {
    @Binds
    abstract fun bindDocumentScanner(scanner: MlKitDocumentScanner): DocumentScanner
}
