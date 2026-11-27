package com.rookhq.monarch.components.datagrid.loadmore

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchLoadMore`. Every `null`
 * uses the built-in string named after it in `strings_loadmore.xml`.
 *
 * @property loadMore the label of the button: "Load more".
 * @property loading the state of the button while a page loads: "Loading".
 * @property failed the message after a failed page: "Couldn't load more.". Also announced.
 * @property retry the label of the button after a failure: "Retry".
 * @property end the line at the end of the list: "That's everything". Also announced.
 * @property count writes the count from the items on screen and the total, when known: "Showing 40 of 120 items".
 * @property loaded writes the announcement after a page loads from the number of items it added:
 * "20 more items loaded.".
 */
@Immutable
public class MonarchLoadMoreLabels(
    public val loadMore: String? = null,
    public val loading: String? = null,
    public val failed: String? = null,
    public val retry: String? = null,
    public val end: String? = null,
    public val count: ((loaded: Int, total: Int?) -> String)? = null,
    public val loaded: ((added: Int) -> String)? = null,
)
