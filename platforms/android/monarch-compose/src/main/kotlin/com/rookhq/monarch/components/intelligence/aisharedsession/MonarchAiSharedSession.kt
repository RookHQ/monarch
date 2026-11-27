package com.rookhq.monarch.components.intelligence.aisharedsession

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarGroup
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarLogic
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The presence and turn-taking bar of an AI conversation shared by several
 * people and agents: who is here, who is typing, whose request is being
 * answered, what waits, and whether you may send.
 *
 * Built by Monarch from `MonarchAvatarGroup`, `MonarchButton` and a
 * `MonarchBottomSheet` with `MonarchListRow` rows and `MonarchChip` Follow toggles;
 * Material 3 has no equivalent. Overlapping avatars are too small to tap one by
 * one, so the whole stack is one button that opens the participant list. Data
 * comes in through parameters and every action goes out through a callback.
 * The answering agent's ring turns (still when animations are removed).
 *
 * The bar is a traversal group. The stack is read as "3 people and 1 agent
 * here" with each visible participant's role or "AI agent", presence and
 * activity. Changes of who is being answered, of who is reviewing and of the
 * queue length are announced through a polite live region; typing and
 * speaking are not, and neither is the first status. A queue move made here is
 * announced with the request's new position.
 *
 * @param participants everyone in the session, people and agents, in display order. Mark one person `you`; their
 * role decides what they can do.
 * @param modifier the modifier of the bar.
 * @param answering the answer in progress: which agent, whose request and an optional label.
 * @param queue requests waiting for their turn, first in line first.
 * @param maxVisible the avatars shown before "+N". With exactly one too many, all are shown.
 * @param following the ids of the people you follow.
 * @param onFollowingChange shows a Follow toggle beside every other person in the list; receives the new set.
 * @param queueExpanded whether the queue is shown, when the app owns it. `null` lets the Queue button own it.
 * @param onQueueExpandedChange called when the Queue button is pressed.
 * @param accessRequested replaces a viewer's Request access button with "Access requested".
 * @param announcesChanges announces status changes and queue moves to TalkBack.
 * @param onWithdraw shows Withdraw on your own requests; receives the request id.
 * @param onReorder shows Move up and Move down to an owner; receives the request ids in their new order and the
 * move.
 * @param onRemove shows Remove on other people's requests to an owner; receives the request id.
 * @param onInvite shows an Invite button; the app opens its own invite flow.
 * @param onRequestAccess shows Request access in a viewer's notice.
 * @param labels replacements for the built-in strings.
 */
@OptIn(ExperimentalLayoutApi::class)
@ExperimentalMonarchApi
@Composable
public fun MonarchAiSharedSession(
    participants: List<MonarchAiSharedSessionParticipant>,
    modifier: Modifier = Modifier,
    answering: MonarchAiSharedSessionAnswering? = null,
    queue: List<MonarchAiSharedSessionRequest> = emptyList(),
    maxVisible: Int = 4,
    following: Set<String> = emptySet(),
    onFollowingChange: ((Set<String>) -> Unit)? = null,
    queueExpanded: Boolean? = null,
    onQueueExpandedChange: ((Boolean) -> Unit)? = null,
    accessRequested: Boolean = false,
    announcesChanges: Boolean = true,
    onWithdraw: ((id: String) -> Unit)? = null,
    onReorder: ((ids: List<String>, move: MonarchAiSharedSessionMove) -> Unit)? = null,
    onRemove: ((id: String) -> Unit)? = null,
    onInvite: (() -> Unit)? = null,
    onRequestAccess: (() -> Unit)? = null,
    labels: MonarchAiSharedSessionLabels = MonarchAiSharedSessionLabels(),
) {
    val texts = MonarchAiSharedSessionTexts(LocalResources.current, labels)
    val logic = MonarchAiSharedSessionLogic
    val groups = logic.groups(participants)
    val clauses = logic.status(participants, answering, queue.size, texts.status())
    val answeringText = clauses.firstOrNull { it.kind == MonarchAiSharedSessionClauseKind.Answering }?.text
    var live by remember { mutableStateOf("") }
    var pendingMove by remember { mutableStateOf<MonarchAiSharedSessionMove?>(null) }
    AnnounceStatus(logic.announcement(clauses, texts.announceSeparator), announcesChanges) { live = it }
    val ids = queue.map { it.id }
    LaunchedEffect(ids) {
        val move = pendingMove ?: return@LaunchedEffect
        pendingMove = null
        val index = ids.indexOf(move.id)
        if (announcesChanges && index >= 0 && index != move.from) {
            live = texts.moved(askerName(participants, queue[index], texts), index + 1, queue.size)
        }
    }
    var internalExpanded by rememberSaveable { mutableStateOf(false) }
    val expanded = queueExpanded ?: internalExpanded
    var listOpen by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier.semantics {
            contentDescription = texts.label
            isTraversalGroup = true
        },
        verticalArrangement = Arrangement.spacedBy(MonarchAiSharedSessionTokens.Gap),
    ) {
        val toggle: () -> Unit = {
            if (queueExpanded == null) internalExpanded = !expanded
            onQueueExpandedChange?.invoke(!expanded)
        }
        Bar(
            session = BarState(participants, groups, answering?.participantId, answeringText, clauses, maxVisible),
            queueSize = queue.size,
            expanded = expanded,
            texts = texts,
            onToggleQueue = toggle,
            onInvite = onInvite,
            onOpenList = { listOpen = true },
        )
        if (logic.viewer(groups.you)) ViewerNotice(accessRequested, onRequestAccess, texts)
        if (queue.isNotEmpty() && expanded) {
            val actions = QueueActions(
                owner = logic.owner(groups.you),
                you = groups.you?.id,
                onWithdraw = onWithdraw,
                onRemove = onRemove,
                onMove = moveAction(ids, onReorder) { pendingMove = it },
            )
            Queue(queue, participants, actions, texts)
        }
        // Always present, so each change is announced once as a change of this live region.
        MonarchLiveStatus(text = live.takeIf { it.isNotEmpty() }, visible = false)
    }
    if (listOpen) {
        MonarchAiSharedSessionList(
            model = MonarchAiSharedSessionListModel(
                groups = groups,
                answeringId = answering?.participantId,
                answeringText = answeringText,
                following = following,
                onFollowingChange = onFollowingChange,
                texts = texts,
            ),
            onClose = { listOpen = false },
        )
    }
}

/** The name of whoever asked [request], or "Someone". */
private fun askerName(
    participants: List<MonarchAiSharedSessionParticipant>,
    request: MonarchAiSharedSessionRequest,
    texts: MonarchAiSharedSessionTexts,
): String = participants.firstOrNull { it.id == request.participantId }?.name ?: texts.someone

/** The owner's move, which records the move for the announcement, or `null` without [onReorder]. */
private fun moveAction(
    ids: List<String>,
    onReorder: ((List<String>, MonarchAiSharedSessionMove) -> Unit)?,
    onMoved: (MonarchAiSharedSessionMove) -> Unit,
): ((Int, Int) -> Unit)? = onReorder?.let { reorder ->
    { from, to ->
        val order = MonarchAiSharedSessionLogic.reorder(ids, from, to)
        if (order != null) {
            val move = MonarchAiSharedSessionMove(ids[from], from, to)
            onMoved(move)
            reorder(order, move)
        }
    }
}

/** What the bar shows of the session. */
private class BarState(
    val participants: List<MonarchAiSharedSessionParticipant>,
    val groups: MonarchAiSharedSessionGroups,
    val answeringId: String?,
    val answeringText: String?,
    val clauses: List<MonarchAiSharedSessionClause>,
    val maxVisible: Int,
)

/** The stack, the status line, the Queue toggle and Invite, wrapping onto more lines when narrow. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Bar(
    session: BarState,
    queueSize: Int,
    expanded: Boolean,
    texts: MonarchAiSharedSessionTexts,
    onToggleQueue: () -> Unit,
    onInvite: (() -> Unit)?,
    onOpenList: () -> Unit,
) {
    val tokens = MonarchAiSharedSessionTokens
    val colors = MonarchTheme.colors
    val quiet = session.clauses.isEmpty()
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (session.participants.isNotEmpty()) Stack(session, texts, onOpenList)
        Text(
            text = if (quiet) texts.quiet else MonarchAiSharedSessionLogic.line(session.clauses, texts.separator),
            style = MonarchTheme.typography.caption,
            color = if (quiet) tokens.quietColor(colors) else tokens.statusColor(colors),
        )
        if (queueSize > 0) {
            MonarchButton(
                text = texts.queueToggle(queueSize),
                trailingIcon = MonarchDefaultIcons.ChevronUp,
                onClick = onToggleQueue,
                modifier = Modifier.semantics {
                    if (expanded) {
                        collapse { onToggleQueue(); true }
                    } else {
                        expand { onToggleQueue(); true }
                    }
                },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
        if (onInvite != null) {
            MonarchButton(
                text = texts.invite,
                onClick = onInvite,
                size = MonarchButtonSize.Sm,
                leadingIcon = MonarchDefaultIcons.Plus,
            )
        }
    }
}

/** Reports [announcement] when it changes, never the first one, and nothing when [announces] is off. */
@Composable
private fun AnnounceStatus(announcement: String, announces: Boolean, onAnnounce: (String) -> Unit) {
    var previous by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(announcement) {
        val before = previous
        previous = announcement
        if (before != null && announces && announcement != before) onAnnounce(announcement)
    }
}

@Composable
private fun Stack(session: BarState, texts: MonarchAiSharedSessionTexts, onOpen: () -> Unit) {
    val participants = session.participants
    val answeringId = session.answeringId
    val answeringText = session.answeringText
    val maxVisible = session.maxVisible
    val groups = session.groups
    val limit = maxVisible.coerceAtLeast(1)
    val visible = MonarchAvatarLogic.visibleCount(participants.size, limit)
    val faces = participants.take(visible).joinToString(texts.facesSeparator) { participant ->
        val doing = if (participant.id == answeringId) {
            answeringText
        } else {
            participant.activity?.let { texts.activity(it) }
        }
        (listOf(texts.displayName(participant)) + texts.details(participant, doing))
            .joinToString(texts.detailsSeparator)
    }
    val here = texts.here(groups.people.size, groups.agents.size)
    val spoken = stringResource(R.string.monarch_aisharedsession_stack, here, faces)
    val showAll = texts.showAll(participants.size)
    val shape = MonarchShapeDefaults.Mark
    Box(
        modifier = Modifier
            .monarchMinimumTouchTarget()
            .clip(shape)
            .monarchFocusRing(shape)
            .clickable(indication = ripple(), interactionSource = null, role = Role.Button, onClickLabel = showAll) {
                onOpen()
            }
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick(label = showAll) {
                    onOpen()
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        MonarchAvatarGroup(
            items = participants,
            label = here,
            maxVisible = limit,
            size = MonarchAvatarSize.Sm,
        ) { participant ->
            MonarchAiSharedSessionFace(participant, participant.id == answeringId, MonarchAvatarSize.Sm)
        }
    }
}

@Composable
private fun ViewerNotice(accessRequested: Boolean, onRequestAccess: (() -> Unit)?, texts: MonarchAiSharedSessionTexts) {
    val tokens = MonarchAiSharedSessionTokens
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .monarchSurface(tokens.noticeColors(colors), MonarchShapeDefaults.Control)
            .padding(tokens.NoticePadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = texts.viewerNotice,
            modifier = Modifier.weight(1f),
            style = MonarchTheme.typography.caption,
            color = tokens.noticeColors(colors).content,
        )
        when {
            accessRequested -> Text(
                text = texts.accessRequested,
                style = MonarchTheme.typography.caption,
                color = tokens.detailColor(colors),
            )
            onRequestAccess != null -> MonarchButton(
                text = texts.requestAccess,
                onClick = onRequestAccess,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}

/** What the viewer may do with the queue. */
private class QueueActions(
    val owner: Boolean,
    val you: String?,
    val onWithdraw: ((String) -> Unit)?,
    val onRemove: ((String) -> Unit)?,
    val onMove: ((from: Int, to: Int) -> Unit)?,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Queue(
    queue: List<MonarchAiSharedSessionRequest>,
    participants: List<MonarchAiSharedSessionParticipant>,
    actions: QueueActions,
    texts: MonarchAiSharedSessionTexts,
) {
    val tokens = MonarchAiSharedSessionTokens
    val colors = MonarchTheme.colors
    val queueTitle = texts.queueTitle
    Column(
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = queueTitle
            isTraversalGroup = true
        },
        verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
    ) {
        queue.forEachIndexed { index, request ->
            val asker = participants.firstOrNull { it.id == request.participantId }
            val askerName = asker?.name ?: texts.someone
            val own = actions.you != null && request.participantId == actions.you
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .monarchSurface(tokens.requestColors(own, colors), MonarchShapeDefaults.Control)
                    .padding(tokens.RequestPadding),
                verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
            ) {
                Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                    Text(
                        text = asker?.let { texts.displayName(it) } ?: texts.someone,
                        style = MonarchTheme.typography.bodyStrong,
                        color = tokens.nameColor(colors),
                    )
                    Text(
                        text = request.preview,
                        style = MonarchTheme.typography.caption,
                        color = tokens.detailColor(colors),
                    )
                }
                RequestButtons(request, index, queue.size, askerName, own, actions, texts)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RequestButtons(
    request: MonarchAiSharedSessionRequest,
    index: Int,
    total: Int,
    asker: String,
    own: Boolean,
    actions: QueueActions,
    texts: MonarchAiSharedSessionTexts,
) {
    val move = actions.onMove.takeIf { actions.owner }
    val withdraw = actions.onWithdraw.takeIf { own }
    val remove = actions.onRemove.takeIf { actions.owner && !own }
    if (move == null && withdraw == null && remove == null) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MonarchAiSharedSessionTokens.SmallGap),
        verticalArrangement = Arrangement.spacedBy(MonarchAiSharedSessionTokens.SmallGap),
    ) {
        if (move != null) {
            QueueButton(texts.moveUp, texts.requestAction(texts.moveUp, asker, index + 1), index > 0) {
                move(index, index - 1)
            }
            QueueButton(texts.moveDown, texts.requestAction(texts.moveDown, asker, index + 1), index < total - 1) {
                move(index, index + 1)
            }
        }
        if (withdraw != null) {
            QueueButton(texts.withdraw, texts.requestAction(texts.withdraw, asker, index + 1), true) {
                withdraw(request.id)
            }
        } else if (remove != null) {
            QueueButton(texts.remove, texts.requestAction(texts.remove, asker, index + 1), true) {
                remove(request.id)
            }
        }
    }
}

@Composable
private fun QueueButton(text: String, spoken: String, enabled: Boolean, onClick: () -> Unit) {
    MonarchButton(
        text = text,
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = spoken },
        variant = MonarchButtonVariant.Ghost,
        size = MonarchButtonSize.Sm,
        enabled = enabled,
    )
}
