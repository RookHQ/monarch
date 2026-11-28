package dev.monarchui.components.scheduling.timezoneselect

import androidx.compose.runtime.Immutable
import dev.monarchui.components.forms.combobox.MonarchComboboxLabels

/**
 * Replacements for the built-in strings of a `MonarchTimezoneSelect`. Every `null` keeps the built-in string
 * `monarch_timezoneselect_<key>`.
 *
 * @property detected the heading of the device's zone, "Detected".
 * @property pinned the heading of the pinned zones, "Pinned".
 * @property recent the heading of the recent zones, "Recent".
 * @property all the heading of the full list, "All time zones".
 * @property empty the message when no zone matches the search, "No time zones match".
 * @property combobox the strings of the underlying `MonarchCombobox`, such as its search field and cancel button.
 * Its `empty` is replaced by [empty].
 */
@Immutable
public class MonarchTimezoneSelectLabels(
    public val detected: String? = null,
    public val pinned: String? = null,
    public val recent: String? = null,
    public val all: String? = null,
    public val empty: String? = null,
    public val combobox: MonarchComboboxLabels = MonarchComboboxLabels(),
)
