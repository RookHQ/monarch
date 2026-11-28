package com.rookhq.monarch.components.feedback.savestatusindicator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchRelativeClock
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A small inline status for saving: "Saving…", "Saved · 2 minutes ago" or
 * "Couldn't save" with a Retry button.
 *
 * Built by Monarch from a `Row` of a mark, a `Text` and a ghost
 * `MonarchButton`: Material 3 has no equivalent. The mark is a small progress
 * ring while saving (a still dot when animations are off) and a dot in the
 * status colour otherwise; [icon] replaces the dot. The words always say the
 * status, so the colour is never the only signal. The outcome (Saved, or the
 * error) is announced through a polite live region; "Saving…" only with
 * [announceSaving], so frequent autosaves stay quiet. Nothing is announced on
 * the first composition.
 *
 * @param status where saving stands. `Idle` draws nothing.
 * @param modifier the modifier of the indicator.
 * @param announceSaving also announces "Saving…".
 * @param savedAt when the last save finished, in epoch milliseconds (UTC). Shown as relative time after "Saved".
 * @param errorMessage replaces "Couldn't save" with the reason, for example "You are offline".
 * @param icon replaces the dot for a status, for example a check mark `MonarchIcon` of size `Sm` with a `null`
 * content description for `Saved`. Called for every status except `Idle` and `Saving`. Monarch bundles no icons.
 * @param onRetry adds a Retry button to the error status.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchSaveStatusIndicator(
    status: MonarchSaveStatus,
    modifier: Modifier = Modifier,
    announceSaving: Boolean = false,
    savedAt: Long? = null,
    errorMessage: String? = null,
    icon: (@Composable (MonarchSaveStatus) -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    labels: MonarchSaveStatusIndicatorLabels = MonarchSaveStatusIndicatorLabels(),
) {
    val colors = MonarchTheme.colors
    val errorText = errorMessage ?: monarchString(labels.error, R.string.monarch_savestatusindicator_error)
    val savedText = monarchString(labels.saved, R.string.monarch_savestatusindicator_saved)
    val savingText = monarchString(labels.saving, R.string.monarch_savestatusindicator_saving)
    val text = when (status) {
        MonarchSaveStatus.Idle -> null
        MonarchSaveStatus.Unsaved -> monarchString(labels.unsaved, R.string.monarch_savestatusindicator_unsaved)
        MonarchSaveStatus.Saving -> savingText
        MonarchSaveStatus.Saved -> savedText
        MonarchSaveStatus.Error -> errorText
    }
    val now = minuteClock(
        running = MonarchSaveStatusIndicatorTokens.showsTime(status, savedAt),
        times = listOf(savedAt),
    )
    val time = savedAt?.takeIf { MonarchSaveStatusIndicatorTokens.showsTime(status, it) }
        ?.let { MonarchSaveStatusIndicatorTokens.relativeTime(it, now) }
    val separator = stringResource(R.string.monarch_savestatusindicator_spoken_separator)
    val spoken = listOfNotNull(text, time).joinToString(separator)
    val announcement = saveAnnouncement(status, announceSaving, savedText, savingText, errorText)
    Box(modifier = modifier) {
        if (text != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchSaveStatusIndicatorTokens.Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false).clearAndSetSemantics { contentDescription = spoken },
                    horizontalArrangement = Arrangement.spacedBy(MonarchSaveStatusIndicatorTokens.Gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SaveMark(status, MonarchSaveStatusIndicatorTokens.markColor(status, colors), icon)
                    Text(
                        text = text,
                        style = MonarchTheme.typography.caption,
                        color = MonarchSaveStatusIndicatorTokens.textColor(status, colors),
                    )
                    if (time != null) {
                        Text(
                            text = MonarchSaveStatusIndicatorTokens.SEPARATOR,
                            style = MonarchTheme.typography.caption,
                            color = colors.textSubtle,
                        )
                        Text(text = time, style = MonarchTheme.typography.caption, color = colors.textMuted)
                    }
                }
                if (status == MonarchSaveStatus.Error && onRetry != null) {
                    MonarchButton(
                        text = monarchString(labels.retry, R.string.monarch_savestatusindicator_retry),
                        onClick = onRetry,
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                    )
                }
            }
        }
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

@Composable
private fun SaveMark(status: MonarchSaveStatus, color: Color?, icon: (@Composable (MonarchSaveStatus) -> Unit)?) {
    if (color == null) return
    val scale = LocalDensity.current.fontScale
    when {
        MonarchSaveStatusIndicatorTokens.showsSpinner(status, MonarchTheme.motion.reducedMotion) ->
            CircularProgressIndicator(
                modifier = Modifier.size(MonarchSaveStatusIndicatorTokens.SpinnerSize * scale),
                color = color,
                strokeWidth = MonarchSaveStatusIndicatorTokens.SpinnerStroke,
                trackColor = Color.Transparent,
            )
        status == MonarchSaveStatus.Saving ->
            // Reduced motion: the ring stands still, an arc as on the web's still frame, not a dot.
            Canvas(Modifier.size(MonarchSaveStatusIndicatorTokens.SpinnerSize * scale)) {
                val stroke = MonarchSaveStatusIndicatorTokens.SpinnerStroke.toPx()
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    ),
                )
            }
        icon != null && status != MonarchSaveStatus.Unsaved ->
            CompositionLocalProvider(LocalContentColor provides color) { icon(status) }
        else -> Canvas(Modifier.size(MonarchSaveStatusIndicatorTokens.MarkSize * scale)) { drawCircle(color) }
    }
}

/**
 * The text of the polite live region after a change of status: "Saved", the
 * error, and "Saving…" when asked. Emptied for a frame first, so the same
 * words are announced again after another save.
 */
@Composable
private fun saveAnnouncement(
    status: MonarchSaveStatus,
    announceSaving: Boolean,
    saved: String,
    saving: String,
    error: String,
): String {
    var announcement by remember { mutableStateOf("") }
    val previous = remember { mutableStateOf(status) }
    LaunchedEffect(status) {
        if (previous.value == status) return@LaunchedEffect
        previous.value = status
        val next = when (status) {
            MonarchSaveStatus.Saved -> saved
            MonarchSaveStatus.Error -> error
            MonarchSaveStatus.Saving -> saving.takeIf { announceSaving }
            else -> null
        } ?: return@LaunchedEffect
        announcement = ""
        withFrameNanos { }
        announcement = next
    }
    return announcement
}

/** The current time, redrawn as often as the freshest of [times] needs while [running], so relative times stay true. */
@Composable
private fun minuteClock(running: Boolean, times: List<Long?>): Long =
    rememberMonarchRelativeClock(times, running = running)
