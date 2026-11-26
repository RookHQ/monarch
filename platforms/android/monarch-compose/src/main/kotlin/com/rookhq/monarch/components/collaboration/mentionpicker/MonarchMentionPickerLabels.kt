package com.rookhq.monarch.components.collaboration.mentionpicker

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchMentionPicker`. Every
 * `null` keeps the built-in string `monarch_mentionpicker_<key>`.
 *
 * @property label the name of the list, "Suggestions".
 * @property people the heading of people when both kinds are present.
 * @property channels the heading of channels when both kinds are present.
 * @property loading shown and read while searching, "Searching…".
 * @property empty shown and read when nothing matches, "No matches".
 * @property error shown and read when the search failed.
 * @property retry the button that repeats a failed search.
 * @property count read when suggestions arrive, given the number: "3 suggestions".
 */
@Immutable
public class MonarchMentionPickerLabels(
    public val label: String? = null,
    public val people: String? = null,
    public val channels: String? = null,
    public val loading: String? = null,
    public val empty: String? = null,
    public val error: String? = null,
    public val retry: String? = null,
    public val count: ((Int) -> String)? = null,
)
