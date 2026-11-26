package com.rookhq.monarch.components.intelligence.aibackgroundtasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgress
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchRelativeTime
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay

/**
 * A tray for the work agents do in the background, so a long task stays
 * visible after the chat is closed: a button with the running and waiting
 * counts opens a card of tasks grouped into "Needs you", "Running" and
 * "Finished", each with its status, progress, elapsed time and actions. It
 * only reports and asks: every action is a callback. Work that outlives the
 * app belongs in a system notification, which the app posts from [onNotify].
 *
 * A secondary `MonarchButton` with `MonarchBadge` counts opens a
 * `MonarchCard`; tasks are bordered cards and show their status as a `MonarchBadge`,
 * progress as `MonarchProgress` and actions as small `MonarchButton`s. With
 * [inline] the panel is drawn in place, for a side pane. Elapsed times refresh
 * every second while a task runs and the tray is visible, unless the person
 * removed animations or [now] is given. A task that succeeds, fails or starts
 * needing input is announced through a polite live region.
 *
 * @param tasks every background task, in any order; the tray groups and sorts them.
 * @param modifier the modifier of the button, or of the panel when [inline].
 * @param inline draws the panel in place, with no button and no sheet.
 * @param now the reference time for elapsed and end times, in epoch milliseconds. `null` follows the clock.
 * @param onOpen shows Open on every task, for example to go to its conversation.
 * @param onCancel shows Cancel on queued, running and waiting tasks.
 * @param onRetry shows Retry on failed tasks.
 * @param onRespond shows Respond on tasks that need input.
 * @param onDismiss shows Dismiss on finished tasks.
 * @param onClearFinished shows Clear finished while there are finished tasks; called with them.
 * @param onNotify called when a task succeeds, fails or starts needing input (the changes the tray announces), so
 * the app can post a system notification.
 * @param icon the button's leading icon, decorative. Monarch bundles no icons.
 * @param statusIcon an icon before each task, given its status, decorative. The status is always also a word.
 * @param labels replacements for the built-in strings.
 * @param initiallyOpen opens the task panel when the control first appears, as the web example does.
 */
@ExperimentalMonarchApi
@Composable
@Suppress("LongParameterList")
public fun MonarchAiBackgroundTasks(
    tasks: List<MonarchAiBackgroundTask>,
    modifier: Modifier = Modifier,
    inline: Boolean = false,
    now: Long? = null,
    onOpen: ((MonarchAiBackgroundTask) -> Unit)? = null,
    onCancel: ((MonarchAiBackgroundTask) -> Unit)? = null,
    onRetry: ((MonarchAiBackgroundTask) -> Unit)? = null,
    onRespond: ((MonarchAiBackgroundTask) -> Unit)? = null,
    onDismiss: ((MonarchAiBackgroundTask) -> Unit)? = null,
    onClearFinished: ((List<MonarchAiBackgroundTask>) -> Unit)? = null,
    onNotify: ((MonarchAiBackgroundTask) -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    statusIcon: (@Composable (MonarchAiBackgroundTaskStatus) -> Unit)? = null,
    labels: MonarchAiBackgroundTasksLabels = MonarchAiBackgroundTasksLabels(),
    initiallyOpen: Boolean = false,
) {
    val texts = aiBackgroundTasksTexts(labels)
    var open by rememberSaveable { mutableStateOf(initiallyOpen) }
    val time = aiBackgroundTasksClock(tasks, visible = inline || open, now = now)
    val announcement = aiBackgroundTasksAnnouncement(tasks, texts, onNotify)
    val panel = AiBackgroundTasksPanelInput(
        tasks = tasks,
        now = time,
        callbacks = AiBackgroundTasksCallbacks(onOpen, onCancel, onRetry, onRespond, onDismiss, onClearFinished),
        statusIcon = statusIcon ?: MonarchDefaultIcons.backgroundTaskStatus,
        texts = texts,
    )
    Box(modifier = modifier) {
        if (inline) {
            AiBackgroundTasksPanel(panel)
        } else {
            // As the web: the button, and under it the panel as a card, not a sheet.
            Column(verticalArrangement = Arrangement.spacedBy(MonarchAiBackgroundTasksTokens.Gap)) {
                AiBackgroundTasksButton(tasks, texts, icon ?: MonarchDefaultIcons.Bot) { open = !open }
                if (open) MonarchCard { AiBackgroundTasksPanel(panel, cards = true) }
            }
        }
        // Always present, so each announced change is read once as a change of this live region.
        Box(
            modifier = Modifier.clearAndSetSemantics {
                if (announcement.isNotEmpty()) contentDescription = announcement
                liveRegion = LiveRegionMode.Polite
            },
        )
    }
}

/** The callbacks of the task actions. */
internal class AiBackgroundTasksCallbacks(
    val onOpen: ((MonarchAiBackgroundTask) -> Unit)?,
    val onCancel: ((MonarchAiBackgroundTask) -> Unit)?,
    val onRetry: ((MonarchAiBackgroundTask) -> Unit)?,
    val onRespond: ((MonarchAiBackgroundTask) -> Unit)?,
    val onDismiss: ((MonarchAiBackgroundTask) -> Unit)?,
    val onClearFinished: ((List<MonarchAiBackgroundTask>) -> Unit)?,
)

/** What the panel draws. */
internal class AiBackgroundTasksPanelInput(
    val tasks: List<MonarchAiBackgroundTask>,
    val now: Long,
    val callbacks: AiBackgroundTasksCallbacks,
    val statusIcon: (@Composable (MonarchAiBackgroundTaskStatus) -> Unit)?,
    val texts: AiBackgroundTasksTexts,
)

/** The reference time: [now], or the clock, read when the tray shows and every second while a task runs. */
@Composable
private fun aiBackgroundTasksClock(tasks: List<MonarchAiBackgroundTask>, visible: Boolean, now: Long?): Long {
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val reduced = MonarchTheme.motion.reducedMotion
    val ticking = now == null && visible && !reduced &&
        tasks.any { it.status == MonarchAiBackgroundTaskStatus.Running && it.startedAt != null }
    LaunchedEffect(visible, ticking, now) {
        if (now != null || !visible) return@LaunchedEffect
        clock = System.currentTimeMillis()
        while (ticking) {
            delay(MonarchAiBackgroundTasksTokens.TICK_MILLIS)
            clock = System.currentTimeMillis()
        }
    }
    return now ?: clock
}

/** Holds the statuses last seen, outside of composition state. */
private class AiBackgroundTasksSeen {
    var statuses: Map<String, MonarchAiBackgroundTaskStatus>? = null
}

/** The text to announce: tasks that changed into Done, Failed or Needs your input; never on first show. */
@Composable
private fun aiBackgroundTasksAnnouncement(
    tasks: List<MonarchAiBackgroundTask>,
    texts: AiBackgroundTasksTexts,
    onNotify: ((MonarchAiBackgroundTask) -> Unit)?,
): String {
    val seen = remember { AiBackgroundTasksSeen() }
    var live by remember { mutableStateOf("") }
    LaunchedEffect(tasks) {
        val before = seen.statuses
        seen.statuses = tasks.associate { it.id to it.status }
        if (before == null) return@LaunchedEffect
        val news = MonarchAiBackgroundTasksRules.news(before, tasks)
        if (news.isNotEmpty()) {
            live = news.joinToString(". ") { texts.task.announce(it.title, texts.task.status(it.status)) }
            news.forEach { onNotify?.invoke(it) }
        }
    }
    return live
}

/** The button: the title with the running or queued count and the waiting count as badges. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiBackgroundTasksButton(
    tasks: List<MonarchAiBackgroundTask>,
    texts: AiBackgroundTasksTexts,
    icon: (@Composable () -> Unit)?,
    onClick: () -> Unit,
) {
    val running = tasks.count { it.status == MonarchAiBackgroundTaskStatus.Running }
    val queued = tasks.count { it.status == MonarchAiBackgroundTaskStatus.Queued }
    val waiting = tasks.count { it.status == MonarchAiBackgroundTaskStatus.NeedsInput }
    MonarchButton(
        onClick = onClick,
        variant = MonarchButtonVariant.Secondary,
        size = MonarchButtonSize.Sm,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MonarchAiBackgroundTasksTokens.InnerGap),
            verticalArrangement = Arrangement.spacedBy(MonarchAiBackgroundTasksTokens.InnerGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let { Box(modifier = Modifier.clearAndSetSemantics { }) { it() } }
            Text(text = texts.title)
            when {
                running > 0 -> MonarchBadge(text = texts.runningCount(running), tone = MonarchTone.Info)
                queued > 0 -> MonarchBadge(text = texts.queuedCount(queued))
            }
            if (waiting > 0) MonarchBadge(text = texts.needsInputCount(waiting), tone = MonarchTone.Warning)
        }
    }
}

/** The heading, Clear finished and the three groups, or the empty text. */
@Composable
private fun AiBackgroundTasksPanel(panel: AiBackgroundTasksPanelInput, cards: Boolean = false) {
    val tokens = MonarchAiBackgroundTasksTokens
    val groups = MonarchAiBackgroundTasksRules.group(panel.tasks)
    Column(
        modifier = Modifier.fillMaxWidth().semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonarchText(
                text = panel.texts.title,
                modifier = Modifier.weight(1f),
                style = MonarchTextStyle.Subheading,
                heading = true,
            )
            val clear = panel.callbacks.onClearFinished
            if (clear != null && groups.finished.isNotEmpty()) {
                MonarchButton(
                    text = panel.texts.actions.clearFinished,
                    onClick = { clear(groups.finished) },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            }
        }
        if (panel.tasks.isEmpty()) {
            Text(
                text = panel.texts.empty,
                style = MonarchTheme.typography.body,
                color = tokens.metaColor(MonarchTheme.colors),
            )
        }
        val (needs, running, finished) = panel.texts.groups
        AiBackgroundTasksGroup(needs, groups.needsInput, panel, cards)
        AiBackgroundTasksGroup(running, groups.running, panel, cards)
        AiBackgroundTasksGroup(finished, groups.finished, panel, cards)
    }
}

/** A group heading with its count and its tasks. */
@Composable
private fun AiBackgroundTasksGroup(
    label: String,
    tasks: List<MonarchAiBackgroundTask>,
    panel: AiBackgroundTasksPanelInput,
    cards: Boolean,
) {
    if (tasks.isEmpty()) return
    val tokens = MonarchAiBackgroundTasksTokens
    Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        Text(
            text = panel.texts.groupHeading(label, tasks.size),
            modifier = Modifier.semantics { heading() },
            style = MonarchTheme.typography.label,
            color = tokens.metaColor(MonarchTheme.colors),
        )
        tasks.forEach { AiBackgroundTasksRow(it, panel, cards) }
    }
}

/** One task: status, title, meta line, detail, progress and actions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiBackgroundTasksRow(task: MonarchAiBackgroundTask, panel: AiBackgroundTasksPanelInput, cards: Boolean) {
    val tokens = MonarchAiBackgroundTasksTokens
    val rules = MonarchAiBackgroundTasksRules
    val colors = MonarchTheme.colors
    Column(
        modifier = if (cards) {
            Modifier
                .fillMaxWidth()
                .border(tokens.CardBorder, colors.borderDefault, MonarchTheme.shapes.md)
                .padding(tokens.CardPadding)
        } else {
            Modifier
        },
        verticalArrangement = Arrangement.spacedBy(tokens.InnerGap),
    ) {
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) { },
            horizontalArrangement = Arrangement.spacedBy(tokens.InnerGap),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.InnerGap)) {
                // The status mark sits in the title line, so it does not narrow the lines under it.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(tokens.InnerGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    panel.statusIcon?.let { Box(modifier = Modifier.clearAndSetSemantics { }) { it(task.status) } }
                    Text(
                        text = task.title,
                        style = MonarchTheme.typography.bodyStrong,
                        color = tokens.titleColor(colors),
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(tokens.InnerGap),
                    verticalArrangement = Arrangement.spacedBy(tokens.InnerGap),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    MonarchBadge(text = panel.texts.task.status(task.status), tone = tokens.statusTone(task.status))
                    aiBackgroundTaskMeta(task, panel).forEach {
                        Text(text = it, style = MonarchTheme.typography.caption, color = tokens.metaColor(colors))
                    }
                }
                rules.detail(task)?.let {
                    Text(
                        text = it,
                        style = MonarchTheme.typography.caption,
                        color = tokens.detailColor(task.status, colors),
                    )
                }
            }
        }
        if (rules.showsBar(task)) {
            MonarchProgress(
                modifier = Modifier.fillMaxWidth(),
                progress = rules.known(task),
                label = panel.texts.task.progressLabel(task.title),
                tone = tokens.barTone(task.status),
            )
        }
        AiBackgroundTasksActions(task, panel)
    }
}

/** The agent, the elapsed time of a running task and the end time of a finished one. */
@Composable
private fun aiBackgroundTaskMeta(task: MonarchAiBackgroundTask, panel: AiBackgroundTasksPanelInput): List<String> {
    val locale = LocalConfiguration.current.locales[0]
    val started = task.startedAt
    val ended = task.endedAt
    return listOfNotNull(
        task.agent,
        if (task.status == MonarchAiBackgroundTaskStatus.Running && started != null) {
            panel.texts.task.elapsed(MonarchAiBackgroundTasksRules.elapsed(panel.now - started, locale))
        } else {
            null
        },
        if (MonarchAiBackgroundTasksRules.finished(task.status) && ended != null) {
            panel.texts.task.ended(
                MonarchRelativeTime.format(ended, panel.now, locale),
            )
        } else {
            null
        },
    )
}

/** The task's buttons, each read with the task's title. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiBackgroundTasksActions(task: MonarchAiBackgroundTask, panel: AiBackgroundTasksPanelInput) {
    val callbacks = panel.callbacks
    val actions = panel.texts.actions
    val finished = MonarchAiBackgroundTasksRules.finished(task.status)
    val buttons = listOfNotNull(
        callbacks.onRespond.takeIf { task.status == MonarchAiBackgroundTaskStatus.NeedsInput }
            ?.let { Triple(actions.respond, MonarchButtonVariant.Primary, it) },
        callbacks.onRetry.takeIf { task.status == MonarchAiBackgroundTaskStatus.Failed }
            ?.let { Triple(actions.retry, MonarchButtonVariant.Secondary, it) },
        callbacks.onOpen?.let { Triple(actions.open, MonarchButtonVariant.Secondary, it) },
        callbacks.onCancel.takeIf { !finished }?.let { Triple(actions.cancel, MonarchButtonVariant.Ghost, it) },
        callbacks.onDismiss.takeIf { finished }?.let { Triple(actions.dismiss, MonarchButtonVariant.Ghost, it) },
    )
    if (buttons.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchAiBackgroundTasksTokens.InnerGap)) {
        buttons.forEach { (text, variant, action) ->
            val name = actions.label(text, task.title)
            MonarchButton(
                text = text,
                onClick = { action(task) },
                modifier = Modifier.semantics { contentDescription = name },
                variant = variant,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}
