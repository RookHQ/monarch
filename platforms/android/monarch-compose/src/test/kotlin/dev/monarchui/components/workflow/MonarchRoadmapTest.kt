@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.workflow

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.workflow.gantttimeline.MonarchGanttTimelineDates
import dev.monarchui.components.workflow.gantttimeline.MonarchGanttTimelineDependency
import dev.monarchui.components.workflow.gantttimeline.MonarchGanttTimelineLabels
import dev.monarchui.components.workflow.gantttimeline.MonarchGanttTimelineMilestone
import dev.monarchui.components.workflow.gantttimeline.MonarchGanttTimelineState
import dev.monarchui.components.workflow.gantttimeline.MonarchGanttTimelineTokens
import dev.monarchui.components.workflow.gantttimeline.MonarchGanttTimelineView
import dev.monarchui.components.workflow.gantttimeline.MonarchGanttTimelineZoom
import dev.monarchui.components.workflow.roadmap.MonarchRoadmap
import dev.monarchui.components.workflow.roadmap.MonarchRoadmapGroup
import dev.monarchui.components.workflow.roadmap.MonarchRoadmapHealth
import dev.monarchui.components.workflow.roadmap.MonarchRoadmapItem
import dev.monarchui.components.workflow.roadmap.MonarchRoadmapLabels
import dev.monarchui.components.workflow.roadmap.MonarchRoadmapRules
import dev.monarchui.components.workflow.roadmap.MonarchRoadmapText
import dev.monarchui.components.workflow.roadmap.MonarchRoadmapTokens
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/** Thursday 1 January 2026, 00:00 UTC. */
private const val RoadmapJan1 = 1_767_225_600_000L
private const val RoadmapDay = 86_400_000L
private val RoadmapUtc: TimeZone = TimeZone.getTimeZone("UTC")

/** The day [offset] days after 1 January 2026. */
private fun roadmapDay(offset: Int): Long = RoadmapJan1 + offset * RoadmapDay

private val Loyalty = MonarchRoadmapItem(
    "p1",
    "Loyalty emails",
    start = roadmapDay(4),
    end = roadmapDay(78),
    groupId = "growth",
    progress = 0.4f,
    progressText = "8/20",
    health = MonarchRoadmapHealth.OnTrack,
)
private val Referral = MonarchRoadmapItem(
    "p2",
    "Referral page",
    start = roadmapDay(59),
    end = roadmapDay(133),
    groupId = "growth",
    progressText = "1/10",
    health = MonarchRoadmapHealth.NotReported,
)
private val Search = MonarchRoadmapItem(
    "p3",
    "Help center search",
    start = roadmapDay(32),
    end = roadmapDay(119),
    groupId = "quality",
    health = MonarchRoadmapHealth.AtRisk,
)
private val Crashes = MonarchRoadmapItem(
    "p4",
    "Crash fixes",
    start = roadmapDay(11),
    end = roadmapDay(57),
    groupId = "quality",
    progress = 0.7f,
    health = MonarchRoadmapHealth.OffTrack,
)
private val Billing = MonarchRoadmapItem("p5", "Billing revamp", progressText = "0/6")

private val Growth = MonarchRoadmapGroup(
    "growth",
    "Grow repeat customers",
    start = roadmapDay(0),
    end = roadmapDay(180),
    health = MonarchRoadmapHealth.OnTrack,
)
private val Quality = MonarchRoadmapGroup("quality", "Fewer support tickets", health = MonarchRoadmapHealth.AtRisk)

private const val LoyaltyBar = "Loyalty emails, Jan 5, 2026 to Mar 20, 2026, 40% complete"

@Composable
private fun Roadmap(
    items: List<MonarchRoadmapItem> = listOf(Loyalty, Referral),
    groups: List<MonarchRoadmapGroup> = listOf(Growth),
    state: MonarchGanttTimelineState? = null,
    view: MonarchGanttTimelineView = MonarchGanttTimelineView.Chart,
    dependencies: List<MonarchGanttTimelineDependency> = emptyList(),
    milestones: List<MonarchGanttTimelineMilestone> = emptyList(),
    toolbar: Boolean = false,
    onChange: ((MonarchRoadmapItem, MonarchGanttTimelineDates) -> Unit)? = null,
    onOpen: ((MonarchRoadmapItem) -> Unit)? = null,
    labels: MonarchRoadmapLabels = MonarchRoadmapLabels(),
    timelineLabels: MonarchGanttTimelineLabels = MonarchGanttTimelineLabels(),
) {
    MonarchRoadmap(
        items = items,
        previousIcon = { MonarchIcon(TestIcon, contentDescription = null) },
        nextIcon = { MonarchIcon(TestIcon, contentDescription = null) },
        state = state ?: remember {
            MonarchGanttTimelineState(start = RoadmapJan1, zoom = MonarchGanttTimelineZoom.Quarter, view = view)
        },
        groups = groups,
        dependencies = dependencies,
        milestones = milestones,
        today = roadmapDay(13),
        timeZone = RoadmapUtc.id,
        toolbar = toolbar,
        onChange = onChange,
        onOpen = onOpen,
        labels = labels,
        timelineLabels = timelineLabels,
    )
}

internal object MonarchRoadmapFixtures : ComponentFixtures {
    override val id = "roadmap"
    override val fixtures = listOf(
        fixture("chart, group with health, read only") { Roadmap() },
        fixture("chart, at risk and off track, open") {
            Roadmap(items = listOf(Search, Crashes), groups = listOf(Quality), onOpen = {})
        },
        fixture("chart, editable, dependency and milestone") {
            Roadmap(
                items = listOf(Crashes, Search),
                groups = emptyList(),
                dependencies = listOf(MonarchGanttTimelineDependency("d1", "p4", "p3")),
                milestones = listOf(MonarchGanttTimelineMilestone("m1", "Launch", roadmapDay(40), itemId = "p3")),
                onChange = { _, _ -> },
                onOpen = {},
            )
        },
        fixture("chart, undated project, no group") { Roadmap(items = listOf(Billing), groups = emptyList()) },
        fixture("chart, empty group") { Roadmap(items = emptyList(), groups = listOf(Quality)) },
        fixture("chart, long name") {
            Roadmap(
                items = listOf(
                    MonarchRoadmapItem(
                        "long",
                        "Move every customer workspace to the new billing provider",
                        start = roadmapDay(10),
                        end = roadmapDay(60),
                        progressText = "12/48",
                        health = MonarchRoadmapHealth.AtRisk,
                    ),
                ),
                groups = emptyList(),
            )
        },
        fixture("toolbar") { Roadmap(items = listOf(Loyalty), groups = emptyList(), toolbar = true) },
        fixture("list, open") {
            Roadmap(items = listOf(Loyalty), view = MonarchGanttTimelineView.List, onOpen = {})
        },
        fixture("list, editable, undated") {
            Roadmap(
                items = listOf(Crashes, Billing),
                groups = emptyList(),
                view = MonarchGanttTimelineView.List,
                onChange = { _, _ -> },
            )
        },
    )
}

/** Plain strings, so the mapping is tested without resources. */
private val PlainText = MonarchRoadmapText(
    health = { it.name },
    count = { "$it projects" },
    meta = { first, second -> "$first / $second" },
)

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchRoadmapTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun rowsCarryProgressTextHealthAndGroupCounts() {
        val rows = MonarchRoadmapRules.rows(
            listOf(Loyalty, Referral, Search, Billing),
            listOf(Growth, Quality, MonarchRoadmapGroup("empty", "Later")),
            PlainText,
        )
        assertEquals(
            listOf("8/20", "1/10", null, "0/6"),
            rows.items.map { it.meta },
        )
        assertEquals(listOf(null, null, null), rows.groups.map { it.meta })
        assertEquals(
            listOf("2 projects / OnTrack", "1 projects / AtRisk", "0 projects"),
            listOf(Growth, Quality, MonarchRoadmapGroup("empty", "Later")).mapIndexed { i, group ->
                MonarchRoadmapRules.groupDescription(group, listOf(2, 1, 0)[i], PlainText)
            },
        )
        assertNull(MonarchRoadmapRules.itemMeta(MonarchRoadmapItem("x", "X"), PlainText))
        val loyalty = rows.items.first()
        assertEquals(Loyalty.start, loyalty.start)
        assertEquals(Loyalty.end, loyalty.end)
        assertEquals("growth", loyalty.groupId)
        assertEquals(0.4f, loyalty.progress)
        assertSame(Search, rows.byId["p3"])
    }

    @Test
    fun barTonesFollowHealthUnlessTheAppGivesOne() {
        assertNull(MonarchRoadmapTokens.barTone(null, null))
        assertNull(MonarchRoadmapTokens.barTone(null, MonarchRoadmapHealth.OnTrack))
        assertNull(MonarchRoadmapTokens.barTone(null, MonarchRoadmapHealth.NotReported))
        assertEquals(MonarchTone.Warning, MonarchRoadmapTokens.barTone(null, MonarchRoadmapHealth.AtRisk))
        assertEquals(MonarchTone.Danger, MonarchRoadmapTokens.barTone(null, MonarchRoadmapHealth.OffTrack))
        assertEquals(
            MonarchTone.Success,
            MonarchRoadmapTokens.barTone(MonarchTone.Success, MonarchRoadmapHealth.OffTrack),
        )
    }

    @Test
    fun barColoursComeFromRolesInEveryTheme() {
        AllThemes.forEach { tokens ->
            val colors = MonarchColors(tokens.colors)
            val onTrack = MonarchGanttTimelineTokens.barColors(
                colors,
                MonarchRoadmapTokens.barTone(null, MonarchRoadmapHealth.OnTrack),
                conflict = false,
            )
            assertEquals(colors.accentSubtle, onTrack.container)
            assertEquals(colors.accentSolid, onTrack.border)
            val atRisk = MonarchGanttTimelineTokens.barColors(
                colors,
                MonarchRoadmapTokens.barTone(null, MonarchRoadmapHealth.AtRisk),
                conflict = false,
            )
            assertEquals(colors.warningSubtle, atRisk.container)
            assertEquals(colors.warningSolid, atRisk.progress)
            val offTrack = MonarchGanttTimelineTokens.barColors(
                colors,
                MonarchRoadmapTokens.barTone(null, MonarchRoadmapHealth.OffTrack),
                conflict = false,
            )
            assertEquals(colors.dangerSubtle, offTrack.container)
            assertEquals(colors.textDefault, offTrack.content)
        }
    }

    @Test
    fun headingsWriteHealthAndCountsInWords() {
        rule.setMonarchContent { Roadmap() }
        rule.onNodeWithText("Grow repeat customers", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("8/20", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("1/10", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithText("On track", useUnmergedTree = true).assertCountEquals(2)
        rule.onNodeWithContentDescription("2 projects · On track", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aProjectWithoutDatesStaysAsARow() {
        rule.setMonarchContent { Roadmap(items = listOf(Billing, Crashes), groups = emptyList()) }
        rule.onNodeWithText("Billing revamp", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("0/6", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("No dates", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Off track", useUnmergedTree = true).assertExists()
    }

    @Test
    fun tappingABarOpensTheRoadmapProject() {
        var opened: MonarchRoadmapItem? = null
        rule.setMonarchContent { Roadmap(onOpen = { opened = it }) }
        rule.onNodeWithContentDescription(LoyaltyBar)
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performSemanticsAction(SemanticsActions.OnClick)
        assertSame(Loyalty, opened)
    }

    @Test
    fun theRoadmapIsReadOnlyByDefault() {
        rule.setMonarchContent { Roadmap() }
        rule.onNodeWithContentDescription(LoyaltyBar)
            .assertHasNoClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
    }

    @Test
    fun editingPassesTheRoadmapProjectBack() {
        val changes = mutableListOf<Pair<MonarchRoadmapItem, MonarchGanttTimelineDates>>()
        rule.setMonarchContent { Roadmap(onChange = { item, dates -> changes += item to dates }) }
        val actions = rule.onNodeWithContentDescription(LoyaltyBar).fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
        actions.first { it.label == "Move a day later" }.action()
        assertEquals(1, changes.size)
        assertSame(Loyalty, changes.single().first)
        assertEquals(MonarchGanttTimelineDates(roadmapDay(5), roadmapDay(79)), changes.single().second)
    }

    @Test
    fun itOpensAtTheQuarterZoom() {
        rule.setMonarchContent {
            MonarchRoadmap(
                items = listOf(Loyalty),
                previousIcon = {},
                nextIcon = {},
                today = roadmapDay(13),
                timeZone = RoadmapUtc.id,
            )
        }
        rule.onNodeWithText("Quarters").assertIsSelected()
    }

    @Test
    fun barsReserve48dp() {
        rule.setMonarchContent { Roadmap(items = listOf(Crashes, Search), groups = emptyList(), onOpen = {}) }
        rule.onNodeWithContentDescription("Crash fixes, Jan 12, 2026 to Feb 27, 2026, 70% complete")
            .assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun projectNamesStayAtTheStartRightToLeft() {
        val environment = MonarchTestEnvironment()
        val pilot = MonarchRoadmapItem("pilot", "Pilot", start = roadmapDay(4), end = roadmapDay(9))
        rule.setMonarchContent(environment) { Roadmap(items = listOf(pilot), groups = emptyList()) }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Pilot", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithContentDescription("Pilot, Jan 5, 2026 to Jan 10, 2026"), direction)
        }
    }

    @Test
    fun longNamesAndHealthWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Roadmap(
                items = listOf(
                    MonarchRoadmapItem(
                        "l",
                        "Move workspaces to billing",
                        start = roadmapDay(10),
                        end = roadmapDay(60),
                        health = MonarchRoadmapHealth.NotReported,
                    ),
                ),
                groups = emptyList(),
            )
        }
        rule.onNodeWithText("Move workspaces to billing", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun builtInStringsHaveDefaultsAndOverrides() {
        rule.setMonarchContent {
            Column {
                Roadmap(items = listOf(Loyalty), groups = listOf(Growth))
                Roadmap(
                    items = listOf(Search),
                    groups = listOf(Quality),
                    labels = MonarchRoadmapLabels(
                        health = { if (it == MonarchRoadmapHealth.AtRisk) "Amber" else it.name },
                        count = { "$it in plan" },
                        meta = { first, second -> "$first, $second" },
                    ),
                    timelineLabels = MonarchGanttTimelineLabels(timeline = "Plan", rowHeader = "Epic"),
                )
            }
        }
        rule.onNodeWithContentDescription("Roadmap").assertExists()
        rule.onNodeWithText("Project", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("1 project · On track", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Plan").assertExists()
        rule.onNodeWithText("Epic", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithText("Amber", useUnmergedTree = true).assertCountEquals(2)
    }

    @Test
    fun theTimelineKeepsEveryOtherLabelTheAppGives() {
        val given = MonarchGanttTimelineLabels(
            rowHeader = "Epic",
            noDates = "Unplanned",
            moveLater = "Push back",
            range = { from, to -> "$from..$to" },
        )
        val merged = MonarchRoadmapRules.timelineLabels(given, timeline = "Roadmap", rowHeader = "Project")
        assertEquals("Roadmap", merged.timeline)
        assertEquals("Epic", merged.rowHeader)
        assertEquals("Unplanned", merged.noDates)
        assertEquals("Push back", merged.moveLater)
        assertSame(given.range, merged.range)
        assertNull(merged.save)
    }
}
