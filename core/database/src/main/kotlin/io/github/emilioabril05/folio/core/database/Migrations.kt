package io.github.emilioabril05.folio.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Versión 1 a 2: cada página guarda su recorte; las existentes quedan con la foto completa. */
internal val Migration1To2: Migration =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            CROP_COLUMN_DEFAULTS.forEach { (column, default) ->
                db.execSQL("ALTER TABLE pages ADD COLUMN $column REAL NOT NULL DEFAULT $default")
            }
        }
    }

private val CROP_COLUMN_DEFAULTS =
    listOf(
        "cropTopLeftX" to "0.0",
        "cropTopLeftY" to "0.0",
        "cropTopRightX" to "1.0",
        "cropTopRightY" to "0.0",
        "cropBottomRightX" to "1.0",
        "cropBottomRightY" to "1.0",
        "cropBottomLeftX" to "0.0",
        "cropBottomLeftY" to "1.0",
    )
