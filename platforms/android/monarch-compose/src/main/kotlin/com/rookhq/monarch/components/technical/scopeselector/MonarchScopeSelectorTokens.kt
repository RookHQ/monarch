package com.rookhq.monarch.components.technical.scopeselector

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchScopeSelector. */
internal object MonarchScopeSelectorTokens {
    /** The gap between the heading, the groups and the paging button. */
    val SectionGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the parts of a group. */
    val GroupGap: Dp = MonarchTokens.Space.space1.dp

    /** The space inside a group's outline. */
    val GroupPadding: Dp = MonarchTokens.Space.space3.dp

    /** The default number of groups shown before "Show more". */
    const val DEFAULT_PAGE_SIZE: Int = 10

    /** The turn of a chevron that points down while its group is collapsed: it points to the end. */
    const val COLLAPSED_ROTATION: Float = -90f

    /** The room kept at the end of a group header for Select all. */
    val SelectAllRoom: Dp = MonarchTokens.Space.space24.dp

    /** The outline of a group: the surface fill with the subtle border, whether or not scopes are selected. */
    fun groupColors(colors: MonarchColors): MonarchSurfaceColors = MonarchSurfaceColors(
        container = colors.bgSurface,
        content = colors.textDefault,
        border = colors.borderSubtle,
    )

    /** The colour of summaries and descriptions. */
    fun mutedColor(colors: MonarchColors) = colors.textMuted

    /** The values of the scopes of [group] that can be changed. */
    fun offered(group: MonarchScopeGroup): List<String> = group.scopes.filter { it.enabled }.map { it.value }

    /** How many scopes of [group] are in [value]. */
    fun selectedCount(group: MonarchScopeGroup, value: List<String>): Int = group.scopes.count { it.value in value }

    /** Whether every scope of [group] that can be changed is selected. */
    fun allOn(group: MonarchScopeGroup, value: List<String>): Boolean {
        val offered = offered(group)
        return offered.isNotEmpty() && value.containsAll(offered)
    }

    /** [value] after Select all (adds the offered scopes) or Clear all (removes them) on [group]. */
    fun toggledAll(group: MonarchScopeGroup, value: List<String>): List<String> {
        val offered = offered(group)
        return if (allOn(group, value)) value - offered.toSet() else (value + offered).distinct()
    }

    /** [value] with the scopes of [group] replaced by [picked], keeping other groups' scopes in place. */
    fun withGroup(group: MonarchScopeGroup, value: List<String>, picked: List<String>): List<String> {
        val inGroup = group.scopes.map { it.value }.toSet()
        return (value.filterNot { it in inGroup } + picked).distinct()
    }

    /**
     * Auto-selection: for each loaded group not in [seen] that has none of its scopes in [value], adds its offered
     * scopes. Marks every loaded group as seen. Returns the new value, or `null` when nothing changed.
     */
    fun autoSelected(groups: List<MonarchScopeGroup>, value: List<String>, seen: MutableSet<String>): List<String>? {
        val added = mutableListOf<String>()
        groups.filter { !it.loading && it.id !in seen }.forEach { group ->
            seen += group.id
            if (group.scopes.none { it.value in value }) added += offered(group)
        }
        return if (added.isEmpty()) null else (value + added).distinct()
    }
}
