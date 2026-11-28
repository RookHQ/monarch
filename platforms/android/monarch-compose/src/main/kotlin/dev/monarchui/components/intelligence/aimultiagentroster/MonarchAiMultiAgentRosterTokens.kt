package dev.monarchui.components.intelligence.aimultiagentroster

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.intelligence.aiagentavatar.MonarchAiAgentState
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchAiMultiAgentRoster. */
internal object MonarchAiMultiAgentRosterTokens {
    val RowGap: Dp = MonarchTokens.Space.space1.dp
    val RowPadding: Dp = MonarchTokens.Space.space2.dp
    val ItemGap: Dp = MonarchTokens.Space.space3.dp
    val TextGap: Dp = MonarchTokens.Space.space0_5.dp
    val SummaryGap: Dp = MonarchTokens.Space.space2.dp

    /** Agents that need attention first, so they are never hidden behind "+N". */
    private val Order = listOf(
        MonarchAiAgentState.Error,
        MonarchAiAgentState.Waiting,
        MonarchAiAgentState.Working,
        MonarchAiAgentState.Idle,
    )

    /** The fill of a row: warning or danger tints for agents that need attention, else none. */
    fun rowColor(state: MonarchAiAgentState, colors: MonarchColors): Color = when (state) {
        MonarchAiAgentState.Waiting -> colors.warningSubtle
        MonarchAiAgentState.Error -> colors.dangerSubtle
        MonarchAiAgentState.Idle, MonarchAiAgentState.Working -> Color.Transparent
    }

    /** The colour of the state text. */
    fun stateColor(state: MonarchAiAgentState, colors: MonarchColors): Color = when (state) {
        MonarchAiAgentState.Idle -> colors.textMuted
        MonarchAiAgentState.Working -> colors.aiStrong
        MonarchAiAgentState.Waiting -> colors.warningStrong
        MonarchAiAgentState.Error -> colors.dangerStrong
    }

    /** The name and role colours. */
    fun nameColor(colors: MonarchColors): Color = colors.textDefault

    /** The role, the task, the summary and the empty text. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The members sorted for the stack: errors, then waiting, working and idle, otherwise in order. */
    fun sortedForStack(members: List<MonarchAiMultiAgentRosterMember>): List<MonarchAiMultiAgentRosterMember> =
        members.sortedBy { Order.indexOf(it.state) }

    /** The number of members in each state; every state is present. */
    fun counts(members: List<MonarchAiMultiAgentRosterMember>): Map<MonarchAiAgentState, Int> =
        MonarchAiAgentState.entries.associateWith { state -> members.count { it.state == state } }

    /** The state of each member by id. */
    fun snapshot(members: List<MonarchAiMultiAgentRosterMember>): Map<String, MonarchAiAgentState> =
        members.associate { it.id to it.state }

    /**
     * The members whose state differs from [previous], in order. Nothing for the first state ([previous]
     * `null`); a member that has just joined counts as changed.
     */
    fun changed(
        previous: Map<String, MonarchAiAgentState>?,
        members: List<MonarchAiMultiAgentRosterMember>,
    ): List<MonarchAiMultiAgentRosterMember> =
        if (previous == null) emptyList() else members.filter { previous[it.id] != it.state }

    /** The spoken text of a list row: the name with its kind, then the role, the task and the state. */
    fun rowDetails(role: String?, task: String?, state: String): List<String> =
        listOfNotNull(role?.takeIf { it.isNotBlank() }, task?.takeIf { it.isNotBlank() }, state)
}
