package com.rookhq.monarch.components.identity.spaceswitcher

import java.text.Normalizer
import java.util.Locale

/** The choice and search logic of MonarchSpaceSwitcher, as pure functions. */
internal object MonarchSpaceSwitcherRules {
    private val marks = Regex("""\p{Mn}+""")

    /** [text] without accents, in lower case, trimmed: the form search compares. */
    fun fold(text: String): String =
        marks.replace(Normalizer.normalize(text.trim(), Normalizer.Form.NFD), "").lowercase(Locale.ROOT)

    /** Whether any of [fields] contains [query], ignoring case and accents. A blank query matches everything. */
    fun matches(query: String, vararg fields: String?): Boolean {
        val wanted = fold(query)
        return wanted.isEmpty() || fields.any { it != null && wanted in fold(it) }
    }

    /** The chosen Space, or `null` when [selection] is `null` or no longer in [spaces] (read as All spaces). */
    fun current(spaces: List<MonarchSpaceOption>, selection: String?): MonarchSpaceOption? =
        selection?.let { id -> spaces.firstOrNull { it.id == id } }

    /** Whether choosing [next] is allowed: All spaces (`null`) only when [allowsAll]. */
    fun accepts(next: String?, allowsAll: Boolean): Boolean = next != null || allowsAll

    /** Whether the switcher draws nothing: no Spaces, nothing loading, and [hidesWhenEmpty]. */
    fun hidden(spaces: List<MonarchSpaceOption>, loading: Boolean, hidesWhenEmpty: Boolean): Boolean =
        hidesWhenEmpty && spaces.isEmpty() && !loading

    /** What TalkBack reads for a Space row: the name and the description. */
    fun spoken(name: String, description: String?): String =
        if (description.isNullOrBlank()) name else "$name, $description"
}
