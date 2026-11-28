package dev.monarchui.components.intelligence.aimemorypanel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.text.Collator
import java.util.Locale

/** One group of the panel: an entity and its memories, most recently updated first. */
internal data class MonarchAiMemoryPanelGroup(val entity: MonarchAiMemoryEntity, val items: List<MonarchAiMemoryItem>) {
    val ids: List<String> get() = items.map { it.id }
}

/** The colours of the panel's text and dividers. */
internal data class MonarchAiMemoryPanelColors(val heading: Color, val muted: Color, val divider: Color)

/** Component tokens and logic of MonarchAiMemoryPanel, built from primitives and roles only. */
internal object MonarchAiMemoryPanelTokens {
    val Gap: Dp = MonarchTokens.Space.space4.dp
    val RowGap: Dp = MonarchTokens.Space.space2.dp
    val LineGap: Dp = MonarchTokens.Space.space1.dp
    val EmptyPadding: Dp = MonarchTokens.Space.space4.dp
    val BorderWidth: Dp = MonarchTokens.Space.px.dp

    /** The longest memory text in a checkbox name, as on the web. */
    const val SHORT_TEXT: Int = 48

    /** The search debounce, as on the web. */
    const val SEARCH_DEBOUNCE_MILLIS: Long = 300L

    private val WHITESPACE = Regex("\\s+")

    /** Whether [memory] matches [query] by its text or its entity's name, ignoring case. An empty query matches. */
    fun matches(memory: MonarchAiMemoryItem, query: String, locale: Locale): Boolean {
        val q = query.trim().lowercase(locale)
        if (q.isEmpty()) return true
        return memory.text.lowercase(locale).contains(q) || memory.entity.name.lowercase(locale).contains(q)
    }

    /**
     * Groups the memories that match [query] by entity: people, then
     * organizations, projects and other, each by name in [locale]'s order,
     * with the most recently updated memory first in each group.
     */
    fun groups(memories: List<MonarchAiMemoryItem>, query: String, locale: Locale): List<MonarchAiMemoryPanelGroup> {
        val collator = Collator.getInstance(locale)
        return memories.filter { matches(it, query, locale) }
            .groupBy { it.entity }
            .map { (entity, items) -> MonarchAiMemoryPanelGroup(entity, items.sortedByDescending { it.updatedAt }) }
            .sortedWith(compareBy<MonarchAiMemoryPanelGroup> { it.entity.kind.ordinal }.thenComparator { a, b ->
                collator.compare(a.entity.name, b.entity.name)
            })
    }

    /** The tri-state of a checkbox over [ids]: on when all are selected, off when none, indeterminate otherwise. */
    fun checkState(selected: Collection<String>, ids: List<String>): ToggleableState {
        val n = ids.count { it in selected }
        return when {
            n == 0 -> ToggleableState.Off
            n == ids.size -> ToggleableState.On
            else -> ToggleableState.Indeterminate
        }
    }

    /** The selection after toggling [ids]: all added when [state] was not on, all removed when it was. */
    fun toggled(selected: List<String>, ids: List<String>, state: ToggleableState): List<String> =
        if (state == ToggleableState.On) selected - ids.toSet() else (selected + ids).distinct()

    /** A memory text made short enough for a checkbox name: white space collapsed, cut at 48 characters with "…". */
    fun shortText(text: String): String {
        val clean = text.replace(WHITESPACE, " ").trim()
        return if (clean.length > SHORT_TEXT) clean.take(SHORT_TEXT - 1).trimEnd() + "…" else clean
    }

    /** Default text for group names, muted text for kinds, dates and empty states, a subtle divider. */
    fun colors(colors: MonarchColors): MonarchAiMemoryPanelColors = MonarchAiMemoryPanelColors(
        heading = colors.textDefault,
        muted = colors.textMuted,
        divider = colors.borderSubtle,
    )
}
