package io.github.emilioabril05.folio.core.domain

import io.github.emilioabril05.folio.core.model.CropQuad
import io.github.emilioabril05.folio.core.model.NormalizedPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CropValidationTest {
    private fun quad(vararg coordinates: Float) =
        CropQuad(
            topLeft = NormalizedPoint(coordinates[0], coordinates[1]),
            topRight = NormalizedPoint(coordinates[2], coordinates[3]),
            bottomRight = NormalizedPoint(coordinates[4], coordinates[5]),
            bottomLeft = NormalizedPoint(coordinates[6], coordinates[7]),
        )

    @Test
    fun `la foto completa es un recorte válido`() {
        assertNull(CropQuad.Full.validate())
    }

    @Test
    fun `un cuadrilátero inclinado dentro de la foto es válido`() {
        assertNull(quad(0.1f, 0.15f, 0.85f, 0.05f, 0.95f, 0.9f, 0.05f, 0.8f).validate())
    }

    @Test
    fun `una esquina fuera de la foto no es válida`() {
        assertEquals(InvalidCrop.OutOfBounds, quad(-0.1f, 0f, 1f, 0f, 1f, 1f, 0f, 1f).validate())
    }

    @Test
    fun `esquinas cruzadas no son válidas`() {
        assertEquals(InvalidCrop.NotConvex, quad(1f, 0f, 0f, 0f, 1f, 1f, 0f, 1f).validate())
    }

    @Test
    fun `un recorte cóncavo no es válido`() {
        assertEquals(InvalidCrop.NotConvex, quad(0f, 0f, 1f, 0f, 0.4f, 0.4f, 0f, 1f).validate())
    }

    @Test
    fun `un recorte de menos del 1 por ciento de la foto es demasiado pequeño`() {
        assertEquals(InvalidCrop.TooSmall, quad(0.5f, 0.5f, 0.55f, 0.5f, 0.55f, 0.55f, 0.5f, 0.55f).validate())
    }
}
