package com.rookhq.monarch.components.search.querybuilder

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchQueryBuilder. */
internal object MonarchQueryBuilderTokens {
    /** The gap between the parts of a group. */
    val GroupGap: Dp = MonarchTokens.Space.space3.dp

    /** The space inside a group's outline; it is also the indent of each nested level. */
    val GroupPadding: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the words and the segments of the AND / OR switch. */
    val JoinGap: Dp = MonarchTokens.Space.space2.dp

    /** The default deepest level, counting the top group. */
    const val DEFAULT_MAX_DEPTH: Int = 3

    /** The default most rules per group. */
    const val DEFAULT_MAX_RULES: Int = 20

    /**
     * The outline of a group: levels alternate between the subtle and the muted fill, so a nested group
     * stands apart from its parent in every theme, always with the default border.
     */
    fun groupColors(colors: MonarchColors, depth: Int): MonarchSurfaceColors = MonarchSurfaceColors(
        container = if (depth % 2 == 1) colors.bgSubtle else colors.bgMuted,
        content = colors.textDefault,
        border = colors.borderDefault,
    )

    /** The colour of the join text and the depth note. */
    fun noteColor(colors: MonarchColors) = colors.textMuted

    /** The combinator a new sub-group starts with: the opposite of its parent's, as on the web. */
    fun childCombinator(parent: MonarchQueryCombinator): MonarchQueryCombinator =
        if (parent == MonarchQueryCombinator.And) MonarchQueryCombinator.Or else MonarchQueryCombinator.And

    /** The first id `"<prefix>-<n>"`, counting from 1, that no group or rule in [query] has. */
    fun freshId(query: MonarchQueryGroup, prefix: String = "q"): String {
        val taken = query.ids().toHashSet()
        return generateSequence(1) { it + 1 }.map { "$prefix-$it" }.first { it !in taken }
    }
}
