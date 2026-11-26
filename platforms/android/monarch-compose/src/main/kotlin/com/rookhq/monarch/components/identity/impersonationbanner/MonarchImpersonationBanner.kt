package com.rookhq.monarch.components.identity.impersonationbanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchRelativeClock
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * A persistent bar shown while a support or admin person acts as another
 * person, with who, since when, why, and one way out: Stop.
 *
 * Built by Monarch: Material 3 has no banner. It uses the inverse surface
 * colours and a solid warning edge at the start, so it looks unlike every
 * other banner, and it has no dismiss button. Place it under the top app
 * bar, for example in the `topBar` slot of a `Scaffold` after the
 * `TopAppBar`, so it stays above scrolling content. The app removes it when
 * the session ends.
 *
 * The text is one TalkBack element. The main line is announced once through
 * a polite live region when the banner appears. Stop is a `MonarchButton`
 * named "Stop viewing as" and the name; it is busy while [onStop] runs and
 * ignores taps. When [onStop] throws, the failure is shown under the text and
 * announced, and the banner stays.
 *
 * @param name the person being viewed as, in bold in the main line.
 * @param onStop ends the session. Runs in a coroutine of the banner; throwing shows [labels]' stop error.
 * @param modifier the modifier of the banner.
 * @param detail a second identifier, such as their email, in brackets after the name in the muted inverse
 * colour.
 * @param reason why the session started, such as a ticket number, after "Reason:".
 * @param startedAtMillis when the session started, in epoch milliseconds (UTC), shown as "Started 5 minutes
 * ago" and refreshed every minute.
 * @param stopping the busy state of Stop when the app tracks the stop itself; `null` tracks it from [onStop].
 * @param icon an icon at the start in the warning colour, for example an eye as a `MonarchIcon` with a `null`
 * content description. Monarch bundles no icons.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchImpersonationBanner(
    name: String,
    onStop: suspend () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    reason: String? = null,
    startedAtMillis: Long? = null,
    stopping: Boolean? = null,
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.Eye,
    labels: MonarchImpersonationBannerLabels = MonarchImpersonationBannerLabels(),
) {
    val tokens = MonarchImpersonationBannerTokens
    val colors = tokens.colors(MonarchTheme.colors)
    val scope = rememberCoroutineScope()
    val currentOnStop by rememberUpdatedState(onStop)
    var running by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val busy = tokens.busy(stopping, running)
    val stop: () -> Unit = {
        if (!busy) {
            running = true
            failed = false
            scope.launch {
                val outcome = runCatching { currentOnStop() }
                running = false
                // A stop cancelled with the banner is not a failure.
                (outcome.exceptionOrNull() as? CancellationException)?.let { throw it }
                failed = outcome.isFailure
            }
        }
    }
    val mainLine = mainLine(name, detail, labels, colors.muted)
    val stacked = tokens.stacked(LocalDensity.current.fontScale)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(colors.container),
    ) {
        Box(Modifier.width(tokens.EdgeWidth).fillMaxHeight().background(colors.edge))
        val content = Modifier.padding(horizontal = tokens.HorizontalPadding, vertical = tokens.VerticalPadding)
        if (stacked) {
            Column(content.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.ContentGap)) {
                BannerText(mainLine, reason, startedAtMillis, failed, icon, labels, Modifier.fillMaxWidth())
                StopButton(name, busy, stop, labels, Modifier.fillMaxWidth())
            }
        } else {
            Row(
                content.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(tokens.ContentGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BannerText(mainLine, reason, startedAtMillis, failed, icon, labels, Modifier.weight(1f))
                StopButton(name, busy, stop, labels, Modifier)
            }
        }
    }
}

@Composable
private fun BannerText(
    mainLine: AnnotatedString,
    reason: String?,
    startedAtMillis: Long?,
    failed: Boolean,
    icon: (@Composable () -> Unit)?,
    labels: MonarchImpersonationBannerLabels,
    modifier: Modifier,
) {
    val tokens = MonarchImpersonationBannerTokens
    val colors = tokens.colors(MonarchTheme.colors)
    val meta = tokens.metaLine(startedLine(startedAtMillis, labels), reasonLine(reason, labels))
    val error = monarchString(labels.stopError, R.string.monarch_impersonationbanner_stop_error)
    // Empty at first and filled after the first frame, so TalkBack announces the main line once.
    var announced by remember { mutableStateOf("") }
    LaunchedEffect(mainLine.text) { announced = mainLine.text }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(tokens.ContentGap)) {
        if (icon != null) {
            CompositionLocalProvider(LocalContentColor provides colors.edge) {
                Box(Modifier.clearAndSetSemantics { }) { icon() }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) { },
                verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
            ) {
                Text(text = mainLine, style = MonarchTheme.typography.body, color = colors.text)
                if (meta != null) Text(text = meta, style = MonarchTheme.typography.caption, color = colors.muted)
            }
            if (failed) Text(text = error, style = MonarchTheme.typography.caption, color = colors.error)
            Box(
                Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                    val spoken = if (failed) error else announced
                    if (spoken.isNotEmpty()) contentDescription = spoken
                },
            )
        }
    }
}

@Composable
private fun StopButton(
    name: String,
    busy: Boolean,
    onClick: () -> Unit,
    labels: MonarchImpersonationBannerLabels,
    modifier: Modifier,
) {
    val spoken = labels.stopLabel?.invoke(name) ?: stringResource(R.string.monarch_impersonationbanner_stop_label, name)
    MonarchButton(
        text = monarchString(labels.stop, R.string.monarch_impersonationbanner_stop),
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = spoken },
        variant = MonarchButtonVariant.Warning,
        size = MonarchButtonSize.Sm,
        loading = busy,
    )
}

/** "You are viewing as **Sam Lee** (sam@example.com)". */
@Composable
private fun mainLine(
    name: String,
    detail: String?,
    labels: MonarchImpersonationBannerLabels,
    muted: Color,
): AnnotatedString {
    val format = stringResource(R.string.monarch_impersonationbanner_viewing_as)
    val custom = labels.viewingAs?.invoke(name)
    return buildAnnotatedString {
        if (custom != null) {
            append(custom)
        } else {
            val parts = format.split("%1\$s", limit = 2)
            append(parts.first())
            withStyle(SpanStyle(fontWeight = FontWeight(MonarchTokens.FontWeight.semibold))) { append(name) }
            if (parts.size > 1) append(parts[1])
        }
        if (!detail.isNullOrBlank()) {
            append(" ")
            withStyle(SpanStyle(color = muted)) { append("($detail)") }
        }
    }
}

@Composable
private fun startedLine(startedAtMillis: Long?, labels: MonarchImpersonationBannerLabels): String? {
    if (startedAtMillis == null) return null
    val now = rememberMonarchRelativeClock(listOf(startedAtMillis))
    val started = monarchString(labels.started, R.string.monarch_impersonationbanner_started)
    return "$started ${MonarchImpersonationBannerTokens.relativeTime(startedAtMillis, now)}"
}

@Composable
private fun reasonLine(reason: String?, labels: MonarchImpersonationBannerLabels): String? {
    if (reason.isNullOrBlank()) return null
    return "${monarchString(labels.reason, R.string.monarch_impersonationbanner_reason)} $reason"
}
