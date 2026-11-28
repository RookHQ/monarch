package dev.monarchui.components.workflow.escalationpolicyeditor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.forms.durationinput.DurationInputMath
import dev.monarchui.components.forms.durationinput.MonarchDurationInputUnit
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchEscalationPolicyEditor. */
internal object MonarchEscalationPolicyEditorTokens {
    /** The gap between the levels, the description and the add button. */
    val SectionGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the parts of one level. */
    val ControlGap: Dp = MonarchTokens.Space.space2.dp

    /** The space inside a level's outline. */
    val CardPadding: Dp = MonarchTokens.Space.space3.dp

    /** The default minutes a new level adds to the last one, as on the web. */
    const val DEFAULT_STEP_MINUTES: Int = 15

    /** The default fewest levels, as on the web. */
    const val DEFAULT_MIN: Int = 1

    /** The default most levels, as on the web. */
    const val DEFAULT_MAX: Int = 10

    /** The step of the time field's buttons, as on the web. */
    const val TIME_STEP: Int = 5

    /** The outline of a level: the subtle border, or the danger border while it shows a problem. */
    fun cardColors(colors: MonarchColors, invalid: Boolean): MonarchSurfaceColors = MonarchSurfaceColors(
        container = colors.bgSurface,
        content = colors.textDefault,
        border = if (invalid) colors.dangerBorder else colors.borderSubtle,
    )

    /** The colour of the summary, the description and the limit note. */
    fun noteColor(colors: MonarchColors): Color = colors.textMuted

    /** The first id `"level-<n>"`, counting from 1, that is not in [existing]. */
    fun freshId(existing: Collection<String>): String {
        val taken = existing.toHashSet()
        return generateSequence(1) { it + 1 }.map { "level-$it" }.first { it !in taken }
    }

    /** The time of a new level: [step] after the last one with cumulative timing, else [step]. */
    fun nextMinutes(levels: List<MonarchEscalationLevel>, timing: MonarchEscalationTiming, step: Int): Int =
        if (timing == MonarchEscalationTiming.Cumulative) (levels.lastOrNull()?.afterMinutes ?: 0) + step else step

    /** [chosen] in the order of [order]; ids that are not in [order] keep their place at the end. */
    fun ordered(chosen: List<String>, order: List<String>): List<String> =
        order.filter { it in chosen } + chosen.filter { it !in order }

    /** A duration as in the summary: "15m", "1h 30m", "1d 2h". */
    fun duration(minutes: Int): String = DurationInputMath.format(minutes, MonarchDurationInputUnit.Days)
}
