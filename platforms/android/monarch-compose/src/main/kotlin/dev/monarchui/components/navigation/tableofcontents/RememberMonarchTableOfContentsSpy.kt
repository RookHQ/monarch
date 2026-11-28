package dev.monarchui.components.navigation.tableofcontents

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.tokens.MonarchTokens

/**
 * The section being read in a `LazyColumn`, for the `selectedId` of a
 * `MonarchTableOfContents`.
 *
 * It follows the web's rule: the first heading whose top is in the top 40%
 * of the list is current; when none is, the last heading scrolled past is
 * current; before the first heading, none is. It reads the list's layout, so
 * it updates as the person scrolls and costs nothing between frames.
 *
 * @param state the state of the `LazyColumn` that shows the page.
 * @param sections the id of each section and the index of its heading item in the list.
 * @param topInset the height of anything that covers the top of the list, such as a pinned header. A heading
 * counts as scrolled past once it goes under it.
 * @return the id of the current section, or `null`.
 */
@Composable
public fun rememberMonarchTableOfContentsSpy(
    state: LazyListState,
    sections: Map<String, Int>,
    topInset: Dp = MonarchTokens.Space.space0.dp,
): State<String?> {
    val inset = with(LocalDensity.current) { topInset.roundToPx() }
    val currentSections by rememberUpdatedState(sections)
    val currentInset by rememberUpdatedState(inset)
    return remember(state) {
        derivedStateOf {
            val layout = state.layoutInfo
            val tops = layout.visibleItemsInfo.associate { it.index to it.offset - layout.viewportStartOffset }
            val headings = currentSections.map { (id, index) ->
                MonarchTableOfContentsHeading(id, index, tops[index])
            }
            MonarchTableOfContentsTokens.current(
                headings = headings,
                firstVisibleIndex = state.firstVisibleItemIndex,
                viewport = layout.viewportEndOffset - layout.viewportStartOffset,
                inset = currentInset,
            )
        }
    }
}
