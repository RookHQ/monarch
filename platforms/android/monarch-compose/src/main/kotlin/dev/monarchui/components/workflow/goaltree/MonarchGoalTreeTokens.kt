package dev.monarchui.components.workflow.goaltree

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.tokens.MonarchTokens
import kotlin.math.roundToInt

/** One visible line of a goal tree: a goal, or the add button under an open goal. */
internal sealed interface GoalTreeLine {
    /** A goal at [depth] (0 at the top), with its number of children, whether it can open, and whether it is open. */
    data class Goal(
        val goal: MonarchGoal,
        val depth: Int,
        val childCount: Int,
        val canToggle: Boolean,
        val open: Boolean,
    ) : GoalTreeLine

    /** The add-child button under the open [parent], one level deeper. */
    data class Add(val parent: MonarchGoal, val depth: Int) : GoalTreeLine
}

/** Component tokens and pure logic of MonarchGoalTree, built from primitives and roles only. */
internal object MonarchGoalTreeTokens {
    /** The indent per level: half a touch target, so three levels still fit a phone. */
    val Indent: Dp = MonarchTokens.Space.space6.dp
    val RowPadding: Dp = MonarchTokens.Space.space2.dp
    val Gap: Dp = MonarchTokens.Space.space1_5.dp
    val InlineGap: Dp = MonarchTokens.Space.space2.dp
    const val EXPANDED_ROTATION: Float = 90f
    private const val PERCENT: Int = 100

    /** The badge tone of a status, as on the web. */
    fun tone(status: MonarchGoalStatus): MonarchTone = when (status) {
        MonarchGoalStatus.NotStarted -> MonarchTone.Neutral
        MonarchGoalStatus.OnTrack -> MonarchTone.Info
        MonarchGoalStatus.AtRisk -> MonarchTone.Warning
        MonarchGoalStatus.OffTrack -> MonarchTone.Danger
        MonarchGoalStatus.Done -> MonarchTone.Success
    }

    /** The tone of the progress bar of a status, as the web's BAR_TONE: accent while on track. */
    fun barTone(status: MonarchGoalStatus): MonarchTone = when (status) {
        MonarchGoalStatus.NotStarted -> MonarchTone.Neutral
        MonarchGoalStatus.OnTrack -> MonarchTone.Accent
        MonarchGoalStatus.AtRisk -> MonarchTone.Warning
        MonarchGoalStatus.OffTrack -> MonarchTone.Danger
        MonarchGoalStatus.Done -> MonarchTone.Success
    }

    /** Progress as a fraction from 0 to 1: clamped, and 0 for not-a-number. */
    fun fraction(progress: Float): Float = if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)

    /** Progress as a whole percentage from 0 to 100. */
    fun percent(progress: Float): Int = (fraction(progress) * PERCENT).roundToInt()

    /**
     * Whether a goal shows its status badge: top-level goals always,
     * children unless quiet and on track or not started.
     */
    fun showsBadge(status: MonarchGoalStatus, depth: Int, quietChildren: Boolean): Boolean =
        depth == 0 || !quietChildren || (status != MonarchGoalStatus.OnTrack && status != MonarchGoalStatus.NotStarted)

    /**
     * The children of every goal, keyed by parent id, and the top-level
     * goals, in their given order. A repeated id keeps its first goal. A goal
     * whose parent is missing or itself is top level.
     */
    fun structure(goals: List<MonarchGoal>): Pair<List<MonarchGoal>, Map<String, List<MonarchGoal>>> {
        val unique = goals.distinctBy { it.id }
        val ids = unique.mapTo(HashSet()) { it.id }
        val roots = mutableListOf<MonarchGoal>()
        val children = LinkedHashMap<String, MutableList<MonarchGoal>>()
        unique.forEach { goal ->
            val parent = goal.parentId?.takeIf { it != goal.id && it in ids }
            if (parent == null) roots += goal else children.getOrPut(parent) { mutableListOf() } += goal
        }
        return roots to children
    }

    /** The goals open at first: every goal with children. */
    fun initiallyExpanded(goals: List<MonarchGoal>): Set<String> = structure(goals).second.keys

    /**
     * The visible lines, depth first. A goal can open when it has children
     * or an add button; its children (and add button) are listed only when it
     * is in [expanded]. A cycle stops at the first goal already on the path.
     */
    fun lines(
        goals: List<MonarchGoal>,
        expanded: Set<String>,
        canAdd: (MonarchGoal, Int) -> Boolean,
    ): List<GoalTreeLine> {
        val (roots, children) = structure(goals)
        val out = mutableListOf<GoalTreeLine>()
        fun visit(goal: MonarchGoal, depth: Int, path: Set<String>) {
            val kids = if (goal.id in path) emptyList() else children[goal.id].orEmpty()
            val adds = canAdd(goal, depth)
            val open = goal.id in expanded
            out += GoalTreeLine.Goal(goal, depth, kids.size, kids.isNotEmpty() || adds, open)
            if (!open) return
            val next = path + goal.id
            kids.forEach { kid -> if (kid.id !in next) visit(kid, depth + 1, next) }
            if (adds) out += GoalTreeLine.Add(goal, depth + 1)
        }
        roots.forEach { visit(it, 0, emptySet()) }
        return out
    }

    /** [expanded] with [id] added or removed. */
    fun toggled(expanded: Set<String>, id: String): Set<String> = if (id in expanded) expanded - id else expanded + id
}
