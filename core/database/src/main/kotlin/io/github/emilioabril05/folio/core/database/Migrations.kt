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

/** Versión 2 a 3: cada página guarda su giro en grados y su filtro; las existentes quedan sin girar. */
internal val Migration2To3: Migration =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE pages ADD COLUMN rotationDegrees INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE pages ADD COLUMN filter TEXT NOT NULL DEFAULT 'Document'")
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
