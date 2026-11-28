@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.workflow.orgchart

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.tokens.MonarchTokens
import androidx.compose.ui.unit.dp

private val PreviewPeople = listOf(
    MonarchOrgChartNode(id = "ana", name = "Ana Silva", title = "Chief executive"),
    MonarchOrgChartNode(id = "jo", parentId = "ana", name = "Jo Park", title = "Head of engineering"),
    MonarchOrgChartNode(
        id = "noor",
        parentId = "ana",
        name = "Noor Ali",
        title = "People lead",
        meta = "Acting",
        dashed = true,
    ),
    MonarchOrgChartNode(id = "ola", parentId = "jo", name = "Ola Berg", title = "Engineer"),
    MonarchOrgChartNode(
        id = "open",
        parentId = "jo",
        name = "Product designer",
        kind = MonarchOrgChartNodeKind.Vacancy,
    ),
)

private val PreviewHeight = (MonarchTokens.Space.space24 * 4).dp

@MonarchPreviews
@Composable
internal fun MonarchOrgChartPreview() {
    PreviewFrame {
        MonarchOrgChart(
            nodes = PreviewPeople,
            expandIcon = { Text("›") },
            modifier = Modifier.height(PreviewHeight),
            focusId = "ola",
            dottedLines = listOf(MonarchOrgChartDottedLine(from = "ola", to = "noor")),
            layout = MonarchOrgChartLayout.Chart,
        )
        MonarchOrgChart(
            nodes = PreviewPeople,
            expandIcon = { Text("›") },
            modifier = Modifier.height(PreviewHeight),
            layout = MonarchOrgChartLayout.List,
            onMoveRequest = { _, _ -> },
        )
    }
}
