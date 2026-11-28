package com.rookhq.monarch.components.intelligence.aiundobanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.collapsible.MonarchCollapsible
import com.rookhq.monarch.components.navigation.link.MonarchLink
import com.rookhq.monarch.components.navigation.link.MonarchLinkVariant
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/** The changes: listed when there are up to three, inside a disclosure when there are more. */
@Composable
internal fun MonarchAiUndoBannerChanges(
    changes: List<MonarchAiChange>,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    labels: MonarchAiUndoBannerLabels,
) {
    if (MonarchAiUndoBannerTokens.collapsesChanges(changes.size)) {
        val title = labels.showChanges?.invoke(changes.size)
            ?: pluralStringResource(R.plurals.monarch_aiundobanner_show_changes, changes.size, changes.size)
        MonarchCollapsible(expanded = open, onExpandedChange = onOpenChange, label = title) {
            MonarchAiUndoBannerChangeList(changes, labels)
        }
    } else {
        MonarchAiUndoBannerChangeList(changes, labels)
    }
}

@Composable
private fun MonarchAiUndoBannerChangeList(changes: List<MonarchAiChange>, labels: MonarchAiUndoBannerLabels) {
    Column(verticalArrangement = Arrangement.spacedBy(MonarchAiUndoBannerTokens.LineGap)) {
        changes.forEach { MonarchAiUndoBannerChange(it, labels) }
    }
}

/** One change as one TalkBack element: "Status changed from Open to Closed". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonarchAiUndoBannerChange(change: MonarchAiChange, labels: MonarchAiUndoBannerLabels) {
    val colors = MonarchTheme.colors
    val style = MonarchTheme.typography.caption
    val spoken = labels.change?.invoke(change.label, change.before, change.after)
        ?: stringResource(R.string.monarch_aiundobanner_change, change.label, change.before, change.after)
    FlowRow(
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(MonarchAiUndoBannerTokens.LineGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = change.label, style = style, color = colors.textMuted)
        Text(
            text = change.before,
            style = style.copy(textDecoration = TextDecoration.LineThrough),
            color = colors.textMuted,
        )
        Text(
            text = MonarchAiUndoBannerTokens.arrow(LocalLayoutDirection.current),
            style = style,
            color = colors.textMuted,
        )
        Text(
            text = change.after,
            style = MonarchTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textDefault,
        )
    }
}

/** Undo, the audit link and Dismiss, each a separate element. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MonarchAiUndoBannerActions(
    state: MonarchAiUndoBannerState,
    onUndo: () -> Unit,
    onAudit: (() -> Unit)?,
    onDismiss: (() -> Unit)?,
    labels: MonarchAiUndoBannerLabels,
) {
    val showsUndo = MonarchAiUndoBannerTokens.showsUndo(state)
    if (!showsUndo && onAudit == null && onDismiss == null) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MonarchAiUndoBannerTokens.ActionGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (showsUndo) {
            MonarchButton(
                text = monarchString(labels.undo, R.string.monarch_aiundobanner_undo),
                onClick = onUndo,
                variant = MonarchButtonVariant.Secondary,
                size = MonarchButtonSize.Sm,
                loading = state == MonarchAiUndoBannerState.Undoing,
                leadingIcon = MonarchDefaultIcons.Retry,
            )
        }
        if (onAudit != null) {
            MonarchLink(
                text = monarchString(labels.audit, R.string.monarch_aiundobanner_audit),
                onClick = onAudit,
                variant = MonarchLinkVariant.Standalone,
            )
        }
        if (onDismiss != null) {
            MonarchButton(
                text = monarchString(labels.dismiss, R.string.monarch_aiundobanner_dismiss),
                onClick = onDismiss,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}
