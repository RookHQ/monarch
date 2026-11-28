package com.rookhq.monarch.components.navigation.tabs

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.foundation.MonarchControlMetrics
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** The colours of a tab in one state. */
internal data class MonarchTabColors(val content: Color, val indicator: Color)

/** The colours of a count pill. */
internal data class MonarchTabCountColors(val container: Color, val content: Color)

/** Component tokens of MonarchTabs: the line tabs of the web, with the touch target kept on a thumb. */
internal object MonarchTabsTokens {
    /** The visible height of a tab (the label area): 36dp. */
    val Height: Dp = MonarchControlMetrics.TabHeight

    /** The space above the label area that completes the 48dp touch target: the divider stays against the tabs. */
    val ReachAbove: Dp = MonarchTouchTarget.Minimum - Height

    /** The thickness of the indicator under the selected tab: 2dp. */
    val IndicatorThickness: Dp = MonarchControlMetrics.TabIndicator

    /** The thickness of the divider under the row: 1dp. */
    val DividerThickness: Dp = MonarchControlMetrics.BorderWidth

    /** Space each side of the label that belongs to its touch target; two add up to the web's 16dp gap. */
    val SidePadding: Dp = MonarchTokens.Space.space2.dp

    /** The padding between the muted track of the pill tabs and the tabs. */
    val PillTrackPadding: Dp = MonarchTokens.Space.space0_5.dp

    /** The space between the icon, the label and the count. */
    val ContentGap: Dp = MonarchTokens.Space.space1_5.dp

    /** The space between the row and the panel under it. */
    val PanelTopPadding: Dp = MonarchTokens.Space.space4.dp

    /** The smallest count pill: 20 by 18dp. */
    val CountMinWidth: Dp = MonarchTokens.Space.space5.dp
    val CountHeight: Dp = (MonarchTokens.Space.space4 + MonarchTokens.Space.space0_5).dp

    /** Inactive muted, active and hovered default, disabled disabled; the indicator is the strong accent. */
    fun colors(selected: Boolean, hovered: Boolean, enabled: Boolean, colors: MonarchColors): MonarchTabColors = when {
        !enabled -> MonarchTabColors(colors.textDisabled, Color.Transparent)
        selected -> MonarchTabColors(colors.textDefault, colors.accentStrong)
        hovered -> MonarchTabColors(colors.textDefault, Color.Transparent)
        else -> MonarchTabColors(colors.textMuted, Color.Transparent)
    }

    /** The label colour of a tab: the strong accent for the current one in time ("today"), else [colors]'s content. */
    fun labelColor(accent: Boolean, base: MonarchTabColors, enabled: Boolean, colors: MonarchColors): Color =
        if (accent && enabled) colors.accentStrong else base.content

    /** The second line of a rich tab: muted, or the strong accent on the current one. */
    fun secondaryColor(accent: Boolean, enabled: Boolean, colors: MonarchColors): Color = when {
        !enabled -> colors.textDisabled
        accent -> colors.accentStrong
        else -> colors.textMuted
    }

    /** The space above and below the two lines of a rich tab, so it stays 48dp with the touch target. */
    val RichVerticalPadding: Dp = MonarchTokens.Space.space1.dp

    fun countColors(tone: MonarchTone, selected: Boolean, colors: MonarchColors): MonarchTabCountColors =
        if (tone == MonarchTone.Neutral) {
            MonarchTabCountColors(colors.bgMuted, if (selected) colors.textDefault else colors.textMuted)
        } else {
            val palette = MonarchTonePalette.of(tone, colors)
            MonarchTabCountColors(palette.subtle, palette.strong)
        }
}
