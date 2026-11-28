package dev.monarchui.components.technical.webhookdeliveries

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchWebhookDeliveries`.
 * Every `null` keeps the built-in string `monarch_webhookdeliveries_<key>`.
 * The status names are not here: replace them with the `statuses` registry.
 *
 * @property label the TalkBack name of the list, "Webhook deliveries".
 * @property status the status filter, "Status".
 * @property anyStatus the status filter's choice that shows every status, "Any status".
 * @property eventType the event type filter, "Event type".
 * @property anyEventType the event type filter's choice that shows every type, "Any event type".
 * @property search the label of the search field, "Find by event ID".
 * @property filterValue the text of a filter button, from the filter's name and its value: "Status: Failed".
 * @property clearFilters the button that clears every filter, "Clear filters".
 * @property redeliver the visible text of the Redeliver button.
 * @property redeliverItem the TalkBack name of a row's Redeliver button: "Redeliver invoice.paid evt_1".
 * @property redeliverQueued announced when a redelivery was queued.
 * @property redeliverFailed announced when a redelivery failed.
 * @property viewDetails the TalkBack action of a row: "View delivery of invoice.paid evt_1".
 * @property notAttempted shown in place of the response before the first attempt.
 * @property httpStatus the response code: "HTTP 500".
 * @property duration the duration of the last attempt, from the formatted milliseconds: "1,203 ms".
 * @property attempts the number of attempts, from the count: "3 attempts".
 * @property nextAttempt when the next retry happens, from the relative time: "Next attempt in 5 minutes".
 * @property emptyTitle shown when there are no deliveries yet.
 * @property emptyDescription the line under it.
 * @property filteredTitle shown when the filters hide every delivery.
 * @property filteredDescription the line under it.
 * @property loading announced while the first page loads.
 * @property loadError shown when the list failed to load.
 * @property endOfList shown under the last page.
 * @property count one part of the counts line, from the count and the status name: "12 delivered".
 * @property eventId the term of the event id in the details, "Event ID".
 * @property destination the term of the destination in the details, "Destination".
 * @property response the term of the response in the details, "Response".
 * @property durationTerm the term of the duration in the details, "Duration".
 * @property error the term of the error in the details, "Error".
 * @property attemptsTerm the term of the attempts in the details, "Attempts".
 * @property lastAttempt the term of the last attempt's time in the details, "Last attempt".
 * @property nextAttemptTerm the term of the next attempt's time in the details, "Next attempt".
 * @property eventTerm the term of the event in a row of the list, "Event".
 * @property whenTerm the term of the time in a row of the list, "When".
 */
@Immutable
public class MonarchWebhookDeliveriesLabels(
    public val label: String? = null,
    public val status: String? = null,
    public val anyStatus: String? = null,
    public val eventType: String? = null,
    public val anyEventType: String? = null,
    public val search: String? = null,
    public val filterValue: ((name: String, value: String) -> String)? = null,
    public val clearFilters: String? = null,
    public val redeliver: String? = null,
    public val redeliverItem: ((delivery: MonarchWebhookDelivery) -> String)? = null,
    public val redeliverQueued: String? = null,
    public val redeliverFailed: String? = null,
    public val viewDetails: ((delivery: MonarchWebhookDelivery) -> String)? = null,
    public val notAttempted: String? = null,
    public val httpStatus: ((code: Int) -> String)? = null,
    public val duration: ((milliseconds: String) -> String)? = null,
    public val attempts: ((count: Int) -> String)? = null,
    public val nextAttempt: ((time: String) -> String)? = null,
    public val emptyTitle: String? = null,
    public val emptyDescription: String? = null,
    public val filteredTitle: String? = null,
    public val filteredDescription: String? = null,
    public val loading: String? = null,
    public val loadError: String? = null,
    public val endOfList: String? = null,
    public val count: ((count: String, status: String) -> String)? = null,
    public val eventId: String? = null,
    public val destination: String? = null,
    public val response: String? = null,
    public val durationTerm: String? = null,
    public val error: String? = null,
    public val attemptsTerm: String? = null,
    public val lastAttempt: String? = null,
    public val nextAttemptTerm: String? = null,
    public val eventTerm: String? = null,
    public val whenTerm: String? = null,
)
