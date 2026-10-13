package io.github.emilioabril05.folio.core.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.emilioabril05.folio.core.domain.DocumentRepository
import io.github.emilioabril05.folio.core.domain.IdProvider
import io.github.emilioabril05.folio.core.domain.LibraryRepository

/** Enlaza las interfaces del dominio con sus implementaciones de datos. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindDocumentRepository(repository: RoomDocumentRepository): DocumentRepository

    @Binds
    abstract fun bindLibraryRepository(repository: RoomLibraryRepository): LibraryRepository

    @Binds
    abstract fun bindIdProvider(provider: UuidIdProvider): IdProvider
}
