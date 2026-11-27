package com.rookhq.monarch.components.workflow.approvalqueue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.forms.checkbox.MonarchCheckbox
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.monarchGhostStartAlign
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.launch

/** One request: swipe actions over the checkbox, avatar, text and buttons. */
@Suppress("LongParameterList")
@Composable
internal fun MonarchApprovalQueueRow(
    item: MonarchApprovalQueueItem,
    now: Long,
    busy: Boolean,
    failed: Boolean,
    selected: Boolean,
    actions: MonarchApprovalQueueRowActions,
    text: MonarchApprovalQueueText,
) {
    val approve = actions.onApprove?.takeIf { item.canDecide && !busy }
    val review = actions.onReview
    val content = @Composable {
        RowContent(item, now, busy, failed, selected, actions, text)
    }
    if (approve == null && review == null) {
        content()
        return
    }
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            when (state.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> SwipeBackground(
                    label = text.approve,
                    colors = MonarchApprovalQueueTokens.swipeColors(approve = true, MonarchTheme.colors),
                    alignment = Alignment.CenterEnd,
                )
                SwipeToDismissBoxValue.StartToEnd -> SwipeBackground(
                    label = text.review,
                    colors = MonarchApprovalQueueTokens.swipeColors(approve = false, MonarchTheme.colors),
                    alignment = Alignment.CenterStart,
                )
                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
        enableDismissFromStartToEnd = review != null,
        enableDismissFromEndToStart = approve != null,
        onDismiss = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> approve?.invoke(item)
                SwipeToDismissBoxValue.StartToEnd -> review?.invoke(item)
                SwipeToDismissBoxValue.Settled -> Unit
            }
            scope.launch { state.reset() }
        },
    ) {
        content()
    }
}

@Composable
private fun SwipeBackground(label: String, colors: MonarchSurfaceColors, alignment: Alignment) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.container)
            .padding(horizontal = MonarchApprovalQueueTokens.SwipePadding)
            // The row's buttons offer the same action, so the swipe background is not read.
            .clearAndSetSemantics { },
        contentAlignment = alignment,
    ) {
        Text(text = label, style = MonarchTheme.typography.label, color = colors.content)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongParameterList")
@Composable
private fun RowContent(
    item: MonarchApprovalQueueItem,
    now: Long,
    busy: Boolean,
    failed: Boolean,
    selected: Boolean,
    actions: MonarchApprovalQueueRowActions,
    text: MonarchApprovalQueueText,
) {
    val tokens = MonarchApprovalQueueTokens
    val name = item.requesterName
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = tokens.RowPadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.ColumnGap),
        verticalAlignment = Alignment.Top,
    ) {
        // The box is drawn at the row's edge, flush with "Select all" above it. The 48dp target is the box and
        // the avatar together (one toggle, as in a mail list), so no inset is needed around the 16dp box.
        val change = actions.onSelectedChange
        val selectLabel = text.selectItem(name)
        Row(
            modifier = (
                if (change == null) {
                    Modifier
                } else {
                    Modifier
                        .heightIn(min = MonarchTouchTarget.Minimum)
                        .toggleable(
                            value = selected && item.canDecide,
                            enabled = item.canDecide,
                            role = Role.Checkbox,
                            onValueChange = { change(item, it) },
                        )
                        .semantics { contentDescription = selectLabel }
                }
                ),
            horizontalArrangement = Arrangement.spacedBy(tokens.ColumnGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (change != null) {
                MonarchCheckbox(
                    checked = selected && item.canDecide,
                    onCheckedChange = null,
                    enabled = item.canDecide,
                )
            }
            MonarchAvatar(
                image = item.requesterImage,
                name = name,
                size = MonarchAvatarSize.Sm,
                seed = item.requesterSeed,
                decorative = true,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.ActionGap)) {
            RequestText(item, now, failed, text)
            val approve = actions.onApprove?.takeIf { item.canDecide }
            val reject = actions.onReject?.takeIf { item.canDecide }
            val review = actions.onReview
            if (approve != null || reject != null || review != null) {
                FlowRow(
                    modifier = if (review != null) Modifier.monarchGhostStartAlign() else Modifier,
                    horizontalArrangement = Arrangement.spacedBy(tokens.ActionGap),
                    verticalArrangement = Arrangement.spacedBy(tokens.ActionGap),
                ) {
                    review?.let {
                        RowButton(
                            text.review,
                            text.reviewItem(name),
                            MonarchButtonVariant.Ghost,
                            icon = MonarchDefaultIcons.Eye,
                        ) { it(item) }
                    }
                    reject?.let {
                        RowButton(
                            text.reject,
                            text.rejectItem(name),
                            MonarchButtonVariant.Secondary,
                            icon = MonarchDefaultIcons.Close,
                            enabled = !busy,
                        ) { it(item) }
                    }
                    approve?.let {
                        RowButton(
                            text.approve,
                            text.approveItem(name),
                            MonarchButtonVariant.Success,
                            icon = MonarchDefaultIcons.Check,
                            loading = busy,
                        ) { it(item) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowButton(
    label: String,
    spoken: String,
    variant: MonarchButtonVariant,
    icon: @Composable () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    MonarchButton(
        text = label,
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = spoken },
        variant = variant,
        size = MonarchButtonSize.Sm,
        enabled = enabled,
        loading = loading,
        leadingIcon = icon,
    )
}

/** The requester, title, time, badges, summary and failure of a request, read as one element. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RequestText(item: MonarchApprovalQueueItem, now: Long, failed: Boolean, text: MonarchApprovalQueueText) {
    val tokens = MonarchApprovalQueueTokens
    val colors = MonarchTheme.colors
    val time = item.submittedMillis?.let { tokens.relativeTime(it, now) }
    val detail = item.requesterDetail?.takeIf { it.isNotBlank() }
    val summary = item.summary?.takeIf { it.isNotBlank() }
    val spoken = tokens.spoken(
        listOf(item.requesterName, detail, item.title, time, text.overdue.takeIf { item.overdue }) +
            item.badges.map { it.label } +
            listOf(summary, text.failed.takeIf { failed }),
    )
    Column(
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
        verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
    ) {
        Text(text = item.requesterName, style = MonarchTheme.typography.bodyStrong, color = colors.textDefault)
        detail?.let { Text(text = it, style = MonarchTheme.typography.caption, color = colors.textMuted) }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.LineGap),
            verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = item.title, style = MonarchTheme.typography.body, color = colors.textDefault)
            if (time != null) {
                Text(text = tokens.META_DOT, style = MonarchTheme.typography.caption, color = colors.textMuted)
                Text(text = time, style = MonarchTheme.typography.caption, color = colors.textMuted)
            }
        }
        // As the web: the chips have a row of their own under the title and the time.
        if (item.overdue || item.badges.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.LineGap),
                verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.overdue) MonarchBadge(text = text.overdue, tone = MonarchTone.Danger)
                item.badges.forEach { badge ->
                    MonarchBadge(text = badge.label, tone = badge.tone, leadingIcon = badge.icon)
                }
            }
        }
        summary?.let { Text(text = it, style = MonarchTheme.typography.caption, color = colors.textMuted) }
        if (failed) MonarchApprovalQueueFailure(text.failed)
    }
}

/** The failure line: text in the strong danger colour, so it does not rest on colour alone. */
@Composable
internal fun MonarchApprovalQueueFailure(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        modifier = modifier,
        style = MonarchTheme.typography.caption,
        color = MonarchTheme.colors.dangerStrong,
    )
}
