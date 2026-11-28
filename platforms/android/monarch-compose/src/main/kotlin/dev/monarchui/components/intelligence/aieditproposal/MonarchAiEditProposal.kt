package dev.monarchui.components.intelligence.aieditproposal

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.intelligence.aiconversation.MonarchAiEditProposalPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiEditProposalState
import dev.monarchui.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadge
import dev.monarchui.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadgeSize
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme

/**
 * A reviewable diff of edits an AI proposes to a document or record. Each
 * change shows a word diff and can be accepted or rejected on its own, or all
 * at once; once applied the edits can be reverted. Nothing changes until a
 * person decides, so AI edits are never applied silently.
 *
 * Built by Monarch from `MonarchButton`, `MonarchBadge`,
 * `MonarchAiGeneratedBadge` and the word diff of `MonarchDiffViewer`; Material
 * 3 has no equivalent. Removed words are struck through and added words
 * underlined, and every decision is also a word, so nothing depends on colour.
 * Decisions are remembered per proposal [MonarchAiEditProposalPart.id] (and
 * survive configuration changes) unless the app passes each change's status.
 *
 * The card is a traversal group whose title is a heading. Each change is one
 * TalkBack element read as its name, its decision, then the diff with
 * "Deleted:" and "Inserted:" before removed and added words; its buttons are
 * named with the change, "Accept, Change 2 of 3". A decision is announced
 * through a polite live region ("Change accepted. 2 left.") and, with a
 * keyboard, input focus moves to the next undecided change's Accept button.
 *
 * @param part the edit-proposal part of the message. Its state decides whether the edits can change; its id
 * scopes remembered decisions.
 * @param hunks the changes in document order.
 * @param modifier the modifier of the card.
 * @param title replaces "Suggested edits". Already localized.
 * @param onAcceptAll accepts every undecided change; shows Accept all while proposed.
 * @param onRejectAll rejects every undecided change; shows Reject all while proposed.
 * @param onAcceptHunk accepts one change by id; shows Accept on each undecided change.
 * @param onRejectHunk rejects one change by id; shows Reject on each undecided change.
 * @param onRevert undoes applied edits; shows Revert once the part is applied.
 * @param statusIcon an icon before a change's decision, for example a check mark for accepted, as a
 * `MonarchIcon` of size `Sm` with a `null` content description, tinted like the decision. Monarch bundles no
 * icons; the decision is always written.
 * @param labels replacements for the built-in strings.
 */
@OptIn(ExperimentalLayoutApi::class)
@ExperimentalMonarchApi
@Composable
public fun MonarchAiEditProposal(
    part: MonarchAiEditProposalPart,
    hunks: List<MonarchAiEditHunk>,
    modifier: Modifier = Modifier,
    title: String? = null,
    onAcceptAll: (() -> Unit)? = null,
    onRejectAll: (() -> Unit)? = null,
    onAcceptHunk: ((id: String) -> Unit)? = null,
    onRejectHunk: ((id: String) -> Unit)? = null,
    onRevert: (() -> Unit)? = null,
    statusIcon: (@Composable (MonarchAiEditHunkStatus) -> Unit)? = null,
    labels: MonarchAiEditProposalLabels = MonarchAiEditProposalLabels(),
) {
    val resources = LocalResources.current
    val editable = part.state == MonarchAiEditProposalState.Proposed
    var decided by rememberSaveable(part.id) { mutableStateOf<Map<String, MonarchAiEditHunkStatus>>(emptyMap()) }
    var announcement by remember(part.id) { mutableStateOf("") }
    val statuses = hunks.map { MonarchAiEditProposalLogic.statusOf(it, decided, part.state) }
    val pending = statuses.count { it == MonarchAiEditHunkStatus.Proposed }
    val acceptRequesters = remember(hunks.size) { List(hunks.size) { FocusRequester() } }
    var focusNext by remember { mutableStateOf<Int?>(null) }
    val callbacks = ProposalCallbacks(onAcceptAll, onRejectAll, onAcceptHunk, onRejectHunk, onRevert)
    // index -1 decides every undecided change.
    val decide: (Int, MonarchAiEditHunkStatus) -> Unit = { index, status ->
        val hunk = hunks.getOrNull(index)
        decided = if (hunk == null) {
            MonarchAiEditProposalLogic.decideAll(hunks, statuses, decided, status)
        } else {
            decided + (hunk.id to status)
        }
        val left = if (hunk == null) 0 else (pending - 1).coerceAtLeast(0)
        announcement = announce(resources, labels, eventOf(status, all = hunk == null), left)
        if (hunk != null) focusNext = MonarchAiEditProposalLogic.nextUndecided(statuses, index)
        callbacks.notify(hunk?.id, status)
    }
    Column(
        modifier = modifier.proposalCard(MonarchTheme.colors, MonarchTheme.shapes.lg),
        verticalArrangement = Arrangement.spacedBy(MonarchAiEditProposalTokens.Gap),
    ) {
        Header(part.state, title, editable, hunks.size, pending, labels)
        hunks.forEachIndexed { index, hunk ->
            val status = statuses[index]
            HunkView(
                hunk = hunk,
                name = labels.hunkName?.invoke(index + 1, hunks.size)
                    ?: stringResource(R.string.monarch_aieditproposal_hunk_name, index + 1, hunks.size),
                status = status,
                statusIcon = statusIcon,
                labels = labels,
                actions = editable && status == MonarchAiEditHunkStatus.Proposed,
                onAccept = onAcceptHunk?.let { { decide(index, MonarchAiEditHunkStatus.Accepted) } },
                onReject = onRejectHunk?.let { { decide(index, MonarchAiEditHunkStatus.Rejected) } },
                acceptFocus = acceptRequesters.getOrNull(index),
            )
        }
        ProposalActions(part.state, pending, callbacks, labels) { status -> decide(-1, status) }
        // Always present, so each decision is announced once as a change of this live region.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
    LaunchedEffect(focusNext, statuses) {
        val next = focusNext ?: return@LaunchedEffect
        focusNext = null
        acceptRequesters.getOrNull(next)?.let { runCatching { it.requestFocus() } }
    }
}

/** The app's callbacks, called after a decision. */
private class ProposalCallbacks(
    val onAcceptAll: (() -> Unit)?,
    val onRejectAll: (() -> Unit)?,
    val onAcceptHunk: ((String) -> Unit)?,
    val onRejectHunk: ((String) -> Unit)?,
    val onRevert: (() -> Unit)?,
) {
    /** Calls the callback of a decision on [id], or on every undecided change when [id] is `null`. */
    fun notify(id: String?, status: MonarchAiEditHunkStatus) {
        val accepted = status == MonarchAiEditHunkStatus.Accepted
        when {
            id == null && accepted -> onAcceptAll?.invoke()
            id == null -> onRejectAll?.invoke()
            accepted -> onAcceptHunk?.invoke(id)
            else -> onRejectHunk?.invoke(id)
        }
    }
}

/** The card: the surface fill, a start accent in the AI border colour, the default border. */
private fun Modifier.proposalCard(colors: MonarchColors, shape: Shape): Modifier {
    val tokens = MonarchAiEditProposalTokens
    val accent = tokens.accent(colors)
    return fillMaxWidth()
        .clip(shape)
        .background(tokens.cardContainer(colors))
        .drawBehind {
            val width = tokens.AccentWidth.toPx()
            val x = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
            drawRect(accent, Offset(x, 0f), Size(width, size.height))
        }
        .border(tokens.HairlineWidth, tokens.cardBorder(colors), shape)
        .padding(tokens.Padding)
        .semantics { isTraversalGroup = true }
}

/** Accept all and Reject all while changes are undecided, Revert once applied. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProposalActions(
    state: MonarchAiEditProposalState,
    pending: Int,
    callbacks: ProposalCallbacks,
    labels: MonarchAiEditProposalLabels,
    onDecideAll: (MonarchAiEditHunkStatus) -> Unit,
) {
    val showAll = state == MonarchAiEditProposalState.Proposed && pending > 0
    val acceptAll = callbacks.onAcceptAll.takeIf { showAll }
    val rejectAll = callbacks.onRejectAll.takeIf { showAll }
    val revert = callbacks.onRevert.takeIf { state == MonarchAiEditProposalState.Applied }
    if (acceptAll == null && rejectAll == null && revert == null) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MonarchAiEditProposalTokens.ActionGap),
        verticalArrangement = Arrangement.spacedBy(MonarchAiEditProposalTokens.ActionGap),
    ) {
        if (acceptAll != null) {
            MonarchButton(
                text = monarchString(labels.acceptAll, R.string.monarch_aieditproposal_accept_all),
                onClick = { onDecideAll(MonarchAiEditHunkStatus.Accepted) },
                variant = MonarchButtonVariant.Primary,
                size = MonarchButtonSize.Sm,
                leadingIcon = MonarchDefaultIcons.Check,
            )
        }
        if (rejectAll != null) {
            MonarchButton(
                text = monarchString(labels.rejectAll, R.string.monarch_aieditproposal_reject_all),
                onClick = { onDecideAll(MonarchAiEditHunkStatus.Rejected) },
                size = MonarchButtonSize.Sm,
            )
        }
        if (revert != null) {
            MonarchButton(
                text = monarchString(labels.revert, R.string.monarch_aieditproposal_revert),
                onClick = revert,
                size = MonarchButtonSize.Sm,
                leadingIcon = MonarchDefaultIcons.Retry,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(
    state: MonarchAiEditProposalState,
    title: String?,
    editable: Boolean,
    total: Int,
    pending: Int,
    labels: MonarchAiEditProposalLabels,
) {
    val tokens = MonarchAiEditProposalTokens
    val colors = MonarchTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(tokens.HeaderGap)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.ActionGap),
            verticalArrangement = Arrangement.spacedBy(tokens.HeaderGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title ?: stringResource(R.string.monarch_aieditproposal_title),
                modifier = Modifier.semantics { heading() },
                style = MonarchTheme.typography.bodyStrong,
                color = tokens.text(colors),
            )
            MonarchAiGeneratedBadge(
                size = MonarchAiGeneratedBadgeSize.Sm,
                label = monarchString(labels.badge, R.string.monarch_aieditproposal_badge),
            )
            MonarchBadge(text = stateName(state, labels), tone = MonarchAiEditProposalLogic.stateTone(state))
        }
        Text(
            text = if (editable) countText(total, pending, labels) else readOnlyText(state, labels),
            style = MonarchTheme.typography.caption,
            color = tokens.muted(colors),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HunkView(
    hunk: MonarchAiEditHunk,
    name: String,
    status: MonarchAiEditHunkStatus,
    statusIcon: (@Composable (MonarchAiEditHunkStatus) -> Unit)?,
    labels: MonarchAiEditProposalLabels,
    actions: Boolean,
    onAccept: (() -> Unit)?,
    onReject: (() -> Unit)?,
    acceptFocus: FocusRequester?,
) {
    val tokens = MonarchAiEditProposalTokens
    val colors = MonarchTheme.colors
    val label = hunk.label ?: name
    val segments = remember(hunk.before, hunk.after) { MonarchAiEditProposalLogic.diff(hunk) }
    val changed = MonarchAiEditProposalLogic.changed(segments)
    val unchanged = monarchString(labels.unchanged, R.string.monarch_aieditproposal_unchanged)
    val decision = if (status == MonarchAiEditHunkStatus.Proposed) null else hunkStateName(status, labels)
    val spokenDiff = if (changed) {
        MonarchAiEditProposalLogic.spoken(
            segments,
            monarchString(labels.deleted, R.string.monarch_aieditproposal_deleted),
            monarchString(labels.inserted, R.string.monarch_aieditproposal_inserted),
        )
    } else {
        "${hunk.after} ($unchanged)"
    }
    val spoken = listOfNotNull(label, decision, spokenDiff).joinToString(", ")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.hunkContainer(colors), MonarchTheme.shapes.md)
            .border(tokens.HairlineWidth, tokens.hunkBorder(colors), MonarchTheme.shapes.md)
            .padding(tokens.HunkPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.HunkGap),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
            verticalArrangement = Arrangement.spacedBy(tokens.HeaderGap),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.ActionGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MonarchTheme.typography.caption,
                    color = tokens.muted(colors),
                )
                if (decision != null) {
                    val color = tokens.statusColor(status, colors)
                    if (statusIcon != null) {
                        CompositionLocalProvider(LocalContentColor provides color) { statusIcon(status) }
                    }
                    Text(text = decision, style = MonarchTheme.typography.caption, color = color)
                }
            }
            if (changed) {
                Text(
                    text = MonarchAiEditProposalLogic.styled(segments, status, colors),
                    style = MonarchTheme.typography.body,
                    color = tokens.text(colors),
                )
            } else {
                Text(
                    text = "${hunk.after} ($unchanged)",
                    style = MonarchTheme.typography.body,
                    color = tokens.muted(colors),
                )
            }
        }
        if (actions && (onAccept != null || onReject != null)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.ActionGap),
                verticalArrangement = Arrangement.spacedBy(tokens.ActionGap),
            ) {
                if (onAccept != null) {
                    val text = monarchString(labels.accept, R.string.monarch_aieditproposal_accept)
                    val named = stringResource(R.string.monarch_aieditproposal_action, text, label)
                    MonarchButton(
                        text = text,
                        onClick = onAccept,
                        modifier = Modifier
                            .then(if (acceptFocus != null) Modifier.focusRequester(acceptFocus) else Modifier)
                            .semantics { contentDescription = named },
                        size = MonarchButtonSize.Sm,
                        leadingIcon = MonarchDefaultIcons.Check,
                    )
                }
                if (onReject != null) {
                    val text = monarchString(labels.reject, R.string.monarch_aieditproposal_reject)
                    val named = stringResource(R.string.monarch_aieditproposal_action, text, label)
                    MonarchButton(
                        text = text,
                        onClick = onReject,
                        modifier = Modifier.semantics { contentDescription = named },
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                        leadingIcon = MonarchDefaultIcons.Close,
                    )
                }
            }
        }
    }
}

private fun eventOf(status: MonarchAiEditHunkStatus, all: Boolean): MonarchAiEditProposalEvent = when {
    status == MonarchAiEditHunkStatus.Accepted && all -> MonarchAiEditProposalEvent.AcceptedAll
    status == MonarchAiEditHunkStatus.Accepted -> MonarchAiEditProposalEvent.Accepted
    all -> MonarchAiEditProposalEvent.RejectedAll
    else -> MonarchAiEditProposalEvent.Rejected
}

private fun announce(
    resources: Resources,
    labels: MonarchAiEditProposalLabels,
    event: MonarchAiEditProposalEvent,
    left: Int,
): String = labels.announcement?.invoke(event, left) ?: when (event) {
    MonarchAiEditProposalEvent.AcceptedAll -> resources.getString(R.string.monarch_aieditproposal_announce_accepted_all)
    MonarchAiEditProposalEvent.RejectedAll -> resources.getString(R.string.monarch_aieditproposal_announce_rejected_all)
    MonarchAiEditProposalEvent.Accepted -> if (left > 0) {
        resources.getQuantityString(R.plurals.monarch_aieditproposal_announce_accepted, left, left)
    } else {
        resources.getString(R.string.monarch_aieditproposal_announce_accepted_last)
    }
    MonarchAiEditProposalEvent.Rejected -> if (left > 0) {
        resources.getQuantityString(R.plurals.monarch_aieditproposal_announce_rejected, left, left)
    } else {
        resources.getString(R.string.monarch_aieditproposal_announce_rejected_last)
    }
}

@Composable
private fun stateName(state: MonarchAiEditProposalState, labels: MonarchAiEditProposalLabels): String =
    labels.state?.invoke(state) ?: stringResource(
        when (state) {
            MonarchAiEditProposalState.Proposed -> R.string.monarch_aieditproposal_state_proposed
            MonarchAiEditProposalState.Applied -> R.string.monarch_aieditproposal_state_applied
            MonarchAiEditProposalState.Rejected -> R.string.monarch_aieditproposal_state_rejected
            MonarchAiEditProposalState.Reverted -> R.string.monarch_aieditproposal_state_reverted
        },
    )

@Composable
private fun hunkStateName(status: MonarchAiEditHunkStatus, labels: MonarchAiEditProposalLabels): String =
    labels.hunkState?.invoke(status) ?: stringResource(
        if (status == MonarchAiEditHunkStatus.Accepted) {
            R.string.monarch_aieditproposal_hunk_accepted
        } else {
            R.string.monarch_aieditproposal_hunk_rejected
        },
    )

@Composable
private fun countText(total: Int, pending: Int, labels: MonarchAiEditProposalLabels): String =
    labels.count?.invoke(total, pending) ?: if (pending == total) {
        pluralStringResource(R.plurals.monarch_aieditproposal_count, total, total)
    } else {
        pluralStringResource(R.plurals.monarch_aieditproposal_count_left, total, pending, total)
    }

@Composable
private fun readOnlyText(state: MonarchAiEditProposalState, labels: MonarchAiEditProposalLabels): String =
    labels.readOnly?.invoke(state) ?: stringResource(
        when (state) {
            MonarchAiEditProposalState.Applied -> R.string.monarch_aieditproposal_read_only_applied
            MonarchAiEditProposalState.Rejected -> R.string.monarch_aieditproposal_read_only_rejected
            else -> R.string.monarch_aieditproposal_read_only_reverted
        },
    )
