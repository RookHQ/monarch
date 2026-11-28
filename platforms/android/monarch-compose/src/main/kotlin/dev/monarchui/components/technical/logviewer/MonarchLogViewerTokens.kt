package dev.monarchui.components.technical.logviewer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the colour roles of MonarchLogViewer. */
internal object MonarchLogViewerTokens {
    /** The space between the toolbar rows, the filters and the log. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The space at the start and end of each line. */
    val LinePadding: Dp = MonarchTokens.Space.space2.dp

    /** The space above and below each line. */
    val LineVerticalPadding: Dp = MonarchTokens.Space.space0_5.dp

    /** The width of the level column in characters, so the levels line up in the monospace text. */
    const val LEVEL_WIDTH: Int = 5

    /** The space between a line's columns. */
    const val COLUMN_SEPARATOR: String = "  "

    /** Space between the jump button and the bottom of the log. */
    val JumpPadding: Dp = MonarchTokens.Space.space2.dp

    /** The width of the line number column, in digits. */
    const val LINE_NUMBER_DIGITS: Int = 4

    /** The level filters show every level at first. */
    val AllLevels: Set<MonarchLogLevel> = MonarchLogLevel.entries.toSet()

    /** The colour of a level's word. */
    fun levelColor(level: MonarchLogLevel, colors: MonarchColors): Color = when (level) {
        MonarchLogLevel.Debug -> colors.textMuted
        MonarchLogLevel.Info -> colors.infoStrong
        MonarchLogLevel.Warn -> colors.warningStrong
        MonarchLogLevel.Error -> colors.dangerStrong
    }

    /** The fill behind a line: warnings and errors stand out; the word says the level as well. */
    fun lineBackground(level: MonarchLogLevel, colors: MonarchColors): Color = when (level) {
        MonarchLogLevel.Warn -> colors.warningSubtle
        MonarchLogLevel.Error -> colors.dangerSubtle
        else -> Color.Transparent
    }

    /** The fill of a search match; matches are underlined as well, so they do not rest on colour alone. */
    fun matchBackground(colors: MonarchColors): Color = colors.accentSubtle

    /** The colour of the time, the line number and the source. */
    fun metaColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of the message. */
    fun textColor(colors: MonarchColors): Color = colors.textDefault

    /** The fill of the log area. */
    fun surface(colors: MonarchColors): Color = colors.bgSubtle
}
