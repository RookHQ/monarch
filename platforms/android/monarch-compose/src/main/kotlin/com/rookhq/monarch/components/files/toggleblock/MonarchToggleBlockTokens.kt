package com.rookhq.monarch.components.files.toggleblock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchToggleBlock, built from primitives only. */
internal object MonarchToggleBlockTokens {
    /** The content is indented under the summary, as on the web. */
    val ContentIndent: Dp = MonarchTokens.Space.space6.dp

    /** The space under the content, before the next block of the document. */
    val ContentBottomPadding: Dp = MonarchTokens.Space.space2.dp

    /** The quarter turn of the chevron that points right when collapsed and down when expanded. */
    const val EXPANDED_TURN: Float = 90f
}
