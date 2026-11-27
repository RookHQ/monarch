package com.rookhq.monarch.components.forms.formactionsbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The Save and Discard bar of a form, for the `bottomBar` of a `Scaffold`,
 * so the actions stay in reach on long forms.
 *
 * Save and Discard respond only while there are unsaved changes ([dirty]).
 * While [pending] Save shows progress and Discard is off; an [error] is kept
 * in the bar. Discard asks for confirmation in a Material 3 `AlertDialog`
 * first. The state is the app's: it sets [pending] while it saves and
 * [error] when the save fails.
 *
 * Built by Monarch: Material 3 has no form action bar, and its `BottomAppBar`
 * has a fixed height that would cut text at large font sizes. The message
 * line is a polite live region, so TalkBack announces "You have unsaved
 * changes" and errors when they appear.
 *
 * @param dirty whether the form has unsaved changes.
 * @param onSave saves the form. Not called while [pending] or when nothing changed.
 * @param modifier the modifier of the bar.
 * @param onDiscard reverts the form. When given, a Discard button is shown.
 * @param pending whether a save is running: Save shows progress and Discard is off.
 * @param confirmDiscard asks before discarding, with a danger confirmation.
 * @param hideWhenClean shows nothing while there are no changes, no error and nothing pending.
 * @param error the message of a failed save, already localised. Shown in the danger colour in place of
 * the status.
 * @param status replaces the "You have unsaved changes" text, for example with the time of the last save.
 * @param labels replacements for the built-in strings.
 * @param windowInsets the insets the bar keeps clear, the navigation bar by default.
 * @param actions more actions placed before Discard, for example a ghost `MonarchButton` "Preview".
 */
@Composable
public fun MonarchFormActionsBar(
    dirty: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    onDiscard: (() -> Unit)? = null,
    pending: Boolean = false,
    confirmDiscard: Boolean = true,
    hideWhenClean: Boolean = false,
    error: String? = null,
    status: (@Composable () -> Unit)? = null,
    labels: MonarchFormActionsBarLabels = MonarchFormActionsBarLabels(),
    windowInsets: WindowInsets = BottomAppBarDefaults.windowInsets,
    actions: (@Composable () -> Unit)? = null,
) {
    val availability = MonarchFormActionsBarTokens.availability(dirty, pending, error != null, hideWhenClean)
    if (availability.hidden) return
    val colors = MonarchFormActionsBarTokens.colors(MonarchTheme.colors)
    var confirming by rememberSaveable { mutableStateOf(false) }
    val discard: () -> Unit = {
        if (confirmDiscard) {
            confirming = true
        } else {
            onDiscard?.invoke()
        }
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true }
            .drawWithContent {
                drawContent()
                val line = MonarchShapeDefaults.BorderWidth.toPx()
                drawLine(colors.divider, Offset(0f, line / 2), Offset(size.width, line / 2), line)
            },
        color = colors.container,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(windowInsets)
                .padding(
                    horizontal = MonarchFormActionsBarTokens.HorizontalPadding,
                    vertical = MonarchFormActionsBarTokens.VerticalPadding,
                ),
        ) {
            val unsaved = monarchString(labels.unsaved, R.string.monarch_formactionsbar_unsaved)
            val hasMessage = error != null || status != null || dirty
            // Always present, so TalkBack announces a message when it appears.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
                    .then(if (hasMessage) Modifier.padding(bottom = MonarchFormActionsBarTokens.Gap) else Modifier),
            ) {
                when {
                    error != null -> Text(
                        text = error,
                        modifier = Modifier.semantics { error(error) },
                        style = MonarchTheme.typography.body,
                        color = colors.error,
                    )
                    status != null -> status()
                    dirty -> Text(text = unsaved, style = MonarchTheme.typography.body, color = colors.status)
                }
            }
            // A row, not a FlowRow: at large font sizes Save takes the width that is left and its label wraps.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MonarchFormActionsBarTokens.Gap, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions?.invoke()
                if (onDiscard != null) {
                    MonarchButton(
                        text = monarchString(labels.discard, R.string.monarch_formactionsbar_discard),
                        onClick = discard,
                        variant = MonarchButtonVariant.Ghost,
                        enabled = availability.discardEnabled,
                    )
                }
                MonarchButton(
                    text = monarchString(labels.save, R.string.monarch_formactionsbar_save),
                    onClick = { if (dirty && !pending) onSave() },
                    modifier = Modifier.weight(1f, fill = false),
                    variant = MonarchButtonVariant.Primary,
                    enabled = availability.saveEnabled,
                    loading = pending,
                    loadingLabel = monarchString(labels.saving, R.string.monarch_formactionsbar_saving),
                )
            }
        }
    }
    if (confirming && onDiscard != null) {
        DiscardConfirmation(
            labels = labels,
            onConfirm = {
                confirming = false
                onDiscard()
            },
            onDismiss = { confirming = false },
        )
    }
}

@Composable
private fun DiscardConfirmation(labels: MonarchFormActionsBarLabels, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = monarchString(labels.discardTitle, R.string.monarch_formactionsbar_discard_title),
        description = monarchString(labels.discardDescription, R.string.monarch_formactionsbar_discard_description),
        actions = {
            MonarchButton(
                text = monarchString(labels.keepEditing, R.string.monarch_formactionsbar_keep_editing),
                onClick = onDismiss,
                variant = MonarchButtonVariant.Ghost,
            )
            MonarchButton(
                text = monarchString(labels.discardConfirm, R.string.monarch_formactionsbar_discard_confirm),
                onClick = onConfirm,
                variant = MonarchButtonVariant.Danger,
            )
        },
    )
}
