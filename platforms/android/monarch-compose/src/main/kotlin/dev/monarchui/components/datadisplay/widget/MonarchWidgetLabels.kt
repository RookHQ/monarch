package dev.monarchui.components.datadisplay.widget

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchWidget`. Every `null`
 * keeps the built-in string `monarch_widget_<key>`.
 *
 * @property menu the TalkBack name of the menu button, given the title: "More actions for Revenue".
 * @property more the visible text of the menu button when it has no icon.
 * @property viewAll the visible text of the View all button.
 * @property viewAllLabel the TalkBack name of the View all button, given the title: "View all: Revenue".
 * @property loading what TalkBack reads for the loading placeholder, given the title: "Loading Revenue".
 * @property error the failure shown when `MonarchWidgetState.Failed` has no message.
 * @property empty the title shown when `MonarchWidgetState.Empty` has no message.
 * @property updated the footer line, given the relative time: "Updated 5 minutes ago".
 */
@Immutable
public class MonarchWidgetLabels(
    public val menu: ((title: String) -> String)? = null,
    public val more: String? = null,
    public val viewAll: String? = null,
    public val viewAllLabel: ((title: String) -> String)? = null,
    public val loading: ((title: String) -> String)? = null,
    public val error: String? = null,
    public val empty: String? = null,
    public val updated: ((relativeTime: String) -> String)? = null,
)
