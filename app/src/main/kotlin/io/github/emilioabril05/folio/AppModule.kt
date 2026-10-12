package io.github.emilioabril05.folio

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.emilioabril05.folio.core.domain.DocumentTitleFormatter
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Dependencias de la app que necesitan recursos o el reloj del sistema. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // ISO 8601 para que los nombres se ordenen solos (plan §11.1).
    private const val TITLE_DATE_PATTERN = "yyyy-MM-dd HH.mm"

    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    fun provideDocumentTitleFormatter(
        @ApplicationContext context: Context,
    ): DocumentTitleFormatter {
        val formatter = DateTimeFormatter.ofPattern(TITLE_DATE_PATTERN).withZone(ZoneId.systemDefault())
        return DocumentTitleFormatter { createdAt ->
            context.getString(R.string.document_default_title, formatter.format(createdAt))
        }
    }
}
