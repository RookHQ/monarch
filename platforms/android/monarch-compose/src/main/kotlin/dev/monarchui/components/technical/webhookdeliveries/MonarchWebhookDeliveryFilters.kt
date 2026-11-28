package dev.monarchui.components.technical.webhookdeliveries

import androidx.compose.runtime.Immutable

/**
 * The filter values of a `MonarchWebhookDeliveries`. An empty string means "any".
 *
 * @property status the status key to show, or empty for every status.
 * @property eventType the event type to show, or empty for every type.
 * @property query text matched against the event id (exactly, ignoring case) and the event type (contains).
 */
@Immutable
public data class MonarchWebhookDeliveryFilters(
    public val status: String = "",
    public val eventType: String = "",
    public val query: String = "",
) {
    /** Whether any filter is set. */
    public val active: Boolean get() = status.isNotEmpty() || eventType.isNotEmpty() || query.isNotBlank()

    /** Whether [delivery] passes every filter. */
    public fun matches(delivery: MonarchWebhookDelivery): Boolean {
        val q = query.trim().lowercase()
        return (status.isEmpty() || delivery.status == status) &&
            (eventType.isEmpty() || delivery.eventType == eventType) &&
            (q.isEmpty() || delivery.eventId?.lowercase() == q || delivery.eventType.lowercase().contains(q))
    }

    /** The deliveries that pass every filter, in their order. */
    public fun filter(deliveries: List<MonarchWebhookDelivery>): List<MonarchWebhookDelivery> =
        deliveries.filter(::matches)
}
