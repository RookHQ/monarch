package dev.monarchui.components.identity.devicelist

import androidx.compose.runtime.Immutable

/**
 * A device that has accessed the account, for `MonarchDeviceList`.
 *
 * @property id a stable id, passed to `onRemove`.
 * @property name the device's name, such as "Ana's Pixel". A blank name reads "Unknown device".
 * @property kind the kind of device, given to the icon slot.
 * @property os the operating system, such as "Android 16".
 * @property browser the browser or app, such as "Chrome".
 * @property location where it was last used, such as "Lisbon, Portugal".
 * @property lastUsedMillis when it was last used, in epoch milliseconds (UTC).
 * @property firstSeenMillis when it was first seen, in epoch milliseconds (UTC).
 * @property blocked whether the device is blocked from the account.
 * @property current whether it is the device showing the list. It has no Remove button.
 */
@Immutable
public class MonarchDevice(
    public val id: String,
    public val name: String,
    public val kind: MonarchDeviceKind = MonarchDeviceKind.Desktop,
    public val os: String? = null,
    public val browser: String? = null,
    public val location: String? = null,
    public val lastUsedMillis: Long? = null,
    public val firstSeenMillis: Long? = null,
    public val blocked: Boolean = false,
    public val current: Boolean = false,
)
