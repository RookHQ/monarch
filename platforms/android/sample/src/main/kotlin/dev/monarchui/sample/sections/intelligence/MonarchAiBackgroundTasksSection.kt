@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aibackgroundtasks.MonarchAiBackgroundTask
import dev.monarchui.components.intelligence.aibackgroundtasks.MonarchAiBackgroundTaskStatus
import dev.monarchui.components.intelligence.aibackgroundtasks.MonarchAiBackgroundTasks
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private const val MinuteMillis = 60_000L

private fun sampleTasks(now: Long) = listOf(
    MonarchAiBackgroundTask(
        "t1",
        "Summarize 40 contracts",
        MonarchAiBackgroundTaskStatus.Running,
        agent = "Research agent",
        progress = 0.3f,
        step = "Reading contract 12 of 40",
        startedAt = now - 3 * MinuteMillis,
    ),
    MonarchAiBackgroundTask("t2", "Book the venue", MonarchAiBackgroundTaskStatus.NeedsInput, step = "Pick a date"),
    MonarchAiBackgroundTask("t3", "Translate the FAQ", MonarchAiBackgroundTaskStatus.Queued),
    MonarchAiBackgroundTask(
        "t4",
        "Draft the newsletter",
        MonarchAiBackgroundTaskStatus.Succeeded,
        result = "Draft saved to Documents.",
        endedAt = now - 5 * MinuteMillis,
    ),
)

/** The catalogue page of AiBackgroundTasks. */
object MonarchAiBackgroundTasksSection : CataloguePage {
    override val title = "AiBackgroundTasks"

    @Composable
    override fun Content() {
        val now = remember { System.currentTimeMillis() }
        var tasks by remember { mutableStateOf(sampleTasks(now)) }
        val update: (MonarchAiBackgroundTask, MonarchAiBackgroundTaskStatus) -> Unit = { task, status ->
            tasks = tasks.map {
                if (it.id == task.id) it.copy(status = status, endedAt = System.currentTimeMillis()) else it
            }
        }
        CatalogueCaption("The tray button opens a sheet")
        MonarchAiBackgroundTasks(
            tasks = tasks,
            // A fixed reference time keeps the catalogue's screenshot tests still; apps usually leave it out.
            now = now,
            onCancel = { update(it, MonarchAiBackgroundTaskStatus.Cancelled) },
            onRespond = { update(it, MonarchAiBackgroundTaskStatus.Succeeded) },
            onDismiss = { task -> tasks = tasks.filterNot { it.id == task.id } },
            onClearFinished = { finished -> tasks = tasks - finished.toSet() },
        )
        CatalogueCaption("Inline, in a side pane")
        MonarchAiBackgroundTasks(
            tasks = tasks,
            inline = true,
            now = now,
            onOpen = {},
            onRetry = { update(it, MonarchAiBackgroundTaskStatus.Running) },
        )
    }
}
