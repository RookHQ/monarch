package dev.monarchui.components.datadisplay.freshnessindicator

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchFreshnessIndicator`.
 * Every `null` uses the built-in string named after it in
 * `strings_freshnessindicator.xml`.
 *
 * @property updated writes the time from the relative time: "Updated 3 minutes ago".
 * @property never shown when the data was never updated: "Never updated".
 * @property checking shown while the status is read: "Checking…".
 * @property stale the flag after the time when the data is stale: "Out of date".
 * @property expired the flag after the time when the data is expired: "Very out of date".
 * @property error shown for the error status without an error text: "Status unavailable".
 * @property refresh the label of the refresh button: "Refresh".
 * @property refreshing the state of the refresh button while a refresh runs: "Refreshing".
 * @property refreshed announced when a refresh ends: "Refreshed.".
 * @property refreshFailed announced when a refresh ends in the error status: "Refresh failed.".
 */
@Immutable
public class MonarchFreshnessIndicatorLabels(
    public val updated: ((time: String) -> String)? = null,
    public val never: String? = null,
    public val checking: String? = null,
    public val stale: String? = null,
    public val expired: String? = null,
    public val error: String? = null,
    public val refresh: String? = null,
    public val refreshing: String? = null,
    public val refreshed: String? = null,
    public val refreshFailed: String? = null,
)
