package dev.monarchui.components.intelligence.ailauncher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchAiLauncher, built from primitives and roles only. */
internal object MonarchAiLauncherTokens {
    /** The width of the working ring. */
    val RingWidth: Dp = MonarchTokens.Space.space0_5.dp

    /** The gap between the button and the working ring. */
    val RingOutset: Dp = MonarchTokens.Space.space0_5.dp

    /** The gap between the icon and the label. */
    val ContentGap: Dp = MonarchTokens.Space.space2.dp

    /** The visible height of the subtle variant. */
    val SubtleHeight: Dp = MonarchTokens.Control.lg.dp

    /** The space at the start and end of the subtle variant, inside its border. */
    val SubtlePadding: Dp = MonarchTokens.Space.space4.dp

    /** The border of the subtle variant. */
    val BorderWidth: Dp = MonarchTokens.Space.px.dp

    /** The share of the outline the moving part of the working ring covers. */
    const val RING_ARC: Float = 0.5f

    /** The time the working ring takes to go around once. */
    const val RING_PERIOD_MILLIS: Int = MonarchTokens.Duration.shimmer

    /** The highest count shown as a number. */
    const val MAX_COUNT: Int = 99

    /** The text of a count badge: the number, "99+" above 99, or `null` for zero or less. */
    fun countText(count: Int): String? = when {
        count <= 0 -> null
        count > MAX_COUNT -> "$MAX_COUNT+"
        else -> count.toString()
    }

    /** Whether the label is shown: as asked, or for the subtle variant only. */
    fun showsLabel(variant: MonarchAiLauncherVariant, showLabel: Boolean?): Boolean =
        showLabel ?: (variant == MonarchAiLauncherVariant.Subtle)

    /** The state TalkBack reads: "Open" while open, then the badge. `null` when there is neither. */
    fun stateDescription(open: Boolean, openLabel: String, badgeLabel: String?): String? =
        listOfNotNull(openLabel.takeIf { open }, badgeLabel).joinToString(", ").ifEmpty { null }

    /** The fill, content and border colours of the button. */
    fun colors(
        variant: MonarchAiLauncherVariant,
        open: Boolean,
        enabled: Boolean,
        pressed: Boolean,
        colors: MonarchColors,
    ): MonarchSurfaceColors = when {
        !enabled -> MonarchSurfaceColors(colors.bgMuted, colors.textDisabled, colors.borderSubtle)
        variant == MonarchAiLauncherVariant.Solid ->
            MonarchSurfaceColors(if (pressed) colors.aiSolidActive else colors.aiSolid, colors.aiOnSolid, null)
        else -> MonarchSurfaceColors(
            if (pressed) colors.bgMuted else colors.bgSurface,
            colors.aiStrong,
            if (open) colors.aiSolid else colors.borderDefault,
        )
    }

    /** The colour of the working ring. */
    fun ringColor(colors: MonarchColors): Color = colors.aiThinking

    /** The fill and text of the dot and count badges. */
    fun badgeColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(colors.dangerSolid, colors.dangerOnSolid, colors.bgSurface)
}
