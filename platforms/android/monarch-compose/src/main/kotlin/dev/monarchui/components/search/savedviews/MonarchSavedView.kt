package dev.monarchui.components.search.savedviews

import androidx.compose.runtime.Immutable

/**
 * One named view of a list in a `MonarchSavedViews` menu: a set of filters,
 * a sort and columns that the app stores.
 *
 * @property id a stable identifier, unique in the menu.
 * @property name the name the person gave the view, shown in the button and the menu.
 * @property count how many items the view has, shown next to its name.
 * @property shared the view is shared with other people: "Shared" is shown.
 * @property default the view opens by default: "Default view" is shown, and Set as default is not offered.
 * @property readOnly a built-in view or someone else's: it cannot be saved over, renamed or deleted.
 * @property group the heading the view is listed under, such as "Built-in" or "Shared with me". Views keep
 * their order within a group; groups are in the order they first appear.
 */
@Immutable
public class MonarchSavedView(
    public val id: String,
    public val name: String,
    public val count: Int? = null,
    public val shared: Boolean = false,
    public val default: Boolean = false,
    public val readOnly: Boolean = false,
    public val group: String? = null,
)
