package com.rookhq.monarch.components.workflow.slaindicator

import android.icu.text.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import com.rookhq.monarch.R
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay
import java.util.Date

/**
 * A service-level target at a glance: on track, at risk, breached, paused,
 * met or not applicable, with the time left or overdue, as a chip or as text.
 *
 * Built by Monarch: `MonarchBadge` has no bordered tinted fill or text form,
 * and a running clock is not the format a deadline needs. While the target
 * runs and [live] is on, the indicator redraws at each whole minute and
 * exactly when it becomes at risk or breached, and a running target whose
 * due time has passed is shown as breached.
 *
 * TalkBack reads one element: the target name and the time left or overdue
 * in words ("First response, 2 hours, 15 minutes left"), or the state name;
 * its state description adds the state name after a countdown, the due date
 * and time, and a note about an earlier breach. Every state has its own
 * text, so it never relies on colour or on the icon.
 *
 * Times are epoch milliseconds in UTC, as in the Material 3 `DatePickerState`.
 *
 * @param state the state the server reported. A running state (on track, at risk) is derived again from
 * [dueAt] and the clock.
 * @param modifier the modifier of the indicator.
 * @param variant `Chip`, a compact rounded rectangle, or `Text`, an icon and text without a surface.
 * @param dueAt when the target is due, in epoch milliseconds. It drives the countdown ("2h 15m left") and
 * the overdue time ("Overdue 40m").
 * @param warnBeforeMillis turns an on-track target at risk once it is due within this many milliseconds.
 * By default the state is trusted.
 * @param earlierBreach an earlier target of the same item was breached. TalkBack says so.
 * @param showNotApplicable writes a missing target as "No SLA" and not as a dash.
 * @param live whether a running target with a due time follows the clock by itself.
 * @param announceChanges announces a change of the shown state through a polite live region. For one
 * prominent indicator, not for every row of a list.
 * @param now a fixed reference time in epoch milliseconds, which also stops the clock.
 * @param target the name of the target, such as "First response". Read by TalkBack, not shown.
 * @param icon draws the icon of a state, for example a clock, a warning or a check `MonarchIcon` with a
 * `null` content description. It takes the content colour of the indicator. Not drawn for not applicable.
 * @param labels replacements for the built-in strings and the duration format.
 * @param onStateChange called when the shown state changes after the indicator appeared, for example when
 * a countdown runs out.
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
public fun MonarchSlaIndicator(
    state: MonarchSlaIndicatorState,
    modifier: Modifier = Modifier,
    variant: MonarchSlaIndicatorVariant = MonarchSlaIndicatorVariant.Chip,
    dueAt: Long? = null,
    warnBeforeMillis: Long? = null,
    earlierBreach: Boolean = false,
    showNotApplicable: Boolean = false,
    live: Boolean = true,
    announceChanges: Boolean = false,
    now: Long? = null,
    target: String? = null,
    icon: (@Composable (MonarchSlaIndicatorState) -> Unit)? = null,
    labels: MonarchSlaIndicatorLabels = MonarchSlaIndicatorLabels(),
    onStateChange: ((MonarchSlaIndicatorState) -> Unit)? = null,
) {
    val tokens = MonarchSlaIndicatorTokens
    val ticking = live && now == null && dueAt != null && tokens.isRunning(state)
    val clock = slaClock(now, ticking, dueAt, warnBeforeMillis)
    val shown = tokens.resolve(state, dueAt, clock, warnBeforeMillis)
    val text = slaTexts(shown, dueAt, clock, showNotApplicable, earlierBreach, labels)

    var announcement by remember { mutableStateOf("") }
    val previous = remember { mutableStateOf(shown) }
    val changed by rememberUpdatedState(slaChanged(text.stateName, target, labels))
    val onChange by rememberUpdatedState(onStateChange)
    LaunchedEffect(shown) {
        if (previous.value != shown) {
            previous.value = shown
            onChange?.invoke(shown)
            announcement = changed
        }
    }

    val colors = tokens.colors(shown, variant, MonarchTheme.colors)
    val chip = variant == MonarchSlaIndicatorVariant.Chip
    val name = listOfNotNull(target?.takeIf { it.isNotBlank() }, text.spoken).joinToString(", ")
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .then(
                    if (chip) {
                        Modifier
                            .monarchSurface(colors, MonarchTheme.shapes.sm)
                            .heightIn(min = tokens.MinHeight)
                            .padding(horizontal = tokens.HorizontalPadding, vertical = tokens.VerticalPadding)
                    } else {
                        Modifier
                    },
                )
                // One element: TalkBack reads the content description in place of the short visible text.
                .semantics(mergeDescendants = true) {
                    contentDescription = name
                    if (text.state.isNotEmpty()) stateDescription = text.state
                },
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null && shown != MonarchSlaIndicatorState.NotApplicable) {
                CompositionLocalProvider(LocalContentColor provides colors.content) { icon(shown) }
            }
            Text(
                text = text.visible,
                style = MonarchTheme.typography.caption.copy(fontFeatureSettings = TabularFigures),
                fontWeight = FontWeight(tokens.weight(shown)),
                color = colors.content,
            )
        }
        if (announceChanges) {
            // Always present while announcing, so each change is read once as a change of this region.
            MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
        }
    }
}

/** What the indicator shows and what TalkBack reads. */
private class SlaTexts(val visible: String, val spoken: String, val state: String, val stateName: String)

@Suppress("LongParameterList")
@Composable
private fun slaTexts(
    shown: MonarchSlaIndicatorState,
    dueAt: Long?,
    clock: Long,
    showNotApplicable: Boolean,
    earlierBreach: Boolean,
    labels: MonarchSlaIndicatorLabels,
): SlaTexts {
    val locale = LocalConfiguration.current.locales[0]
    val under = stringResource(R.string.monarch_slaindicator_less_than_a_minute)
    val underSpoken = stringResource(R.string.monarch_slaindicator_less_than_a_minute_spoken)
    val format = remember(locale, under, underSpoken) { SlaDurationFormat(locale, under, underSpoken) }
    val visibleDuration = labels.duration ?: format::visible
    val spokenDuration = labels.duration ?: format::spoken
    val remainingTemplate = stringResource(R.string.monarch_slaindicator_remaining)
    val overdueTemplate = stringResource(R.string.monarch_slaindicator_overdue)
    val remainingText: (String) -> String = labels.remaining ?: { remainingTemplate.format(locale, it) }
    val overdueText: (String) -> String = labels.overdue ?: { overdueTemplate.format(locale, it) }

    val stateName = slaStateName(shown, labels)
    val (visible, spoken) = when {
        shown == MonarchSlaIndicatorState.NotApplicable -> {
            val mark = monarchString(labels.notApplicableMark, R.string.monarch_slaindicator_not_applicable_mark)
            (if (showNotApplicable) stateName else mark) to stateName
        }
        shown == MonarchSlaIndicatorState.Breached && dueAt != null && dueAt < clock ->
            overdueText(visibleDuration(clock - dueAt)) to overdueText(spokenDuration(clock - dueAt))
        MonarchSlaIndicatorTokens.isRunning(shown) && dueAt != null ->
            remainingText(visibleDuration(dueAt - clock)) to remainingText(spokenDuration(dueAt - clock))
        else -> stateName to stateName
    }
    val countdown = spoken != stateName
    val state = slaStateDescription(shown, dueAt, earlierBreach, labels, stateName.takeIf { countdown })
    return SlaTexts(visible = visible, spoken = spoken, state = state, stateName = stateName)
}

/** The state description: the state name after a countdown, the due time, an earlier breach. */
@Composable
private fun slaStateDescription(
    shown: MonarchSlaIndicatorState,
    dueAt: Long?,
    earlierBreach: Boolean,
    labels: MonarchSlaIndicatorLabels,
    stateName: String?,
): String {
    val locale = LocalConfiguration.current.locales[0]
    val dueTemplate = stringResource(R.string.monarch_slaindicator_due)
    val showsDue = shown != MonarchSlaIndicatorState.Met && shown != MonarchSlaIndicatorState.NotApplicable
    val dueText = if (showsDue && dueAt != null) {
        val formatted = remember(locale, dueAt) {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(dueAt))
        }
        labels.due?.invoke(formatted) ?: dueTemplate.format(locale, formatted)
    } else {
        null
    }
    val earlier = if (earlierBreach && shown != MonarchSlaIndicatorState.Breached) {
        monarchString(labels.earlierBreach, R.string.monarch_slaindicator_earlier_breach)
    } else {
        null
    }
    return listOfNotNull(stateName, dueText, earlier).joinToString(", ")
}

@Composable
private fun slaStateName(state: MonarchSlaIndicatorState, labels: MonarchSlaIndicatorLabels): String = when (state) {
    MonarchSlaIndicatorState.OnTrack -> monarchString(labels.onTrack, R.string.monarch_slaindicator_on_track)
    MonarchSlaIndicatorState.AtRisk -> monarchString(labels.atRisk, R.string.monarch_slaindicator_at_risk)
    MonarchSlaIndicatorState.Breached -> monarchString(labels.breached, R.string.monarch_slaindicator_breached)
    MonarchSlaIndicatorState.Paused -> monarchString(labels.paused, R.string.monarch_slaindicator_paused)
    MonarchSlaIndicatorState.Met -> monarchString(labels.met, R.string.monarch_slaindicator_met)
    MonarchSlaIndicatorState.NotApplicable ->
        monarchString(labels.notApplicable, R.string.monarch_slaindicator_not_applicable)
}

@Composable
private fun slaChanged(stateName: String, target: String?, labels: MonarchSlaIndicatorLabels): String {
    val custom = labels.changed
    return when {
        custom != null -> custom(stateName, target)
        target.isNullOrBlank() -> stringResource(R.string.monarch_slaindicator_changed_no_target, stateName)
        else -> stringResource(R.string.monarch_slaindicator_changed, target, stateName)
    }
}

/** The reference time: [fixed], or the clock, advanced while [ticking] at the moments the text can change. */
@Composable
private fun slaClock(fixed: Long?, ticking: Boolean, dueAt: Long?, warnBeforeMillis: Long?): Long {
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(ticking, dueAt, warnBeforeMillis) {
        clock = System.currentTimeMillis()
        while (ticking && dueAt != null) {
            delay(MonarchSlaIndicatorTokens.nextTick(clock, dueAt, warnBeforeMillis))
            clock = System.currentTimeMillis()
        }
    }
    return fixed ?: clock
}

private const val TabularFigures = "tnum"
