package com.rookhq.monarch.components.forms.combobox

import com.rookhq.monarch.internal.MonarchSearchText

/** What the list of a combobox shows for a query. */
internal class ComboboxResults(
    /** The options to list, at most `limit`. */
    val shown: List<MonarchComboboxOption>,
    /** The number of matching options before the limit. */
    val total: Int,
    /** Whether the Create row is offered for the typed text. */
    val showCreate: Boolean,
)

/** How a combobox matches, orders and cuts its options, and whether it can create one. */
internal class ComboboxRules(
    val filter: ((MonarchComboboxOption, String) -> Boolean)? = null,
    val sortByMatch: Boolean = false,
    val limit: Int? = null,
    val canCreate: Boolean = false,
)

/** Matching, ordering and cutting the options of MonarchCombobox. Pure, so it is tested without a view. */
internal object ComboboxFilter {
    private val wordBreak = Regex("[\\s\\-_/.,()]+")

    /** [text] for comparison: lower case without accents, so "Zoë" matches "zoe". */
    fun normalize(text: String): String = MonarchSearchText.normalize(text)

    /** The default filter: the label, hint or keywords contain the query, ignoring case and accents. */
    fun matches(option: MonarchComboboxOption, query: String): Boolean {
        val needle = normalize(query)
        return listOfNotNull(option.label, option.hint, option.keywords).any { normalize(it).contains(needle) }
    }

    /** How well a label matches: 0 starts with the query, 1 has a word that does, 2 anything else. */
    fun rank(option: MonarchComboboxOption, query: String): Int {
        val label = normalize(option.label)
        val needle = normalize(query)
        return when {
            label.startsWith(needle) -> 0
            label.split(wordBreak).any { it.startsWith(needle) } -> 1
            else -> 2
        }
    }

    /**
     * The options for [query]: every option while it is blank, else those the
     * rules' filter (or [matches]) keeps for the trimmed query, best first when
     * sorting by match, cut to the limit. Create is offered when the rules allow
     * it, the query is not blank and no label equals it.
     */
    fun results(options: List<MonarchComboboxOption>, query: String, rules: ComboboxRules): ComboboxResults {
        val trimmed = query.trim()
        val keep = rules.filter ?: ::matches
        val sortByMatch = rules.sortByMatch
        val limit = rules.limit
        val canCreate = rules.canCreate
        val matched = if (trimmed.isEmpty()) options else options.filter { keep(it, trimmed) }
        val ordered = if (sortByMatch && trimmed.isNotEmpty()) matched.sortedBy { rank(it, trimmed) } else matched
        val shown = if (limit != null && limit >= 0) ordered.take(limit) else ordered
        val exact = options.any { normalize(it.label) == normalize(trimmed) }
        return ComboboxResults(shown, ordered.size, canCreate && trimmed.isNotEmpty() && !exact)
    }

    /** The options split into runs of the same group, in order: each run's group and options. */
    fun sections(options: List<MonarchComboboxOption>): List<Pair<String?, List<MonarchComboboxOption>>> {
        val sections = mutableListOf<Pair<String?, List<MonarchComboboxOption>>>()
        options.forEach { option ->
            val last = sections.lastOrNull()
            if (last != null && last.first == option.group) {
                sections[sections.lastIndex] = last.first to last.second + option
            } else {
                sections += option.group to listOf(option)
            }
        }
        return sections
    }
}
