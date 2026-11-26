package com.rookhq.monarch.components.search.facetedfilterpanel

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchFacetedFilterPanel`. Every `null` keeps the built-in string
 * `monarch_facetedfilterpanel_<key>`.
 *
 * @property title the panel heading, "Filters". Also the name TalkBack reads for the panel.
 * @property clearAll the button that clears every filter, "Clear all".
 * @property clearGroup the visible button that clears one group, "Clear".
 * @property clearGroupLabel what TalkBack reads for that button, from the group: "Clear Status filter".
 * @property activeCount the number of active filters in a group, under its name: "2 selected".
 * @property showMore the button that shows the hidden options, from their number: "Show 4 more".
 * @property showFewer the button that hides them again, "Show fewer".
 * @property noOptions shown in an options facet without options, "No options".
 * @property range the chosen range of a range facet, from the minimum and maximum with their unit: "20 – 150 MB".
 * @property expanded the state TalkBack reads for an open group, "Expanded".
 * @property collapsed the state TalkBack reads for a closed group, "Collapsed".
 * @property minimum the label of the minimum field of a range facet, "Minimum".
 * @property maximum the label of the maximum field of a range facet, "Maximum".
 */
@Immutable
public class MonarchFacetedFilterPanelLabels(
    public val title: String? = null,
    public val clearAll: String? = null,
    public val clearGroup: String? = null,
    public val clearGroupLabel: ((String) -> String)? = null,
    public val activeCount: ((Int) -> String)? = null,
    public val showMore: ((Int) -> String)? = null,
    public val showFewer: String? = null,
    public val noOptions: String? = null,
    public val range: ((String, String) -> String)? = null,
    public val expanded: String? = null,
    public val collapsed: String? = null,
    public val minimum: String? = null,
    public val maximum: String? = null,
)
