package com.rookhq.monarch.components.navigation.favoriteslist

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.interaction.reorderablelist.MonarchReorderableListLabels

/**
 * Replacements for the built-in strings of a `MonarchFavoritesList`. Every
 * `null` uses the built-in string in `strings_favoriteslist.xml`.
 *
 * @property title the heading: "Favorites".
 * @property edit the visible text of the button that starts edit mode: "Edit".
 * @property editLabel the spoken name of that button: "Edit favorites".
 * @property done the visible text and spoken name of the button while editing: "Done".
 * @property remove the visible text of a row's remove button when no icon is given: "Remove".
 * @property removeLabel writes the spoken name of a remove button and of the row's remove action from the
 * item's title: "Remove Roadmap from favorites".
 * @property removed writes the announcement after a removal from the item's title:
 * "Roadmap removed from favorites."
 * @property reorder replacements for the strings of the reorderable list used while editing: the handle's
 * name, Move up, Move down and the move announcements.
 */
@Immutable
public class MonarchFavoritesListLabels(
    public val title: String? = null,
    public val edit: String? = null,
    public val editLabel: String? = null,
    public val done: String? = null,
    public val remove: String? = null,
    public val removeLabel: ((title: String) -> String)? = null,
    public val removed: ((title: String) -> String)? = null,
    public val reorder: MonarchReorderableListLabels = MonarchReorderableListLabels(),
)
