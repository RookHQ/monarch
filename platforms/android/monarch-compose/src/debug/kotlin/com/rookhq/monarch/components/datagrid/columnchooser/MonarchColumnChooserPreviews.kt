package com.rookhq.monarch.components.datagrid.columnchooser

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

/** Lazy lists need a bounded height inside the scrolling preview column. */
private const val PreviewHeightSteps = 4

private val PreviewColumns = listOf(
    MonarchColumnChooserColumn("Name", id = "name", hideable = false),
    MonarchColumnChooserColumn("Status", id = "status"),
    MonarchColumnChooserColumn("Owner", id = "owner"),
)

@MonarchPreviews
@Composable
internal fun MonarchColumnChooserPreview() {
    PreviewFrame {
        MonarchColumnChooser(
            columns = PreviewColumns,
            hiddenColumns = setOf("owner"),
            onHiddenColumnsChange = {},
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * PreviewHeightSteps),
            onReset = {},
        )
        MonarchColumnChooser(
            columns = PreviewColumns,
            hiddenColumns = emptySet(),
            onHiddenColumnsChange = {},
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * PreviewHeightSteps),
            onOrderChange = {},
        )
    }
}
