package dev.monarchui.components.workflow.runningtimer

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.R
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.typography.tabularnumberstyle.MonarchTabularNumberStyle
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.monarchTimeZone
import dev.monarchui.theme.MonarchTheme
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A running stopwatch for time tracking: a panel with a large clock, Start,
 * Stop, "I stopped earlier" and a confirmed Discard, or a compact indicator
 * for a top bar.
 *
 * Built by Monarch from `MonarchButton`, `MonarchMenu`, `MonarchDialog`
 * and the themed `TimeInput`. The clock ticks once a second while a timer
 * runs and is read by TalkBack in words; only starting and stopping are
 * announced, not the ticking. The actions suspend: the button of the running
 * action shows a spinner, the others are disabled, and a thrown error is
 * shown and announced. The app changes [startedAt] when an action succeeds.
 * Showing the timer outside the app (an ongoing notification) stays in the
 * app.
 *
 * @param startedAt when the running timer started, in epoch milliseconds; `null` when no timer runs.
 * @param modifier the modifier of the timer.
 * @param variant a panel, or a compact indicator.
 * @param label what is being timed, such as an issue key. Shown under the panel's clock and in the indicator
 * ("Timer" when `null`).
 * @param warnAfterMillis shows a warning once the timer has run this long. `null` turns it off.
 * @param startEnabled whether Start can be pressed, for example only once a required project is chosen.
 * @param pending an action runs outside the component: Start shows a spinner and the other buttons are disabled.
 * @param showWhenStopped keeps the indicator visible, at 0:00:00, while no timer runs.
 * @param now a fixed reference time, which also stops the clock. For previews, screenshots and tests.
 * @param timeZone the zone of the times in the earlier-stop dialog. An IANA id such as `"Europe/Berlin"`. `null`, or an
 * id the device does not know, is the device's zone.
 * @param menuIcon the icon of the indicator's menu button, such as a down chevron. Without it the button reads
 * "More".
 * @param labels replacements for the built-in strings.
 * @param onStart starts a timer. Adds Start to the stopped panel.
 * @param onStop stops the timer and logs the time. Adds Stop to the panel and the indicator.
 * @param onStopAt stops the timer at an earlier time, in epoch milliseconds. Adds "I stopped earlier", which asks
 * for the time of day between the start and now.
 * @param onDiscard throws the running time away. Adds Discard, confirmed first.
 * @param onOpen opens what is being timed. Makes the indicator's clock a button.
 * @param fields fields for the entry being timed, such as a project picker. Shown in the panel between the clock
 * and the buttons.
 */
@Suppress("LongParameterList")
@Composable
public fun MonarchRunningTimer(
    startedAt: Long?,
    modifier: Modifier = Modifier,
    variant: MonarchRunningTimerVariant = MonarchRunningTimerVariant.Panel,
    label: String? = null,
    warnAfterMillis: Long? = MonarchRunningTimerTokens.DEFAULT_WARN_AFTER_MILLIS,
    startEnabled: Boolean = true,
    pending: Boolean = false,
    showWhenStopped: Boolean = false,
    now: Long? = null,
    timeZone: String? = null,
    menuIcon: (@Composable () -> Unit)? = null,
    labels: MonarchRunningTimerLabels = MonarchRunningTimerLabels(),
    onStart: (suspend () -> Unit)? = null,
    onStop: (suspend () -> Unit)? = null,
    onStopAt: (suspend (Long) -> Unit)? = null,
    onDiscard: (suspend () -> Unit)? = null,
    onOpen: (() -> Unit)? = null,
    fields: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val running = startedAt != null
    val clockNow = rememberRunningTimerNow(now, running)
    val text = runningTimerText(labels)
    val scope = rememberCoroutineScope()
    val runner = remember(scope) { RunningTimerRunner(scope) }
    runner.failed = text.failed
    var discarding by rememberSaveable { mutableStateOf(false) }
    var stoppingEarlier by rememberSaveable { mutableStateOf(false) }
    val announcement = rememberRunningTimerAnnouncement(running, text)
    LaunchedEffect(running) {
        if (!running) {
            discarding = false
            stoppingEarlier = false
        }
    }
    val state = runningTimerState(startedAt, clockNow, label, warnAfterMillis, pending, runner)
    val actions = runningTimerActions(
        runner = runner,
        onStart = onStart,
        onStop = onStop,
        onStopEarlier = { stoppingEarlier = true }.takeIf { onStopAt != null },
        onDiscard = { discarding = true }.takeIf { onDiscard != null },
        onOpen = onOpen,
    )
    Box(modifier = modifier) {
        val panel = variant == MonarchRunningTimerVariant.Panel
        if (panel) RunningTimerPanel(state, actions, text, startEnabled, warnAfterMillis, fields)
        if (!panel && (running || showWhenStopped)) RunningTimerIndicator(state, actions, text, menuIcon)
        // Always present, so that starting and stopping are announced even when the indicator hides.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
    if (startedAt != null) {
        RunningTimerOpenDialogs(
            start = startedAt,
            now = clockNow,
            zone = monarchTimeZone(timeZone),
            text = text,
            runner = runner,
            discard = onDiscard.takeIf { discarding },
            stopAt = onStopAt.takeIf { stoppingEarlier },
            onCloseDiscard = { discarding = false },
            onCloseStopAt = { stoppingEarlier = false },
        )
    }
}

/** The callbacks of the timer's own buttons: Start and Stop run through [runner]. */
@Suppress("LongParameterList")
private fun runningTimerActions(
    runner: RunningTimerRunner,
    onStart: (suspend () -> Unit)?,
    onStop: (suspend () -> Unit)?,
    onStopEarlier: (() -> Unit)?,
    onDiscard: (() -> Unit)?,
    onOpen: (() -> Unit)?,
): RunningTimerActions = RunningTimerActions(
    start = onStart?.let { action -> { runner.run(RunningTimerAction.Start, action) {} } },
    stop = onStop?.let { action -> { runner.run(RunningTimerAction.Stop, action) {} } },
    stopEarlier = onStopEarlier,
    discard = onDiscard,
    open = onOpen,
)

/** Runs the timer's actions one at a time and keeps their busy state and failure. */
@Stable
internal class RunningTimerRunner(private val scope: CoroutineScope) {
    var busy: RunningTimerAction? by mutableStateOf(null)
        private set
    var error: String? by mutableStateOf(null)
        private set
    var failed: String = ""

    /** Runs [block] as [action]; [after] runs when it succeeds, a thrown error becomes [error]. */
    fun run(action: RunningTimerAction, block: suspend () -> Unit, after: () -> Unit) {
        busy = action
        error = null
        scope.launch {
            val outcome = runCatching { block() }
            busy = null
            val thrown = outcome.exceptionOrNull()
            if (thrown is CancellationException) throw thrown
            if (thrown == null) after() else error = thrown.message?.takeIf { it.isNotBlank() } ?: failed
        }
    }
}

@Composable
private fun runningTimerState(
    startedAt: Long?,
    now: Long,
    label: String?,
    warnAfterMillis: Long?,
    pending: Boolean,
    runner: RunningTimerRunner,
): RunningTimerState {
    val tokens = MonarchRunningTimerTokens
    val locale = LocalConfiguration.current.locales[0]
    val seconds = startedAt?.let { tokens.elapsedSeconds(it, now) } ?: 0L
    return RunningTimerState(
        running = startedAt != null,
        clock = tokens.clock(seconds),
        spoken = tokens.spoken(seconds, locale),
        label = label,
        long = tokens.longRunning(startedAt, now, warnAfterMillis),
        busy = runner.busy,
        anyBusy = pending || runner.busy != null,
        pending = pending,
        error = runner.error,
    )
}

/** The discard confirmation or the earlier-stop dialog, when one is open. */
@Suppress("LongParameterList")
@Composable
private fun RunningTimerOpenDialogs(
    start: Long,
    now: Long,
    zone: TimeZone,
    text: RunningTimerText,
    runner: RunningTimerRunner,
    discard: (suspend () -> Unit)?,
    stopAt: (suspend (Long) -> Unit)?,
    onCloseDiscard: () -> Unit,
    onCloseStopAt: () -> Unit,
) {
    if (discard != null) {
        RunningTimerDiscardDialog(
            text = text,
            busy = runner.busy == RunningTimerAction.Discard,
            onDismiss = onCloseDiscard,
            onConfirm = { runner.run(RunningTimerAction.Discard, discard, onCloseDiscard) },
        )
    }
    if (stopAt != null) {
        RunningTimerStopEarlierDialog(
            start = start,
            now = now,
            zone = zone,
            text = text,
            busy = runner.busy == RunningTimerAction.StopAt,
            error = runner.error.takeIf { runner.busy == null },
            onDismiss = onCloseStopAt,
            onSave = { at -> runner.run(RunningTimerAction.StopAt, { stopAt(at) }, onCloseStopAt) },
        )
    }
}

/** What the timer shows, resolved once. */
internal class RunningTimerState(
    val running: Boolean,
    val clock: String,
    val spoken: String,
    val label: String?,
    val long: Boolean,
    val busy: RunningTimerAction?,
    val anyBusy: Boolean,
    val pending: Boolean,
    val error: String?,
)

/** The actions the app offered, as callbacks of the timer's own buttons. */
internal class RunningTimerActions(
    val start: (() -> Unit)?,
    val stop: (() -> Unit)?,
    val stopEarlier: (() -> Unit)?,
    val discard: (() -> Unit)?,
    val open: (() -> Unit)?,
)

/** [now], or the current time refreshed every second while a timer runs. */
@Composable
private fun rememberRunningTimerNow(now: Long?, running: Boolean): Long {
    var current by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(now, running) {
        if (now != null) return@LaunchedEffect
        current = System.currentTimeMillis()
        while (running) {
            delay(MonarchRunningTimerTokens.SECOND_MILLIS - current % MonarchRunningTimerTokens.SECOND_MILLIS)
            current = System.currentTimeMillis()
        }
    }
    return now ?: current
}

/** "Timer running" or "Timer stopped" after a change, empty before the first change. */
@Composable
private fun rememberRunningTimerAnnouncement(running: Boolean, text: RunningTimerText): String {
    var previous by remember { mutableStateOf(running) }
    var announcement by remember { mutableStateOf("") }
    LaunchedEffect(running) {
        if (previous != running) {
            previous = running
            announcement = if (running) text.running else text.stopped
        }
    }
    return announcement
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RunningTimerPanel(
    state: RunningTimerState,
    actions: RunningTimerActions,
    text: RunningTimerText,
    startEnabled: Boolean,
    warnAfterMillis: Long?,
    fields: (@Composable ColumnScope.() -> Unit)?,
) {
    val tokens = MonarchRunningTimerTokens
    val colors = tokens.colors(MonarchTheme.colors, state.running, state.long, failed = false)
    Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        Column(
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = listOfNotNull(state.label, state.spoken).joinToString(", ")
                stateDescription = listOfNotNull(if (state.running) text.running else text.stopped, state.error)
                    .joinToString(", ")
            },
        ) {
            Text(
                text = state.clock,
                style = MonarchTabularNumberStyle.applyTo(MonarchTheme.typography.display),
                color = colors.clock,
            )
            state.label?.let {
                Text(text = it, style = MonarchTheme.typography.body, color = MonarchTheme.colors.textMuted)
            }
        }
        fields?.let { Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap), content = it) }
        RunningTimerPanelButtons(state, actions, text, startEnabled)
        if (state.long && warnAfterMillis != null) {
            Text(
                text = text.longRunning(
                    MonarchRunningTimerTokens.hoursMinutes(warnAfterMillis / tokens.MINUTE_MILLIS, true),
                ),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MonarchTheme.typography.caption,
                color = colors.warning,
            )
        }
        state.error?.let {
            Text(
                text = it,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MonarchTheme.typography.caption,
                color = colors.error,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RunningTimerPanelButtons(
    state: RunningTimerState,
    actions: RunningTimerActions,
    text: RunningTimerText,
    startEnabled: Boolean,
) {
    val tokens = MonarchRunningTimerTokens
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.SmallGap),
        verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
    ) {
        if (state.running) {
            actions.stop?.let {
                MonarchButton(
                    text = text.stop,
                    onClick = it,
                    variant = MonarchButtonVariant.Danger,
                    enabled = !state.anyBusy || state.busy == RunningTimerAction.Stop,
                    loading = state.busy == RunningTimerAction.Stop,
                )
            }
            actions.stopEarlier?.let {
                MonarchButton(text = text.stoppedEarlier, onClick = it, enabled = !state.anyBusy)
            }
            actions.discard?.let {
                MonarchButton(
                    text = text.discard,
                    onClick = it,
                    variant = MonarchButtonVariant.Ghost,
                    enabled = !state.anyBusy,
                )
            }
        } else {
            actions.start?.let {
                MonarchButton(
                    text = text.start,
                    onClick = it,
                    leadingIcon = MonarchDefaultIcons.Play,
                    variant = MonarchButtonVariant.Primary,
                    enabled = startEnabled && (!state.anyBusy || state.busy == RunningTimerAction.Start),
                    loading = state.busy == RunningTimerAction.Start || (state.pending && state.busy == null),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RunningTimerIndicator(
    state: RunningTimerState,
    actions: RunningTimerActions,
    text: RunningTimerText,
    menuIcon: (@Composable () -> Unit)?,
) {
    val tokens = MonarchRunningTimerTokens
    val colors = tokens.colors(MonarchTheme.colors, state.running, state.long, state.error != null)
    val shape = MonarchTheme.shapes.md
    val label = state.label ?: text.defaultLabel
    val open = actions.open?.takeIf { state.running }
    val clickable = if (open != null) {
        Modifier
            .monarchMinimumTouchTarget()
            .clickable(onClickLabel = text.open, role = Role.Button, onClick = open)
    } else {
        Modifier
    }
    // A FlowRow, so the stop and menu buttons wrap under the clock instead of shrinking at large font sizes.
    FlowRow(
        modifier = Modifier
            .clip(shape)
            .background(colors.container, shape)
            .padding(start = tokens.IndicatorPadding),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = clickable.semantics(mergeDescendants = true) {
                contentDescription = "$label, ${state.spoken}"
                stateDescription = listOfNotNull(if (state.running) text.running else text.stopped, state.error)
                    .joinToString(", ")
            },
            horizontalArrangement = Arrangement.spacedBy(tokens.SmallGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MonarchTheme.typography.label, color = colors.content, maxLines = 1)
            Text(
                text = state.clock,
                style = MonarchTabularNumberStyle.applyTo(MonarchTheme.typography.label),
                color = colors.content,
            )
        }
        if (state.running) {
            actions.stop?.let { stop ->
                MonarchButton(
                    onClick = stop,
                    modifier = Modifier.semantics { contentDescription = text.stopAndLog },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                    enabled = !state.anyBusy,
                    loading = state.busy == RunningTimerAction.Stop,
                ) { Text(text.stop) }
            }
            RunningTimerMenu(state, actions, text, menuIcon)
        }
    }
}

@Composable
private fun RunningTimerMenu(
    state: RunningTimerState,
    actions: RunningTimerActions,
    text: RunningTimerText,
    menuIcon: (@Composable () -> Unit)?,
) {
    if (actions.stopEarlier == null && actions.discard == null) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    val named = Modifier.semantics { contentDescription = text.moreActions }
    Box {
        if (menuIcon != null) {
            MonarchIconButton(
                label = text.moreActions,
                onClick = { expanded = true },
                enabled = !state.anyBusy,
                icon = menuIcon,
            )
        } else {
            MonarchButton(
                onClick = { expanded = true },
                modifier = named,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                enabled = !state.anyBusy,
            ) { Text(text.more) }
        }
        MonarchMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.stopEarlier?.let { stopEarlier ->
                MonarchMenuItem(
                    text = text.stoppedEarlier,
                    onClick = {
                        expanded = false
                        stopEarlier()
                    },
                )
            }
            actions.discard?.let { discard ->
                MonarchMenuItem(
                    text = text.discard,
                    onClick = {
                        expanded = false
                        discard()
                    },
                    destructive = true,
                )
            }
        }
    }
}

/** The built-in strings, each replaced by its label when the app gave one. */
internal class RunningTimerText(private val labels: MonarchRunningTimerLabels, private val resources: Resources) {
    private fun pick(override: String?, @StringRes id: Int): String = override ?: resources.getString(id)
    private fun format(@StringRes id: Int, value: String): String = resources.getString(id, value)

    val start: String get() = pick(labels.start, R.string.monarch_runningtimer_start)
    val stop: String get() = pick(labels.stop, R.string.monarch_runningtimer_stop)
    val stopAndLog: String get() = pick(labels.stopAndLog, R.string.monarch_runningtimer_stop_and_log)
    val more: String get() = pick(labels.more, R.string.monarch_runningtimer_more)
    val moreActions: String get() = pick(labels.moreActions, R.string.monarch_runningtimer_more_actions)
    val discard: String get() = pick(labels.discard, R.string.monarch_runningtimer_discard)
    val discardTitle: String get() = pick(labels.discardTitle, R.string.monarch_runningtimer_discard_title)
    val discardMessage: String get() = pick(labels.discardMessage, R.string.monarch_runningtimer_discard_message)
    val discardConfirm: String get() = pick(labels.discardConfirm, R.string.monarch_runningtimer_discard_confirm)
    val stoppedEarlier: String get() = pick(labels.stoppedEarlier, R.string.monarch_runningtimer_stopped_earlier)
    val stoppedEarlierTitle: String
        get() = pick(labels.stoppedEarlierTitle, R.string.monarch_runningtimer_stopped_earlier_title)
    val stoppedEarlierMessage: String
        get() = pick(labels.stoppedEarlierMessage, R.string.monarch_runningtimer_stopped_earlier_message)
    val save: String get() = pick(labels.save, R.string.monarch_runningtimer_save)
    val cancel: String get() = pick(labels.cancel, R.string.monarch_runningtimer_cancel)
    val stoppedAtFuture: String get() = pick(labels.stoppedAtFuture, R.string.monarch_runningtimer_stopped_at_future)
    val stoppedAtTooEarly: String
        get() = pick(labels.stoppedAtTooEarly, R.string.monarch_runningtimer_stopped_at_too_early)
    val failed: String get() = pick(labels.failed, R.string.monarch_runningtimer_failed)
    val running: String get() = pick(labels.running, R.string.monarch_runningtimer_running)
    val stopped: String get() = pick(labels.stopped, R.string.monarch_runningtimer_stopped)
    val defaultLabel: String get() = pick(labels.defaultLabel, R.string.monarch_runningtimer_default_label)
    val open: String get() = pick(labels.open, R.string.monarch_runningtimer_open)

    fun started(time: String): String =
        labels.started?.invoke(time) ?: format(R.string.monarch_runningtimer_started, time)

    fun workDuration(duration: String): String =
        labels.workDuration?.invoke(duration) ?: format(R.string.monarch_runningtimer_work_duration, duration)

    fun longRunning(duration: String): String =
        labels.longRunning?.invoke(duration) ?: format(R.string.monarch_runningtimer_long_running, duration)
}

@Composable
private fun runningTimerText(labels: MonarchRunningTimerLabels): RunningTimerText {
    val resources = LocalResources.current
    return remember(labels, resources) { RunningTimerText(labels, resources) }
}
