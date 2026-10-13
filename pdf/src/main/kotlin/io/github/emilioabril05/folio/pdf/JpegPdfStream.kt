package io.github.emilioabril05.folio.pdf

import java.io.OutputStream

private const val POINTS_PER_INCH = 72f
private const val FIRST_PAGE_OBJECT = 3
private const val OBJECTS_PER_PAGE = 3
private const val XREF_OFFSET_DIGITS = 10
private const val GRAY_COMPONENTS = 1
private const val COLOR_COMPONENTS = 3

/**
 * Escribe un PDF 1.4 cuyas páginas son imágenes JPEG incrustadas tal cual (`DCTDecode`), sin volver a
 * comprimirlas. Así el peso del PDF es casi el de los JPEG, algo que `PdfDocument` de Android no permite.
 */
internal class JpegPdfStream(
    output: OutputStream,
    private val pageCount: Int,
) {
    private val out = CountingOutputStream(output)
    private val offsets = mutableMapOf<Int, Long>()
    private var nextPage = 0

    init {
        out.write("%PDF-1.4\n%âãÏÓ\n".toByteArray(Charsets.ISO_8859_1))
        writeObject(1, "<< /Type /Catalog /Pages 2 0 R >>")
        val kids = (0 until pageCount).joinToString(" ") { "${pageObject(it)} 0 R" }
        writeObject(2, "<< /Type /Pages /Kids [ $kids ] /Count $pageCount >>")
    }

    /** Agrega la siguiente página: una imagen JPEG de [width] × [height] píxeles a [dpi] puntos por pulgada. */
    fun addPage(
        jpeg: ByteArray,
        width: Int,
        height: Int,
        dpi: Int,
    ) {
        val index = nextPage++
        val pointsWidth = width * POINTS_PER_INCH / dpi
        val pointsHeight = height * POINTS_PER_INCH / dpi
        val content = "q $pointsWidth 0 0 $pointsHeight 0 0 cm /Im0 Do Q".toByteArray(Charsets.ISO_8859_1)
        val colorSpace = if (jpegComponents(jpeg) == GRAY_COMPONENTS) "/DeviceGray" else "/DeviceRGB"

        writeObject(
            pageObject(index),
            "<< /Type /Page /Parent 2 0 R /MediaBox [ 0 0 $pointsWidth $pointsHeight ] " +
                "/Resources << /XObject << /Im0 ${imageObject(index)} 0 R >> >> " +
                "/Contents ${contentObject(index)} 0 R >>",
        )
        writeStream(contentObject(index), "<< /Length ${content.size} >>", content)
        writeStream(
            imageObject(index),
            "<< /Type /XObject /Subtype /Image /Width $width /Height $height /ColorSpace $colorSpace " +
                "/BitsPerComponent 8 /Filter /DCTDecode /Length ${jpeg.size} >>",
            jpeg,
        )
    }

    /** Cierra el PDF con su tabla de referencias. */
    fun finish() {
        val infoObject = FIRST_PAGE_OBJECT + pageCount * OBJECTS_PER_PAGE
        writeObject(infoObject, "<< /Producer (APUNT) >>")
        val xrefStart = out.count
        val size = infoObject + 1
        val xref = StringBuilder("xref\n0 $size\n0000000000 65535 f \n")
        for (number in 1 until size) {
            xref.append(offsets.getValue(number).toString().padStart(XREF_OFFSET_DIGITS, '0')).append(" 00000 n \n")
        }
        xref.append("trailer\n<< /Size $size /Root 1 0 R /Info $infoObject 0 R >>\nstartxref\n$xrefStart\n%%EOF\n")
        out.write(xref.toString().toByteArray(Charsets.ISO_8859_1))
        out.flush()
    }

    private fun pageObject(index: Int) = FIRST_PAGE_OBJECT + index * OBJECTS_PER_PAGE

    private fun contentObject(index: Int) = pageObject(index) + 1

    private fun imageObject(index: Int) = pageObject(index) + 2

    private fun writeObject(
        number: Int,
        body: String,
    ) {
        offsets[number] = out.count
        out.write("$number 0 obj\n$body\nendobj\n".toByteArray(Charsets.ISO_8859_1))
    }

    private fun writeStream(
        number: Int,
        dictionary: String,
        data: ByteArray,
    ) {
        offsets[number] = out.count
        out.write("$number 0 obj\n$dictionary\nstream\n".toByteArray(Charsets.ISO_8859_1))
        out.write(data)
        out.write("\nendstream\nendobj\n".toByteArray(Charsets.ISO_8859_1))
    }
}

private const val MARKER_PREFIX = 0xFF
private const val START_OF_IMAGE_LENGTH = 2
private const val SEGMENT_HEADER = 4
private const val COMPONENTS_OFFSET = 9
private const val BYTE_MASK = 0xFF
private const val MARKER_LENGTH_BYTES = 2
private const val LENGTH_HIGH_OFFSET = 2
private const val LENGTH_LOW_OFFSET = 3
private const val BITS_PER_BYTE = 8
private val START_OF_FRAME_MARKERS = (0xC0..0xCF).toSet() - setOf(0xC4, 0xC8, 0xCC)

/** Número de canales del JPEG (1 = grises, 3 = color), leído de su marcador SOF. */
internal fun jpegComponents(jpeg: ByteArray): Int {
    var index = START_OF_IMAGE_LENGTH
    var components = COLOR_COMPONENTS
    var scanning = true
    while (scanning && index + SEGMENT_HEADER < jpeg.size) {
        val marker = jpeg[index + 1].toInt() and BYTE_MASK
        val high = jpeg[index + LENGTH_HIGH_OFFSET].toInt() and BYTE_MASK
        val low = jpeg[index + LENGTH_LOW_OFFSET].toInt() and BYTE_MASK
        val isFrame = marker in START_OF_FRAME_MARKERS && index + COMPONENTS_OFFSET < jpeg.size
        when {
            jpeg[index].toInt() and BYTE_MASK != MARKER_PREFIX -> {
                scanning = false
            }

            isFrame -> {
                components = jpeg[index + COMPONENTS_OFFSET].toInt() and BYTE_MASK
                scanning = false
            }

            else -> {
                index += MARKER_LENGTH_BYTES + ((high shl BITS_PER_BYTE) or low)
            }
        }
    }
    return components
}

private class CountingOutputStream(
    private val delegate: OutputStream,
) : OutputStream() {
    var count = 0L
        private set

    override fun write(b: Int) {
        delegate.write(b)
        count++
    }

    override fun write(
        b: ByteArray,
        off: Int,
        len: Int,
    ) {
        delegate.write(b, off, len)
        count += len
    }

    override fun flush() = delegate.flush()
}
