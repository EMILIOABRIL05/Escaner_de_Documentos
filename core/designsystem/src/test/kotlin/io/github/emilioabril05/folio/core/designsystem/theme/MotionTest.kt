package io.github.emilioabril05.folio.core.designsystem.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class MotionTest {
    @Test
    fun `con quitar animaciones la duración es cero`() {
        assertEquals(0, FolioMotion.durationMillis(FolioMotion.MEDIUM_MILLIS, reducedMotion = true))
    }

    @Test
    fun `sin quitar animaciones se respeta la duración`() {
        assertEquals(
            FolioMotion.MEDIUM_MILLIS,
            FolioMotion.durationMillis(FolioMotion.MEDIUM_MILLIS, reducedMotion = false),
        )
    }
}
