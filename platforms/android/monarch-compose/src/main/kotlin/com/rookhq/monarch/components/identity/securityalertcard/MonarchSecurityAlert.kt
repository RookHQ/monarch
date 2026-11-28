package com.rookhq.monarch.components.identity.securityalertcard

/**
 * One security alert of a `MonarchSecurityAlertCard`.
 *
 * @property id a stable id, passed back to `onMarkRead` and `onDismiss`.
 * @property title what happened, such as "New sign-in on a Pixel 9". Already localised.
 * @property description more detail, such as the place or the device.
 * @property severity how serious it is. Shown as a badge in words.
 * @property dateMillis when it happened, in epoch milliseconds (UTC), shown as a relative time.
 * @property read whether the person has seen it.
 * @property actionLabel the title of a follow-up button, such as "Review sessions"; it runs `onAction`.
 */
public data class MonarchSecurityAlert(
    public val id: String,
    public val title: String,
    public val description: String? = null,
    public val severity: MonarchSecurityAlertSeverity = MonarchSecurityAlertSeverity.Info,
    public val dateMillis: Long? = null,
    public val read: Boolean = false,
    public val actionLabel: String? = null,
)
