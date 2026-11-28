package com.rookhq.monarch.components.scheduling.eventchip

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.R
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * The colours of an event chip in one state.
 *
 * @property container the fill; `Color.Transparent` for none.
 * @property marker the dot or the bar.
 * @property title the title.
 * @property secondary the time, the status word and the detail.
 * @property border the outline of a tentative bar or block, or `null`.
 */
@Immutable
internal data class EventChipColors(
    val container: Color,
    val marker: Color,
    val title: Color,
    val secondary: Color,
    val border: Color?,
)

/** Component tokens of MonarchEventChip, built from primitives and roles only. */
internal object MonarchEventChipTokens {
    val DotSize: Dp = MonarchTokens.Space.space2.dp
    val DotStroke: Dp = (MonarchTokens.Space.px + MonarchTokens.Space.px / 2).dp
    val BarWidth: Dp = (MonarchTokens.Space.space0_5 + MonarchTokens.Space.px).dp
    val BarDash: Dp = MonarchTokens.Space.space1.dp
    val BarGap: Dp = (MonarchTokens.Space.space0_5 + MonarchTokens.Space.px).dp
    val Gap: Dp = MonarchTokens.Space.space1_5.dp
    val DotHorizontalPadding: Dp = MonarchTokens.Space.space1.dp
    val DotVerticalPadding: Dp = MonarchTokens.Space.space0_5.dp
    val BarHorizontalPadding: Dp = MonarchTokens.Space.space2.dp
    val BarVerticalPadding: Dp = MonarchTokens.Space.space1.dp
    val RowGap: Dp = MonarchTokens.Space.space2.dp
    val GutterWidth: Dp = MonarchTokens.Space.space20.dp
    const val TITLE_WEIGHT: Int = MonarchTokens.FontWeight.medium
    const val BLOCK_TITLE_WEIGHT: Int = MonarchTokens.FontWeight.semibold
    const val STATUS_WEIGHT: Int = MonarchTokens.FontWeight.medium

    /** From this font scale on, the title of a dot chip wraps instead of being cut with an ellipsis. */
    const val WRAP_FONT_SCALE: Float = 1.3f

    /** The share of the event colour in the tint of a bar or block, as on the web. */
    private const val TINT = 0.14f
    private const val TENTATIVE_TINT = 0.06f
    private const val PRESSED_TINT = 0.22f

    /** How far the marker of a past event moves towards the surface colour. */
    private const val PAST_FADE = 0.45f

    /** Where the colour bar of a chip [width] wide starts: at the start edge, the right edge right to left. */
    fun barStart(width: Float, barWidth: Float, layoutDirection: LayoutDirection): Float =
        if (layoutDirection == LayoutDirection.Rtl) width - barWidth else 0f

    /** The fill: none for a dot (muted when pressed), the muted fill for inactive events, else a tint. */
    private fun container(
        colors: MonarchColors,
        base: Color,
        tinted: Boolean,
        status: MonarchEventChipStatus,
        pressed: Boolean,
    ): Color = when {
        !tinted -> if (pressed) colors.bgMuted else Color.Transparent
        isInactive(status) -> if (pressed) colors.bgEmphasis else colors.bgMuted
        status == MonarchEventChipStatus.Tentative ->
            lerp(colors.bgSurface, base, if (pressed) TINT else TENTATIVE_TINT)
        else -> lerp(colors.bgSurface, base, if (pressed) PRESSED_TINT else TINT)
    }

    /** The built-in name of a status. */
    @StringRes
    fun statusRes(status: MonarchEventChipStatus): Int = when (status) {
        MonarchEventChipStatus.Confirmed -> R.string.monarch_eventchip_confirmed
        MonarchEventChipStatus.Tentative -> R.string.monarch_eventchip_tentative
        MonarchEventChipStatus.Cancelled -> R.string.monarch_eventchip_cancelled
        MonarchEventChipStatus.Declined -> R.string.monarch_eventchip_declined
    }

    /** Whether the event no longer takes place for the person. */
    fun isInactive(status: MonarchEventChipStatus): Boolean =
        status == MonarchEventChipStatus.Cancelled || status == MonarchEventChipStatus.Declined

    /** Whether the dot is drawn as a ring instead of a filled circle. */
    fun isHollow(status: MonarchEventChipStatus): Boolean = status != MonarchEventChipStatus.Confirmed

    /**
     * What TalkBack reads: "title, time, status, detail". The status is left
     * out when confirmed, the detail in a dot chip, and empty parts are
     * skipped.
     */
    fun spokenName(
        title: String,
        time: String?,
        status: String?,
        detail: String?,
        variant: MonarchEventChipVariant,
    ): String = listOfNotNull(
        title,
        time,
        status,
        detail.takeIf { variant != MonarchEventChipVariant.Dot },
    ).filter { it.isNotBlank() }.joinToString(", ")

    /**
     * The colours of a chip. The event colour is only ever the marker, a
     * border and a light tint mixed into the surface colour; text keeps the
     * Monarch text roles, so it stays readable in every theme.
     *
     * @param eventColor the event's colour; unspecified is the accent.
     */
    @Suppress("LongParameterList")
    fun colors(
        colors: MonarchColors,
        eventColor: Color,
        variant: MonarchEventChipVariant,
        status: MonarchEventChipStatus,
        past: Boolean,
        pressed: Boolean,
    ): EventChipColors {
        val base = if (eventColor.isSpecified) eventColor else colors.accentSolid
        val inactive = isInactive(status)
        val tinted = variant != MonarchEventChipVariant.Dot
        return EventChipColors(
            container = container(colors, base, tinted, status, pressed),
            marker = when {
                inactive -> colors.borderStrong
                past -> lerp(base, colors.bgSurface, PAST_FADE)
                else -> base
            },
            title = if (inactive || past) colors.textMuted else colors.textDefault,
            secondary = colors.textMuted,
            border = if (tinted && status == MonarchEventChipStatus.Tentative) base else null,
        )
    }
}
