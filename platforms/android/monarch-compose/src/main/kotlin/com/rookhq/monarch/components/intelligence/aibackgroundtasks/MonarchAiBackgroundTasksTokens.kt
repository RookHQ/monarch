package com.rookhq.monarch.components.intelligence.aibackgroundtasks

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchAiBackgroundTasks, from primitives and roles only. */
internal object MonarchAiBackgroundTasksTokens {
    /** The space between groups and between tasks. */
    val Gap: Dp = MonarchTokens.Space.space4.dp

    /** The space inside a task and between the parts of the button. */
    val InnerGap: Dp = MonarchTokens.Space.space2.dp

    /** The border of a task card. */
    val CardBorder: Dp = MonarchTokens.Space.px.dp

    /** The padding inside a task card. */
    val CardPadding: Dp = MonarchTokens.Space.space3.dp

    /** How often elapsed times refresh while a task runs. */
    const val TICK_MILLIS: Long = 1_000L

    /** Titles: default text. */
    fun titleColor(colors: MonarchColors): Color = colors.textDefault

    /** Meta lines, steps, results and headings: muted text. */
    fun metaColor(colors: MonarchColors): Color = colors.textMuted

    /** The detail of a failed task: danger text; other details are muted. */
    fun detailColor(status: MonarchAiBackgroundTaskStatus, colors: MonarchColors): Color =
        if (status == MonarchAiBackgroundTaskStatus.Failed) colors.dangerStrong else colors.textMuted

    /** The tone of the status word in the meta line. */
    fun statusTone(status: MonarchAiBackgroundTaskStatus): MonarchTone = when (status) {
        MonarchAiBackgroundTaskStatus.Queued, MonarchAiBackgroundTaskStatus.Cancelled -> MonarchTone.Neutral
        MonarchAiBackgroundTaskStatus.Running -> MonarchTone.Ai
        MonarchAiBackgroundTaskStatus.NeedsInput -> MonarchTone.Warning
        MonarchAiBackgroundTaskStatus.Succeeded -> MonarchTone.Success
        MonarchAiBackgroundTaskStatus.Failed -> MonarchTone.Danger
    }

    /** The tone of a task's bar: the accent while running, then the status, as on the web. */
    fun barTone(status: MonarchAiBackgroundTaskStatus): MonarchTone = when (status) {
        MonarchAiBackgroundTaskStatus.Queued, MonarchAiBackgroundTaskStatus.Cancelled -> MonarchTone.Neutral
        MonarchAiBackgroundTaskStatus.Running -> MonarchTone.Accent
        MonarchAiBackgroundTaskStatus.NeedsInput -> MonarchTone.Warning
        MonarchAiBackgroundTaskStatus.Succeeded -> MonarchTone.Success
        MonarchAiBackgroundTaskStatus.Failed -> MonarchTone.Danger
    }
}
