package com.rookhq.monarch.components.identity.spaceswitcher

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchSpaceSwitcher`. Every
 * `null` keeps the built-in string `monarch_spaceswitcher_<key>`.
 *
 * @property label the name of the button and the title of the sheet, "Space".
 * @property all the choice of every Space, "All spaces".
 * @property placeholder the button text while no Space is chosen and All spaces is not offered, "Choose a Space".
 * @property search the placeholder of the search field, "Search spaces".
 * @property empty shown when nothing matches the search, "No spaces match.".
 * @property loading shown while the Spaces load, "Loading spaces…".
 * @property create the New Space action.
 * @property manage the Manage Spaces action.
 * @property cancel the button that closes the sheet without a change.
 */
@Immutable
public class MonarchSpaceSwitcherLabels(
    public val label: String? = null,
    public val all: String? = null,
    public val placeholder: String? = null,
    public val search: String? = null,
    public val empty: String? = null,
    public val loading: String? = null,
    public val create: String? = null,
    public val manage: String? = null,
    public val cancel: String? = null,
)
