package dev.monarchui.components.forms.passwordstrengthmeter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchPasswordStrengthMeter, built from primitives and roles only. */
internal object MonarchPasswordStrengthMeterTokens {
    const val SEGMENTS: Int = 4
    val Gap: Dp = MonarchTokens.Space.space1_5.dp
    val SegmentGap: Dp = MonarchTokens.Space.space1.dp
    val SegmentHeight: Dp = MonarchTokens.Space.space1.dp
    val RuleGap: Dp = MonarchTokens.Space.space1.dp
    val MarkGap: Dp = MonarchTokens.Space.space1_5.dp
    val MarkSize: Dp = MonarchTokens.Space.space3.dp
    val MarkBorder: Dp = MonarchTokens.Space.space0_5.dp

    /** How many of the four segments are filled: none while empty, at least one otherwise. */
    fun filledSegments(strength: MonarchPasswordStrength?): Int =
        if (strength == null) 0 else maxOf(1, strength.ordinal)

    /** The fill of the filled segments: danger when weak, warning when fair, success when good or strong. */
    fun fill(strength: MonarchPasswordStrength, colors: MonarchColors): Color = when (strength) {
        MonarchPasswordStrength.VeryWeak, MonarchPasswordStrength.Weak -> colors.dangerSolid
        MonarchPasswordStrength.Fair -> colors.warningSolid
        MonarchPasswordStrength.Good, MonarchPasswordStrength.Strong -> colors.successSolid
    }

    /** The colour of segment [index], counted from 0. */
    fun segment(index: Int, strength: MonarchPasswordStrength?, colors: MonarchColors): Color =
        if (strength != null && index < filledSegments(strength)) fill(strength, colors) else colors.bgEmphasis

    /** The colour of a rule's text and mark. */
    fun rule(met: Boolean, colors: MonarchColors): Color = if (met) colors.successStrong else colors.textMuted
}
