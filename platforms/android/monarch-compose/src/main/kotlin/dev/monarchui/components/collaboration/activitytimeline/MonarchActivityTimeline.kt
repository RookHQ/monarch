package dev.monarchui.components.collaboration.activitytimeline

import android.icu.text.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.datagrid.loadmore.MonarchLoadMore
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import dev.monarchui.components.feedback.skeleton.MonarchSkeleton
import dev.monarchui.components.feedback.skeleton.MonarchSkeletonShape
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchRelativeTime
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchTimeZone
import dev.monarchui.internal.rememberMonarchRelativeClock
import dev.monarchui.theme.MonarchTheme
import java.util.Locale
import java.util.TimeZone

/**
 * A vertical feed of events (actor, verb, target, time and a marker) grouped under day headings, with
 * expandable details and Load more.
 *
 * Built by Monarch on a Compose `LazyColumn`: each day is a `stickyHeader` that stays at the top while its events
 * scroll, and the events are lazy items, so long histories stay fast. The feed scrolls itself: give it a bounded
 * height, or let it fill a screen or a pane. Avatars are `MonarchAvatar`, details a `MonarchCollapsible`, tags
 * `MonarchTag`, the footer `MonarchLoadMore`; loading, empty and failed states use `MonarchSkeleton`,
 * `MonarchEmptyState` and `MonarchInlineErrorRetry`.
 *
 * For TalkBack, day headings are headings and each event is one element read as its sentence, its absolute date
 * and time, "Automated" for rules, its tags and its body ("Ana Ruiz moved Acme renewal, Mar 4, 2026, 2:00 PM,
 * Negotiation"); the relative time is visual. Markers and the line are decoration. Details are a separate
 * disclosure that says Expanded or Collapsed.
 *
 * @param events the events, newest first or oldest first; the order is kept. The app filters them; put filter
 * controls above the timeline.
 * @param modifier the modifier of the list.
 * @param groupByDay groups events under Today, Yesterday and dates in [timeZone].
 * @param marker a dot in the event's tone, or the actor's avatar. An event's icon always wins.
 * @param density regular, or compact for side panels.
 * @param expandedIds the ids of events whose details are open. `null` lets the timeline keep its own set.
 * @param onExpandedChange called with the new set when details open or close.
 * @param loading shows three placeholder rows for the first page.
 * @param failed shows the error message, with Try again when [onRetry] is set.
 * @param hasMore shows Load more after the feed when [onLoadMore] is set.
 * @param loadingMore Load more shows that the next page is loading.
 * @param loadMoreFailed Load more shows that the next page failed, with a retry.
 * @param now the reference time for relative times and day headings, as epoch milliseconds. `null` is the current
 * time, refreshed every minute; pass a fixed time in tests and screenshots.
 * @param timeZone the zone of day boundaries and dates. An IANA id such as `"Europe/Berlin"`. `null`, or an id the
 * device does not know, is the device's zone.
 * @param state the list's scroll state.
 * @param contentPadding the padding around the feed inside the list.
 * @param labels replacements for the built-in strings.
 * @param onRetry retries the first page after a failure.
 * @param onLoadMore loads the next page.
 */
@Composable
public fun MonarchActivityTimeline(
    events: List<MonarchActivityTimelineEvent>,
    modifier: Modifier = Modifier,
    groupByDay: Boolean = true,
    marker: MonarchActivityTimelineMarker = MonarchActivityTimelineMarker.Dot,
    density: MonarchActivityTimelineDensity = MonarchActivityTimelineDensity.Regular,
    expandedIds: Set<String>? = null,
    onExpandedChange: ((Set<String>) -> Unit)? = null,
    loading: Boolean = false,
    failed: Boolean = false,
    hasMore: Boolean = false,
    loadingMore: Boolean = false,
    loadMoreFailed: Boolean = false,
    now: Long? = null,
    timeZone: String? = null,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    labels: MonarchActivityTimelineLabels = MonarchActivityTimelineLabels(),
    onRetry: (() -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
) {
    when {
        loading -> Loading(monarchString(labels.loading, R.string.monarch_activitytimeline_loading), modifier)
        failed -> Failed(monarchString(labels.error, R.string.monarch_activitytimeline_error), onRetry, modifier)
        events.isEmpty() -> Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            MonarchEmptyState(
                title = monarchString(labels.empty, R.string.monarch_activitytimeline_empty),
                size = MonarchEmptyStateSize.Sm,
            )
        }
        else -> Feed(
            events = events,
            options = FeedOptions(
                groupByDay, marker, density, expandedIds, onExpandedChange, hasMore, loadingMore, loadMoreFailed,
                monarchTimeZone(timeZone), labels, onLoadMore,
            ),
            now = rememberMonarchRelativeClock(events.map { it.time }, fixed = now),
            state = state,
            contentPadding = contentPadding,
            modifier = modifier,
        )
    }
}

/** The settings of the feed, passed on unchanged. */
@Suppress("LongParameterList")
private class FeedOptions(
    val groupByDay: Boolean,
    val marker: MonarchActivityTimelineMarker,
    val density: MonarchActivityTimelineDensity,
    val expandedIds: Set<String>?,
    val onExpandedChange: ((Set<String>) -> Unit)?,
    val hasMore: Boolean,
    val loadingMore: Boolean,
    val loadMoreFailed: Boolean,
    val zone: TimeZone,
    val labels: MonarchActivityTimelineLabels,
    val onLoadMore: (() -> Unit)?,
)

@Composable
private fun Feed(
    events: List<MonarchActivityTimelineEvent>,
    options: FeedOptions,
    now: Long,
    state: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier,
) {
    val labels = options.labels
    var ownExpanded by rememberSaveable { mutableStateOf(listOf<String>()) }
    val expanded = options.expandedIds ?: ownExpanded.toSet()
    val toggle: (String, Boolean) -> Unit = { id, open ->
        val next = if (open) expanded + id else expanded - id
        if (options.expandedIds == null) ownExpanded = next.toList()
        options.onExpandedChange?.invoke(next)
    }
    val locale = LocalConfiguration.current.locales[0]
    val formats = remember(locale, options.zone) { DateFormats(locale, options.zone) }
    val automated = monarchString(labels.automated, R.string.monarch_activitytimeline_automated)
    val details = monarchString(labels.details, R.string.monarch_activitytimeline_details)
    val hideDetails = monarchString(labels.hideDetails, R.string.monarch_activitytimeline_hide_details)
    val today = monarchString(labels.today, R.string.monarch_activitytimeline_today)
    val yesterday = monarchString(labels.yesterday, R.string.monarch_activitytimeline_yesterday)
    val listName = monarchString(labels.list, R.string.monarch_activitytimeline_list)
    val rowText: (MonarchActivityTimelineEvent) -> MonarchActivityTimelineRowText = { event ->
        MonarchActivityTimelineRowText(
            relative = MonarchRelativeTime.format(event.time, now, locale),
            spoken = MonarchActivityTimelineDays.spoken(event, formats.dateTime(event.time), automated),
            automated = automated.takeIf { MonarchActivityTimelineDays.automated(event) },
            details = details,
            hideDetails = hideDetails,
        )
    }
    val days = remember(events, options.groupByDay, options.zone) {
        if (options.groupByDay) {
            MonarchActivityTimelineDays.group(events, options.zone)
        } else {
            listOf(MonarchActivityTimelineDay(day = 0, events = events))
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth().then(
            if (options.groupByDay) Modifier else Modifier.semantics { contentDescription = listName },
        ),
        state = state,
        contentPadding = contentPadding,
    ) {
        days.forEach { day ->
            if (options.groupByDay) {
                val first = day.events.first().time
                val heading = MonarchActivityTimelineDays.heading(
                    first, now, options.zone, today, yesterday, formats::day,
                )
                stickyHeader(key = "day-${day.day}", contentType = "day") { DayHeading(heading) }
            }
            eventItems(day.events, rowText, options, expanded, toggle)
        }
        val onLoadMore = options.onLoadMore
        if (options.hasMore && onLoadMore != null) {
            item(key = "load-more", contentType = "load-more") {
                MonarchLoadMore(
                    hasMore = true,
                    onLoadMore = onLoadMore,
                    loading = options.loadingMore,
                    failed = options.loadMoreFailed,
                    showEnd = false,
                    onRetry = onLoadMore,
                    labels = labels.loadMore,
                )
            }
        }
    }
}

/** The events of one day, the last without a line under its marker. */
private fun LazyListScope.eventItems(
    events: List<MonarchActivityTimelineEvent>,
    rowText: (MonarchActivityTimelineEvent) -> MonarchActivityTimelineRowText,
    options: FeedOptions,
    expanded: Set<String>,
    toggle: (String, Boolean) -> Unit,
) {
    events.forEachIndexed { index, event ->
        item(key = "event-${event.id}", contentType = "event") {
            MonarchActivityTimelineRow(
                event = event,
                text = rowText(event),
                marker = options.marker,
                density = options.density,
                last = index == events.lastIndex,
                expanded = event.id in expanded,
                onExpandedChange = { toggle(event.id, it) },
            )
        }
    }
}

/** A day heading, on the canvas colour so it covers the events that scroll under it. */
@Composable
private fun DayHeading(text: String) {
    MonarchText(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .background(MonarchActivityTimelineTokens.headingBackground(MonarchTheme.colors))
            .padding(vertical = MonarchActivityTimelineTokens.HeadingPadding),
        style = MonarchTextStyle.Caption,
        color = MonarchTheme.colors.textMuted,
        heading = true,
    )
}

/** Three placeholder rows, read as one element: "Loading activity". */
@Composable
private fun Loading(name: String, modifier: Modifier) {
    val gap = MonarchActivityTimelineTokens.eventGap(MonarchActivityTimelineDensity.Regular)
    Column(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = name
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        repeat(MonarchActivityTimelineTokens.LOADING_ROWS) {
            Row(horizontalArrangement = Arrangement.spacedBy(MonarchActivityTimelineTokens.MarkerGap)) {
                MonarchSkeleton(
                    modifier = Modifier.size(MonarchActivityTimelineTokens.Marker),
                    shape = MonarchSkeletonShape.Circle,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MonarchActivityTimelineTokens.PartGap),
                ) {
                    MonarchSkeleton(modifier = Modifier.fillMaxWidth(), shape = MonarchSkeletonShape.Text)
                    MonarchSkeleton(
                        modifier = Modifier.fillMaxWidth(MonarchActivityTimelineTokens.LOADING_LINE),
                        shape = MonarchSkeletonShape.Text,
                    )
                }
            }
        }
    }
}

/** The error: Try again with [onRetry], otherwise the message alone, announced politely. */
@Composable
private fun Failed(message: String, onRetry: (() -> Unit)?, modifier: Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        if (onRetry != null) {
            MonarchInlineErrorRetry(message = message, onRetry = onRetry)
        } else {
            MonarchText(
                text = message,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = MonarchActivityTimelineTokens.error(MonarchTheme.colors),
            )
        }
    }
}

/** The date formats of the feed in one locale and zone. */
private class DateFormats(locale: Locale, zone: TimeZone) {
    private val icuZone = android.icu.util.TimeZone.getTimeZone(zone.id)
    private val dateTime = DateFormat.getInstanceForSkeleton(DATE_TIME, locale).apply { timeZone = icuZone }
    private val monthDay = DateFormat.getInstanceForSkeleton(MONTH_DAY, locale).apply { timeZone = icuZone }
    private val monthDayYear =
        DateFormat.getInstanceForSkeleton(MONTH_DAY_YEAR, locale).apply { timeZone = icuZone }

    /** "Mar 4, 2026, 2:00 PM". */
    fun dateTime(time: Long): String = dateTime.format(java.util.Date(time))

    /** "Wednesday, March 4", or with the year. */
    fun day(time: Long, withYear: Boolean): String =
        (if (withYear) monthDayYear else monthDay).format(java.util.Date(time))

    private companion object {
        const val DATE_TIME = "yMMMdjmm"
        const val MONTH_DAY = "EEEEMMMMd"
        const val MONTH_DAY_YEAR = "EEEEMMMMdy"
    }
}
