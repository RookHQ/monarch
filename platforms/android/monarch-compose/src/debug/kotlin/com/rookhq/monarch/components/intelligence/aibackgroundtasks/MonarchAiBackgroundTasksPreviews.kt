@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence.aibackgroundtasks

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private const val PreviewNow = 1_800_000_000_000L
private const val PreviewMinute = 60_000L

private val PreviewTasks = listOf(
    MonarchAiBackgroundTask(
        "t1",
        "Summarize 40 contracts",
        MonarchAiBackgroundTaskStatus.Running,
        agent = "Research agent",
        progress = 0.3f,
        step = "Reading contract 12 of 40",
        startedAt = PreviewNow - 3 * PreviewMinute,
    ),
    MonarchAiBackgroundTask("t2", "Book the venue", MonarchAiBackgroundTaskStatus.NeedsInput, step = "Pick a date"),
    MonarchAiBackgroundTask(
        "t3",
        "Draft the newsletter",
        MonarchAiBackgroundTaskStatus.Failed,
        error = "The source page was unavailable.",
        endedAt = PreviewNow - PreviewMinute,
    ),
)

@MonarchPreviews
@Composable
internal fun MonarchAiBackgroundTasksPreview() {
    PreviewFrame {
        MonarchAiBackgroundTasks(tasks = PreviewTasks, now = PreviewNow)
        MonarchAiBackgroundTasks(
            tasks = PreviewTasks.drop(1),
            inline = true,
            now = PreviewNow,
            onRespond = {},
            onRetry = {},
            onDismiss = {},
            onClearFinished = {},
        )
    }
}
