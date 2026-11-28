package dev.monarchui.components.identity.dataexportrequest

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchDataExportRequest, from primitives and roles only. */
internal object MonarchDataExportRequestTokens {
    /** The space between the parts of the card. */
    val Gap: Dp = MonarchTokens.Space.space3.dp

    /** The space between the title and the status badge. */
    val HeadingGap: Dp = MonarchTokens.Space.space2.dp

    /** The description and the preparation note: muted text. */
    fun noteColor(colors: MonarchColors): Color = colors.textMuted

    /** The status line: default text. */
    fun statusColor(colors: MonarchColors): Color = colors.textDefault
}
