package dev.monarchui.components.intelligence.aiundobanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * An inline banner after the AI changed something on the person's behalf:
 * what changed, before and after, an Undo and a link to the audit log,
 * optionally for a limited time.
 *
 * Built by Monarch in the layout of `MonarchBanner`, which has one action and
 * a tone: this banner needs Undo, the audit link and Dismiss, and the AI
 * colour roles, which `MonarchTone` does not have.
 *
 * TalkBack reads "AI change:", the title and the description as one element,
 * and each change as one element, "Status changed from Open to Closed". Undo
 * runs [onUndo] in a coroutine of the banner and is busy while it runs. The
 * results ("Changes undone", the failure, the end of the time limit) are shown
 * and announced through a polite live region.
 *
 * With [durationMillis], Undo is offered for that long, extended to the time
 * the person asked for in the accessibility settings ("Time to take action").
 * The time does not run while the undo runs or while the changes disclosure
 * is open.
 *
 * @param title what the AI did, in the past tense: "AI changed 3 fields".
 * @param onUndo reverts the change. Runs in a coroutine of the banner; throw to report a failure.
 * @param modifier the modifier of the banner.
 * @param description one line of detail.
 * @param changes the fields that changed, before and after. More than three are put in a disclosure.
 * @param durationMillis how long Undo stays available, in milliseconds; `null` keeps it.
 * @param icon a glyph at the start in the AI strong colour, for example a `MonarchIcon` with a `null` content
 * description. Hidden from TalkBack. Drawn by default, as on the web; `null` draws none.
 * @param onAudit shows a "View in audit log" link that calls it.
 * @param onExpire called once when [durationMillis] runs out without an undo.
 * @param onDismiss shows a Dismiss button that calls it. The app removes the banner.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchAiUndoBanner(
    title: String,
    onUndo: suspend () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    changes: List<MonarchAiChange> = emptyList(),
    durationMillis: Long? = null,
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.Bot,
    onAudit: (() -> Unit)? = null,
    onExpire: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    labels: MonarchAiUndoBannerLabels = MonarchAiUndoBannerLabels(),
) {
    val tokens = MonarchAiUndoBannerTokens
    var state by remember { mutableStateOf(MonarchAiUndoBannerState.Active) }
    var changesOpen by remember { mutableStateOf(false) }
    AiUndoBannerExpiry(durationMillis, tokens.counting(durationMillis, state, changesOpen), onExpire) {
        state = MonarchAiUndoBannerState.Expired
    }
    val scope = rememberCoroutineScope()
    val currentOnUndo by rememberUpdatedState(onUndo)
    val runUndo: () -> Unit = {
        if (tokens.canUndo(state)) {
            state = MonarchAiUndoBannerState.Undoing
            scope.launch {
                val outcome = runCatching { currentOnUndo() }
                // An undo cancelled with the banner is not a failure.
                (outcome.exceptionOrNull() as? CancellationException)?.let { throw it }
                state = if (outcome.isSuccess) MonarchAiUndoBannerState.Undone else MonarchAiUndoBannerState.Failed
            }
        }
    }
    val colors = tokens.colors(state, MonarchTheme.colors)
    val shape = MonarchShapeDefaults.Container
    Row(
        modifier = modifier
            .background(colors.container, shape)
            .border(MonarchShapeDefaults.BorderWidth, colors.border, shape)
            .padding(tokens.Padding),
        horizontalArrangement = Arrangement.spacedBy(tokens.ContentGap),
    ) {
        if (icon != null) {
            Box(Modifier.clearAndSetSemantics { }) {
                CompositionLocalProvider(LocalContentColor provides colors.icon, content = icon)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.ContentGap)) {
            AiUndoBannerHeader(title, description, labels)
            if (changes.isNotEmpty()) {
                MonarchAiUndoBannerChanges(changes, changesOpen, { changesOpen = it }, labels)
            }
            AiUndoBannerStatus(state, colors.status, labels)
            MonarchAiUndoBannerActions(
                state = state,
                onUndo = runUndo,
                onAudit = onAudit,
                onDismiss = onDismiss,
                labels = labels,
            )
        }
    }
}

/** Runs the time limit while [counting], keeping the time left across pauses, and expires once. */
@Composable
private fun AiUndoBannerExpiry(
    durationMillis: Long?,
    counting: Boolean,
    onExpire: (() -> Unit)?,
    expire: () -> Unit,
) {
    val accessibility = LocalAccessibilityManager.current
    var left by remember(durationMillis) {
        val limit = durationMillis ?: 0L
        val recommended = accessibility?.calculateRecommendedTimeoutMillis(
            limit,
            containsIcons = false,
            containsText = true,
            containsControls = true,
        ) ?: limit
        mutableLongStateOf(recommended)
    }
    val currentOnExpire by rememberUpdatedState(onExpire)
    val currentExpire by rememberUpdatedState(expire)
    LaunchedEffect(counting, durationMillis) {
        if (!counting) return@LaunchedEffect
        while (left > 0) {
            val step = minOf(left, MonarchAiUndoBannerTokens.TICK_MILLIS)
            delay(step)
            left -= step
        }
        currentExpire()
        currentOnExpire?.invoke()
    }
}

@Composable
private fun AiUndoBannerHeader(title: String, description: String?, labels: MonarchAiUndoBannerLabels) {
    val colors = MonarchTheme.colors
    val prefix = monarchString(labels.aiPrefix, R.string.monarch_aiundobanner_ai_prefix)
    val spoken = listOfNotNull("$prefix $title", description?.takeIf { it.isNotBlank() }).joinToString(", ")
    Column(
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
        verticalArrangement = Arrangement.spacedBy(MonarchAiUndoBannerTokens.LineGap),
    ) {
        Text(text = title, style = MonarchTheme.typography.bodyStrong, color = colors.textDefault)
        if (!description.isNullOrBlank()) {
            Text(text = description, style = MonarchTheme.typography.body, color = colors.textMuted)
        }
    }
}

/** The status line: always present as a polite live region, so each result is announced. */
@Composable
private fun AiUndoBannerStatus(
    state: MonarchAiUndoBannerState,
    color: Color,
    labels: MonarchAiUndoBannerLabels,
) {
    val status = when (state) {
        MonarchAiUndoBannerState.Undone -> monarchString(labels.undone, R.string.monarch_aiundobanner_undone)
        MonarchAiUndoBannerState.Failed -> monarchString(labels.undoFailed, R.string.monarch_aiundobanner_undo_failed)
        MonarchAiUndoBannerState.Expired -> monarchString(labels.expired, R.string.monarch_aiundobanner_expired)
        else -> null
    }
    Box(
        Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            if (status != null) contentDescription = status
        },
    ) {
        if (status != null) {
            Text(
                text = status,
                modifier = Modifier.clearAndSetSemantics { },
                style = MonarchTheme.typography.caption,
                color = color,
            )
        }
    }
}
