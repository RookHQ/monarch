package com.rookhq.monarch.components.intelligence.aispendcap

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchGlyphs
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** The outcome of checking a new limit in the form of MonarchAiSpendCap. */
internal enum class MonarchAiSpendCapCheck {
    /** The amount is empty. */
    Required,

    /** The amount is zero or less. */
    Positive,

    /** The amount is above the highest allowed. */
    Max,

    /** The amount is below what is spent and has not been confirmed. */
    BelowSpent,

    /** The amount can be saved. */
    Ok,
}

/** Component tokens and the pure logic of MonarchAiSpendCap. */
internal object MonarchAiSpendCapTokens {
    /** The padding inside the card. */
    val Padding: Dp = MonarchTokens.Space.space4.dp

    /** The gap between the parts of the card. */
    val Gap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between items in a row. */
    val ItemGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between lines. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The default warn-at choices. */
    val DefaultWarnAtOptions: List<Double> = listOf(0.5, 0.75, 0.8, 0.9)

    /** The default share at which the state becomes warning. */
    const val DEFAULT_WARN_AT: Double = 0.8

    /** The state of a cap: no limit, reached at or over the limit (always for zero), warning at [warnAt], else ok. */
    fun state(spent: Long, limit: Long?, warnAt: Double): MonarchAiSpendCapState {
        if (limit == null) return MonarchAiSpendCapState.NoLimit
        val used = spent.coerceAtLeast(0)
        return when {
            limit <= 0 || used >= limit -> MonarchAiSpendCapState.Reached
            used >= limit * warnAt || used.toDouble() / limit >= warnAt -> MonarchAiSpendCapState.Warning
            else -> MonarchAiSpendCapState.Ok
        }
    }

    /** The tone of the state badge. */
    fun tone(state: MonarchAiSpendCapState): MonarchTone = when (state) {
        MonarchAiSpendCapState.Warning -> MonarchTone.Warning
        MonarchAiSpendCapState.Reached -> MonarchTone.Danger
        MonarchAiSpendCapState.Ok, MonarchAiSpendCapState.NoLimit -> MonarchTone.Neutral
    }

    /** The glyph of a state, as the web's STATE_ICON: success, warning, error (circle-x) and info. */
    fun stateGlyph(state: MonarchAiSpendCapState): ImageVector = when (state) {
        MonarchAiSpendCapState.Ok -> MonarchGlyphs.CheckCircle
        MonarchAiSpendCapState.Warning -> MonarchGlyphs.Warning
        MonarchAiSpendCapState.Reached -> MonarchGlyphs.Cancel
        MonarchAiSpendCapState.NoLimit -> MonarchGlyphs.Info
    }

    /** The fill of the bar, as the web's Progress tones: muted, or the strong warning or danger role. */
    fun barColor(state: MonarchAiSpendCapState, colors: MonarchColors): Color = when (state) {
        MonarchAiSpendCapState.Warning -> colors.warningStrong
        MonarchAiSpendCapState.Reached -> colors.dangerStrong
        MonarchAiSpendCapState.Ok, MonarchAiSpendCapState.NoLimit -> colors.textMuted
    }

    /** The track of the bar. */
    fun trackColor(colors: MonarchColors): Color = colors.bgEmphasis

    /** The card: the surface colour with the subtle border. */
    fun cardColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.borderSubtle)

    /** The filled share of the bar, from 0 to 1; `null` when there is no bar (no limit, or a limit of zero). */
    fun fraction(spent: Long, limit: Long?): Float? {
        if (limit == null || limit <= 0) return null
        return (spent.coerceIn(0, limit).toDouble() / limit).toFloat()
    }

    /** Checks a new limit, in the component's unit, against what is spent and the highest allowed. */
    fun check(next: Long?, spent: Long, maxLimit: Long?, warnedBelow: Long?): MonarchAiSpendCapCheck = when {
        next == null -> MonarchAiSpendCapCheck.Required
        next <= 0 -> MonarchAiSpendCapCheck.Positive
        maxLimit != null && next > maxLimit -> MonarchAiSpendCapCheck.Max
        next < spent.coerceAtLeast(0) && warnedBelow != next -> MonarchAiSpendCapCheck.BelowSpent
        else -> MonarchAiSpendCapCheck.Ok
    }
}
