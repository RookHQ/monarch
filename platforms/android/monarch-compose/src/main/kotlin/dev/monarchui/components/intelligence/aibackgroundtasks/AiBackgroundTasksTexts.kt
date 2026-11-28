package dev.monarchui.components.intelligence.aibackgroundtasks

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import dev.monarchui.R
import dev.monarchui.internal.monarchString

/** The built-in strings of MonarchAiBackgroundTasks, or their replacements, as plain functions. */
internal class AiBackgroundTasksTexts(
    val title: String,
    val runningCount: (Int) -> String,
    val queuedCount: (Int) -> String,
    val needsInputCount: (Int) -> String,
    val groups: Triple<String, String, String>,
    val groupHeading: (String, Int) -> String,
    val task: AiBackgroundTasksTaskTexts,
    val actions: AiBackgroundTasksActionTexts,
    val empty: String,
)

/** The texts of one task: its status word, elapsed and end times and progress name. */
internal class AiBackgroundTasksTaskTexts(
    val status: (MonarchAiBackgroundTaskStatus) -> String,
    val elapsed: (String) -> String,
    val ended: (String) -> String,
    val progressLabel: (String) -> String,
    val announce: (String, String) -> String,
)

/** The button texts of MonarchAiBackgroundTasks. */
internal class AiBackgroundTasksActionTexts(
    val open: String,
    val cancel: String,
    val retry: String,
    val respond: String,
    val dismiss: String,
    val clearFinished: String,
    val label: (String, String) -> String,
)

@Composable
internal fun aiBackgroundTasksTexts(labels: MonarchAiBackgroundTasksLabels): AiBackgroundTasksTexts {
    val locale = LocalConfiguration.current.locales[0]
    fun format(pattern: String, vararg args: Any): String = String.format(locale, pattern, *args)
    val resources = LocalResources.current
    val heading = stringResource(R.string.monarch_aibackgroundtasks_group_heading)
    val statuses = MonarchAiBackgroundTaskStatus.entries.associateWith {
        stringResource(aiBackgroundTaskStatusText(it))
    }
    val elapsed = stringResource(R.string.monarch_aibackgroundtasks_elapsed)
    val ended = stringResource(R.string.monarch_aibackgroundtasks_ended)
    val progress = stringResource(R.string.monarch_aibackgroundtasks_progress_label)
    val actionLabel = stringResource(R.string.monarch_aibackgroundtasks_action_label)
    val announce = stringResource(R.string.monarch_aibackgroundtasks_announce)
    return AiBackgroundTasksTexts(
        title = monarchString(labels.title, R.string.monarch_aibackgroundtasks_title),
        runningCount = labels.runningCount
            ?: { resources.getQuantityString(R.plurals.monarch_aibackgroundtasks_running_count, it, it) },
        queuedCount = labels.queuedCount
            ?: { resources.getQuantityString(R.plurals.monarch_aibackgroundtasks_queued_count, it, it) },
        needsInputCount = labels.needsInputCount
            ?: { resources.getQuantityString(R.plurals.monarch_aibackgroundtasks_needs_input_count, it, it) },
        groups = Triple(
            monarchString(labels.needsInputGroup, R.string.monarch_aibackgroundtasks_group_needs_input),
            monarchString(labels.runningGroup, R.string.monarch_aibackgroundtasks_group_running),
            monarchString(labels.finishedGroup, R.string.monarch_aibackgroundtasks_group_finished),
        ),
        groupHeading = labels.groupHeading ?: { label, count -> format(heading, label, count) },
        task = AiBackgroundTasksTaskTexts(
            status = { labels.status?.invoke(it) ?: statuses.getValue(it) },
            elapsed = labels.elapsed ?: { format(elapsed, it) },
            ended = labels.ended ?: { format(ended, it) },
            progressLabel = labels.progressLabel ?: { format(progress, it) },
            announce = labels.announce ?: { title, status -> format(announce, title, status) },
        ),
        actions = AiBackgroundTasksActionTexts(
            open = monarchString(labels.open, R.string.monarch_aibackgroundtasks_open),
            cancel = monarchString(labels.cancel, R.string.monarch_aibackgroundtasks_cancel),
            retry = monarchString(labels.retry, R.string.monarch_aibackgroundtasks_retry),
            respond = monarchString(labels.respond, R.string.monarch_aibackgroundtasks_respond),
            dismiss = monarchString(labels.dismiss, R.string.monarch_aibackgroundtasks_dismiss),
            clearFinished = monarchString(labels.clearFinished, R.string.monarch_aibackgroundtasks_clear_finished),
            label = labels.actionLabel ?: { action, title -> format(actionLabel, action, title) },
        ),
        empty = monarchString(labels.empty, R.string.monarch_aibackgroundtasks_empty),
    )
}

private fun aiBackgroundTaskStatusText(status: MonarchAiBackgroundTaskStatus): Int = when (status) {
    MonarchAiBackgroundTaskStatus.Queued -> R.string.monarch_aibackgroundtasks_status_queued
    MonarchAiBackgroundTaskStatus.Running -> R.string.monarch_aibackgroundtasks_status_running
    MonarchAiBackgroundTaskStatus.NeedsInput -> R.string.monarch_aibackgroundtasks_status_needs_input
    MonarchAiBackgroundTaskStatus.Succeeded -> R.string.monarch_aibackgroundtasks_status_succeeded
    MonarchAiBackgroundTaskStatus.Failed -> R.string.monarch_aibackgroundtasks_status_failed
    MonarchAiBackgroundTaskStatus.Cancelled -> R.string.monarch_aibackgroundtasks_status_cancelled
}
