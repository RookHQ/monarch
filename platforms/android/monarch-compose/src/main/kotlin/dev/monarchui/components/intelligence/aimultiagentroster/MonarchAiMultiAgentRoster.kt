package dev.monarchui.components.intelligence.aimultiagentroster

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.components.identity.avatar.MonarchAvatarGroup
import dev.monarchui.components.identity.avatar.MonarchAvatarLogic
import dev.monarchui.components.identity.avatar.MonarchAvatarShape
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.intelligence.aiagentavatar.MonarchAiAgentAvatar
import dev.monarchui.components.intelligence.aiagentavatar.MonarchAiAgentAvatarLabels
import dev.monarchui.components.intelligence.aiagentavatar.MonarchAiAgentState
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * Every agent taking part in a task and what each is doing now: a list with
 * the state and the current task, or a compact stack of pictures for a header.
 *
 * Built by Monarch from `MonarchAiAgentAvatar` rows in a `Column`, or from
 * `MonarchAvatarGroup` for the stack; Material 3 has no equivalent. The list
 * is not lazy, so it can sit in a scrolling screen; a task has a handful of
 * agents. In the stack, agents with errors come first, then those waiting for
 * a person, so they are never hidden behind "+N". Agents waiting or in error
 * are tinted rows, and every state is also written, so nothing depends on the
 * ring, the tint or the motion.
 *
 * A list row is one TalkBack element: the name with "(AI agent)", the role,
 * the task and the state. In the stack each picture is read by its avatar and
 * "+N" reads every hidden agent with its state. A state change is announced
 * through a polite live region, never the first state.
 *
 * @param members the agents, each with a state and an optional task.
 * @param modifier the modifier of the roster.
 * @param variant a row per agent, or overlapping pictures beside the summary.
 * @param maxVisible in the stack, the pictures shown before "+N". With exactly one too many, all are shown.
 * @param showSummary shows the "3 agents · 2 working" line.
 * @param announcesChanges announces state changes to TalkBack. Never the first state.
 * @param labels replacements for the built-in strings.
 */
@OptIn(ExperimentalLayoutApi::class)
@ExperimentalMonarchApi
@Composable
public fun MonarchAiMultiAgentRoster(
    members: List<MonarchAiMultiAgentRosterMember>,
    modifier: Modifier = Modifier,
    variant: MonarchAiMultiAgentRosterVariant = MonarchAiMultiAgentRosterVariant.List,
    maxVisible: Int = 4,
    showSummary: Boolean = true,
    announcesChanges: Boolean = true,
    labels: MonarchAiMultiAgentRosterLabels = MonarchAiMultiAgentRosterLabels(),
) {
    val tokens = MonarchAiMultiAgentRosterTokens
    val colors = MonarchTheme.colors
    val stateName = stateNames(labels)
    val announcement = rememberRosterAnnouncement(members, announcesChanges, labels, stateName)
    val summary = if (showSummary && members.isNotEmpty()) summaryText(members, labels) else null
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.SummaryGap)) {
        when {
            members.isEmpty() -> Text(
                text = monarchString(labels.empty, R.string.monarch_aimultiagentroster_empty),
                style = MonarchTheme.typography.caption,
                color = tokens.mutedColor(colors),
            )
            variant == MonarchAiMultiAgentRosterVariant.Stack -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.SummaryGap),
                verticalArrangement = Arrangement.spacedBy(tokens.SummaryGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Stack(members, maxVisible, labels, stateName)
                if (summary != null) SummaryLine(summary)
            }
            else -> {
                ListRows(members, labels, stateName)
                if (summary != null) SummaryLine(summary)
            }
        }
        // Always present, so each change is announced once as a change of this live region.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

@Composable
private fun SummaryLine(summary: String) {
    Text(
        text = summary,
        style = MonarchTheme.typography.caption,
        color = MonarchAiMultiAgentRosterTokens.mutedColor(MonarchTheme.colors),
    )
}

@Composable
private fun ListRows(
    members: List<MonarchAiMultiAgentRosterMember>,
    labels: MonarchAiMultiAgentRosterLabels,
    stateName: (MonarchAiAgentState) -> String,
) {
    val listLabel = monarchString(labels.label, R.string.monarch_aimultiagentroster_label)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = listLabel
                isTraversalGroup = true
            },
        verticalArrangement = Arrangement.spacedBy(MonarchAiMultiAgentRosterTokens.RowGap),
    ) {
        members.forEach { member -> MemberRow(member, labels, stateName) }
    }
}

@Composable
private fun MemberRow(
    member: MonarchAiMultiAgentRosterMember,
    labels: MonarchAiMultiAgentRosterLabels,
    stateName: (MonarchAiAgentState) -> String,
) {
    val tokens = MonarchAiMultiAgentRosterTokens
    val colors = MonarchTheme.colors
    val agent = member.agent
    val state = stateName(member.state)
    val kind = monarchString(labels.agentKind, R.string.monarch_aimultiagentroster_agent_kind)
    val name = stringResource(R.string.monarch_aimultiagentroster_name_with_kind, agent.name, kind)
    val details = tokens.rowDetails(agent.role, member.task, state)
    val spoken = stringResource(
        R.string.monarch_aimultiagentroster_row,
        name,
        details.joinToString(stringResource(R.string.monarch_aimultiagentroster_row_separator)),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.rowColor(member.state, colors), MonarchShapeDefaults.Control)
            .padding(tokens.RowPadding)
            .clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchAiAgentAvatar(agent = agent, image = member.image, state = member.state, decorative = true)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.TextGap)) {
            Text(text = agent.name, style = MonarchTheme.typography.bodyStrong, color = tokens.nameColor(colors))
            agent.role?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, style = MonarchTheme.typography.caption, color = tokens.mutedColor(colors))
            }
            member.task?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, style = MonarchTheme.typography.caption, color = tokens.mutedColor(colors))
            }
        }
        Text(
            text = state,
            style = MonarchTheme.typography.caption,
            color = tokens.stateColor(member.state, colors),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun Stack(
    members: List<MonarchAiMultiAgentRosterMember>,
    maxVisible: Int,
    labels: MonarchAiMultiAgentRosterLabels,
    stateName: (MonarchAiAgentState) -> String,
) {
    val sorted = MonarchAiMultiAgentRosterTokens.sortedForStack(members)
    val visible = maxVisible.coerceAtLeast(1)
    val avatarLabels = MonarchAiAgentAvatarLabels(
        kind = monarchString(labels.agentKind, R.string.monarch_aimultiagentroster_agent_kind),
        state = stateName,
    )
    val separator = stringResource(R.string.monarch_aimultiagentroster_hidden_separator)
    val hiddenText = sorted.drop(MonarchAvatarLogic.visibleCount(sorted.size, visible)).map { member ->
        stringResource(R.string.monarch_aimultiagentroster_hidden, member.agent.name, stateName(member.state))
    }
    MonarchAvatarGroup(
        items = sorted,
        label = monarchString(labels.label, R.string.monarch_aimultiagentroster_label),
        maxVisible = visible,
        size = MonarchAvatarSize.Sm,
        shape = MonarchAvatarShape.Square,
        overflowLabel = { hiddenText.joinToString(separator) },
    ) { member ->
        MonarchAiAgentAvatar(
            agent = member.agent,
            image = member.image,
            state = member.state,
            size = MonarchAvatarSize.Sm,
            labels = avatarLabels,
        )
    }
}

@Composable
private fun stateNames(labels: MonarchAiMultiAgentRosterLabels): (MonarchAiAgentState) -> String {
    val names = MonarchAiAgentState.entries.associateWith { state ->
        labels.state?.invoke(state) ?: stringResource(stateResource(state))
    }
    return { state -> names.getValue(state) }
}

private fun stateResource(state: MonarchAiAgentState): Int = when (state) {
    MonarchAiAgentState.Idle -> R.string.monarch_aimultiagentroster_idle
    MonarchAiAgentState.Working -> R.string.monarch_aimultiagentroster_working
    MonarchAiAgentState.Waiting -> R.string.monarch_aimultiagentroster_waiting
    MonarchAiAgentState.Error -> R.string.monarch_aimultiagentroster_error
}

@Composable
private fun summaryText(
    members: List<MonarchAiMultiAgentRosterMember>,
    labels: MonarchAiMultiAgentRosterLabels,
): String {
    val counts = MonarchAiMultiAgentRosterTokens.counts(members)
    labels.summary?.let { return it(members.size, counts) }
    val working = counts.getValue(MonarchAiAgentState.Working)
    val waiting = counts.getValue(MonarchAiAgentState.Waiting)
    val errors = counts.getValue(MonarchAiAgentState.Error)
    val parts = listOfNotNull(
        pluralStringResource(R.plurals.monarch_aimultiagentroster_agents, members.size, members.size),
        countPart(R.plurals.monarch_aimultiagentroster_summary_working, working),
        countPart(R.plurals.monarch_aimultiagentroster_summary_waiting, waiting),
        countPart(R.plurals.monarch_aimultiagentroster_summary_error, errors),
    )
    return parts.joinToString(stringResource(R.string.monarch_aimultiagentroster_separator))
}

/** A summary part for [count] agents, or `null` when there are none. */
@Composable
private fun countPart(id: Int, count: Int): String? = if (count > 0) pluralStringResource(id, count, count) else null

/** The latest announcement of state changes; empty until something changes. */
@Composable
private fun rememberRosterAnnouncement(
    members: List<MonarchAiMultiAgentRosterMember>,
    announces: Boolean,
    labels: MonarchAiMultiAgentRosterLabels,
    stateName: (MonarchAiAgentState) -> String,
): String {
    val resources = LocalResources.current
    val currentLabels by rememberUpdatedState(labels)
    val currentStateName by rememberUpdatedState(stateName)
    var previous by remember { mutableStateOf<Map<String, MonarchAiAgentState>?>(null) }
    var announcement by remember { mutableStateOf("") }
    val signature = MonarchAiMultiAgentRosterTokens.snapshot(members)
    LaunchedEffect(signature, announces) {
        val changed = MonarchAiMultiAgentRosterTokens.changed(previous, members)
        previous = signature
        if (!announces || changed.isEmpty()) return@LaunchedEffect
        val separator = resources.getString(R.string.monarch_aimultiagentroster_changes_separator)
        announcement = changed.joinToString(separator) {
            val state = currentStateName(it.state)
            currentLabels.changed?.invoke(it.agent.name, state)
                ?: resources.getString(R.string.monarch_aimultiagentroster_changed, it.agent.name, state)
        }
    }
    return announcement
}
