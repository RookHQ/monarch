package com.rookhq.monarch.components.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.forcegraph.ForceGraphBounds
import com.rookhq.monarch.components.charts.forcegraph.ForceGraphGeometry
import com.rookhq.monarch.components.charts.forcegraph.ForceGraphPoint
import com.rookhq.monarch.components.charts.forcegraph.ForceGraphProjection
import com.rookhq.monarch.components.charts.forcegraph.ForceGraphSimulation
import com.rookhq.monarch.components.charts.forcegraph.ForceGraphViewState
import com.rookhq.monarch.components.charts.forcegraph.MonarchForceGraph
import com.rookhq.monarch.components.charts.forcegraph.MonarchForceGraphEdge
import com.rookhq.monarch.components.charts.forcegraph.MonarchForceGraphEdgeKind
import com.rookhq.monarch.components.charts.forcegraph.MonarchForceGraphGroup
import com.rookhq.monarch.components.charts.forcegraph.MonarchForceGraphLabels
import com.rookhq.monarch.components.charts.forcegraph.MonarchForceGraphLayoutOptions
import com.rookhq.monarch.components.charts.forcegraph.MonarchForceGraphNode
import com.rookhq.monarch.components.charts.forcegraph.MonarchForceGraphTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.onNodeWithTextOrDescription
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val notes = listOf(
    MonarchForceGraphNode("Roadmap", group = "plans", description = "Plan"),
    MonarchForceGraphNode("Pricing", group = "plans"),
    MonarchForceGraphNode("Interviews", group = "research"),
)
private val noteLinks = listOf(
    MonarchForceGraphEdge("Roadmap", "Pricing"),
    MonarchForceGraphEdge("Roadmap", "Interviews", kind = "cites"),
)
private val FixtureHeight = 96.dp
private val TestHeight = 200.dp

internal object MonarchForceGraphFixtures : ComponentFixtures {
    override val id = "force-graph"
    override val fixtures = listOf(
        fixture("one group, selectable, selected") {
            MonarchForceGraph(
                title = "Notes",
                nodes = notes.map { MonarchForceGraphNode(it.label, group = "plans") },
                edges = noteLinks,
                height = FixtureHeight,
                selectedId = "Pricing",
                onSelect = {},
            )
        },
        fixture("groups in the legend") {
            MonarchForceGraph(title = "Notes", nodes = notes, edges = noteLinks, height = FixtureHeight)
        },
        fixture("dashed link kind, orphan, no labels") {
            MonarchForceGraph(
                title = "Notes",
                nodes = notes.map { MonarchForceGraphNode(it.label) } + MonarchForceGraphNode("Inbox"),
                edges = noteLinks,
                edgeKinds = listOf(MonarchForceGraphEdgeKind("cites", "Cites", MonarchChartColor.Data3, dashed = true)),
                height = FixtureHeight,
                showLabels = false,
            )
        },
        fixture("no nodes") {
            MonarchForceGraph(title = "Notes", nodes = emptyList(), edges = emptyList(), height = FixtureHeight)
        },
        fixture("loading") {
            MonarchForceGraph(
                title = "Notes",
                nodes = notes,
                edges = noteLinks,
                status = MonarchChartFrameStatus.Loading,
                height = FixtureHeight,
            )
        },
        fixture("error with retry") {
            MonarchForceGraph(
                title = "Notes",
                nodes = notes,
                edges = noteLinks,
                status = MonarchChartFrameStatus.Error,
                onRetry = {},
                height = FixtureHeight,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchForceGraphTest {
    @get:Rule
    val rule = createComposeRule()

    private val roadmap = "Roadmap, plans. 2 connections: Pricing, Interviews"
    private val pricing = "Pricing, plans. 1 connection: Roadmap"
    private val interviews = "Interviews, research. 1 connection: Roadmap"

    @Test
    fun theCanvasIsNamedWithTheSummaryAndEveryNodeIsReadInLabelOrder() {
        rule.setMonarchContent {
            MonarchForceGraph(title = "Notes", nodes = notes, edges = noteLinks, height = TestHeight)
        }
        rule.onNodeWithContentDescription("3 nodes and 2 links in 2 groups. Most connected: Roadmap (2)")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
        listOf(interviews, pricing, roadmap).forEachIndexed { position, text ->
            rule.onNodeWithContentDescription(text)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, position.toFloat()))
                .assertHasNoClickAction()
        }
        rule.onNodeWithContentDescription("Legend").assertExists()
    }

    @Test
    fun nodesAreButtonsWithTalkBackWhenSelectable() {
        val chosen = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchForceGraph(
                title = "Notes",
                nodes = notes,
                edges = noteLinks,
                height = TestHeight,
                selectedId = "Pricing",
                onSelect = { chosen += it.id },
            )
        }
        rule.onNodeWithContentDescription(pricing).assertIsSelected()
        rule.onNodeWithContentDescription(roadmap)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        assertEquals(listOf("Roadmap"), chosen)
        rule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite) and
                SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("$roadmap. Plan")),
        ).assertExists()
    }

    @Test
    fun aTapOnANodeInspectsIt() {
        val chosen = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchForceGraph(
                title = "Notes",
                nodes = notes,
                edges = noteLinks,
                height = TestHeight,
                onSelect = { chosen += it.id },
            )
        }
        // The node's element has no pointer input, so the tap reaches the canvas under it.
        rule.onNodeWithContentDescription(interviews).performClick()
        rule.waitForIdle()
        assertEquals(listOf("Interviews"), chosen)
        rule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite) and
                SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(interviews)),
        ).assertExists()
    }

    @Test
    fun zoomStopsAtItsLimitsAndResetViewRestoresIt() {
        rule.setMonarchContent {
            MonarchForceGraph(title = "Notes", nodes = notes, edges = noteLinks, height = TestHeight)
        }
        val zoomIn = rule.onNode((hasText("Zoom in") or hasContentDescription("Zoom in")) and hasClickAction())
        repeat(5) { zoomIn.performClick() }
        zoomIn.assertIsNotEnabled()
        rule.onNode(hasText("Reset view") and hasClickAction())
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        zoomIn.assertIsEnabled()
        val zoomOut = rule.onNode((hasText("Zoom out") or hasContentDescription("Zoom out")) and hasClickAction())
        repeat(5) { zoomOut.performClick() }
        zoomOut.assertIsNotEnabled()
    }

    @Test
    fun viewAsTableListsEveryNodeWithItsGroupAndLinks() {
        rule.setMonarchContent {
            MonarchForceGraph(
                title = "Notes",
                nodes = notes + MonarchForceGraphNode("Inbox"),
                edges = noteLinks,
                height = TestHeight,
            )
        }
        rule.onNode(hasText("View as table") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Node, Group, Connections").assertExists()
        rule.onNodeWithContentDescription("Inbox, Group No group, Connections 0 connections").assertExists()
        rule.onNodeWithContentDescription("Roadmap, Group plans, Connections Pricing, Interviews")
            .assertExists()
    }

    @Test
    fun invalidLinksAndRepeatedIdsAreIgnored() {
        rule.setMonarchContent {
            MonarchForceGraph(
                title = "Notes",
                nodes = notes + MonarchForceGraphNode("Roadmap again", id = "Roadmap"),
                edges = noteLinks +
                    MonarchForceGraphEdge("Roadmap", "Roadmap") +
                    MonarchForceGraphEdge("Roadmap", "Nowhere"),
                groups = listOf(
                    MonarchForceGraphGroup("plans", "Plans"),
                    MonarchForceGraphGroup("research", "Research"),
                ),
                height = TestHeight,
            )
        }
        rule.onNodeWithContentDescription("3 nodes and 2 links in 2 groups. Most connected: Roadmap (2)").assertExists()
        rule.onNodeWithContentDescription("Pricing, Plans. 1 connection: Roadmap").assertExists()
    }

    @Test
    fun noNodesShowTheEmptyState() {
        rule.setMonarchContent {
            MonarchForceGraph(
                title = "Notes",
                nodes = emptyList(),
                edges = emptyList(),
                emptyMessage = "Link two notes.",
            )
        }
        rule.onNodeWithText("No data yet").assertExists()
        rule.onNodeWithText("Link two notes.").assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchForceGraph(
                title = "Notes",
                nodes = notes,
                edges = noteLinks,
                edgeKinds = listOf(MonarchForceGraphEdgeKind("cites", "Cite")),
                height = TestHeight,
                labels = MonarchForceGraphLabels(
                    zoomIn = "Agrandir",
                    nodeCount = { "$it nœuds" },
                    linkCount = { "$it liens" },
                    groupCount = { "$it groupes" },
                    connectionCount = { "$it liaisons" },
                    size = "%1\$s et %2\$s",
                    inGroups = "%1\$s dans %2\$s",
                    mostConnected = "Le plus relié : %1\$s (%2\$s)",
                    describeLinked = "%1\$s (%2\$s), %3\$s : %4\$s",
                    linkWithKind = "%1\$s [%2\$s]",
                    edgeLegend = "Types de lien",
                ),
            )
        }
        rule.onNodeWithTextOrDescription("Agrandir").assertExists()
        rule.onNodeWithContentDescription("3 nœuds et 2 liens dans 2 groupes. Le plus relié : Roadmap (2)")
            .assertExists()
        rule.onNodeWithContentDescription("Roadmap (plans), 2 liaisons : Pricing, Interviews [Cite]").assertExists()
        rule.onNodeWithContentDescription("Types de lien").assertExists()
    }

    @Test
    fun theGraphIsMirroredRightToLeft() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchForceGraph(
                title = "Notes",
                nodes = notes,
                edges = noteLinks,
                height = TestHeight,
                showLabels = false,
            )
        }
        val summary = "3 nodes and 2 links in 2 groups. Most connected: Roadmap (2)"
        fun fromStart(rtl: Boolean): Float {
            val canvas = rule.onNodeWithContentDescription(summary).getUnclippedBoundsInRoot()
            val node = rule.onNodeWithContentDescription(pricing).getUnclippedBoundsInRoot()
            val centre = (node.left + node.right) / 2
            return (if (rtl) canvas.right - centre else centre - canvas.left).value
        }
        val ltr = fromStart(rtl = false)
        environment.layoutDirection = LayoutDirection.Rtl
        rule.waitForIdle()
        val rtl = fromStart(rtl = true)
        assertTrue(abs(ltr - rtl) < 1f, "Expected the node mirrored: $ltr from the start in LTR, $rtl in RTL")
    }

    @Test
    fun theControlsWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchForceGraph(title = "Notes", nodes = notes, edges = noteLinks, height = TestHeight)
        }
        rule.onNodeWithText("Reset view", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithTextOrDescription("Zoom out", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theLayoutIsDeterministicCentredAndSpreadsLinkedNodes() {
        val ids = listOf("a", "b", "c", "d")
        val links = listOf(0 to 1, 1 to 2, 2 to 3)
        val options = MonarchForceGraphLayoutOptions()
        val first = ForceGraphSimulation.layout(ids, links, options)
        assertEquals(first, ForceGraphSimulation.layout(ids, links, options))
        assertTrue(abs(first.sumOf { it.x }) < 0.1 && abs(first.sumOf { it.y }) < 0.1, "Not centered: $first")
        for ((s, t) in links) {
            val d = Math.hypot(first[s].x - first[t].x, first[s].y - first[t].y)
            assertTrue(d > options.collide * 0.9, "Linked nodes overlap: $d")
        }
        assertEquals(300, ForceGraphSimulation.defaultTicks(300))
        assertEquals(150, ForceGraphSimulation.defaultTicks(600))
        assertEquals(60, ForceGraphSimulation.defaultTicks(10_000))
        val large = ForceGraphSimulation.layout(
            (0 until 320).map { "n$it" },
            (1 until 320).map { it - 1 to it },
            MonarchForceGraphLayoutOptions(ticks = 4),
        )
        assertEquals(320, large.size)
        assertTrue(large.all { it.x.isFinite() && it.y.isFinite() })
        val single = ForceGraphSimulation.layout(listOf("only"), emptyList(), options)
        assertEquals(listOf(ForceGraphPoint(0.0, 0.0)), single)
    }

    @Test
    fun theProjectionMirrorsRightToLeftAndRoundTrips() {
        val bounds = ForceGraphBounds.of(listOf(ForceGraphPoint(-50.0, -20.0), ForceGraphPoint(50.0, 20.0)))
        val ltr = ForceGraphProjection(200f, 100f, 1.0, bounds, 1.0, Offset(10f, 0f), rtl = false)
        val rtl = ForceGraphProjection(200f, 100f, 1.0, bounds, 1.0, Offset(10f, 0f), rtl = true)
        val point = ForceGraphPoint(-50.0, 20.0)
        assertEquals(Offset(60f, 70f), ltr.toScreen(point))
        assertEquals(Offset(140f, 70f), rtl.toScreen(point))
        val back = rtl.fromScreen(rtl.toScreen(point))
        assertTrue(abs(back.x - point.x) < 1e-3 && abs(back.y - point.y) < 1e-3)
        assertEquals(2.5, ForceGraphGeometry.fit(bounds, 400f, 400f, 10f, 0f))
        assertEquals(0.5, ForceGraphGeometry.fit(bounds, 70f, 400f, 10f, 0f))
        assertEquals(1, ForceGraphGeometry.hit(listOf(Offset(0f, 0f), Offset(30f, 0f)), Offset(25f, 0f), 24f))
        assertNull(ForceGraphGeometry.hit(listOf(Offset(0f, 0f)), Offset(30f, 0f), 24f))
    }

    @Test
    fun theViewZoomsAroundTheCentreWithinItsLimits() {
        val view = ForceGraphViewState()
        view.panBy(Offset(10f, 4f), rtl = false)
        view.zoomBy(2.0)
        assertEquals(2.0, view.zoom)
        assertEquals(Offset(20f, 8f), view.pan)
        view.zoomBy(10.0)
        assertEquals(3.0, view.zoom)
        assertFalse(view.canZoomIn)
        view.panBy(Offset(5f, 0f), rtl = true)
        assertEquals(Offset(25f, 12f), view.pan)
        view.moved["a"] = ForceGraphPoint(1.0, 1.0)
        view.reset()
        assertEquals(1.0, view.zoom)
        assertEquals(Offset.Zero, view.pan)
        assertTrue(view.moved.isEmpty())
    }

    @Test
    fun coloursComeFromTheThemeInEveryTheme() {
        for (theme in AllThemes) {
            val colors = MonarchColors(theme.colors)
            val dashed = MonarchForceGraphEdgeKind("k", "K", MonarchChartColor.Data2)
            assertEquals(colors.data3, MonarchForceGraphTokens.nodeColor(MonarchChartColor.Data3, false, colors))
            assertEquals(colors.textDisabled, MonarchForceGraphTokens.nodeColor(MonarchChartColor.Data3, true, colors))
            assertEquals(colors.data2, MonarchForceGraphTokens.edgeColor(dashed, false, colors))
            assertEquals(colors.textSubtle, MonarchForceGraphTokens.edgeColor(null, false, colors))
            assertEquals(colors.borderSubtle, MonarchForceGraphTokens.edgeColor(dashed, true, colors))
            assertEquals(colors.bgSurface, MonarchForceGraphTokens.nodeOutline(colors))
            assertEquals(colors.bgSurface, MonarchForceGraphTokens.orphanFill(colors))
            assertEquals(colors.textSubtle, MonarchForceGraphTokens.orphanOutline(false, colors))
            assertEquals(colors.accentSolid, MonarchForceGraphTokens.selectedRing(colors))
            assertEquals(colors.textDefault, MonarchForceGraphTokens.inspectedRing(colors))
            assertEquals(colors.bgSubtle, MonarchForceGraphTokens.canvasColor(colors))
            assertEquals(colors.textDefault, MonarchForceGraphTokens.labelColor(false, colors))
        }
    }
}
