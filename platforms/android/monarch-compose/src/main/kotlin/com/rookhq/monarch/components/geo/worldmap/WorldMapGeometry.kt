package com.rookhq.monarch.components.geo.worldmap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/**
 * The parsed outline of MonarchWorldMap: closed rings of points per country
 * and a centroid per country, in the 1000 by 394 map space. Parsed once, on
 * first use.
 */
internal object WorldMapGeometry {
    /** The width of the map space. */
    const val WIDTH: Float = WorldMapGeometrySource.WIDTH

    /** The height of the map space. */
    const val HEIGHT: Float = WorldMapGeometrySource.HEIGHT

    /** The outline of each country that has one: rings of x, y pairs. */
    val shapes: Map<String, List<FloatArray>> by lazy { parseShapes(WorldMapGeometrySource.SHAPES) }

    /** The position of each country's largest landmass, including countries too small for an outline. */
    val centroids: Map<String, Offset> by lazy { parseCentroids(WorldMapGeometrySource.CENTROIDS) }

    /** Whether a country can be drawn: it has an outline or a centroid. */
    fun isPlaced(code: String): Boolean = shapes.containsKey(code) || centroids.containsKey(code)

    /**
     * Reads entries of a two-letter code followed by path data with the
     * commands `M` and `m` (move), `L` and `l` (line) and `z` (close).
     */
    fun parseShapes(source: String): Map<String, List<FloatArray>> {
        val reader = WorldMapPathReader(source)
        val result = LinkedHashMap<String, List<FloatArray>>()
        while (true) {
            val (code, rings) = reader.nextCountry() ?: break
            if (rings.isNotEmpty()) result[code] = rings
        }
        return result
    }

    /** Reads entries of a two-letter code followed by x and y. */
    fun parseCentroids(source: String): Map<String, Offset> {
        val parts = source.trim().split(Regex("\\s+"))
        val result = LinkedHashMap<String, Offset>()
        var index = 0
        while (index + 2 <= parts.lastIndex) {
            val x = parts[index + 1].toFloatOrNull()
            val y = parts[index + 2].toFloatOrNull()
            if (x != null && y != null) result[parts[index]] = Offset(x, y)
            index += ENTRY_PARTS
        }
        return result
    }

    /** Whether [point] is inside [rings] by the even-odd rule, so holes and islands work. */
    fun contains(rings: List<FloatArray>, point: Offset): Boolean =
        rings.count { crossings(it, point) % 2 == 1 } % 2 == 1

    /** How many edges of [ring] a ray from [point] towards the right crosses. */
    private fun crossings(ring: FloatArray, point: Offset): Int {
        val count = ring.size / 2
        if (count < MIN_RING) return 0
        var crossed = 0
        var px = ring[(count - 1) * 2]
        var py = ring[(count - 1) * 2 + 1]
        for (i in 0 until count) {
            val vx = ring[i * 2]
            val vy = ring[i * 2 + 1]
            val spans = (vy > point.y) != (py > point.y)
            if (spans && point.x < (px - vx) * (point.y - vy) / (py - vy) + vx) crossed++
            px = vx
            py = vy
        }
        return crossed
    }

    /** The centre of the bounds of [rings], for a country without a centroid. */
    fun boundsCentre(rings: List<FloatArray>): Offset? {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        rings.forEach { ring ->
            for (i in 0 until ring.size / 2) {
                minX = minOf(minX, ring[i * 2])
                maxX = maxOf(maxX, ring[i * 2])
                minY = minOf(minY, ring[i * 2 + 1])
                maxY = maxOf(maxY, ring[i * 2 + 1])
            }
        }
        return if (minX.isFinite()) Offset((minX + maxX) / 2, (minY + maxY) / 2) else null
    }

    /** Where a country's mark and TalkBack element sit: its centroid, or the centre of its outline. */
    fun anchor(code: String): Offset? = centroids[code] ?: shapes[code]?.let(::boundsCentre)

    /** The rings as a path, scaled from the map space by [scale]. */
    fun path(rings: List<FloatArray>, scale: Float): Path = Path().apply {
        rings.forEach { ring ->
            val count = ring.size / 2
            if (count < MIN_RING) return@forEach
            moveTo(ring[0] * scale, ring[1] * scale)
            for (i in 1 until count) lineTo(ring[i * 2] * scale, ring[i * 2 + 1] * scale)
            close()
        }
    }

    private const val ENTRY_PARTS = 3
    private const val MIN_RING = 3
}

/** Splits outline data into country codes, path commands and numbers, and builds the rings of each country. */
private class WorldMapPathReader(private val source: String) {
    private var index = 0

    /** The next country's code and rings, or `null` at the end. */
    fun nextCountry(): Pair<String, List<FloatArray>>? {
        while (index < source.length && !isCode(index)) index++
        if (index >= source.length) return null
        val code = source.substring(index, index + 2)
        index += 2
        val builder = WorldMapRingBuilder()
        var reading = true
        while (reading) reading = readStep(builder)
        return code to builder.finish()
    }

    /** Reads one command or one pair of numbers into [builder]; `false` at the end of the country. */
    private fun readStep(builder: WorldMapRingBuilder): Boolean {
        skipSpaces()
        if (index >= source.length || isCode(index)) return false
        val c = source[index]
        if (c.isLetter()) {
            index++
            builder.command(c)
            return true
        }
        val pair = readPair()
        pair?.let { builder.pair(it.first, it.second) }
        return pair != null
    }

    private fun readPair(): Pair<Float, Float>? {
        val first = readNumber() ?: return null
        skipSpaces()
        return readNumber()?.let { first to it }
    }

    private fun isCode(at: Int): Boolean =
        at + 1 < source.length && source[at].isUpperCase() && source[at + 1].isUpperCase()

    private fun skipSpaces() {
        while (index < source.length && (source[index].isWhitespace() || source[index] == ',')) index++
    }

    private fun readNumber(): Float? {
        val start = index
        if (index < source.length && (source[index] == '-' || source[index] == '+')) index++
        while (index < source.length && (source[index].isDigit() || source[index] == '.')) index++
        return source.substring(start, index).toFloatOrNull()
    }
}

/** Follows path commands and collects the closed rings of one country. */
private class WorldMapRingBuilder {
    private val rings = mutableListOf<FloatArray>()
    private val ring = ArrayList<Float>()
    private var command = 'M'
    private var x = 0f
    private var y = 0f
    private var startX = 0f
    private var startY = 0f

    fun command(next: Char) {
        command = next
        if (next == 'z' || next == 'Z') {
            closeRing()
            x = startX
            y = startY
        }
    }

    fun pair(first: Float, second: Float) {
        val absolute = command == 'M' || command == 'L'
        if (command == 'M' || command == 'm') {
            closeRing()
            moveBy(first, second, absolute)
            startX = x
            startY = y
            // Pairs after a move are lines, as in SVG.
            command = if (absolute) 'L' else 'l'
        } else {
            moveBy(first, second, absolute)
        }
        ring += x
        ring += y
    }

    fun finish(): List<FloatArray> {
        closeRing()
        return rings
    }

    private fun moveBy(first: Float, second: Float, absolute: Boolean) {
        x = if (absolute) first else x + first
        y = if (absolute) second else y + second
    }

    private fun closeRing() {
        if (ring.size >= MIN_POINTS * 2) rings += ring.toFloatArray()
        ring.clear()
    }

    private companion object {
        const val MIN_POINTS = 3
    }
}
