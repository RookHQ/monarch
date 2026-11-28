package com.rookhq.monarch.screenshots

import android.graphics.Bitmap
import java.awt.image.BufferedImage
import java.awt.image.IndexColorModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

/**
 * Keeps the stored images small without Git LFS or an outside tool.
 *
 * [reduce] gives an image at most 256 distinct colours, so it can be written with a palette. An image that already
 * has 256 or fewer is kept exactly. Otherwise a median-cut quantiser picks the palette from the colour histogram
 * and each palette entry is the most frequent colour of its box, not the mean, so a large flat fill (a pale pill,
 * a dark card, the canvas) always keeps its exact colour and only the anti-aliasing around text and edges is
 * merged. It is a pure function of the pixels: the image that is recorded and the image that is compared later
 * go through the same step, so reducing never causes a diff. [writeIndexed] writes such an image as a palette PNG
 * at the highest deflate level.
 */
internal object ScreenshotPixels {
    private val budgets = listOf(256, 128, 64, 32, 16)
    private val scales = listOf(1f, 0.75f, 0.5f, 0.35f)
    private const val MaxColours = 256

    /** The most bytes one stored image may take: 40 KB. */
    const val MaxBytes = 40_000

    /**
     * [source] with at most [MaxColours] colours that encodes to at most [MaxBytes]. The full 256-colour palette is
     * tried first, then 128, 64, 32 and 16. When even 16 colours are too big, the image is scaled down first, by
     * 0.75, 0.5, then 0.35, and the budgets are tried again. The result is the same for the same input.
     */
    fun reduce(source: Bitmap): Bitmap {
        var smallest: Bitmap = source
        for (scale in scales) {
            val scaled = if (scale == 1f) source else Bitmap.createScaledBitmap(
                source,
                (source.width * scale).toInt().coerceAtLeast(1),
                (source.height * scale).toInt().coerceAtLeast(1),
                true,
            )
            val width = scaled.width
            val height = scaled.height
            val pixels = IntArray(width * height).also { scaled.getPixels(it, 0, width, 0, 0, width, height) }
            for (budget in budgets) {
                val candidate = Bitmap.createBitmap(quantise(pixels, budget), width, height, Bitmap.Config.ARGB_8888)
                smallest = candidate
                if (encode(candidate).size <= MaxBytes) return candidate
            }
        }
        return smallest
    }

    /**
     * [pixels] (alpha dropped) with at most [budget] distinct colours. Colours are kept as they are when there are
     * few enough. Otherwise the histogram is split by median cut, always the box with the most error (the sum of
     * count times squared distance to its most frequent colour) along its widest channel at the weighted median,
     * and every colour becomes the most frequent colour of its box.
     */
    internal fun quantise(pixels: IntArray, budget: Int): IntArray {
        val opaque = IntArray(pixels.size) { pixels[it] or (0xFF shl 24) }
        val (colours, counts) = histogram(opaque)
        if (colours.size <= budget) return opaque
        val boxes = ArrayList<ColourBox>()
        boxes += ColourBox(IntArray(colours.size) { it }, colours, counts)
        while (boxes.size < budget) {
            val worst = boxes.indices.filter { boxes[it].error > 0L }.maxByOrNull { boxes[it].error } ?: break
            val (low, high) = boxes[worst].split(colours, counts)
            boxes[worst] = low
            boxes += high
        }
        val target = IntArray(colours.size)
        for (box in boxes) for (index in box.members) target[index] = colours[box.mode]
        return IntArray(opaque.size) { target[java.util.Arrays.binarySearch(colours, opaque[it])] }
    }

    /** The distinct values of [pixels] in ascending order, and how often each occurs. */
    private fun histogram(pixels: IntArray): Pair<IntArray, IntArray> {
        val sorted = pixels.copyOf().also { it.sort() }
        val colours = ArrayList<Int>()
        val counts = ArrayList<Int>()
        for (value in sorted) {
            if (colours.isNotEmpty() && colours.last() == value) counts[counts.lastIndex] += 1
            else {
                colours += value
                counts += 1
            }
        }
        return colours.toIntArray() to counts.toIntArray()
    }

    /** A set of histogram entries (indices into the sorted colour list) and its most frequent colour. */
    private class ColourBox(val members: IntArray, colours: IntArray, counts: IntArray) {
        val mode: Int = members.maxWith(compareBy<Int>({ counts[it] }, { -it }))
        val error: Long = members.sumOf { distance(colours[it], colours[mode]) * counts[it] }

        fun split(colours: IntArray, counts: IntArray): Pair<ColourBox, ColourBox> {
            var best = 0
            var widest = -1
            for (shift in intArrayOf(16, 8, 0)) {
                val values = members.map { (colours[it] shr shift) and 0xFF }
                val range = values.max() - values.min()
                if (range > widest) { widest = range; best = shift }
            }
            val ordered = members.sortedWith(compareBy<Int>({ (colours[it] shr best) and 0xFF }, { it }))
            val total = ordered.sumOf { counts[it].toLong() }
            var running = 0L
            var cut = 1
            for ((position, index) in ordered.withIndex()) {
                running += counts[index]
                if (running * 2 >= total) { cut = (position + 1).coerceIn(1, ordered.size - 1); break }
            }
            return ColourBox(ordered.subList(0, cut).toIntArray(), colours, counts) to
                ColourBox(ordered.subList(cut, ordered.size).toIntArray(), colours, counts)
        }

        private fun distance(a: Int, b: Int): Long {
            val dr = ((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)
            val dg = ((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)
            val db = (a and 0xFF) - (b and 0xFF)
            return (dr * dr + dg * dg + db * db).toLong()
        }
    }

    /** The PNG bytes [writeIndexed] stores for [bitmap]. */
    fun encode(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val pixels = IntArray(width * height).also { bitmap.getPixels(it, 0, width, 0, 0, width, height) }
        image.setRGB(0, 0, width, height, pixels, 0, width)
        return ByteArrayOutputStream().also { writeIndexed(it, image) }.toByteArray()
    }

    /** Writes [image] as a palette PNG when it has at most 256 colours, and as RGB otherwise. */
    fun writeIndexed(file: File, image: BufferedImage) {
        file.delete()
        file.outputStream().use { writeIndexed(it, image) }
    }

    private fun writeIndexed(sink: OutputStream, image: BufferedImage) {
        val width = image.width
        val height = image.height
        val rgb = IntArray(width * height).also { image.getRGB(0, 0, width, height, it, 0, width) }
        val palette = rgb.map { it or (0xFF shl 24) }.distinct().sorted()
        val output = if (palette.size <= MaxColours) {
            val index = palette.withIndex().associate { it.value to it.index }
            val model = IndexColorModel(
                8,
                palette.size,
                ByteArray(palette.size) { (palette[it] shr 16).toByte() },
                ByteArray(palette.size) { (palette[it] shr 8).toByte() },
                ByteArray(palette.size) { palette[it].toByte() },
            )
            BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, model).also { out ->
                val data = out.raster
                for (y in 0 until height) for (x in 0 until width) {
                    data.setSample(x, y, 0, index.getValue(rgb[y * width + x] or (0xFF shl 24)))
                }
            }
        } else {
            BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).also {
                it.setRGB(0, 0, width, height, rgb, 0, width)
            }
        }
        val writer = ImageIO.getImageWritersByFormatName("png").next()
        try {
            ImageIO.createImageOutputStream(sink).use { stream ->
                writer.output = stream
                val param = writer.defaultWriteParam
                if (param.canWriteCompressed()) {
                    param.compressionMode = ImageWriteParam.MODE_EXPLICIT
                    param.compressionQuality = 0f
                }
                writer.write(null, IIOImage(output, null, null), param)
            }
        } finally {
            writer.dispose()
        }
    }

    /**
     * Shortens every run of identical rows to at most [keep] rows. The screen capture of a dialog is mostly the
     * flat scrim above and below it; this keeps the dialog and drops the empty middle.
     */
    fun collapseFlatRows(source: Bitmap, keep: Int = 24): Bitmap {
        val width = source.width
        val rows = (0 until source.height).map { y ->
            IntArray(width).also { source.getPixels(it, 0, width, 0, y, width, 1) }
        }
        val kept = mutableListOf<IntArray>()
        var run = 0
        rows.forEachIndexed { y, row ->
            run = if (y > 0 && row.contentEquals(rows[y - 1])) run + 1 else 0
            if (run < keep) kept += row
        }
        val out = Bitmap.createBitmap(width, kept.size, Bitmap.Config.ARGB_8888)
        kept.forEachIndexed { y, row -> out.setPixels(row, 0, width, 0, y, width, 1) }
        return out
    }
}
