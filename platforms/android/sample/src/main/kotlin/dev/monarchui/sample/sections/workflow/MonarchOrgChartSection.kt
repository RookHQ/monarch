@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.workflow

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.components.workflow.orgchart.MonarchOrgChart
import dev.monarchui.components.workflow.orgchart.MonarchOrgChartDottedLine
import dev.monarchui.components.workflow.orgchart.MonarchOrgChartLayout
import dev.monarchui.components.workflow.orgchart.MonarchOrgChartMoveTarget
import dev.monarchui.components.workflow.orgchart.MonarchOrgChartNode
import dev.monarchui.components.workflow.orgchart.MonarchOrgChartNodeKind
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay

/** A chevron that points to the end; it mirrors in right-to-left layouts. */
private val OrgChartChevron: ImageVector by lazy {
    ImageVector.Builder(
        name = "Chevron",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = true,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(8.6f, 7.4f)
            lineTo(10f, 6f)
            lineTo(16f, 12f)
            lineTo(10f, 18f)
            lineTo(8.6f, 16.6f)
            lineTo(13.2f, 12f)
            close()
        }
    }.build()
}

private val OrgChartPeople = listOf(
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
    MonarchOrgChartNode(id = "sam", parentId = "ana", name = "Sam Lee", title = "Head of finance", childCount = 2),
    MonarchOrgChartNode(id = "ola", parentId = "jo", name = "Ola Berg", title = "Engineer"),
    MonarchOrgChartNode(id = "kai", parentId = "jo", name = "Kai Moss", title = "Engineer"),
    MonarchOrgChartNode(
        id = "open",
        parentId = "jo",
        name = "Product designer",
        kind = MonarchOrgChartNodeKind.Vacancy,
    ),
)

private val OrgChartHeight = 420.dp

/** How long the sample pretends a team takes to load. */
private const val OrgChartLoadMillis = 800L

/** The catalogue page of OrgChart. */
object MonarchOrgChartSection : CataloguePage {
    override val title = "OrgChart"

    @Composable
    override fun Content() {
        var people by remember { mutableStateOf(OrgChartPeople) }
        var selected by rememberSaveable { mutableStateOf<String?>("ola") }
        val chevron: @Composable () -> Unit = {
            MonarchIcon(OrgChartChevron, contentDescription = null, size = MonarchIconSize.Sm)
        }
        CatalogueCaption("Chart: focus, dotted line, team loaded on demand, drag to move")
        MonarchOrgChart(
            nodes = people,
            expandIcon = chevron,
            modifier = Modifier.height(OrgChartHeight),
            focusId = selected,
            dottedLines = listOf(MonarchOrgChartDottedLine(from = "ola", to = "noor")),
            layout = MonarchOrgChartLayout.Chart,
            onSelect = { selected = it.id },
            onLoadChildren = { manager ->
                delay(OrgChartLoadMillis)
                people = people + listOf(
                    MonarchOrgChartNode(id = "lin", parentId = manager.id, name = "Lin Wu", title = "Accountant"),
                    MonarchOrgChartNode(id = "max", parentId = manager.id, name = "Max Roy", title = "Analyst"),
                )
            },
            onMoveRequest = { person, target ->
                people = people.map { node ->
                    if (node.id != person.id) return@map node
                    when (target) {
                        is MonarchOrgChartMoveTarget.Manager -> node.copyUnder(target.id)
                        MonarchOrgChartMoveTarget.TopLevel -> node.copyUnder(null)
                        MonarchOrgChartMoveTarget.Unspecified -> node
                    }
                }
            },
        )
        MonarchText(text = "Selected: ${selected ?: "nobody"}", color = MonarchTheme.colors.textMuted)
        CatalogueCaption("List, the phone form")
        MonarchOrgChart(
            nodes = OrgChartPeople,
            expandIcon = chevron,
            modifier = Modifier.height(OrgChartHeight),
            layout = MonarchOrgChartLayout.List,
        )
    }

    private fun MonarchOrgChartNode.copyUnder(manager: String?): MonarchOrgChartNode = MonarchOrgChartNode(
        id = id,
        parentId = manager,
        name = name,
        title = title,
        meta = meta,
        image = image,
        kind = kind,
        dashed = dashed,
        childCount = childCount,
    )
}
