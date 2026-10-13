package io.github.emilioabril05.folio.core.database

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            instrumentation = InstrumentationRegistry.getInstrumentation(),
            file = ApplicationProvider.getApplicationContext<Context>().getDatabasePath(DB_NAME),
            driver = AndroidSQLiteDriver(),
            databaseClass = FolioDatabase::class,
        )

    @Test
    fun `migrar de la versión 1 a la 2 conserva la página y le pone la foto completa`() {
        helper.createDatabase(1).use { connection ->
            connection.execSQL(
                "INSERT INTO documents (id, title, createdAtMillis, updatedAtMillis) VALUES ('doc-1', 'Escaneo', 1, 2)",
            )
            connection.execSQL("INSERT INTO pages (id, documentId, position) VALUES ('p-1', 'doc-1', 4)")
        }

        helper.runMigrationsAndValidate(2, listOf(Migration1To2)).use { connection ->
            assertEquals("p-1 doc-1 4", connection.readPageColumns("id", "documentId", "position"))
            assertEquals(FULL_PHOTO, connection.readPageNumbers(*FULL_PHOTO_COLUMNS))
        }
    }

    private fun SQLiteConnection.readPageColumns(vararg columns: String): String =
        prepare("SELECT ${columns.joinToString()} FROM pages").use { statement ->
            statement.step()
            columns.indices.joinToString(" ") { statement.getText(it) }
        }

    private fun SQLiteConnection.readPageNumbers(vararg columns: String): List<Double> =
        prepare("SELECT ${columns.joinToString()} FROM pages").use { statement ->
            statement.step()
            columns.indices.map { statement.getDouble(it) }
        }

    private companion object {
        const val DB_NAME = "migration-test.db"
        val FULL_PHOTO_COLUMNS =
            arrayOf(
                "cropTopLeftX",
                "cropTopLeftY",
                "cropTopRightX",
                "cropTopRightY",
                "cropBottomRightX",
                "cropBottomRightY",
                "cropBottomLeftX",
                "cropBottomLeftY",
            )
        val FULL_PHOTO = listOf(0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0)
    }
}
