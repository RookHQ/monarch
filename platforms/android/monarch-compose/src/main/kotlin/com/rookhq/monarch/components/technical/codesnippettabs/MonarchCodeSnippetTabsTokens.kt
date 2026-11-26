package com.rookhq.monarch.components.technical.codesnippettabs

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchCodeSnippetTabs. */
internal object MonarchCodeSnippetTabsTokens {
    /** The space between the tabs and the code block. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The snippets with distinct ids, in order: a repeated id is left out. */
    fun distinct(snippets: List<MonarchCodeSnippet>): List<MonarchCodeSnippet> = snippets.distinctBy { it.id }

    /**
     * The id of the snippet to show: the first of [candidates] (the app's selection, the synced choice, the set's
     * own choice) that a snippet has, else the first snippet's, or `null` when there are no snippets.
     */
    fun resolve(snippets: List<MonarchCodeSnippet>, vararg candidates: String?): String? {
        val ids = snippets.map { it.id }
        return candidates.firstOrNull { it != null && it in ids } ?: ids.firstOrNull()
    }
}
