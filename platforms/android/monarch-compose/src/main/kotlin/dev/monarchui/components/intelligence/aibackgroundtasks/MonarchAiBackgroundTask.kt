package dev.monarchui.components.intelligence.aibackgroundtasks

import androidx.compose.runtime.Immutable

/**
 * One piece of work an agent is doing in the background, shown by `MonarchAiBackgroundTasks`.
 *
 * @property id the stable id; status changes are told apart by it.
 * @property title what the task is, such as "Summarize 40 contracts". Already localised.
 * @property status where the task is.
 * @property agent the name of the agent doing the work.
 * @property progress completion from 0 to 1, when the product can measure it.
 * @property indeterminate shows the moving bar even when [progress] is set, because the figure is not reliable yet.
 * @property step what the agent is doing right now, such as "Reading contract 12 of 40".
 * @property startedAt when the task started, in epoch milliseconds.
 * @property endedAt when the task ended, in epoch milliseconds.
 * @property result a one-line outcome, shown once the task has succeeded.
 * @property error what went wrong, shown once the task has failed.
 */
@Immutable
public data class MonarchAiBackgroundTask(
    public val id: String,
    public val title: String,
    public val status: MonarchAiBackgroundTaskStatus,
    public val agent: String? = null,
    public val progress: Float? = null,
    public val indeterminate: Boolean = false,
    public val step: String? = null,
    public val startedAt: Long? = null,
    public val endedAt: Long? = null,
    public val result: String? = null,
    public val error: String? = null,
)
