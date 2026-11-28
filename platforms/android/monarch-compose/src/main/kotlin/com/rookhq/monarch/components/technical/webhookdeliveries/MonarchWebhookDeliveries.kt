package com.rookhq.monarch.components.technical.webhookdeliveries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusRegistry
import com.rookhq.monarch.components.datagrid.loadmore.MonarchLoadMore
import com.rookhq.monarch.components.datagrid.loadmore.MonarchLoadMoreLabels
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateKind
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeleton
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.rememberMonarchRelativeClock
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * The delivery log of a webhook endpoint, or any outbound integration: the
 * status, event, response, attempts and time of each delivery, with filters,
 * paging, a details sheet and Redeliver on each row. Data and actions come
 * from the app; nothing is fetched here.
 *
 * Built by Monarch for phones, where the web shows a table: a `LazyColumn` of
 * rows, each a tappable summary with a `MonarchStatusBadge` and a
 * `MonarchButton` Redeliver. Tapping a row opens a
 * `MonarchBottomSheet` with every fact as a `MonarchDescriptionList` (the event
 * id is copyable), Redeliver and the app's [details], or calls [onSelect]
 * instead when the app shows its own detail screen. The filters are a
 * `MonarchTextField` that finds an event id and menu buttons that open
 * `MonarchMenu`s of statuses and event types. The first page
 * loads with `MonarchSkeleton` rows; a failure is a `MonarchInlineErrorRetry`;
 * no deliveries, or none that match, is a `MonarchEmptyState`; further pages
 * come from a `MonarchLoadMore` footer.
 *
 * Give the list a bounded height (a screen, or `Modifier.height`): it is lazy
 * and does not go inside a vertically scrolling parent. In a loose height it
 * takes the height of its rows.
 *
 * TalkBack reads each row as one element (status, time, event, event id,
 * response, attempts, error) whose action is "View delivery of …"; Redeliver
 * names the event it sends. A queued or failed redelivery and the counts line
 * are announced through polite live regions.
 *
 * @param deliveries the deliveries to show, newest first. Ids must be unique.
 * @param modifier the modifier of the component.
 * @param statuses status names and tones over the built-in ones ([MonarchWebhookDeliveryStatus]), or the app's
 * own keys.
 * @param filterable shows the search field and the filter menus.
 * @param filters the filter values. `null` keeps them inside the component.
 * @param onFiltersChange called with the new filter values when the person changes one.
 * @param manualFiltering the server filters: [deliveries] are shown as given. Send [filters] with the next request.
 * @param filterStatuses the status keys the status filter offers. `null` offers every key of the merged registry.
 * @param eventTypes the event types the event type filter offers. The filter is hidden when empty.
 * @param showDestination shows each delivery's destination, when one list covers several endpoints.
 * @param counts totals per status key, shown above the list as "12 delivered · 3 retrying", in map order.
 * @param loading the first page is loading: placeholder rows replace the list.
 * @param failed the list failed to load: the failure replaces the list.
 * @param onRetry called by Try again after a load failure. Without it the failure has no button.
 * @param hasMore there is another page.
 * @param onLoadMore loads the next page. Adds the Load more footer.
 * @param loadingMore the next page is loading.
 * @param loadMoreFailed loading the next page failed.
 * @param now the time relative times are measured from, in epoch milliseconds. `null` follows the clock,
 * refreshed every minute.
 * @param locale the locale of numbers and dates. `null` uses the first locale of the configuration.
 * @param timeZone the time zone of the dates in the details sheet. An IANA id such as `"Europe/Berlin"`. `null`, or an
 * id the device does not know, is the device's zone.
 * @param onRedeliver sends a delivery again. While it runs, the button shows progress; when it returns,
 * "Redelivery queued." is announced; when it throws, "Could not redeliver. Try again." is. Without it there is
 * no Redeliver.
 * @param canRedeliver which deliveries offer Redeliver. `null` offers it on every one that is not pending or
 * sending.
 * @param onSelect opens the app's own detail screen for a delivery. `null` opens the built-in details sheet.
 * @param labels replacements for the built-in strings.
 * @param emptyAction shown in the first-run empty state, such as a "Send a test event" button.
 * @param actions more actions on each row, after Redeliver.
 * @param details more content at the end of the details sheet, such as the request and response bodies.
 */
@ExperimentalMonarchApi
@Composable
public fun MonarchWebhookDeliveries(
    deliveries: List<MonarchWebhookDelivery>,
    modifier: Modifier = Modifier,
    statuses: MonarchStatusRegistry? = null,
    filterable: Boolean = true,
    filters: MonarchWebhookDeliveryFilters? = null,
    onFiltersChange: ((MonarchWebhookDeliveryFilters) -> Unit)? = null,
    manualFiltering: Boolean = false,
    filterStatuses: List<String>? = null,
    eventTypes: List<String> = emptyList(),
    showDestination: Boolean = false,
    counts: Map<String, Int> = emptyMap(),
    loading: Boolean = false,
    failed: Boolean = false,
    onRetry: (() -> Unit)? = null,
    hasMore: Boolean = false,
    onLoadMore: (() -> Unit)? = null,
    loadingMore: Boolean = false,
    loadMoreFailed: Boolean = false,
    now: Long? = null,
    locale: Locale? = null,
    timeZone: String? = null,
    onRedeliver: (suspend (MonarchWebhookDelivery) -> Unit)? = null,
    canRedeliver: ((MonarchWebhookDelivery) -> Boolean)? = null,
    onSelect: ((MonarchWebhookDelivery) -> Unit)? = null,
    labels: MonarchWebhookDeliveriesLabels = MonarchWebhookDeliveriesLabels(),
    emptyAction: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.(MonarchWebhookDelivery) -> Unit)? = null,
    details: (@Composable ColumnScope.(MonarchWebhookDelivery) -> Unit)? = null,
) {
    val resources = LocalResources.current
    val text = remember(labels, resources) { MonarchWebhookDeliveriesText(labels, resources) }
    val registry = remember(statuses, text) { MonarchWebhookDeliveriesTokens.registry(text::statusName, statuses) }
    val configLocale = LocalConfiguration.current.locales[0]
    val shownLocale = locale ?: configLocale
    var ownFilters by rememberSaveable(stateSaver = FiltersSaver) { mutableStateOf(MonarchWebhookDeliveryFilters()) }
    val current = filters ?: ownFilters
    val setFilters: (MonarchWebhookDeliveryFilters) -> Unit = {
        if (filters == null) ownFilters = it
        onFiltersChange?.invoke(it)
    }
    val rows = MonarchWebhookDeliveriesTokens.shown(deliveries, current, manualFiltering)
    val scope = rememberCoroutineScope()
    val redelivery = remember { WebhookRedelivery() }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    val model = MonarchWebhookDeliveriesRowModel(
        registry = registry,
        now = webhookDeliveriesClock(
            now,
            deliveries.isNotEmpty(),
            deliveries.flatMap { listOf(it.atMillis, it.nextAttemptMillis) },
        ),
        locale = shownLocale,
        showDestination = showDestination,
        text = text,
        actions = actions,
    )
    val redeliverFor: (MonarchWebhookDelivery) -> (() -> Unit)? = { delivery ->
        redelivery.handler(delivery, onRedeliver, canRedeliver, scope, text)
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MonarchWebhookDeliveriesTokens.SectionGap)) {
        if (filterable) {
            MonarchWebhookDeliveriesFilterBar(
                filters = current,
                onFiltersChange = setFilters,
                statusKeys = MonarchWebhookDeliveriesTokens.statusChoices(filterStatuses, registry),
                eventTypes = eventTypes,
                registry = registry,
                text = text,
            )
        }
        WebhookDeliveriesCounts(MonarchWebhookDeliveriesFormat.countLine(counts, registry, text, shownLocale))
        when {
            loading -> WebhookDeliveriesLoading(text.loading)
            failed -> WebhookDeliveriesFailure(text.loadError, onRetry)
            else -> WebhookDeliveriesList(
                rows = rows,
                model = model,
                busy = redelivery.busy,
                redeliverFor = redeliverFor,
                onOpen = { delivery -> if (onSelect != null) onSelect(delivery) else openId = delivery.id },
                empty = {
                    WebhookDeliveriesEmpty(current.active, text, emptyAction) {
                        setFilters(MonarchWebhookDeliveryFilters())
                    }
                },
                footer = onLoadMore?.let { load ->
                    {
                        MonarchLoadMore(
                            hasMore = hasMore,
                            onLoadMore = load,
                            modifier = Modifier.fillMaxWidth(),
                            loading = loadingMore,
                            failed = loadMoreFailed,
                            loadedCount = rows.size,
                            labels = MonarchLoadMoreLabels(end = text.endOfList),
                        )
                    }
                },
            )
        }
        // Always present, so each result is announced once as a change of this live region.
        MonarchLiveStatus(text = redelivery.announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
    val open = openId?.let { id -> deliveries.firstOrNull { it.id == id } }
    if (open != null) {
        MonarchWebhookDeliveriesSheet(
            delivery = open,
            model = model,
            timeZone = timeZone,
            busy = open.id in redelivery.busy,
            onRedeliver = redeliverFor(open),
            onDismiss = { openId = null },
            details = details,
        )
    }
}

/** The lazy list: the rows, or the empty state, then the paging footer. */
@Suppress("LongParameterList")
@Composable
private fun ColumnScope.WebhookDeliveriesList(
    rows: List<MonarchWebhookDelivery>,
    model: MonarchWebhookDeliveriesRowModel,
    busy: Set<String>,
    redeliverFor: (MonarchWebhookDelivery) -> (() -> Unit)?,
    onOpen: (MonarchWebhookDelivery) -> Unit,
    empty: @Composable () -> Unit,
    footer: (@Composable () -> Unit)?,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .semantics { contentDescription = model.text.label },
    ) {
        deliveryItems(rows, model, busy, redeliverFor, onOpen)
        if (rows.isEmpty()) {
            item(key = EmptyKey) { empty() }
        } else if (footer != null) {
            item(key = FooterKey) { footer() }
        }
    }
}

/** The totals per status, read again by TalkBack when they change. Nothing without counts. */
@Composable
private fun WebhookDeliveriesCounts(line: String) {
    if (line.isEmpty()) return
    MonarchText(
        text = line,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = MonarchTextStyle.Caption,
        color = MonarchWebhookDeliveriesTokens.metaColor(MonarchTheme.colors),
    )
}

private fun LazyListScope.deliveryItems(
    rows: List<MonarchWebhookDelivery>,
    model: MonarchWebhookDeliveriesRowModel,
    busy: Set<String>,
    redeliverFor: (MonarchWebhookDelivery) -> (() -> Unit)?,
    onOpen: (MonarchWebhookDelivery) -> Unit,
) {
    items(rows, key = { it.id }) { delivery ->
        if (delivery !== rows.first()) MonarchDivider(variant = MonarchDividerVariant.Subtle)
        MonarchWebhookDeliveriesRow(
            delivery = delivery,
            model = model,
            busy = delivery.id in busy,
            onRedeliver = redeliverFor(delivery),
            onOpen = { onOpen(delivery) },
        )
    }
}

/** Placeholder rows, read by TalkBack as one polite "Loading deliveries". */
@Composable
private fun WebhookDeliveriesLoading(label: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = label
                liveRegion = LiveRegionMode.Polite
            },
        verticalArrangement = Arrangement.spacedBy(MonarchWebhookDeliveriesTokens.LineGap),
    ) {
        repeat(MonarchWebhookDeliveriesTokens.SKELETON_ROWS) {
            MonarchSkeleton(modifier = Modifier.fillMaxWidth().height(MonarchWebhookDeliveriesTokens.SkeletonHeight))
        }
    }
}

@Composable
private fun WebhookDeliveriesFailure(message: String, onRetry: (() -> Unit)?) {
    if (onRetry != null) {
        MonarchInlineErrorRetry(message = message, onRetry = onRetry, modifier = Modifier.fillMaxWidth())
    } else {
        MonarchText(
            text = message,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color = MonarchWebhookDeliveriesTokens.errorColor(MonarchTheme.colors),
        )
    }
}

@Composable
private fun WebhookDeliveriesEmpty(
    filtered: Boolean,
    text: MonarchWebhookDeliveriesText,
    emptyAction: (@Composable () -> Unit)?,
    onClearFilters: () -> Unit,
) {
    if (filtered) {
        MonarchEmptyState(
            title = text.filteredTitle,
            modifier = Modifier.fillMaxWidth(),
            kind = MonarchEmptyStateKind.Filtered,
            size = MonarchEmptyStateSize.Sm,
            message = text.filteredDescription,
            onClearFilters = onClearFilters,
            clearFiltersLabel = text.clearFilters,
        )
    } else {
        MonarchEmptyState(
            title = text.emptyTitle,
            modifier = Modifier.fillMaxWidth(),
            size = MonarchEmptyStateSize.Sm,
            message = text.emptyDescription,
            actions = emptyAction,
        )
    }
}

/** The rows with a redelivery in progress, and the last announcement. */
private class WebhookRedelivery {
    var busy: Set<String> by mutableStateOf(emptySet())
    var announcement: String by mutableStateOf("")

    /** What Redeliver does for [delivery], or `null` when it offers none. */
    fun handler(
        delivery: MonarchWebhookDelivery,
        onRedeliver: (suspend (MonarchWebhookDelivery) -> Unit)?,
        canRedeliver: ((MonarchWebhookDelivery) -> Boolean)?,
        scope: CoroutineScope,
        text: MonarchWebhookDeliveriesText,
    ): (() -> Unit)? {
        val allowed = canRedeliver?.invoke(delivery) ?: MonarchWebhookDeliveriesTokens.redeliverable(delivery)
        if (onRedeliver == null || !allowed) return null
        return { start(scope, delivery, onRedeliver, text) }
    }

    private fun announce(message: String) {
        // A repeated message is changed slightly so the live region reads it again.
        announcement = if (message == announcement) "$message " else message
    }

    private fun start(
        scope: CoroutineScope,
        delivery: MonarchWebhookDelivery,
        handler: suspend (MonarchWebhookDelivery) -> Unit,
        text: MonarchWebhookDeliveriesText,
    ) {
        if (delivery.id in busy) return
        busy = busy + delivery.id
        scope.launch {
            val error = runCatching { handler(delivery) }.exceptionOrNull()
            busy = busy - delivery.id
            if (error is CancellationException) throw error
            announce(if (error == null) text.redeliverQueued else text.redeliverFailed)
        }
    }
}

/** The reference time: [fixed], or the clock, redrawn as often as the freshest of [times] needs while [running]. */
@Composable
private fun webhookDeliveriesClock(fixed: Long?, running: Boolean, times: List<Long?>): Long =
    rememberMonarchRelativeClock(times, fixed = fixed, running = running)

private val FiltersSaver = listSaver<MonarchWebhookDeliveryFilters, String>(
    save = { listOf(it.status, it.eventType, it.query) },
    restore = { MonarchWebhookDeliveryFilters(it[0], it[1], it[2]) },
)

private const val EmptyKey = "\u0000empty"
private const val FooterKey = "\u0000footer"
