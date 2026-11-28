package dev.monarchui.components.collaboration.recordingindicator

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens
import kotlinx.coroutines.delay

/**
 * A "REC" badge with the recorded time, shown while a call or a session is
 * recorded.
 *
 * Built by Monarch: Material 3 has no equivalent. The badge is one TalkBack
 * element read as "Recording" (or "Recording, Paused") with the time in words
 * as its state. Starting, pausing, resuming and stopping are each announced
 * once through a polite live region; the advancing time is not announced. The
 * state is also shown as text and as a dot or a square, never by colour
 * alone. The dot does not pulse when the user has turned animations off.
 *
 * Times are epoch milliseconds in UTC, as in the Material 3 `DatePickerState`.
 *
 * @param recording whether recording is on. While `false` nothing is drawn, so the stop is announced.
 * @param modifier the modifier of the recording indicator.
 * @param variant `Subtle` on the app's surfaces, `Solid` over video.
 * @param paused recording is paused: the badge says so and the time stops.
 * @param since when recording started, in epoch milliseconds. The time advances from it every second.
 * @param elapsedMillis the recorded time, when the app counts it itself (for example without pauses).
 * Takes precedence over [since] and does not advance by itself.
 * @param showElapsed shows the time. `null` shows it whenever [since] or [elapsedMillis] is given.
 * @param onStop stops recording. Adds a stop button, named "Stop recording".
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchRecordingIndicator(
    recording: Boolean,
    modifier: Modifier = Modifier,
    variant: MonarchRecordingIndicatorVariant = MonarchRecordingIndicatorVariant.Subtle,
    paused: Boolean = false,
    since: Long? = null,
    elapsedMillis: Long? = null,
    showElapsed: Boolean? = null,
    onStop: (() -> Unit)? = null,
    labels: MonarchRecordingIndicatorLabels = MonarchRecordingIndicatorLabels(),
) {
    val tokens = MonarchRecordingIndicatorTokens
    val now = recordingNow(ticking = tokens.ticks(recording, paused, since, elapsedMillis))
    val announcement = when (recordingChange(recording, paused)) {
        RecordingChange.Started -> monarchString(labels.started, R.string.monarch_recordingindicator_started)
        RecordingChange.Stopped -> monarchString(labels.stopped, R.string.monarch_recordingindicator_stopped)
        RecordingChange.Paused ->
            monarchString(labels.pausedAnnouncement, R.string.monarch_recordingindicator_paused_announcement)
        RecordingChange.Resumed -> monarchString(labels.resumed, R.string.monarch_recordingindicator_resumed)
        null -> ""
    }
    Box(modifier = modifier) {
        if (recording) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RecordingBadge(
                    variant = variant,
                    paused = paused,
                    millis = tokens.recordedMillis(since, elapsedMillis, now),
                    showsTime = tokens.showsTime(showElapsed, since, elapsedMillis),
                    labels = labels,
                )
                if (onStop != null) {
                    StopButton(
                        onStop = onStop,
                        label = monarchString(labels.stop, R.string.monarch_recordingindicator_stop),
                    )
                }
            }
        }
        // Always present, so each change is announced once as a change of this live region.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

@Composable
private fun RecordingBadge(
    variant: MonarchRecordingIndicatorVariant,
    paused: Boolean,
    millis: Long,
    showsTime: Boolean,
    labels: MonarchRecordingIndicatorLabels,
) {
    val tokens = MonarchRecordingIndicatorTokens
    val colors = tokens.colors(variant, MonarchTheme.colors)
    val recordingText = monarchString(labels.recording, R.string.monarch_recordingindicator_recording)
    val pausedText = monarchString(labels.paused, R.string.monarch_recordingindicator_paused)
    val spoken = if (paused) {
        stringResource(R.string.monarch_recordingindicator_spoken_paused, recordingText, pausedText)
    } else {
        recordingText
    }
    val spokenTime = tokens.spokenTime(millis)
    val markSize = tokens.MarkSize * LocalDensity.current.fontScale
    val style = MonarchTheme.typography.caption
    Row(
        modifier = Modifier
            .monarchSurface(colors, MonarchShapeDefaults.Mark)
            .heightIn(min = tokens.MinHeight)
            .padding(horizontal = tokens.HorizontalPadding, vertical = tokens.VerticalPadding)
            .clearAndSetSemantics {
                contentDescription = spoken
                if (showsTime) stateDescription = spokenTime
            },
        horizontalArrangement = Arrangement.spacedBy(tokens.ContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (paused) {
            Box(modifier = Modifier.size(markSize).background(colors.content))
        } else {
            RecordingDot(color = tokens.dotColor(variant, MonarchTheme.colors), size = markSize)
        }
        Text(
            text = if (paused) pausedText else monarchString(labels.rec, R.string.monarch_recordingindicator_rec),
            style = style,
            fontWeight = FontWeight(tokens.LABEL_WEIGHT),
            letterSpacing = MonarchTokens.LetterSpacing.caps.em,
            color = colors.content,
        )
        if (showsTime) {
            Text(
                text = tokens.clock(millis),
                style = style.copy(fontFeatureSettings = TabularFigures),
                fontWeight = FontWeight(tokens.TIME_WEIGHT),
                color = colors.content,
            )
        }
    }
}

/** The recording dot. It pulses unless animations are off; the pulse is read in the draw phase only. */
@Composable
private fun RecordingDot(color: Color, size: Dp) {
    if (MonarchTheme.motion.reducedMotion) {
        Box(modifier = Modifier.size(size).drawBehind { drawCircle(color) })
        return
    }
    val transition = rememberInfiniteTransition(label = "recording dot")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = MonarchRecordingIndicatorTokens.PULSE_LOW_ALPHA,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = MonarchRecordingIndicatorTokens.PULSE_DURATION_MILLIS / 2,
                easing = MonarchTheme.motion.standard,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    // A transient fade of the animation, not a dimmed colour role.
    Box(modifier = Modifier.size(size).drawBehind { drawCircle(color = color, alpha = alpha) })
}

/** The outlined stop button: a secondary `MonarchIconButton` with a square in the strong danger colour. */
@Composable
private fun StopButton(onStop: () -> Unit, label: String) {
    val colors = MonarchRecordingIndicatorTokens.stopColors(MonarchTheme.colors)
    MonarchIconButton(
        label = label,
        onClick = onStop,
        variant = MonarchButtonVariant.Secondary,
        shape = MonarchShapeDefaults.Mark,
    ) {
        Box(
            modifier = Modifier
                .size(MonarchRecordingIndicatorTokens.StopGlyphSize)
                .background(colors.content, MonarchTheme.shapes.xs),
        )
    }
}

/** The current time, advanced every second while [ticking]. */
@Composable
private fun recordingNow(ticking: Boolean): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(ticking) {
        now = System.currentTimeMillis()
        while (ticking) {
            val second = MonarchRecordingIndicatorTokens.MILLIS_PER_SECOND
            delay(second - now % second)
            now = System.currentTimeMillis()
        }
    }
    return now
}

/**
 * The last change of state since the first composition, which is never
 * announced. Worked out during composition and remembered once committed.
 */
@Composable
private fun recordingChange(recording: Boolean, paused: Boolean): RecordingChange? {
    val previous = remember { PreviousRecordingState() }
    val change = if (previous.known) {
        MonarchRecordingIndicatorTokens.change(previous.recording, previous.paused, recording, paused)
            ?: previous.change
    } else {
        null
    }
    SideEffect {
        previous.known = true
        previous.recording = recording
        previous.paused = paused
        previous.change = change
    }
    return change
}

/** The state seen last, kept outside snapshot state because it is only read on the next composition. */
private class PreviousRecordingState {
    var known: Boolean = false
    var recording: Boolean = false
    var paused: Boolean = false
    var change: RecordingChange? = null
}

private const val TabularFigures = "tnum"
