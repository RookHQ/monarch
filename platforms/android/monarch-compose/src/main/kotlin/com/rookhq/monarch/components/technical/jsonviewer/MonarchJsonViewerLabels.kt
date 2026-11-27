package com.rookhq.monarch.components.technical.jsonviewer

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchJsonViewer`. Every `null` uses the built-in string in
 * `strings_jsonviewer.xml`.
 *
 * @property label the spoken name of the tree: "JSON".
 * @property search the label of the search field: "Search keys and values".
 * @property searchPrompt the placeholder of the search field: "Search".
 * @property clearSearch the spoken name of the button that clears the search: "Clear search".
 * @property matches the match count, from the count and whether the search stopped early: "No matches", "1 match",
 * "12 matches", "500+ matches".
 * @property collapseAll the button that closes every node: "Collapse all".
 * @property copyPath the button that copies the selected path: "Copy path".
 * @property copyValue the button that copies the selected value: "Copy value".
 * @property selectedPath spoken before the selected path: "Selected".
 * @property keys the summary of an object, from the number of keys: "3 keys".
 * @property items the summary of an array, from the number of items: "2 items".
 * @property showMore the "Show more" row, from how many it adds and how many are hidden: "Show 100 more (250
 * hidden)".
 * @property truncated added after a cut string, from its full length: "… (1,200 characters)".
 * @property expanded the spoken state of an open object or array: "Expanded".
 * @property collapsed the spoken state of a closed object or array: "Collapsed".
 */
@Immutable
public class MonarchJsonViewerLabels(
    public val label: String? = null,
    public val search: String? = null,
    public val searchPrompt: String? = null,
    public val clearSearch: String? = null,
    public val matches: ((count: Int, capped: Boolean) -> String)? = null,
    public val collapseAll: String? = null,
    public val copyPath: String? = null,
    public val copyValue: String? = null,
    public val selectedPath: String? = null,
    public val keys: ((count: Int) -> String)? = null,
    public val items: ((count: Int) -> String)? = null,
    public val showMore: ((next: Int, remaining: Int) -> String)? = null,
    public val truncated: ((length: Int) -> String)? = null,
    public val expanded: String? = null,
    public val collapsed: String? = null,
)
