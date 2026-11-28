package dev.monarchui.components.datagrid.columnmapping

import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** The suggestion and checking logic of `MonarchColumnMapping`, the same as the web's. */
public object MonarchColumnMappingDefaults {
    /** The lowest similarity that is suggested. */
    public const val DEFAULT_THRESHOLD: Double = 0.75

    private const val MIN_CONTAINED = 3
    private const val CONTAINS_BASE = 0.6
    private const val CONTAINS_RANGE = 0.3
    private const val PERCENT = 100.0
    private val NotLetterOrDigit = Regex("[^\\p{L}\\p{N}]")
    private val WordBreak = Regex("[^\\p{L}\\p{N}]+")
    private val CamelHump = Regex("([a-z])([A-Z])")

    /**
     * Suggests a field for each column, as the web's suggestColumnMapping.
     * Exact matches (the header equals a field's id, label or alias,
     * ignoring case and punctuation) come first, then close matches scoring
     * at least [threshold]. Each field is suggested for one column at most.
     * The result is keyed by column name; columns without a suggestion are
     * left out.
     */
    public fun suggest(
        sources: List<MonarchColumnMappingSource>,
        fields: List<MonarchColumnMappingField>,
        threshold: Double = DEFAULT_THRESHOLD,
    ): Map<String, MonarchColumnMappingSuggestion> {
        val candidates = sources.flatMap { source ->
            fields.mapNotNull { field ->
                val best = (listOf(field.id, field.label) + field.aliases).maxOf { similarity(source.name, it) }
                if (best >= threshold) Triple(source.name, field.id, best) else null
            }
        }.sortedByDescending { it.third }
        val out = LinkedHashMap<String, MonarchColumnMappingSuggestion>()
        val used = mutableSetOf<String>()
        candidates.forEach { (source, field, score) ->
            if (source in out || field in used) return@forEach
            val confidence = if (score == 1.0) {
                MonarchColumnMappingConfidence.Exact
            } else {
                MonarchColumnMappingConfidence.Similar
            }
            out[source] = MonarchColumnMappingSuggestion(field, confidence, (score * PERCENT).roundToInt() / PERCENT)
            used += field
        }
        return out
    }

    /** The suggestions as a mapping: column name to field id. */
    public fun mapping(suggestions: Map<String, MonarchColumnMappingSuggestion>): Map<String, String> =
        suggestions.mapValues { it.value.fieldId }

    /**
     * What still blocks [mapping]: required [fields] that no column is mapped
     * to, and fields chosen for more than one column.
     */
    public fun issues(
        mapping: Map<String, String>,
        fields: List<MonarchColumnMappingField>,
    ): MonarchColumnMappingIssues {
        val byField = LinkedHashMap<String, MutableList<String>>()
        mapping.forEach { (source, field) ->
            if (field.isNotEmpty()) byField.getOrPut(field) { mutableListOf() } += source
        }
        return MonarchColumnMappingIssues(
            missing = fields.filter { it.required && it.id !in byField },
            duplicates = byField.filterValues { it.size > 1 }.mapValues { it.value.toList() },
        )
    }

    /** Lower case, letters and digits only: "E-mail Address" gives "emailaddress". */
    internal fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKD).lowercase().replace(NotLetterOrDigit, "")

    private fun words(value: String): Set<String> =
        Normalizer.normalize(value, Normalizer.Form.NFKD)
            .replace(CamelHump, "$1 $2")
            .lowercase()
            .split(WordBreak)
            .filter { it.isNotEmpty() }
            .toSet()

    /** The similarity of two short names, 0 to 1: the best of word overlap, containment and edit distance. */
    internal fun similarity(a: String, b: String): Double {
        val na = normalize(a)
        val nb = normalize(b)
        if (na.isEmpty() || nb.isEmpty()) return 0.0
        if (na == nb) return 1.0
        val wa = words(a)
        val wb = words(b)
        val shared = wa.count { it in wb }
        val dice = if (wa.size + wb.size > 0) 2.0 * shared / (wa.size + wb.size) else 0.0
        val shorter = min(na.length, nb.length)
        val longer = max(na.length, nb.length)
        val contains = if (shorter >= MIN_CONTAINED && (nb in na || na in nb)) {
            CONTAINS_BASE + CONTAINS_RANGE * shorter / longer
        } else {
            0.0
        }
        val edit = 1.0 - distance(na, nb).toDouble() / longer
        return maxOf(dice, contains, edit)
    }

    private fun distance(a: String, b: String): Int {
        val row = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var diagonal = row[0]
            row[0] = i
            for (j in 1..b.length) {
                val up = row[j]
                row[j] = minOf(up + 1, row[j - 1] + 1, diagonal + if (a[i - 1] == b[j - 1]) 0 else 1)
                diagonal = up
            }
        }
        return row[b.length]
    }
}
