package dev.monarchui.components.technical.webhookdeliveries

import androidx.compose.runtime.Immutable

/**
 * One attempt, or series of attempts, to deliver one event to one endpoint,
 * as shown by `MonarchWebhookDeliveries`.
 *
 * @property id the stable id of the delivery.
 * @property status a status key: one of [MonarchWebhookDeliveryStatus] or a key of the app's own registry.
 * @property eventType the event name, such as `invoice.paid`.
 * @property attempts the attempts made so far.
 * @property atMillis when it was last attempted or delivered (or created, if never attempted), in epoch
 * milliseconds, UTC.
 * @property eventId the event's id, copyable in the details.
 * @property destination where it was sent, when one list covers several endpoints.
 * @property responseCode the HTTP status the endpoint answered with, or `null` when there was no answer.
 * @property error what went wrong, in plain words: "No response within 10 seconds".
 * @property durationMillis how long the last attempt took, in milliseconds.
 * @property nextAttemptMillis when the next automatic retry happens, for pending or retrying deliveries, in epoch
 * milliseconds, UTC.
 */
@Immutable
public data class MonarchWebhookDelivery(
    public val id: String,
    public val status: String,
    public val eventType: String,
    public val attempts: Int,
    public val atMillis: Long,
    public val eventId: String? = null,
    public val destination: String? = null,
    public val responseCode: Int? = null,
    public val error: String? = null,
    public val durationMillis: Long? = null,
    public val nextAttemptMillis: Long? = null,
)
