package dev.monarchui.components.datagrid.cursorlist

import androidx.compose.runtime.Immutable
import dev.monarchui.components.datagrid.loadmore.MonarchLoadMoreLabels

/**
 * Replacements for the built-in strings of a `MonarchCursorList`. Every
 * `null` uses the built-in string named after it in `strings_cursorlist.xml`.
 *
 * @property loading the name of the first-page placeholder: "Loading".
 * @property emptyTitle the title of the empty state: "Nothing here yet".
 * @property emptyMessage the message of the empty state: "Items appear here once they are added.".
 * @property errorTitle the title of the error state when the first page fails: "This list could not be loaded".
 * @property retry the retry button of the error state: "Retry".
 * @property footer the strings of the `MonarchLoadMore` footer: the button, the count, the failure and the end.
 */
@Immutable
public class MonarchCursorListLabels(
    public val loading: String? = null,
    public val emptyTitle: String? = null,
    public val emptyMessage: String? = null,
    public val errorTitle: String? = null,
    public val retry: String? = null,
    public val footer: MonarchLoadMoreLabels = MonarchLoadMoreLabels(),
)
