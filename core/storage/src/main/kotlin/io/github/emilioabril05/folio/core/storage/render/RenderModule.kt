package io.github.emilioabril05.folio.core.storage.render

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.emilioabril05.folio.core.domain.PageRenderer

/** Dibuja las páginas con `Matrix.setPolyToPoly` de Android. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RenderModule {
    @Binds
    abstract fun bindPageRenderer(renderer: BitmapPageRenderer): PageRenderer
}
