package io.github.emilioabril05.folio.core.storage

import android.content.Context
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.emilioabril05.folio.core.domain.PageSource
import io.github.emilioabril05.folio.core.domain.PhotoCaptureTarget
import java.io.File
import java.util.UUID
import javax.inject.Inject

/** Crea archivos vacíos en la carpeta temporal de capturas y los expone con una URI `content://` propia de la app. */
class FileProviderPhotoCaptureTarget internal constructor(
    private val cacheDirectory: File,
    private val uriFor: (File) -> String,
) : PhotoCaptureTarget {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(
        cacheDirectory = context.cacheDir,
        uriFor = { file ->
            FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file).toString()
        },
    )

    override fun newPhotoTarget(): PageSource {
        val directory = File(cacheDirectory, CAPTURES_DIRECTORY).apply { mkdirs() }
        val file = File(directory, "${UUID.randomUUID()}$PHOTO_EXTENSION").apply { createNewFile() }
        return PageSource(uriFor(file))
    }

    private companion object {
        const val CAPTURES_DIRECTORY = "captures"
        const val PHOTO_EXTENSION = ".jpg"
        const val AUTHORITY_SUFFIX = ".files"
    }
}
