package dev.monarchui.components.intelligence.aisubagenttree

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** One visible row of the tree: an agent, how deep it is, and whether it can open and is open. */
internal data class MonarchAiSubagentTreeLine(
    val agent: MonarchAiSubagent,
    val depth: Int,
    val hasChildren: Boolean,
    val open: Boolean,
)

/** Component tokens and the pure logic of MonarchAiSubagentTree. */
internal object MonarchAiSubagentTreeTokens {
    /** The indent of each level. */
    val Indent: Dp = MonarchTokens.Space.space6.dp

    /** The padding above and below a row's text. */
    val RowPadding: Dp = MonarchTokens.Space.space2.dp

    /** The padding at the end of a row's text. */
    val EndPadding: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the parts of a row and between the parts of the tree. */
    val Gap: Dp = MonarchTokens.Space.space1.dp

    /** The gap between the summary, the tree and the actions. */
    val SectionGap: Dp = MonarchTokens.Space.space3.dp

    /** The width of the mark at the start of the selected row. */
    val SelectedMarkWidth: Dp = MonarchTokens.Space.space0_5.dp

    /** The turn of the chevron of an open agent, in degrees. */
    const val EXPANDED_ROTATION: Float = 90f

    /** Every agent of the tree in reading order: the parent, then its children. */
    fun flatten(agents: List<MonarchAiSubagent>): List<MonarchAiSubagent> =
        agents.flatMap { listOf(it) + flatten(it.children) }

    /** How many agents are in each status. */
    fun counts(agents: List<MonarchAiSubagent>): Map<MonarchAiSubagentStatus, Int> =
        flatten(agents).groupingBy { it.status }.eachCount()

    private fun active(status: MonarchAiSubagentStatus): Boolean =
        status == MonarchAiSubagentStatus.Working || status == MonarchAiSubagentStatus.NeedsInput

    /**
     * The agents open at first: every agent with children that is working or
     * needs input, or has such an agent anywhere under it, so nothing active
     * is hidden in a closed branch.
     */
    fun defaultExpanded(agents: List<MonarchAiSubagent>): Set<String> {
        val open = mutableSetOf<String>()
        fun walk(agent: MonarchAiSubagent): Boolean {
            var anyActive = active(agent.status)
            agent.children.forEach { if (walk(it)) anyActive = true }
            if (anyActive && agent.children.isNotEmpty()) open += agent.id
            return anyActive
        }
        agents.forEach { walk(it) }
        return open
    }

    /** The rows shown: every top-level agent and the children of open agents. */
    fun lines(agents: List<MonarchAiSubagent>, expanded: Set<String>): List<MonarchAiSubagentTreeLine> {
        val out = mutableListOf<MonarchAiSubagentTreeLine>()
        fun walk(list: List<MonarchAiSubagent>, depth: Int) {
            list.forEach { agent ->
                val hasChildren = agent.children.isNotEmpty()
                val open = hasChildren && agent.id in expanded
                out += MonarchAiSubagentTreeLine(agent, depth, hasChildren, open)
                if (open) walk(agent.children, depth + 1)
            }
        }
        walk(agents, 0)
        return out
    }

    /** [expanded] with [id] opened or closed. */
    fun toggled(expanded: Set<String>, id: String): Set<String> = if (id in expanded) expanded - id else expanded + id

    /**
     * The agents whose status is news since [before]: a changed status, or a
     * new agent that already needs input or failed. Nothing on the first look.
     */
    fun news(
        before: Map<String, MonarchAiSubagentStatus>?,
        agents: List<MonarchAiSubagent>,
    ): List<MonarchAiSubagent> {
        if (before == null) return emptyList()
        return flatten(agents).filter { agent ->
            val previous = before[agent.id]
            if (previous == null) {
                agent.status == MonarchAiSubagentStatus.NeedsInput || agent.status == MonarchAiSubagentStatus.Failed
            } else {
                previous != agent.status
            }
        }
    }

    /** The tone of a status badge. */
    fun tone(status: MonarchAiSubagentStatus): MonarchTone = when (status) {
        MonarchAiSubagentStatus.Working -> MonarchTone.Ai
        MonarchAiSubagentStatus.NeedsInput -> MonarchTone.Warning
        MonarchAiSubagentStatus.Done -> MonarchTone.Success
        MonarchAiSubagentStatus.Failed -> MonarchTone.Danger
        MonarchAiSubagentStatus.Waiting, MonarchAiSubagentStatus.Stopped -> MonarchTone.Neutral
    }

    /** The fill of a row: the accent subtle colour when selected. */
    fun rowColor(selected: Boolean, colors: MonarchColors): Color =
        if (selected) colors.accentSubtle else Color.Transparent

    /** The mark at the start of the selected row, so selection does not rest on the fill alone. */
    fun selectedMarkColor(colors: MonarchColors): Color = colors.accentSolid
}
