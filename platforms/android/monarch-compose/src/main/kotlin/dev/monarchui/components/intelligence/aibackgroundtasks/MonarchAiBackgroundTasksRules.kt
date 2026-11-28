package dev.monarchui.components.intelligence.aibackgroundtasks

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import java.util.Locale

/** The three sections of the tray, each already sorted. */
internal data class AiBackgroundTaskGroups(
    val needsInput: List<MonarchAiBackgroundTask>,
    val running: List<MonarchAiBackgroundTask>,
    val finished: List<MonarchAiBackgroundTask>,
)

/** The grouping, timing and announcement logic of MonarchAiBackgroundTasks, as pure functions. */
internal object MonarchAiBackgroundTasksRules {
    private const val SECOND_MILLIS = 1_000L
    private const val MINUTE_SECONDS = 60L
    private const val HOUR_SECONDS = 3_600L

    private val announced = setOf(
        MonarchAiBackgroundTaskStatus.Succeeded,
        MonarchAiBackgroundTaskStatus.Failed,
        MonarchAiBackgroundTaskStatus.NeedsInput,
    )

    /** Whether [status] has ended. */
    fun finished(status: MonarchAiBackgroundTaskStatus): Boolean = status in setOf(
        MonarchAiBackgroundTaskStatus.Succeeded,
        MonarchAiBackgroundTaskStatus.Failed,
        MonarchAiBackgroundTaskStatus.Cancelled,
    )

    /**
     * Splits [tasks] into "needs you" (longest waiting first), "running" (running before queued, oldest first) and
     * "finished" (most recently ended first). Tasks without a time go last; ties keep the given order.
     */
    fun group(tasks: List<MonarchAiBackgroundTask>): AiBackgroundTaskGroups {
        val oldestFirst = compareBy<MonarchAiBackgroundTask, Long?>(nullsLast()) { it.startedAt }
        val queuedLast = compareBy<MonarchAiBackgroundTask> { it.status == MonarchAiBackgroundTaskStatus.Queued }
        val pending = setOf(MonarchAiBackgroundTaskStatus.Running, MonarchAiBackgroundTaskStatus.Queued)
        return AiBackgroundTaskGroups(
            needsInput = tasks.filter { it.status == MonarchAiBackgroundTaskStatus.NeedsInput }.sortedWith(oldestFirst),
            running = tasks.filter { it.status in pending }.sortedWith(queuedLast.then(oldestFirst)),
            finished = tasks
                .filter { finished(it.status) }
                .sortedWith(compareBy(nullsLast(reverseOrder())) { it.endedAt }),
        )
    }

    /** The progress shown as a figure, clamped to 0..1, or `null` for a moving bar. */
    fun known(task: MonarchAiBackgroundTask): Float? =
        task.progress?.takeIf { !task.indeterminate && !it.isNaN() }?.coerceIn(0f, 1f)

    /** Whether a task shows a bar: while running, and while waiting only with a real figure. */
    fun showsBar(task: MonarchAiBackgroundTask): Boolean =
        task.status == MonarchAiBackgroundTaskStatus.Running || (!finished(task.status) && known(task) != null)

    /** The detail line: the error, the result or the current step. */
    fun detail(task: MonarchAiBackgroundTask): String? = when {
        task.status == MonarchAiBackgroundTaskStatus.Failed -> task.error
        task.status == MonarchAiBackgroundTaskStatus.Succeeded -> task.result
        finished(task.status) -> null
        else -> task.step
    }

    /** A duration in short units of [locale]: "42 sec", "3 min 12 sec", "1 hr 4 min". Negative reads as zero. */
    fun elapsed(millis: Long, locale: Locale): String {
        val format = MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT)
        val total = maxOf(0L, millis / SECOND_MILLIS)
        val hours = total / HOUR_SECONDS
        val minutes = total % HOUR_SECONDS / MINUTE_SECONDS
        val seconds = total % MINUTE_SECONDS
        // CLDR data differs by release ("3 mins" or "3 min"); English is written the same everywhere, as on the web.
        val english = locale.language == "en"
        fun unit(value: Long, unit: MeasureUnit): String = when {
            !english -> format.format(Measure(value, unit))
            unit == MeasureUnit.HOUR -> "$value hr"
            unit == MeasureUnit.MINUTE -> "$value min"
            else -> "$value sec"
        }
        return when {
            hours > 0 -> "${unit(hours, MeasureUnit.HOUR)} ${unit(minutes, MeasureUnit.MINUTE)}"
            minutes > 0 -> "${unit(minutes, MeasureUnit.MINUTE)} ${unit(seconds, MeasureUnit.SECOND)}"
            else -> unit(seconds, MeasureUnit.SECOND)
        }
    }

    /** The tasks that changed into a status worth announcing since [before]; never tasks that are new. */
    fun news(
        before: Map<String, MonarchAiBackgroundTaskStatus>,
        tasks: List<MonarchAiBackgroundTask>,
    ): List<MonarchAiBackgroundTask> = tasks.filter { task ->
        val was = before[task.id]
        was != null && was != task.status && task.status in announced
    }
}
