package dev.monarchui.components.technical.webhookdeliveries

/**
 * The delivery status keys `MonarchWebhookDeliveries` labels out of the box.
 * An app may use its own keys and describe them with the `statuses` registry.
 */
public object MonarchWebhookDeliveryStatus {
    /** Waiting for its first attempt. Shown in the neutral tone as "Pending". */
    public const val Pending: String = "pending"

    /** Being sent now. Shown in the info tone as "Sending". */
    public const val Sending: String = "sending"

    /** The endpoint accepted it. Shown in the success tone as "Delivered". */
    public const val Succeeded: String = "succeeded"

    /** An attempt failed and another is scheduled. Shown in the warning tone as "Retrying". */
    public const val Retrying: String = "retrying"

    /** The last attempt failed. Shown in the danger tone as "Failed". */
    public const val Failed: String = "failed"

    /** Every retry failed and no more are scheduled. Shown in the danger tone as "Gave up". */
    public const val Gone: String = "gone"
}
