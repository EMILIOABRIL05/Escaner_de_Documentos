package io.github.emilioabril05.folio.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provee la base de datos y sus DAO. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideFolioDatabase(
        @ApplicationContext context: Context,
    ): FolioDatabase = Room.databaseBuilder(context, FolioDatabase::class.java, FolioDatabase.NAME).build()

    @Provides
    fun provideDocumentDao(database: FolioDatabase): DocumentDao = database.documentDao()
}
