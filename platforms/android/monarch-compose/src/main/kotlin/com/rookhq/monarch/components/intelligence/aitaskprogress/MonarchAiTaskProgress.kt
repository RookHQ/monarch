package com.rookhq.monarch.components.intelligence.aitaskprogress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgress
import com.rookhq.monarch.components.datadisplay.tag.MonarchChip
import com.rookhq.monarch.components.intelligence.aichainofsteps.MonarchAiChainOfSteps
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStep
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchRelativeClock
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A long-running assistant task in a card, with its status, a progress bar,
 * when it started and how long is left, its steps, and controls to cancel,
 * minimize or be notified.
 *
 * Built from Monarch parts: `MonarchCard`, a `MonarchBadge` for the status,
 * `MonarchProgress` for the bar, `MonarchAiChainOfSteps` for the steps,
 * `MonarchButton` for Cancel and a `MonarchChip` for the "Notify me
 * when done" toggle. The title is a heading; the status is written in the
 * badge, never shown by colour alone. The bar is one TalkBack element named
 * "<title> progress" with "40% complete" as its state, or the status name
 * when it is indeterminate. A change of status is announced as "<title>:
 * <status>" through a live region that is always present: politely, and
 * assertively for a failure. The first status is not announced. The start
 * text ("Started 5 minutes ago") refreshes every minute unless [now] is given.
 *
 * @param title what the task is, for example "Summarising 40 documents".
 * @param status where the task is.
 * @param startedAt when the task started, in epoch milliseconds (UTC).
 * @param modifier the modifier of the card.
 * @param steps the steps, shown as a `MonarchAiChainOfSteps`. A task may have only a bar.
 * @param progress the completed fraction from 0 to 1. `null` uses the share of finished (done, failed or skipped)
 * steps; with no steps the bar is indeterminate while running.
 * @param etaMillis the estimated time left in milliseconds, shown while running.
 * @param notifying whether "Notify me when done" is on.
 * @param now the reference time of the start text, in epoch milliseconds, for previews, tests and identical
 * renders. `null` uses the clock, refreshed every minute.
 * @param onNotifyingChange shows the "Notify me when done" toggle while the task runs or is paused, and is called
 * with the new value.
 * @param onMinimize shows a Minimize button, for example to dock the task in a tray.
 * @param onCancel shows a Cancel button while the task runs or is paused.
 * @param statusIcon an icon in the status badge, given the status. Decorative: the status is written.
 * @param minimizeIcon an icon for the Minimize button, which then becomes an icon button named "Minimize".
 * Monarch bundles no icons; without it the button is written.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchAiTaskProgress(
    title: String,
    status: MonarchAiTaskStatus,
    startedAt: Long,
    modifier: Modifier = Modifier,
    steps: List<MonarchAiPlanStep> = emptyList(),
    progress: Float? = null,
    etaMillis: Long? = null,
    notifying: Boolean = false,
    now: Long? = null,
    onNotifyingChange: ((Boolean) -> Unit)? = null,
    onMinimize: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    statusIcon: (@Composable (MonarchAiTaskStatus) -> Unit)? = null,
    minimizeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronDown,
    labels: MonarchAiTaskProgressLabels = MonarchAiTaskProgressLabels(),
) {
    val tokens = MonarchAiTaskProgressTokens
    val statusName = labels.status?.invoke(status) ?: stringResource(tokens.statusRes(status))
    val fraction = tokens.fraction(progress, steps, status)
    val percentText = fraction?.let {
        val percent = tokens.percent(it)
        labels.progressValue?.invoke(percent) ?: stringResource(R.string.monarch_aitaskprogress_progress_value, percent)
    }
    val announcement = rememberStatusAnnouncement(title, status, statusName, labels)
    MonarchCard(modifier = modifier.semantics { isTraversalGroup = true }, tone = MonarchTone.Ai) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
            Header(title, status, statusName, statusIcon, onMinimize, minimizeIcon, labels)
            Bar(title, status, statusName, fraction, percentText, labels)
            Meta(startedAt, now, percentText, status, etaMillis, labels)
            if (steps.isNotEmpty()) MonarchAiChainOfSteps(steps = steps)
            if (tokens.active(status) && (onCancel != null || onNotifyingChange != null)) {
                Actions(notifying, onNotifyingChange, onCancel, labels)
            }
        }
        Box(
            Modifier.semantics {
                liveRegion = if (announcement.urgent) LiveRegionMode.Assertive else LiveRegionMode.Polite
                if (announcement.text.isNotEmpty()) contentDescription = announcement.text
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongParameterList")
@Composable
private fun Header(
    title: String,
    status: MonarchAiTaskStatus,
    statusName: String,
    statusIcon: (@Composable (MonarchAiTaskStatus) -> Unit)?,
    onMinimize: (() -> Unit)?,
    minimizeIcon: (@Composable () -> Unit)?,
    labels: MonarchAiTaskProgressLabels,
) {
    val tokens = MonarchAiTaskProgressTokens
    Row(verticalAlignment = Alignment.CenterVertically) {
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
            verticalArrangement = Arrangement.spacedBy(tokens.ItemGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MonarchTheme.typography.subheading,
                color = MonarchTheme.colors.textDefault,
            )
            MonarchBadge(
                text = statusName,
                tone = tokens.tone(status),
                leadingIcon = statusIcon?.let { icon -> { icon(status) } },
            )
        }
        if (onMinimize != null) {
            val minimize = monarchString(labels.minimize, R.string.monarch_aitaskprogress_minimize)
            if (minimizeIcon != null) {
                MonarchIconButton(label = minimize, onClick = onMinimize) { minimizeIcon() }
            } else {
                MonarchButton(
                    text = minimize,
                    onClick = onMinimize,
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun Bar(
    title: String,
    status: MonarchAiTaskStatus,
    statusName: String,
    fraction: Float?,
    percentText: String?,
    labels: MonarchAiTaskProgressLabels,
) {
    val barName = labels.progressLabel?.invoke(title)
        ?: stringResource(R.string.monarch_aitaskprogress_progress_label, title)
    val value = MonarchAiTaskProgressTokens.barValue(fraction, status)
    Box(
        Modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = barName
            stateDescription = percentText ?: statusName
            progressBarRangeInfo = if (value == null) {
                ProgressBarRangeInfo.Indeterminate
            } else {
                ProgressBarRangeInfo(value, 0f..1f)
            }
        },
    ) {
        // An indeterminate bar loops; with animations removed it stays still and empty.
        val shown = if (value == null && MonarchTheme.motion.reducedMotion) 0f else value
        MonarchProgress(
            modifier = Modifier.fillMaxWidth(),
            progress = shown,
            tone = MonarchAiTaskProgressTokens.barTone(status),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongParameterList")
@Composable
private fun Meta(
    startedAt: Long,
    now: Long?,
    percentText: String?,
    status: MonarchAiTaskStatus,
    etaMillis: Long?,
    labels: MonarchAiTaskProgressLabels,
) {
    val tokens = MonarchAiTaskProgressTokens
    val relative = tokens.relative(startedAt, rememberMonarchRelativeClock(listOf(startedAt), fixed = now))
    val started = labels.started?.invoke(relative) ?: stringResource(R.string.monarch_aitaskprogress_started, relative)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.SectionGap),
        verticalArrangement = Arrangement.spacedBy(tokens.ItemGap),
    ) {
        MetaText(started)
        if (percentText != null) MetaText(percentText)
        if (etaMillis != null && tokens.showsEta(status, etaMillis)) MetaText(etaText(etaMillis, labels))
    }
}

@Composable
private fun MetaText(text: String) {
    Text(text = text, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
}

@Composable
private fun etaText(millis: Long, labels: MonarchAiTaskProgressLabels): String =
    labels.eta?.invoke(millis) ?: when (val eta = MonarchAiTaskProgressTokens.eta(millis)) {
        AiTaskEta.UnderAMinute -> stringResource(R.string.monarch_aitaskprogress_eta_under_minute)
        is AiTaskEta.Minutes ->
            pluralStringResource(R.plurals.monarch_aitaskprogress_eta_minutes, eta.minutes, eta.minutes)
        is AiTaskEta.Hours -> pluralStringResource(R.plurals.monarch_aitaskprogress_eta_hours, eta.hours, eta.hours)
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Actions(
    notifying: Boolean,
    onNotifyingChange: ((Boolean) -> Unit)?,
    onCancel: (() -> Unit)?,
    labels: MonarchAiTaskProgressLabels,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MonarchAiTaskProgressTokens.ItemGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (onNotifyingChange != null) {
            MonarchChip(
                text = monarchString(labels.notify, R.string.monarch_aitaskprogress_notify),
                selected = notifying,
                onSelectedChange = onNotifyingChange,
                leadingIcon = MonarchDefaultIcons.Bell,
            )
        }
        if (onCancel != null) {
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_aitaskprogress_cancel),
                onClick = onCancel,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}

/** The text of the live region, and whether it is assertive. */
private class TaskAnnouncement(val text: String, val urgent: Boolean)

/** The latest status announcement. Empty until the status changes after the first composition. */
@Composable
private fun rememberStatusAnnouncement(
    title: String,
    status: MonarchAiTaskStatus,
    statusName: String,
    labels: MonarchAiTaskProgressLabels,
): TaskAnnouncement {
    val resources = LocalResources.current
    var previous by remember { mutableStateOf<MonarchAiTaskStatus?>(null) }
    var announcement by remember { mutableStateOf(TaskAnnouncement("", urgent = false)) }
    LaunchedEffect(status) {
        val urgent = MonarchAiTaskProgressTokens.announcement(previous, status)
        previous = status
        if (urgent != null) {
            val text = labels.announce?.invoke(title, statusName)
                ?: resources.getString(R.string.monarch_aitaskprogress_announce, title, statusName)
            announcement = TaskAnnouncement(text, urgent)
        }
    }
    return announcement
}
