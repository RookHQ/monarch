package dev.monarchui.components.datagrid.loadmore

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchLoadMorePreview() {
    PreviewFrame {
        MonarchLoadMore(hasMore = true, onLoadMore = {}, loadedCount = 40, totalCount = 120)
        MonarchLoadMore(hasMore = true, onLoadMore = {}, loading = true, loadedCount = 40)
        MonarchLoadMore(hasMore = true, onLoadMore = {}, failed = true)
        MonarchLoadMore(hasMore = false, onLoadMore = {}, loadedCount = 120, totalCount = 120)
    }
}
