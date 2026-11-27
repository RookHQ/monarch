package com.rookhq.monarch.components.workflow.approvaltimeline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.internal.monarchTimeZone
import com.rookhq.monarch.internal.rememberMonarchRelativeClock
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The history and outlook of one approval: who submitted it, each step with its decision, who is deciding now and
 * what comes next.
 *
 * Built by Monarch as a vertical timeline of Compose layouts: each step is a marker on a rail, the label and status,
 * who decided with a small decorative `MonarchAvatar` and when, who can decide next, the comment and the note.
 * Read-only: the app supplies the steps from its approval engine. Times are relative to [now] and become a date and
 * time once they are more than [absoluteAfterMillis] away.
 *
 * For TalkBack, the timeline is one group named "Approval timeline" and each step is one element read as its label,
 * status, actor, absolute date and time, approvers, delegation, comment and note; the step waiting for a decision adds
 * "Current step". Markers differ by shape (check, cross, dot, dash, empty, dashed outline) and the status is written,
 * so nothing relies on colour.
 *
 * @param steps the steps in order, from submission to the outcome.
 * @param modifier the modifier of the timeline.
 * @param now the reference time for relative times, as epoch milliseconds. `null` is the current time, refreshed every
 * minute; pass a fixed time in tests and screenshots.
 * @param absoluteAfterMillis how far from [now] a time is shown as a date and time instead of "2 hours ago". Seven
 * days by default.
 * @param showAvatars shows a small avatar before the name of whoever decided or submitted.
 * @param timeZone the zone of absolute dates. An IANA id such as `"Europe/Berlin"`. `null`, or an id the device does
 * not know, is the device's zone.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchApprovalTimeline(
    steps: List<MonarchApprovalTimelineStep>,
    modifier: Modifier = Modifier,
    now: Long? = null,
    absoluteAfterMillis: Long = MonarchApprovalTimelineTokens.DefaultAbsoluteAfterMillis,
    showAvatars: Boolean = true,
    timeZone: String? = null,
    labels: MonarchApprovalTimelineLabels = MonarchApprovalTimelineLabels(),
) {
    val strings = monarchApprovalTimelineStrings(labels)
    if (steps.isEmpty()) {
        MonarchText(
            text = strings.empty,
            modifier = modifier.fillMaxWidth(),
            color = MonarchApprovalTimelineTokens.muted(MonarchTheme.colors),
        )
        return
    }
    val context = MonarchApprovalTimelineContext(
        now = rememberMonarchRelativeClock(steps.map { it.date }, fixed = now),
        absoluteAfterMillis = absoluteAfterMillis,
        zone = monarchTimeZone(timeZone),
        locale = LocalConfiguration.current.locales[0],
        showAvatars = showAvatars,
        markerSize = MonarchApprovalTimelineTokens.markerSize(LocalDensity.current.fontScale),
        strings = strings,
    )
    val name = strings.label
    Column(modifier = modifier.fillMaxWidth().semantics { contentDescription = name }) {
        steps.forEachIndexed { index, step ->
            MonarchApprovalTimelineRow(step, context, last = index == steps.lastIndex)
        }
    }
}
