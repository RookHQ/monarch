package dev.monarchui.components.identity.devicelist

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchDeviceList`. Every
 * `null` keeps the built-in string `monarch_devicelist_<key>`.
 *
 * @property listLabel the spoken name of the list, "Devices".
 * @property unnamed the name of a device with a blank name, "Unknown device".
 * @property current the badge of the device showing the list, "This device".
 * @property blocked the badge of a blocked device, "Blocked".
 * @property lastUsed before the last-used time, "Last used".
 * @property firstSeen before the first-seen time, "First seen".
 * @property remove the row button and the confirm button, "Remove".
 * @property removeLabel the spoken name of a row button: "Remove Ana's Pixel".
 * @property confirmTitle the title of the confirmation: "Remove Ana's Pixel?".
 * @property confirmMessage the text of the confirmation.
 * @property cancel the button that closes the confirmation, "Cancel".
 * @property removed announced after a device is removed: "Removed Ana's Pixel".
 * @property removeFailed shown and announced when removal failed: "Couldn't remove Ana's Pixel. Try again.".
 * @property loading the loading state, "Loading devices".
 * @property emptyTitle the empty state, "No devices yet".
 * @property emptyMessage the line under the empty state.
 * @property errorTitle the failed state, "Couldn't load your devices".
 */
@Immutable
public class MonarchDeviceListLabels(
    public val listLabel: String? = null,
    public val unnamed: String? = null,
    public val current: String? = null,
    public val blocked: String? = null,
    public val lastUsed: String? = null,
    public val firstSeen: String? = null,
    public val remove: String? = null,
    public val removeLabel: ((String) -> String)? = null,
    public val confirmTitle: ((String) -> String)? = null,
    public val confirmMessage: String? = null,
    public val cancel: String? = null,
    public val removed: ((String) -> String)? = null,
    public val removeFailed: ((String) -> String)? = null,
    public val loading: String? = null,
    public val emptyTitle: String? = null,
    public val emptyMessage: String? = null,
    public val errorTitle: String? = null,
)
