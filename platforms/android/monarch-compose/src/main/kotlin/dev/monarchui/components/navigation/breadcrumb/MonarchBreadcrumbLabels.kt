package dev.monarchui.components.navigation.breadcrumb

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchBreadcrumb`.
 * Every `null` keeps the built-in string `monarch_breadcrumb_<key>`.
 *
 * @property label the TalkBack name of the trail, "Breadcrumb".
 * @property more the TalkBack name of the button that opens the collapsed levels, from their count:
 * "Show 3 more levels".
 * @property current the state TalkBack reads on the last level, "Current page".
 */
@Immutable
public class MonarchBreadcrumbLabels(
    public val label: String? = null,
    public val more: ((count: Int) -> String)? = null,
    public val current: String? = null,
)
