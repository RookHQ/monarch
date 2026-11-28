@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.workflow

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.workflow.orgchart.MonarchOrgChart
import dev.monarchui.components.workflow.orgchart.MonarchOrgChartDottedLine
import dev.monarchui.components.workflow.orgchart.MonarchOrgChartDragState
import dev.monarchui.components.workflow.orgchart.MonarchOrgChartLabels
import dev.monarchui.components.workflow.orgchart.MonarchOrgChartLayout
import dev.monarchui.components.workflow.orgchart.MonarchOrgChartMoveTarget
import dev.monarchui.components.workflow.orgchart.MonarchOrgChartNode
import dev.monarchui.components.workflow.orgchart.MonarchOrgChartNodeKind
import dev.monarchui.components.workflow.orgchart.MonarchOrgChartTokens
import dev.monarchui.components.workflow.orgchart.MonarchOrgChartTree
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val Ana = MonarchOrgChartNode(id = "ana", name = "Ana Silva", title = "Chief executive")
private val Jo = MonarchOrgChartNode(id = "jo", parentId = "ana", name = "Jo Park", title = "Head of engineering")
private val Noor = MonarchOrgChartNode(id = "noor", parentId = "ana", name = "Noor Ali", meta = "Acting", dashed = true)
private val Ola = MonarchOrgChartNode(id = "ola", parentId = "jo", name = "Ola Berg")
private val Open = MonarchOrgChartNode(
    id = "open",
    parentId = "jo",
    name = "Product designer",
    kind = MonarchOrgChartNodeKind.Vacancy,
)
private typealias Move = Pair<String, MonarchOrgChartMoveTarget>

private val People = listOf(Ana, Jo, Noor, Ola, Open)
private val ChartHeight = 600.dp

private val Chevron: @androidx.compose.runtime.Composable (
) -> Unit = { MonarchIcon(TestIcon, contentDescription = null) }

internal object MonarchOrgChartFixtures : ComponentFixtures {
    override val id = "org-chart"
    override val fixtures = listOf(
        fixture("list, focused, move on") {
            MonarchOrgChart(
                nodes = People,
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                focusId = "ola",
                layout = MonarchOrgChartLayout.List,
                onSelect = {},
                onMoveRequest = { _, _ -> },
            )
        },
        fixture("chart with dotted line") {
            MonarchOrgChart(
                nodes = People,
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                dottedLines = listOf(MonarchOrgChartDottedLine(from = "ola", to = "noor")),
                layout = MonarchOrgChartLayout.Chart,
                onSelect = {},
            )
        },
        fixture("chart, teams hidden, no zoom") {
            MonarchOrgChart(
                nodes = People,
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                expanded = emptySet(),
                layout = MonarchOrgChartLayout.Chart,
                zoomable = false,
            )
        },
        fixture("disabled, unloaded team") {
            MonarchOrgChart(
                nodes = listOf(Ana, MonarchOrgChartNode(id = "jo", parentId = "ana", name = "Jo Park", childCount = 4)),
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                enabled = false,
                onSelect = {},
            )
        },
        fixture("empty") {
            MonarchOrgChart(nodes = emptyList(), expandIcon = Chevron, modifier = Modifier.height(ChartHeight))
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchOrgChartTest {
    @get:Rule
    val rule = createComposeRule()

    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    private fun actionLabels(description: String): List<String> =
        rule.onNodeWithContentDescription(description).fetchSemanticsNode()
            .config.getOrElse(SemanticsActions.CustomActions) { emptyList() }.map { it.label }

    @Test
    fun aChartWiderThanTheScreenStartsOnTheTopOfTheTree() {
        rule.setMonarchContent {
            MonarchOrgChart(
                nodes = People,
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight).requiredWidth(320.dp),
                layout = MonarchOrgChartLayout.Chart,
            )
        }
        rule.waitForIdle()
        val bounds = rule.onNodeWithContentDescription("Ana Silva, Chief executive", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val width = with(rule.density) { 320.dp.toPx() }
        assertTrue(bounds.left >= 0f && bounds.right <= width, "the top card is cut at the edge: $bounds in $width")
    }

    @Test
    fun eachCardIsOneElementWithItsNotesAndTeamState() {
        rule.setMonarchContent {
            MonarchOrgChart(
                nodes = People,
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                dottedLines = listOf(MonarchOrgChartDottedLine(from = "ola", to = "noor")),
                layout = MonarchOrgChartLayout.List,
            )
        }
        rule.onNodeWithContentDescription("Organization chart").assertExists()
        val ana = rule.onNodeWithContentDescription("Ana Silva, Chief executive")
        assertEquals("Team shown, 2 direct reports", ana.stateDescription())
        rule.onNodeWithContentDescription("Noor Ali, Acting, Temporary reporting line").assertExists()
        rule.onNodeWithContentDescription("Ola Berg, Also reports to Noor Ali").assertExists()
        rule.onNodeWithContentDescription("Product designer, Open position").assertExists()
        assertNull(rule.onNodeWithContentDescription("Ola Berg, Also reports to Noor Ali").stateDescription())
    }

    @Test
    fun theTopTwoLevelsStartOpenAndTheChipTogglesATeam() {
        rule.setMonarchContent {
            MonarchOrgChart(
                nodes = People,
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                layout = MonarchOrgChartLayout.List,
            )
        }
        rule.onNodeWithContentDescription("Ola Berg").assertExists()
        rule.onNodeWithContentDescription("Hide team of Jo Park")
            .assert(isButton)
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithContentDescription("Ola Berg").assertDoesNotExist()
        assertEquals(
            "Team hidden, 2 direct reports",
            rule.onNodeWithContentDescription("Jo Park, Head of engineering").stateDescription(),
        )
        rule.onNodeWithContentDescription("Show team of Jo Park").performClick()
        rule.onNodeWithContentDescription("Ola Berg").assertExists()
    }

    @Test
    fun cardsHaveTeamAndMoveActions() {
        val moves = mutableListOf<Pair<String, MonarchOrgChartMoveTarget>>()
        rule.setMonarchContent {
            MonarchOrgChart(
                nodes = People,
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                layout = MonarchOrgChartLayout.List,
                onMoveRequest = { node, target -> moves += node.id to target },
            )
        }
        assertEquals(listOf("Hide team of Jo Park", "Move"), actionLabels("Jo Park, Head of engineering"))
        assertEquals(emptyList(), actionLabels("Product designer, Open position"))
        val move = rule.onNodeWithContentDescription("Ola Berg").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].single { it.label == "Move" }
        rule.runOnIdle { move.action() }
        assertEquals(listOf<Move>("ola" to MonarchOrgChartMoveTarget.Unspecified), moves)
        rule.onNodeWithText("Touch and hold a person, then drag them onto their new manager.").assertExists()
    }

    @Test
    fun theFocusedPersonIsSelectedAndTheirPathOpens() {
        rule.setMonarchContent {
            MonarchOrgChart(
                nodes = People + MonarchOrgChartNode(id = "kai", parentId = "ola", name = "Kai Moss"),
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                focusId = "kai",
                layout = MonarchOrgChartLayout.List,
            )
        }
        rule.onNodeWithContentDescription("Kai Moss").assertIsSelected()
    }

    @Test
    fun selectingACardCallsBack() {
        var selected: String? = null
        rule.setMonarchContent {
            MonarchOrgChart(
                nodes = People,
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                layout = MonarchOrgChartLayout.List,
                onSelect = { selected = it.id },
            )
        }
        rule.onNodeWithContentDescription(
            "Ola Berg",
        ).assert(isButton).assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals("ola", selected)
    }

    @Test
    fun withoutOnSelectCardsAreNotButtons() {
        rule.setMonarchContent {
            MonarchOrgChart(
                nodes = People,
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                layout = MonarchOrgChartLayout.List,
            )
        }
        rule.onNodeWithContentDescription("Ola Berg").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun aDisabledChartDoesNotRespond() {
        rule.setMonarchContent {
            MonarchOrgChart(
                nodes = People,
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                layout = MonarchOrgChartLayout.List,
                enabled = false,
                onSelect = {},
                onMoveRequest = { _, _ -> },
            )
        }
        rule.onNodeWithContentDescription("Hide team of Jo Park").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Ola Berg").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        assertEquals(emptyList(), actionLabels("Ola Berg"))
    }

    @Test
    fun aTeamLoadsBeforeItOpens() {
        var people by mutableStateOf(
            listOf(Ana, MonarchOrgChartNode(id = "jo", parentId = "ana", name = "Jo Park", childCount = 1)),
        )
        val gate = CompletableDeferred<Unit>()
        rule.setMonarchContent {
            MonarchOrgChart(
                nodes = people,
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                layout = MonarchOrgChartLayout.List,
                onLoadChildren = { manager ->
                    gate.await()
                    people = people + MonarchOrgChartNode(id = "ola", parentId = manager.id, name = "Ola Berg")
                },
            )
        }
        rule.onNodeWithContentDescription("Show team of Jo Park").performClick()
        assertEquals("Loading team", rule.onNodeWithContentDescription("Show team of Jo Park").stateDescription())
        rule.onNodeWithContentDescription("Show team of Jo Park").assertIsNotEnabled()
        gate.complete(Unit)
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Ola Berg").assertExists()
        rule.onNodeWithContentDescription("Hide team of Jo Park").assertExists()
    }

    @Test
    fun aFailedLoadIsAnnouncedPolitely() {
        rule.setMonarchContent {
            MonarchOrgChart(
                nodes = listOf(Ana, MonarchOrgChartNode(id = "jo", parentId = "ana", name = "Jo Park", childCount = 2)),
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                layout = MonarchOrgChartLayout.List,
                onLoadChildren = { error("offline") },
            )
        }
        rule.onNodeWithContentDescription("Show team of Jo Park").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Could not load the team of Jo Park.")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        rule.onNodeWithContentDescription("Show team of Jo Park").assertExists()
    }

    @Test
    fun aControlledChartReportsTheNewSet() {
        val reported = mutableListOf<Set<String>>()
        rule.setMonarchContent {
            MonarchOrgChart(
                nodes = People,
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                expanded = setOf("ana"),
                onExpandedChange = { reported += it },
                layout = MonarchOrgChartLayout.List,
            )
        }
        rule.onNodeWithContentDescription("Show team of Jo Park").performClick()
        assertEquals(listOf(setOf("ana", "jo")), reported)
        rule.onNodeWithContentDescription("Ola Berg").assertDoesNotExist()
    }

    @Test
    fun dragOntoAnotherCardRequestsAMove() {
        val moves = mutableListOf<Pair<String, MonarchOrgChartMoveTarget>>()
        rule.setMonarchContent {
            MonarchOrgChart(
                nodes = People,
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                layout = MonarchOrgChartLayout.List,
                onMoveRequest = { node, target -> moves += node.id to target },
            )
        }
        val noor = rule.onNodeWithContentDescription(
            "Noor Ali, Acting, Temporary reporting line",
        ).fetchSemanticsNode().boundsInRoot
        val ola = rule.onNodeWithContentDescription("Ola Berg").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithContentDescription("Ola Berg").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveTo(center + (noor.center - ola.center) / 2f)
            moveTo(center + (noor.center - ola.center))
            up()
        }
        assertEquals(listOf<Move>("ola" to MonarchOrgChartMoveTarget.Manager("noor")), moves)
    }

    @Test
    fun theZoomButtonsChangeTheLevelWithinLimits() {
        rule.setMonarchContent {
            MonarchOrgChart(
                nodes = People,
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                layout = MonarchOrgChartLayout.Chart,
            )
        }
        rule.onNodeWithContentDescription("Reset zoom, 100%")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        rule.onNodeWithContentDescription("Zoom in").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("Reset zoom, 110%").performClick()
        repeat(6) { rule.onNodeWithContentDescription("Zoom out").performClick() }
        rule.onNodeWithContentDescription("Reset zoom, 40%").assertExists()
        rule.onNodeWithContentDescription("Zoom out").assertIsNotEnabled()
    }

    @Test
    fun chipsKeepA48dpTouchTargetOnTheScreenAtEveryZoomLevel() {
        rule.setMonarchContent {
            MonarchOrgChart(
                nodes = People,
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                layout = MonarchOrgChartLayout.Chart,
            )
        }
        val chips = SemanticsMatcher("a team chip") { node ->
            node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().any { "team of" in it }
        }
        fun assertChips() {
            val nodes = rule.onAllNodes(chips).fetchSemanticsNodes()
            assertTrue(nodes.isNotEmpty())
            nodes.forEach { node ->
                val side = with(rule.density) {
                    minOf(node.touchBoundsInRoot.width, node.touchBoundsInRoot.height).toDp()
                }
                assertTrue(side >= 47.5.dp, "A chip's touch target on the screen is $side")
            }
        }
        assertChips()
        repeat(6) {
            rule.onNodeWithContentDescription("Zoom out").performClick()
            assertChips()
        }
        rule.onNodeWithContentDescription("Reset zoom, 40%").assertExists()
        assertEquals(48.dp / 0.4f, MonarchOrgChartTokens.chipTouchSize(0.4f))
        assertEquals(48.dp, MonarchOrgChartTokens.chipTouchSize(1.6f))
    }

    @Test
    fun theZoomSymbolsAndIconsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchOrgChart(
                nodes = People,
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                layout = MonarchOrgChartLayout.Chart,
                labels = MonarchOrgChartLabels(zoomOutSymbol = "Smaller"),
                zoomInIcon = Chevron,
            )
        }
        rule.onNodeWithText("Smaller", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("+", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithContentDescription("Zoom in").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("Reset zoom, 110%").assertExists()
    }

    @Test
    fun chartCardsShowEveryVisiblePerson() {
        rule.setMonarchContent {
            MonarchOrgChart(
                nodes = People,
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                layout = MonarchOrgChartLayout.Chart,
            )
        }
        listOf("Ana Silva, Chief executive", "Jo Park, Head of engineering", "Ola Berg").forEach {
            rule.onNodeWithContentDescription(it).assertExists()
        }
    }

    @Test
    fun autoPicksTheListOnAPhoneAndTheChartWhenWide() {
        rule.setMonarchContent {
            MonarchOrgChart(
                nodes = People,
                expandIcon = Chevron,
                modifier = Modifier.requiredWidth(MonarchOrgChartTokens.ChartMinWidth).height(ChartHeight),
            )
        }
        rule.onNodeWithContentDescription("Zoom in").assertExists()
        assertFalse(MonarchOrgChartTokens.showsChart(MonarchOrgChartLayout.Auto, 320.dp, 1f))
        assertFalse(MonarchOrgChartTokens.showsChart(MonarchOrgChartLayout.Auto, 800.dp, 2f))
        assertTrue(MonarchOrgChartTokens.showsChart(MonarchOrgChartLayout.Auto, 800.dp, 1f))
        assertTrue(MonarchOrgChartTokens.showsChart(MonarchOrgChartLayout.Chart, 320.dp, 2f))
    }

    @Test
    fun theChipComesBeforeTheCardRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchOrgChart(
                nodes = People,
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                layout = MonarchOrgChartLayout.List,
            )
        }
        val chip = rule.onNodeWithContentDescription("Hide team of Jo Park").getUnclippedBoundsInRoot()
        val card = rule.onNodeWithContentDescription("Jo Park, Head of engineering").getUnclippedBoundsInRoot()
        // The chip sits under the start edge of the card: the right half in a right-to-left layout.
        assertTrue(chip.left + chip.right > card.left + card.right, "chip $chip card $card")
    }

    @Test
    fun namesAreNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchOrgChart(nodes = People, expandIcon = Chevron, modifier = Modifier.height(ChartHeight))
        }
        rule.onNodeWithText("Ana Silva", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchOrgChart(
                nodes = People,
                expandIcon = Chevron,
                modifier = Modifier.height(ChartHeight),
                layout = MonarchOrgChartLayout.List,
                labels = MonarchOrgChartLabels(
                    label = "Organigrama",
                    vacant = "Vacante",
                    expanded = "Equipo visible",
                    reports = { "$it subordinados" },
                    collapse = { "Ocultar equipo de $it" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Organigrama").assertExists()
        rule.onNodeWithContentDescription("Product designer, Vacante").assertExists()
        rule.onNodeWithContentDescription("Ocultar equipo de Jo Park").assertExists()
        assertEquals(
            "Equipo visible, 2 subordinados",
            rule.onNodeWithContentDescription("Jo Park, Head of engineering").stateDescription(),
        )
    }

    @Test
    fun theTreeIsSafeWithMissingSelfRepeatedAndCyclicManagers() {
        val tree = MonarchOrgChartTree(
            listOf(
                MonarchOrgChartNode(id = "a", parentId = "missing", name = "A"),
                MonarchOrgChartNode(id = "b", parentId = "b", name = "B"),
                MonarchOrgChartNode(id = "c", parentId = "a", name = "C", childCount = 3),
                MonarchOrgChartNode(id = "c", parentId = "b", name = "C again"),
                MonarchOrgChartNode(id = "x", parentId = "y", name = "X"),
                MonarchOrgChartNode(id = "y", parentId = "x", name = "Y"),
            ),
        )
        assertEquals(listOf("a", "b"), tree.roots.map { it.id })
        assertEquals(listOf("a", "c", "b"), tree.visible(setOf("a", "b", "c", "x")).map { it.first.id })
        assertEquals(listOf(0, 1, 0), tree.visible(setOf("a")).map { it.second })
        assertEquals(3, tree.reportCount(tree.byId.getValue("c")))
        assertTrue(tree.needsLoad(tree.byId.getValue("c")))
        assertEquals(listOf("y"), tree.ancestors("x"))
        assertEquals(setOf("a", "b"), tree.initiallyExpanded(null))
    }

    @Test
    fun movesIntoOwnTeamToTheCurrentManagerOrToAVacancyAreRefused() {
        val tree = MonarchOrgChartTree(People)
        assertTrue(tree.canMove("ola", "noor"))
        assertFalse(tree.canMove("ola", "jo"))
        assertFalse(tree.canMove("jo", "ola"))
        assertFalse(tree.canMove("ola", "open"))
        assertFalse(tree.canMove("open", "noor"))
        assertFalse(tree.canMove("ola", "ola"))
        assertTrue(tree.canMoveToTop("ola"))
        assertFalse(tree.canMoveToTop("ana"))
    }

    @Test
    fun theDragStateFindsTargetsAndTheBackground() {
        val tree = MonarchOrgChartTree(People)
        val drag = MonarchOrgChartDragState()
        drag.cards["ola"] = Rect(0f, 0f, 100f, 50f)
        drag.cards["noor"] = Rect(0f, 100f, 100f, 150f)
        drag.cards["jo"] = Rect(0f, 200f, 100f, 250f)
        drag.area = Rect(0f, 0f, 400f, 400f)
        drag.start("ola")
        drag.move(Offset(0f, 120f), Offset(50f, 120f), tree)
        assertEquals("noor", drag.target)
        drag.move(Offset(0f, 100f), Offset(50f, 220f), tree)
        assertNull(drag.target)
        assertNull(drag.finish(tree))
        drag.start("ola")
        drag.move(Offset(200f, 0f), Offset(300f, 300f), tree)
        assertEquals(MonarchOrgChartMoveTarget.TopLevel, drag.finish(tree))
        assertNull(drag.dragged)
    }

    @Test
    fun zoomIsRoundedAndKeptInRange() {
        assertEquals(0.4f, MonarchOrgChartTokens.clampZoom(0.1f))
        assertEquals(1.6f, MonarchOrgChartTokens.clampZoom(3f))
        assertEquals(110, MonarchOrgChartTokens.zoomPercent(MonarchOrgChartTokens.clampZoom(1.07f)))
        assertEquals(MonarchOrgChartTokens.ListIndent * 4, MonarchOrgChartTokens.listIndent(9))
    }

    @Test
    fun cardColoursComeFromTheTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchOrgChartTokens
            val plain = tokens.cardColors(
                colors,
                vacancy = false,
                dashed = false,
                focused = false,
                dropTarget = false,
                enabled = true,
            )
            assertEquals(colors.bgSurface, plain.container)
            assertEquals(colors.borderSubtle, plain.border)
            val focused = tokens.cardColors(
                colors,
                vacancy = false,
                dashed = false,
                focused = true,
                dropTarget = false,
                enabled = true,
            )
            assertEquals(colors.accentSolid, focused.border)
            assertTrue(focused.borderWidth > plain.borderWidth)
            val target = tokens.cardColors(
                colors,
                vacancy = false,
                dashed = false,
                focused = false,
                dropTarget = true,
                enabled = true,
            )
            assertEquals(colors.accentSubtle, target.container)
            assertEquals(colors.accentStrong, target.border)
            val vacancy = tokens.cardColors(
                colors,
                vacancy = true,
                dashed = false,
                focused = false,
                dropTarget = false,
                enabled = true,
            )
            assertEquals(colors.bgMuted, vacancy.container)
            assertEquals(colors.borderStrong, vacancy.border)
            val off = tokens.cardColors(
                colors,
                vacancy = false,
                dashed = true,
                focused = true,
                dropTarget = false,
                enabled = false,
            )
            assertEquals(colors.textDisabled, off.name)
            assertEquals(colors.borderSubtle, off.border)
            assertEquals(colors.borderDefault, tokens.lineColor(colors))
            assertEquals(colors.textSubtle, tokens.dottedColor(colors))
        }
    }
}
