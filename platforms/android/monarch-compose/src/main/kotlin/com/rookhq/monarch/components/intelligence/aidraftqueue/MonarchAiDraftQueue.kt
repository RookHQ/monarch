package com.rookhq.monarch.components.intelligence.aidraftqueue

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgress
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateKind
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.intelligence.aidraftqueue.MonarchAiDraftQueueTokens.Action
import com.rookhq.monarch.components.intelligence.aitypingdots.MonarchAiTypingDots
import com.rookhq.monarch.components.intelligence.aitypingdots.MonarchAiTypingDotsSize
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

private typealias Tokens = MonarchAiDraftQueueTokens

/**
 * A review queue for AI-written drafts (replies, messages, notes): each
 * draft with its context and Approve, Edit, Regenerate, Skip and Send.
 * Nothing is sent without a person approving it.
 *
 * Built by Monarch as a column of `MonarchCard`s, one per draft, with a
 * `MonarchBadge` for the status, a `MonarchProgress` bar for the review and
 * small `MonarchButton`s in a `FlowRow`. It is not lazy: place it in a
 * screen that scrolls. The queue is controlled: each button asks the app to
 * change a draft, and the queue shows the drafts it is given. While a
 * draft's status is `Regenerating` its body is replaced by "AI is still
 * writing…" and it offers no actions. Edit replaces the body with a
 * multi-line `MonarchTextField` and Save and Cancel. The body is plain
 * text, never markup.
 *
 * TalkBack reads the queue by its title, or "Drafts to review". Each draft's
 * title is a heading; its status is a badge with a name, so it does not
 * rest on colour; its body is read after "AI draft". After Approve, Send or
 * Skip, focus moves to the next draft that needs review (wrapping to the
 * top), and every status change is announced through a polite live region,
 * for example "Approved: Reply to Alex Morgan".
 *
 * @param drafts the drafts, in review order. Ids must be unique.
 * @param onApprove called with the id of a draft the person approved.
 * @param onEdit called with the id and the trimmed edited text when the person saves a changed edit.
 * @param onRegenerate asks for a new version; set the draft's status to `Regenerating` while it is written.
 * @param onSkip called with the id of a skipped draft.
 * @param modifier the modifier of the queue.
 * @param title a heading above the queue, already localised. It also names the queue.
 * @param onSend adds Send to approved drafts; called with the id.
 * @param advancesOnAction whether focus moves to the next draft that needs review after Approve, Send or Skip.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchAiDraftQueue(
    drafts: List<MonarchAiDraft>,
    onApprove: (id: String) -> Unit,
    onEdit: (id: String, body: String) -> Unit,
    onRegenerate: (id: String) -> Unit,
    onSkip: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onSend: ((id: String) -> Unit)? = null,
    advancesOnAction: Boolean = true,
    labels: MonarchAiDraftQueueLabels = MonarchAiDraftQueueLabels(),
) {
    val name = title ?: monarchString(labels.label, R.string.monarch_aidraftqueue_label)
    val headings = remember { mutableMapOf<String, FocusRequester>() }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var editText by rememberSaveable { mutableStateOf("") }
    val callbacks = DraftCallbacks(
        onAction = { action, draft ->
            when (action) {
                Action.Approve -> onApprove(draft.id)
                Action.Send -> onSend?.invoke(draft.id)
                Action.Skip -> onSkip(draft.id)
                Action.Regenerate -> onRegenerate(draft.id)
                Action.Edit -> {
                    editText = draft.body
                    editingId = draft.id
                }
            }
            if (advancesOnAction && action.advances) {
                Tokens.next(draft.id, drafts)?.let { headings[it]?.requestFocus() }
            }
        },
        onSave = { draft ->
            Tokens.edited(editText, draft.body)?.let { onEdit(draft.id, it) }
            editingId = null
        },
        onCancel = { editingId = null },
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                isTraversalGroup = true
                contentDescription = name
            },
        verticalArrangement = Arrangement.spacedBy(Tokens.StackGap),
    ) {
        Header(title, drafts, labels)
        Announcer(drafts, labels)
        if (drafts.isEmpty()) {
            MonarchEmptyState(
                title = monarchString(labels.empty, R.string.monarch_aidraftqueue_empty),
                kind = MonarchEmptyStateKind.Success,
                size = MonarchEmptyStateSize.Sm,
            )
        } else {
            drafts.forEach { draft ->
                key(draft.id) {
                    val heading = remember { FocusRequester() }
                    DisposableEffect(heading) {
                        headings[draft.id] = heading
                        onDispose { headings.remove(draft.id) }
                    }
                    DraftCard(
                        draft = draft,
                        heading = heading,
                        canSend = onSend != null,
                        editing = editingId == draft.id,
                        editText = editText,
                        onEditTextChange = { editText = it },
                        callbacks = callbacks,
                        labels = labels,
                    )
                }
            }
        }
    }
}

/** What a draft card can ask the queue to do. */
private class DraftCallbacks(
    val onAction: (Action, MonarchAiDraft) -> Unit,
    val onSave: (MonarchAiDraft) -> Unit,
    val onCancel: () -> Unit,
)

@Composable
private fun Header(title: String?, drafts: List<MonarchAiDraft>, labels: MonarchAiDraftQueueLabels) {
    if (title == null && drafts.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.TextGap)) {
        if (title != null) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MonarchTheme.typography.title,
                color = MonarchTheme.colors.textDefault,
            )
        }
        if (drafts.isNotEmpty()) {
            val reviewed = Tokens.reviewedCount(drafts)
            val progress = labels.progress?.invoke(reviewed, drafts.size)
                ?: pluralStringResource(R.plurals.monarch_aidraftqueue_progress, drafts.size, reviewed, drafts.size)
            Text(text = progress, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
            // Hidden: the line above says the same in words.
            MonarchProgress(
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
                progress = Tokens.fraction(drafts),
            )
        }
    }
}

/** A polite live region that is always present and speaks each status change. */
@Composable
private fun Announcer(drafts: List<MonarchAiDraft>, labels: MonarchAiDraftQueueLabels) {
    val statuses = Tokens.statuses(drafts)
    var previous by remember { mutableStateOf<Map<String, MonarchAiDraftStatus>?>(null) }
    var announcement by remember { mutableStateOf("") }
    val texts = Tokens.changed(previous.orEmpty(), drafts).map { announceText(it, labels) }
    LaunchedEffect(statuses) {
        if (previous != null && texts.isNotEmpty()) announcement = texts.joinToString(". ")
        previous = statuses
    }
    MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
}

@Composable
private fun announceText(draft: MonarchAiDraft, labels: MonarchAiDraftQueueLabels): String =
    labels.announce?.invoke(draft.status, draft.title) ?: if (draft.status == MonarchAiDraftStatus.Draft) {
        stringResource(R.string.monarch_aidraftqueue_announce_ready, draft.title)
    } else {
        stringResource(R.string.monarch_aidraftqueue_announce, statusText(draft.status, labels), draft.title)
    }

@Composable
private fun statusText(status: MonarchAiDraftStatus, labels: MonarchAiDraftQueueLabels): String =
    labels.status?.invoke(status) ?: stringResource(
        when (status) {
            MonarchAiDraftStatus.Draft -> R.string.monarch_aidraftqueue_status_draft
            MonarchAiDraftStatus.Regenerating -> R.string.monarch_aidraftqueue_status_regenerating
            MonarchAiDraftStatus.Approved -> R.string.monarch_aidraftqueue_status_approved
            MonarchAiDraftStatus.Sent -> R.string.monarch_aidraftqueue_status_sent
            MonarchAiDraftStatus.Skipped -> R.string.monarch_aidraftqueue_status_skipped
        },
    )

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DraftCard(
    draft: MonarchAiDraft,
    heading: FocusRequester,
    canSend: Boolean,
    editing: Boolean,
    editText: String,
    onEditTextChange: (String) -> Unit,
    callbacks: DraftCallbacks,
    labels: MonarchAiDraftQueueLabels,
) {
    val colors = MonarchTheme.colors
    MonarchCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Tokens.CardGap)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Gap),
                verticalArrangement = Arrangement.spacedBy(Tokens.TextGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = draft.title,
                    modifier = Modifier
                        .focusRequester(heading)
                        .focusable()
                        .semantics { heading() },
                    style = MonarchTheme.typography.heading,
                    color = colors.textDefault,
                )
                MonarchBadge(
                    text = statusText(draft.status, labels),
                    tone = Tokens.tone(draft.status),
                    leadingIcon = Tokens.statusIcon(draft.status),
                )
            }
            draft.context?.takeIf { it.isNotEmpty() }?.let { ContextBlock(it, labels) }
            when {
                draft.status == MonarchAiDraftStatus.Regenerating -> Writing(labels)
                editing -> Editor(draft, editText, onEditTextChange, callbacks, labels)
                else -> Body(draft, labels)
            }
            val actions = Tokens.actions(draft.status, canSend)
            if (actions.isNotEmpty() && !editing) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Gap),
                    verticalArrangement = Arrangement.spacedBy(Tokens.Gap),
                ) {
                    actions.forEach { action ->
                        MonarchButton(
                            text = actionText(action, labels),
                            onClick = { callbacks.onAction(action, draft) },
                            variant = action.variant,
                            size = MonarchButtonSize.Sm,
                            leadingIcon = action.icon,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun actionText(action: Action, labels: MonarchAiDraftQueueLabels): String = when (action) {
    Action.Approve -> monarchString(labels.approve, R.string.monarch_aidraftqueue_approve)
    Action.Send -> monarchString(labels.send, R.string.monarch_aidraftqueue_send)
    Action.Edit -> monarchString(labels.edit, R.string.monarch_aidraftqueue_edit)
    Action.Regenerate -> monarchString(labels.regenerate, R.string.monarch_aidraftqueue_regenerate)
    Action.Skip -> monarchString(labels.skip, R.string.monarch_aidraftqueue_skip)
}

@Composable
private fun ContextBlock(context: String, labels: MonarchAiDraftQueueLabels) {
    val colors = MonarchTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Tokens.contextFill(colors), MonarchTheme.shapes.md)
            .padding(Tokens.ContextPadding)
            .semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(Tokens.TextGap),
    ) {
        Text(
            text = monarchString(labels.context, R.string.monarch_aidraftqueue_context),
            style = MonarchTheme.typography.caption,
            color = colors.textMuted,
        )
        Text(text = context, style = MonarchTheme.typography.body, color = colors.textMuted)
    }
}

@Composable
private fun Writing(labels: MonarchAiDraftQueueLabels) {
    val text = monarchString(labels.writing, R.string.monarch_aidraftqueue_writing)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Tokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Hidden: the text next to it says the same.
        MonarchAiTypingDots(
            modifier = Modifier.clearAndSetSemantics { },
            size = MonarchAiTypingDotsSize.Sm,
            announce = false,
        )
        Text(text = text, style = MonarchTheme.typography.body, color = MonarchTheme.colors.textMuted)
    }
}

@Composable
private fun Body(draft: MonarchAiDraft, labels: MonarchAiDraftQueueLabels) {
    val prefix = monarchString(labels.aiDraft, R.string.monarch_aidraftqueue_ai_draft)
    val spoken = stringResource(R.string.monarch_aidraftqueue_spoken_body, prefix, draft.body)
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.TextGap)) {
        // "AI draft" is read with the body; the web shows no badge, the AI accent edge of the card says it.
        Text(
            text = draft.body,
            modifier = Modifier.semantics { contentDescription = spoken },
            style = MonarchTheme.typography.body,
            color = Tokens.bodyColor(draft.status, MonarchTheme.colors),
        )
    }
}

@Composable
private fun Editor(
    draft: MonarchAiDraft,
    text: String,
    onTextChange: (String) -> Unit,
    callbacks: DraftCallbacks,
    labels: MonarchAiDraftQueueLabels,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(draft.id) { focus.requestFocus() }
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Gap)) {
        MonarchTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            label = labels.editField?.invoke(draft.title)
                ?: stringResource(R.string.monarch_aidraftqueue_edit_field, draft.title),
            singleLine = false,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Gap)) {
            MonarchButton(
                text = monarchString(labels.save, R.string.monarch_aidraftqueue_save),
                onClick = { callbacks.onSave(draft) },
                variant = MonarchButtonVariant.Primary,
                size = MonarchButtonSize.Sm,
                enabled = text.isNotBlank(),
            )
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_aidraftqueue_cancel),
                onClick = callbacks.onCancel,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}
