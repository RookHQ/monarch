package com.rookhq.monarch.components.datadisplay.dashboardgrid

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.datadisplay.widget.MonarchWidget
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

/** Lazy grids need a bounded height inside the scrolling preview column. */
private const val PreviewHeightSteps = 4

private val PreviewWidgets = listOf("Revenue", "Team load")

@MonarchPreviews
@Composable
internal fun MonarchDashboardGridPreview() {
    PreviewFrame {
        MonarchDashboardGrid(
            items = PreviewWidgets,
            key = { it },
            itemLabel = { it },
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * PreviewHeightSteps),
        ) { MonarchWidget(title = it) { Text("12,400") } }
        MonarchDashboardGrid(
            items = PreviewWidgets,
            key = { it },
            itemLabel = { it },
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * PreviewHeightSteps),
            onLayoutChange = {},
            editing = true,
        ) { MonarchWidget(title = it) { Text("12,400") } }
    }
}
