package dev.monarchui.components.scheduling.eventpopover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import dev.monarchui.R
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.descriptionlist.MonarchDescriptionItem
import dev.monarchui.components.datadisplay.descriptionlist.MonarchDescriptionList
import dev.monarchui.components.datadisplay.descriptionlist.MonarchDescriptionListLayout
import dev.monarchui.components.scheduling.eventchip.MonarchEventChipStatus
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchTimeZone
import dev.monarchui.theme.MonarchTheme

/**
 * The content of a quick view of a calendar event: a colour swatch, the
 * title, a subtitle, a status badge, the date and time in a stated zone,
 * labelled details, a note and actions.
 *
 * This is the content, not the overlay: show it in a `MonarchPopover`
 * anchored to the `MonarchEventChip` that opens it, or in a Material 3
 * `ModalBottomSheet` on a phone. The popover or sheet handles dismissal and
 * returns focus. Details are a `MonarchDescriptionList` and the status a
 * `MonarchBadge`.
 *
 * The title is a heading; cancelled and declined titles are also struck
 * through. The time row is one TalkBack element read as "Time, Sat, Nov 14,
 * 2026, 9:30 AM – 10:15 AM EST"; each detail row is one element; the swatch is
 * decorative and the status is written. Actions are separate buttons that
 * wrap onto more lines when they do not fit.
 *
 * @param title the event title.
 * @param modifier the modifier of the content.
 * @param subtitle a second line under the title, such as the calendar or the service.
 * @param start when the event starts, in epoch milliseconds (UTC). For an all-day event, its first day as a UTC
 * midnight, as the Material 3 date picker gives it. Without it there is no time row.
 * @param end when the event ends; for an all-day event its last day, inclusive. An end before the start is ignored.
 * @param allDay shows the day or day range and "All day" instead of times.
 * @param color the event's colour for the swatch. `Color.Unspecified` uses the accent role.
 * @param status confirmed, tentative, cancelled or declined, shown as a badge in the success, warning, neutral or
 * danger tone.
 * @param details labelled rows under the time.
 * @param note a sentence on a subtle fill under the details, such as a cancellation reason.
 * @param timeZone the zone the times are shown in, named after the times. An IANA id such as `"Europe/Berlin"`. `null`,
 * or an id the device does not know, is the device's zone.
 * @param labels replacements for the built-in strings.
 * @param actions buttons at the bottom, laid out in a wrapping row. Dismiss the popover or sheet from them.
 */
@Composable
public fun MonarchEventPopover(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    start: Long? = null,
    end: Long? = null,
    allDay: Boolean = false,
    color: Color = Color.Unspecified,
    status: MonarchEventChipStatus? = null,
    details: List<MonarchEventPopoverDetail> = emptyList(),
    note: String? = null,
    timeZone: String? = null,
    labels: MonarchEventPopoverLabels = MonarchEventPopoverLabels(),
    actions: (@Composable () -> Unit)? = null,
) {
    val tokens = MonarchEventPopoverTokens
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
        Header(title, subtitle, color, status, labels)
        if (start != null) TimeRow(start, end, allDay, timeZone, labels)
        if (details.isNotEmpty()) {
            MonarchDescriptionList(
                items = details.map { detail ->
                    MonarchDescriptionItem(
                        term = detail.label,
                        value = detail.value,
                        content = detail.icon?.let { icon -> { DetailValue(detail.value, icon) } },
                    )
                },
                layout = MonarchDescriptionListLayout.Inline,
            )
        }
        if (!note.isNullOrBlank()) {
            Text(
                text = note,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tokens.noteFill(MonarchTheme.colors), MonarchShapeDefaults.Control)
                    .padding(tokens.NotePadding),
                style = MonarchTheme.typography.body,
                color = MonarchTheme.colors.textDefault,
            )
        }
        if (actions != null) Actions(actions)
    }
}

@Composable
private fun Header(
    title: String,
    subtitle: String?,
    color: Color,
    status: MonarchEventChipStatus?,
    labels: MonarchEventPopoverLabels,
) {
    val tokens = MonarchEventPopoverTokens
    val colors = MonarchTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap)) {
        Box(
            Modifier
                .padding(top = tokens.SwatchTop)
                .size(tokens.SwatchSize)
                .background(tokens.swatch(color, colors), MonarchShapeDefaults.Mark),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.TextGap)) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MonarchTheme.typography.subheading,
                color = colors.textDefault,
                textDecoration = tokens.titleDecoration(status),
            )
            if (!subtitle.isNullOrBlank()) {
                Text(text = subtitle, style = MonarchTheme.typography.caption, color = colors.textMuted)
            }
            if (status != null) {
                MonarchBadge(
                    text = labels.status?.invoke(status) ?: stringResource(tokens.statusRes(status)),
                    tone = tokens.tone(status),
                )
            }
        }
    }
}

@Composable
private fun TimeRow(
    start: Long,
    end: Long?,
    allDay: Boolean,
    timeZone: String?,
    labels: MonarchEventPopoverLabels,
) {
    val locale = LocalConfiguration.current.locales[0]
    val resources = LocalResources.current
    val zone = monarchTimeZone(timeZone)
    val allDayLabel = monarchString(labels.allDay, R.string.monarch_eventpopover_all_day)
    val text = remember(start, end, allDay, zone, locale, allDayLabel) {
        MonarchEventPopoverTokens.formatWhen(start, end, allDay, zone, locale, allDayLabel) { from, to ->
            resources.getString(R.string.monarch_eventpopover_range, from, to)
        }
    }
    val name = monarchString(labels.time, R.string.monarch_eventpopover_time)
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = "$name, ${text.date}, ${text.time}" },
        horizontalArrangement = Arrangement.spacedBy(MonarchEventPopoverTokens.ItemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchDefaultIcons.Clock()
        Text(
            text = buildAnnotatedString {
                append(text.date)
                withStyle(SpanStyle(color = colors.textMuted)) { append(" \u00B7 ${text.time}") }
            },
            style = MonarchTheme.typography.body,
            color = colors.textDefault,
        )
    }
}

@Composable
private fun DetailValue(value: String, icon: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MonarchEventPopoverTokens.ItemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.clearAndSetSemantics { }) { icon() }
        Text(text = value, style = MonarchTheme.typography.body, color = MonarchTheme.colors.textDefault)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Actions(actions: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MonarchEventPopoverTokens.ItemGap),
        verticalArrangement = Arrangement.spacedBy(MonarchEventPopoverTokens.ItemGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) { actions() }
}
