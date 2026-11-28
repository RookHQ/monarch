package dev.monarchui.components.technical.domainprogress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNotice
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.MonarchRelativeTime
import dev.monarchui.internal.rememberMonarchRelativeClock
import dev.monarchui.theme.MonarchTheme

/**
 * Stage-by-stage progress for connecting a custom domain (DNS record found, HTTPS certificate issued, live), with the
 * problems the product found, what public DNS shows right now and that checking continues on its own.
 *
 * Built by Monarch as a vertical stepper of Compose layouts rather than on `MonarchStepper`, because that stepper
 * always draws a current step and has no in-progress spinner: here every step can be done once the domain is live,
 * and the step in progress shows a Material 3 `CircularProgressIndicator` (a still dot when animations are off).
 * Blocking problems are `MonarchInlineNotice` warnings; an informational one is a neutral notice. The component never
 * checks DNS itself: the app polls (see [MonarchDomainProgressDefaults.recheckIntervalMillis]) and passes the new
 * stage.
 *
 * For TalkBack, the steps are one group named "Progress for book.example.com". Each step is one element read as its
 * title with "in progress, Being issued now, usually 1 to 5 minutes." as its state. When the active step or its
 * state changes, "Step 2 of 3: HTTPS certificate, in progress." is announced through a polite live region.
 *
 * @param host the custom host name, such as `book.example.com`.
 * @param stage where the domain is in its connection.
 * @param modifier the modifier of the progress.
 * @param problems what the last check found. Blocking problems mark their step and are shown as warnings; without
 * one, the first informational problem is shown quietly.
 * @param dnsFound what public DNS shows for the host right now. `null` hides the line.
 * @param checkedAt when the product last checked, as epoch milliseconds (UTC). `null` hides the line.
 * @param now the reference time for [checkedAt], as epoch milliseconds. `null` is the current time, refreshed every
 * minute; pass a fixed time in tests and screenshots.
 * @param rechecking whether the product keeps checking on its own; adds "We keep checking automatically…".
 * @param notify how the product tells the user it went live; changes the end of the rechecking sentence.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchDomainProgress(
    host: String,
    stage: MonarchDomainStage,
    modifier: Modifier = Modifier,
    problems: List<MonarchDomainProblem> = emptyList(),
    dnsFound: MonarchDomainDnsSnapshot? = null,
    checkedAt: Long? = null,
    now: Long? = null,
    rechecking: Boolean = true,
    notify: MonarchDomainNotify? = null,
    labels: MonarchDomainProgressLabels = MonarchDomainProgressLabels(),
) {
    val strings = monarchDomainProgressStrings(labels)
    val items = strings.items(host, stage, problems)
    val active = strings.announcement(items)
    var previous by remember { mutableStateOf(active) }
    var announcement by remember { mutableStateOf("") }
    LaunchedEffect(active) {
        if (active != previous) {
            announcement = active
            previous = active
        }
    }
    val colors = MonarchTheme.colors
    val side = MonarchDomainProgressTokens.markerSize(LocalDensity.current.fontScale)
    val listLabel = strings.listLabel(host)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MonarchDomainProgressTokens.SectionGap),
    ) {
        Column(modifier = Modifier.fillMaxWidth().semantics { contentDescription = listLabel }) {
            items.forEachIndexed { index, item ->
                Step(item, strings.state(item.state), side, last = index == items.lastIndex)
            }
        }
        val shown = MonarchDomainProgressRules.shownProblems(problems)
        if (shown.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(MonarchDomainProgressTokens.TextGap)) {
                shown.forEach { MonarchInlineNotice(message = it.message, tone = MonarchDomainProgressRules.tone(it)) }
            }
        }
        if (stage != MonarchDomainStage.Live) {
            val lines = listOfNotNull(
                dnsFound?.let {
                    strings.dnsSeen(host, MonarchDomainProgressRules.snapshotText(it, strings.noRecord))
                },
                strings.rechecking(notify).takeIf { rechecking },
            )
            if (lines.isNotEmpty()) {
                MonarchText(
                    text = lines.joinToString(" "),
                    style = MonarchTextStyle.Caption,
                    color = MonarchDomainProgressTokens.muted(colors),
                )
            }
            if (checkedAt != null) {
                val relative = MonarchRelativeTime.format(
                    checkedAt,
                    rememberMonarchRelativeClock(listOf(checkedAt), fixed = now),
                    LocalConfiguration.current.locales[0],
                )
                MonarchText(
                    text = strings.checked(relative),
                    style = MonarchTextStyle.Caption,
                    color = MonarchDomainProgressTokens.muted(colors),
                )
            }
        }
        // Always present, so a change of the active step is announced without moving focus.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** One step: the marker on the line, the title and the detail, read as one element. */
@Composable
private fun Step(item: MonarchDomainProgressItem, spokenState: String, side: Dp, last: Boolean) {
    val colors = MonarchTheme.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val line = MonarchDomainProgressTokens.connector(item.state, colors)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (last) return@drawBehind
                val half = side.toPx() / 2
                val x = if (rtl) size.width - half else half
                drawLine(line, Offset(x, side.toPx()), Offset(x, size.height), MonarchDomainProgressTokens.Line.toPx())
            }
            .padding(bottom = MonarchDomainProgressTokens.stepGap(last))
            .semantics(mergeDescendants = true) {
                contentDescription = item.title
                stateDescription = MonarchDomainProgressRules.stepState(spokenState, item.detail)
            },
        horizontalArrangement = Arrangement.spacedBy(MonarchDomainProgressTokens.MarkerGap),
    ) {
        MonarchDomainProgressMarker(item.state, side)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MonarchDomainProgressTokens.TextGap),
        ) {
            MonarchText(
                text = item.title,
                style = MonarchTextStyle.BodyStrong,
                color = MonarchDomainProgressTokens.title(item.state, colors),
            )
            MonarchText(
                text = item.detail,
                style = MonarchTextStyle.Caption,
                color = MonarchDomainProgressTokens.muted(colors),
            )
        }
    }
}

