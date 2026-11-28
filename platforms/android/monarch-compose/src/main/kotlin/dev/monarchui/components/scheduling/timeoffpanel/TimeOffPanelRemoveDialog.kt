package dev.monarchui.components.scheduling.timeoffpanel

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.overlays.dialog.MonarchDialog
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.launch

/** The danger confirmation of MonarchTimeOffPanel. It cannot be dismissed while removing. */
@Composable
internal fun TimeOffPanelRemoveDialog(
    summary: String,
    labels: MonarchTimeOffPanelLabels,
    onRemove: suspend () -> Unit,
    onDismiss: () -> Unit,
) {
    var running by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val remove by rememberUpdatedState(onRemove)
    val failedText = monarchString(labels.failed, R.string.monarch_timeoffpanel_failed)
    val description = monarchString(labels.removeDescription, R.string.monarch_timeoffpanel_remove_description)
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = monarchString(labels.removeTitle, R.string.monarch_timeoffpanel_remove_title),
        description = if (summary.isEmpty()) description else "$summary. $description",
        dismissible = !running,
        actions = {
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_timeoffpanel_cancel),
                onClick = onDismiss,
                variant = MonarchButtonVariant.Ghost,
                enabled = !running,
            )
            MonarchButton(
                text = monarchString(labels.removeConfirm, R.string.monarch_timeoffpanel_remove_confirm),
                onClick = {
                    running = true
                    failure = null
                    scope.launch {
                        val problem = timeOffPanelAttempt(failedText) { remove() }
                        running = false
                        failure = problem
                        if (problem == null) onDismiss()
                    }
                },
                variant = MonarchButtonVariant.Danger,
                loading = running,
            )
        },
    ) {
        failure?.let { message ->
            Text(
                text = message,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                color = MonarchTimeOffPanelTokens.error(MonarchTheme.colors),
            )
        }
    }
}
