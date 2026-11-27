package com.rookhq.monarch.components.collaboration.slashmenu

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchSlashMenu`. Every
 * `null` keeps the built-in string `monarch_slashmenu_<key>`.
 *
 * @property label the name of the list, "Insert block".
 * @property empty shown and read when nothing matches, given what was typed with the trigger:
 * "Nothing matches “/xyz”".
 * @property count read as the list changes, given the number of entries: "3 options".
 */
@Immutable
public class MonarchSlashMenuLabels(
    public val label: String? = null,
    public val empty: ((String) -> String)? = null,
    public val count: ((Int) -> String)? = null,
)
