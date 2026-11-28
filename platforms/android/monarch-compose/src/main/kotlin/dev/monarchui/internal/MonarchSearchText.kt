package dev.monarchui.internal

import java.text.Normalizer
import java.util.Locale

/**
 * Text as every Monarch search compares it: lower case and without accents,
 * so "Zoë" matches "zoe" and "Ärzte" matches "arzte". Used by the combobox,
 * the multi-select, the entity, country, phone-country and time zone pickers.
 */
internal object MonarchSearchText {
    private val marks = Regex("\\p{Mn}+")

    /** [text] for comparison: decomposed, without combining marks, in lower case. */
    fun normalize(text: String): String =
        marks.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").lowercase(Locale.ROOT)

    /** Whether [text] contains [query], ignoring case and accents. */
    fun contains(text: String, query: String): Boolean = normalize(text).contains(normalize(query))
}
