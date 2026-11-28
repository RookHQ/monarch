package dev.monarchui.components.datagrid.cursorlist

/** What a cursor list shows. */
internal enum class CursorListBody { Error, Loading, Empty, Items }

/** The rules of MonarchCursorList. It draws nothing of its own: its parts have their own tokens. */
internal object MonarchCursorListTokens {
    /**
     * What the list shows: with no items, the error state after a failure,
     * the placeholder while loading and the empty state when there is no
     * more; otherwise the items and the footer.
     */
    fun body(count: Int, hasMore: Boolean, loading: Boolean, failed: Boolean): CursorListBody = when {
        count > 0 -> CursorListBody.Items
        failed -> CursorListBody.Error
        loading -> CursorListBody.Loading
        hasMore -> CursorListBody.Items
        else -> CursorListBody.Empty
    }

    /**
     * The index to move focus to once a page asked for from the footer has
     * arrived: the first new item, or `null` while it is still loading or
     * when nothing was added.
     */
    fun focusTarget(pendingFrom: Int?, count: Int, loading: Boolean): Int? =
        pendingFrom?.takeIf { !loading && count > it }

    /** The placeholder shows fewer rows than the default, as on the web. */
    const val PLACEHOLDER_ROWS: Int = 3
}
