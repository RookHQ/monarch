package com.rookhq.monarch.components.datagrid.cursorlist

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchCursorListPreview() {
    PreviewFrame {
        MonarchCursorList(
            items = List(4) { "Ticket ${it + 1}" },
            label = "Tickets",
            hasMore = true,
            onLoadMore = {},
            totalCount = 40,
            modifier = Modifier.height(MonarchTheme.spacing.space24 * 3),
        ) { MonarchText(text = it) }
    }
}

@MonarchPreviews
@Composable
internal fun MonarchCursorListStatesPreview() {
    PreviewFrame {
        MonarchCursorList(items = emptyList<String>(), label = "Tickets", hasMore = false, onLoadMore = {}) {}
        MonarchCursorList(
            items = emptyList<String>(),
            label = "Tickets",
            hasMore = true,
            onLoadMore = {},
            failed = true,
        ) {}
    }
}
