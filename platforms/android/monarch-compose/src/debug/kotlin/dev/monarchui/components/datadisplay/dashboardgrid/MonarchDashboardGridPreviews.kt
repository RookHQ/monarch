package dev.monarchui.components.datadisplay.dashboardgrid

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.components.datadisplay.widget.MonarchWidget
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

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
