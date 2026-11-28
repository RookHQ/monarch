package dev.monarchui.components.technical.webhookdeliveries

import android.content.res.Resources
import dev.monarchui.R

/** The strings of a MonarchWebhookDeliveries: each override from [labels], or the built-in resource. */
internal class MonarchWebhookDeliveriesText(
    private val labels: MonarchWebhookDeliveriesLabels,
    private val resources: Resources,
) {
    private fun string(override: String?, id: Int): String = override ?: resources.getString(id)

    val label: String get() = string(labels.label, R.string.monarch_webhookdeliveries_label)
    val eventTerm: String get() = string(labels.eventTerm, R.string.monarch_webhookdeliveries_event_term)
    val whenTerm: String get() = string(labels.whenTerm, R.string.monarch_webhookdeliveries_when_term)
    val status: String get() = string(labels.status, R.string.monarch_webhookdeliveries_status)
    val anyStatus: String get() = string(labels.anyStatus, R.string.monarch_webhookdeliveries_any_status)
    val eventType: String get() = string(labels.eventType, R.string.monarch_webhookdeliveries_event_type)
    val anyEventType: String get() = string(labels.anyEventType, R.string.monarch_webhookdeliveries_any_event_type)
    val search: String get() = string(labels.search, R.string.monarch_webhookdeliveries_search)
    val searchHint: String get() = resources.getString(R.string.monarch_webhookdeliveries_search_hint)
    val clearFilters: String get() = string(labels.clearFilters, R.string.monarch_webhookdeliveries_clear_filters)
    val redeliver: String get() = string(labels.redeliver, R.string.monarch_webhookdeliveries_redeliver)
    val redeliverQueued: String
        get() = string(labels.redeliverQueued, R.string.monarch_webhookdeliveries_redeliver_queued)
    val redeliverFailed: String
        get() = string(labels.redeliverFailed, R.string.monarch_webhookdeliveries_redeliver_failed)
    val notAttempted: String get() = string(labels.notAttempted, R.string.monarch_webhookdeliveries_not_attempted)
    val emptyTitle: String get() = string(labels.emptyTitle, R.string.monarch_webhookdeliveries_empty_title)
    val emptyDescription: String
        get() = string(labels.emptyDescription, R.string.monarch_webhookdeliveries_empty_description)
    val filteredTitle: String get() = string(labels.filteredTitle, R.string.monarch_webhookdeliveries_filtered_title)
    val filteredDescription: String
        get() = string(labels.filteredDescription, R.string.monarch_webhookdeliveries_filtered_description)
    val loading: String get() = string(labels.loading, R.string.monarch_webhookdeliveries_loading)
    val loadError: String get() = string(labels.loadError, R.string.monarch_webhookdeliveries_load_error)
    val endOfList: String get() = string(labels.endOfList, R.string.monarch_webhookdeliveries_end_of_list)
    val eventId: String get() = string(labels.eventId, R.string.monarch_webhookdeliveries_event_id)
    val destination: String get() = string(labels.destination, R.string.monarch_webhookdeliveries_destination)
    val response: String get() = string(labels.response, R.string.monarch_webhookdeliveries_response)
    val durationTerm: String get() = string(labels.durationTerm, R.string.monarch_webhookdeliveries_duration_term)
    val error: String get() = string(labels.error, R.string.monarch_webhookdeliveries_error)
    val attemptsTerm: String get() = string(labels.attemptsTerm, R.string.monarch_webhookdeliveries_attempts_term)
    val lastAttempt: String get() = string(labels.lastAttempt, R.string.monarch_webhookdeliveries_last_attempt)
    val nextAttemptTerm: String
        get() = string(labels.nextAttemptTerm, R.string.monarch_webhookdeliveries_next_attempt_term)

    /** The built-in name of a built-in status key, or `null` for any other key. */
    fun statusName(key: String): String? = when (key) {
        MonarchWebhookDeliveryStatus.Pending -> R.string.monarch_webhookdeliveries_status_pending
        MonarchWebhookDeliveryStatus.Sending -> R.string.monarch_webhookdeliveries_status_sending
        MonarchWebhookDeliveryStatus.Succeeded -> R.string.monarch_webhookdeliveries_status_succeeded
        MonarchWebhookDeliveryStatus.Retrying -> R.string.monarch_webhookdeliveries_status_retrying
        MonarchWebhookDeliveryStatus.Failed -> R.string.monarch_webhookdeliveries_status_failed
        MonarchWebhookDeliveryStatus.Gone -> R.string.monarch_webhookdeliveries_status_gone
        else -> null
    }?.let(resources::getString)

    /** The event type and id that name a delivery in spoken labels: "invoice.paid evt_1". */
    private fun subject(delivery: MonarchWebhookDelivery): String =
        listOfNotNull(delivery.eventType, delivery.eventId).joinToString(" ")

    fun redeliverItem(delivery: MonarchWebhookDelivery): String = labels.redeliverItem?.invoke(delivery)
        ?: resources.getString(R.string.monarch_webhookdeliveries_redeliver_item, subject(delivery))

    fun viewDetails(delivery: MonarchWebhookDelivery): String = labels.viewDetails?.invoke(delivery)
        ?: resources.getString(R.string.monarch_webhookdeliveries_view_details, subject(delivery))

    fun httpStatus(code: Int): String = labels.httpStatus?.invoke(code)
        ?: resources.getString(R.string.monarch_webhookdeliveries_http_status, code)

    fun duration(milliseconds: String): String = labels.duration?.invoke(milliseconds)
        ?: resources.getString(R.string.monarch_webhookdeliveries_duration, milliseconds)

    fun attempts(count: Int, formatted: String): String = labels.attempts?.invoke(count)
        ?: resources.getQuantityString(R.plurals.monarch_webhookdeliveries_attempts, count, formatted)

    fun nextAttempt(time: String): String = labels.nextAttempt?.invoke(time)
        ?: resources.getString(R.string.monarch_webhookdeliveries_next_attempt, time)

    fun filterValue(name: String, value: String): String = labels.filterValue?.invoke(name, value)
        ?: resources.getString(R.string.monarch_webhookdeliveries_filter_value, name, value)

    fun count(count: String, status: String): String = labels.count?.invoke(count, status)
        ?: resources.getString(R.string.monarch_webhookdeliveries_count, count, status)
}
