package io.github.emilioabril05.folio.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

/** Base de datos local de Folio. */
@Database(
    entities = [DocumentEntity::class, PageEntity::class],
    version = FolioDatabase.VERSION,
    exportSchema = true,
)
abstract class FolioDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    abstract fun libraryDao(): LibraryDao

    companion object {
        const val VERSION = 4
        const val NAME = "folio.db"
    }
}
