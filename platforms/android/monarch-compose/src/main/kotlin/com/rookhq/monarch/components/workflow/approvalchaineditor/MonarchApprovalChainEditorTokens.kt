package com.rookhq.monarch.components.workflow.approvalchaineditor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchApprovalChainEditor. */
internal object MonarchApprovalChainEditorTokens {
    /** The gap between the heading, the list and the add button. */
    val SectionGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the fields of one step. */
    val ControlGap: Dp = MonarchTokens.Space.space2.dp

    /** The default fewest steps, as on the web. */
    const val DEFAULT_MIN: Int = 1

    /** The default most steps, as on the web. */
    const val DEFAULT_MAX: Int = 10

    /** The longest step label, as on the web. */
    const val LABEL_MAX_LENGTH: Int = 60

    /** The colour of the description and the limit note. */
    fun noteColor(colors: MonarchColors): Color = colors.textMuted

    /** The first id `"step-<n>"`, counting from 1, that is not in [existing]. */
    fun freshId(existing: Collection<String>): String {
        val taken = existing.toHashSet()
        return generateSequence(1) { it + 1 }.map { "step-$it" }.first { it !in taken }
    }

    /** [steps] with the one at [from] moved to [to]. */
    fun moved(steps: List<MonarchApprovalChainStep>, from: Int, to: Int): List<MonarchApprovalChainStep> =
        steps.toMutableList().apply { add(to, removeAt(from)) }
}
