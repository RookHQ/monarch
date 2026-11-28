package dev.monarchui.internal

import java.text.NumberFormat
import java.util.Locale

/**
 * A file size as the web writes it: binary steps of 1024 with the units B, KB, MB, GB and TB, one decimal at most
 * (`3.2 MB`, `47.1 KB`, `18 MB`). The platform formatter counts in thousands and writes "kB", so the same file would
 * read differently on the web and here.
 */
internal fun monarchFormatBytes(bytes: Long, locale: Locale = Locale.getDefault()): String {
    if (bytes < 0) return "0 B"
    var value = bytes.toDouble()
    var unit = 0
    while (value >= STEP && unit < Units.lastIndex) {
        value /= STEP
        unit++
    }
    var rounded = if (unit == 0) value else Math.round(value * TENTH) / TENTH
    // 1,048,575 bytes is 1023.99 KB, which rounds to 1024 KB: step up to 1 MB.
    if (rounded >= STEP && unit < Units.lastIndex) {
        rounded = 1.0
        unit++
    }
    val format = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = if (unit == 0) 0 else 1
        minimumFractionDigits = 0
    }
    return "${format.format(rounded)} ${Units[unit]}"
}

private val Units = listOf("B", "KB", "MB", "GB", "TB")
private const val STEP = 1024.0
private const val TENTH = 10.0
