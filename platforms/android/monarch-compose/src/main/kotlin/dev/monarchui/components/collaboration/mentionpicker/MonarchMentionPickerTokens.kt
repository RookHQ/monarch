package dev.monarchui.components.collaboration.mentionpicker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and logic of MonarchMentionPicker, built from primitives and roles only. */
internal object MonarchMentionPickerTokens {
    /** The padding of headings, the empty text and the error. */
    val Padding: Dp = MonarchTokens.Space.space4.dp

    /** The space inside a heading and between placeholder parts. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The top border of the strip. */
    val BorderWidth: Dp = MonarchTokens.Space.px.dp

    /** The tallest the list grows before it scrolls: about four rows at the default font size. */
    val MaxListHeight: Dp = (MonarchTokens.Space.space16 * 4).dp

    /** The width of the text of a placeholder row. */
    val PlaceholderWidth: Dp = (MonarchTokens.Space.space16 * 2).dp

    /** The number of placeholder rows while searching. */
    const val PLACEHOLDER_ROWS: Int = 3

    /** The number sign on the tile of a channel. */
    const val CHANNEL_MARK: String = "#"

    /** The strip: the surface colour. */
    fun background(colors: MonarchColors): Color = colors.bgSurface

    /** The top border of the strip. */
    fun border(colors: MonarchColors): Color = colors.borderSubtle

    /** Headings, the empty text and the error in the muted text colour. */
    fun noteColor(colors: MonarchColors): Color = colors.textMuted

    /** Whether the picker shows: a query, unless a two-word query found nothing, which is ordinary prose. */
    fun isOpen(query: MonarchMentionQuery?, status: MonarchMentionPickerStatus, count: Int): Boolean =
        query != null && !(status == MonarchMentionPickerStatus.Ready && count == 0 && ' ' in query.text)

    /** Whether people and channels are shown under headings: when both kinds are present. */
    fun grouped(items: List<MonarchMentionItem>): Boolean = items.map { it.kind }.distinct().size > 1
}
