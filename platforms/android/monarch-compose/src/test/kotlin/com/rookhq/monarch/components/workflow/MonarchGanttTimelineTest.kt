@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.workflow

import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.components.workflow.gantttimeline.GanttTimelineDays
import com.rookhq.monarch.components.workflow.gantttimeline.GanttTimelineLine
import com.rookhq.monarch.components.workflow.gantttimeline.GanttTimelineRules
import com.rookhq.monarch.components.workflow.gantttimeline.GanttTimelineSpan
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimeline
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineDates
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineDependency
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineDependencyKind
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineGroup
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineItem
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineLabels
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineMilestone
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineShift
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineState
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineTokens
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineUndated
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineView
import com.rookhq.monarch.components.workflow.gantttimeline.MonarchGanttTimelineZoom
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Monday 5 January 2026, 00:00 UTC. */
private const val Jan5 = 1_767_571_200_000L
private const val Day = 86_400_000L
private const val Hour = 3_600_000L
private val Utc: TimeZone = TimeZone.getTimeZone("UTC")

private fun jan(day: Int): Long = Jan5 + (day - 5) * Day

private val Brief = MonarchGanttTimelineItem(
    "brief",
    "Write the brief",
    start = jan(5),
    end = jan(9),
    progress = 1f,
    tone = MonarchTone.Success,
)
private val Design = MonarchGanttTimelineItem("design", "Design", start = jan(12), end = jan(23), progress = 0.6f)
private val Build = MonarchGanttTimelineItem(
    "build",
    "Build",
    start = jan(21),
    end = jan(44),
    conflict = "Starts before Design ends",
)
private val Api = MonarchGanttTimelineItem("api", "API changes", start = jan(26), end = jan(35), parentId = "build")
private val Launch = MonarchGanttTimelineItem("launch", "Launch", end = jan(51))
private val Research = MonarchGanttTimelineItem("research", "Pricing research")

@Composable
private fun Timeline(
    items: List<MonarchGanttTimelineItem> = listOf(Brief, Design),
    state: MonarchGanttTimelineState? = null,
    groups: List<MonarchGanttTimelineGroup> = emptyList(),
    dependencies: List<MonarchGanttTimelineDependency> = emptyList(),
    milestones: List<MonarchGanttTimelineMilestone> = emptyList(),
    undated: MonarchGanttTimelineUndated = MonarchGanttTimelineUndated.List,
    toolbar: Boolean = false,
    onChange: ((MonarchGanttTimelineItem, MonarchGanttTimelineDates) -> Unit)? = null,
    onOpen: ((MonarchGanttTimelineItem) -> Unit)? = null,
    labels: MonarchGanttTimelineLabels = MonarchGanttTimelineLabels(),
    view: MonarchGanttTimelineView = MonarchGanttTimelineView.Chart,
    zoom: MonarchGanttTimelineZoom = MonarchGanttTimelineZoom.Month,
    itemMeta: (@Composable (MonarchGanttTimelineItem) -> Unit)? = null,
    groupMeta: (@Composable (MonarchGanttTimelineGroup) -> Unit)? = null,
) {
    MonarchGanttTimeline(
        items = items,
        previousIcon = { MonarchIcon(TestIcon, contentDescription = null) },
        nextIcon = { MonarchIcon(TestIcon, contentDescription = null) },
        state = state ?: remember { MonarchGanttTimelineState(zoom = zoom, view = view) },
        groups = groups,
        dependencies = dependencies,
        milestones = milestones,
        undated = undated,
        today = jan(14) + 12 * Hour,
        timeZone = Utc.id,
        toolbar = toolbar,
        onChange = onChange,
        onOpen = onOpen,
        labels = labels,
        itemMeta = itemMeta,
        groupMeta = groupMeta,
    )
}

internal object MonarchGanttTimelineFixtures : ComponentFixtures {
    override val id = "gantt-timeline"
    override val fixtures = listOf(
        fixture("chart, read only, month") { Timeline() },
        fixture("chart, editable, open, progress") {
            Timeline(items = listOf(Design), onChange = { _, _ -> }, onOpen = {})
        },
        fixture("chart, conflict and nested parent") { Timeline(items = listOf(Build, Api), onOpen = {}) },
        fixture("chart, parent without dates, collapsed") {
            Timeline(
                items = listOf(MonarchGanttTimelineItem("p", "Phase two"), Api.copyParent("p")),
                state = remember { MonarchGanttTimelineState(collapsed = setOf("p")) },
            )
        },
        fixture("chart, one-day marker, undated row") {
            Timeline(
                items = listOf(Launch, Research),
                undated = MonarchGanttTimelineUndated.Row,
                onChange = { _, _ -> },
            )
        },
        fixture("chart, group and milestones") {
            Timeline(
                items = listOf(Design.inGroup("g")),
                groups = listOf(
                    MonarchGanttTimelineGroup("g", "Website", start = jan(5), end = jan(30), meta = "3 items"),
                ),
                milestones = listOf(
                    MonarchGanttTimelineMilestone("m1", "Design review", jan(16), done = true),
                    MonarchGanttTimelineMilestone("m2", "Sign-off", jan(15), itemId = "design"),
                ),
            )
        },
        fixture("chart, week zoom, dependency") {
            Timeline(
                dependencies = listOf(MonarchGanttTimelineDependency("d", "brief", "design")),
                zoom = MonarchGanttTimelineZoom.Week,
            )
        },
        fixture("chart, quarter zoom, broken dependency") {
            Timeline(
                items = listOf(Design, Build),
                dependencies = listOf(MonarchGanttTimelineDependency("d", "design", "build", conflict = true)),
                zoom = MonarchGanttTimelineZoom.Quarter,
            )
        },
        fixture("chart, year zoom, finish to finish") {
            Timeline(
                dependencies = listOf(
                    MonarchGanttTimelineDependency(
                        "d",
                        "brief",
                        "design",
                        MonarchGanttTimelineDependencyKind.FinishToFinish,
                    ),
                ),
                zoom = MonarchGanttTimelineZoom.Year,
            )
        },
        fixture("chart, outside the window") {
            Timeline(
                items = listOf(
                    MonarchGanttTimelineItem("old", "Discovery", start = jan(5) - 60 * Day, end = jan(5) - 30 * Day),
                    MonarchGanttTimelineItem("new", "Next year", start = jan(5) + 400 * Day, end = jan(5) + 420 * Day),
                ),
            )
        },
        fixture("chart, unscheduled list") { Timeline(items = listOf(Design, Research)) },
        fixture("chart, long label") {
            Timeline(
                items = listOf(
                    MonarchGanttTimelineItem(
                        "long",
                        "Migrate every customer workspace to the new billing provider",
                        start = jan(6),
                        end = jan(8),
                        meta = "Blocked by legal review",
                    ),
                ),
            )
        },
        fixture("toolbar") { Timeline(items = listOf(Brief), toolbar = true) },
        fixture("list, editable, open") {
            Timeline(items = listOf(Design), onChange = { _, _ -> }, onOpen = {}, view = MonarchGanttTimelineView.List)
        },
        fixture("list, parent and child") {
            Timeline(items = listOf(Build, Api), view = MonarchGanttTimelineView.List)
        },
        fixture("list, group, milestone, unscheduled") {
            Timeline(
                items = listOf(Research),
                groups = listOf(MonarchGanttTimelineGroup("g", "Website", start = jan(5), end = jan(30))),
                milestones = listOf(MonarchGanttTimelineMilestone("m", "Beta", jan(40), done = true)),
                view = MonarchGanttTimelineView.List,
            )
        },
    )
}

private fun MonarchGanttTimelineItem.copyParent(parent: String) =
    MonarchGanttTimelineItem(id, label, start, end, parentId = parent)

private fun MonarchGanttTimelineItem.inGroup(group: String) =
    MonarchGanttTimelineItem(id, label, start, end, groupId = group, progress = progress)

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchGanttTimelineTest {
    @get:Rule
    val rule = createComposeRule()

    private val designBar = "Design, Jan 12, 2026 to Jan 23, 2026, 60% complete"

    private var savedLocale: Locale = Locale.getDefault()

    @Before
    fun pinLocale() {
        savedLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(savedLocale)
    }

    @Test
    fun spansSwapReversedDatesAndAnItemWithOneDateIsAMarker() {
        val day = GanttTimelineDays.day(jan(12))
        assertEquals(GanttTimelineSpan(day, day + 3), GanttTimelineRules.span(jan(15) + 5 * Hour, jan(12)))
        assertEquals(GanttTimelineSpan(day, day), GanttTimelineRules.span(null, jan(12)))
        assertNull(GanttTimelineRules.span(null, null))
        assertTrue(GanttTimelineRules.isPoint(MonarchGanttTimelineDates(null, jan(12))))
        assertFalse(GanttTimelineRules.isPoint(MonarchGanttTimelineDates(jan(5), jan(12))))
        assertEquals(12, GanttTimelineRules.span(Design.start, Design.end)?.days)
    }

    @Test
    fun shiftingMovesWholeDaysAndTheEdgesNeverCross() {
        val dates = MonarchGanttTimelineDates(jan(12) + 3 * Hour, jan(14))
        assertEquals(MonarchGanttTimelineDates(jan(14), jan(16)), dates.shifted(MonarchGanttTimelineShift.Move, 2))
        assertEquals(MonarchGanttTimelineDates(jan(11), jan(14)), dates.shifted(MonarchGanttTimelineShift.Start, -1))
        assertEquals(MonarchGanttTimelineDates(jan(14), jan(14)), dates.shifted(MonarchGanttTimelineShift.Start, 5))
        assertEquals(MonarchGanttTimelineDates(jan(12), jan(12)), dates.shifted(MonarchGanttTimelineShift.End, -9))
        assertEquals(MonarchGanttTimelineDates(jan(12), jan(14)), dates.shifted(MonarchGanttTimelineShift.Move, 0))
        val marker = MonarchGanttTimelineDates(null, jan(20))
        assertEquals(MonarchGanttTimelineDates(null, jan(21)), marker.shifted(MonarchGanttTimelineShift.Move, 1))
        assertEquals(MonarchGanttTimelineDates(jan(19), jan(20)), marker.shifted(MonarchGanttTimelineShift.Start, -1))
    }

    @Test
    fun rowsFollowGroupsAndParentsAndListItemsWithoutDatesApart() {
        val group = MonarchGanttTimelineGroup("g", "Website")
        val items = listOf(Design.inGroup("g"), Build, Api, Research, Launch)
        val lines = GanttTimelineRules.lines(items, listOf(group), emptySet(), MonarchGanttTimelineUndated.List)
        val ids = lines.lines.map {
            when (it) {
                is GanttTimelineLine.Group -> "g:${it.group.id}"
                is GanttTimelineLine.Item -> "${it.item.id}@${it.depth}"
            }
        }
        assertEquals(listOf("g:g", "design@0", "build@0", "api@1", "launch@0"), ids)
        assertEquals(listOf("research"), lines.unscheduled.map { it.id })
        val build = lines.lines.filterIsInstance<GanttTimelineLine.Item>().first { it.item.id == "build" }
        assertEquals(1, build.children)

        val collapsed = GanttTimelineRules.lines(items, emptyList(), setOf("build"), MonarchGanttTimelineUndated.Row)
        val shown = collapsed.lines.filterIsInstance<GanttTimelineLine.Item>().map { it.item.id }
        assertEquals(listOf("design", "build", "research", "launch"), shown)
        assertTrue(collapsed.unscheduled.isEmpty())
    }

    @Test
    fun aParentWithoutDatesRollsUpItsChildrenAndCyclesAreIgnored() {
        val parent = MonarchGanttTimelineItem("p", "Phase two")
        val a = MonarchGanttTimelineItem("a", "A", start = jan(6), end = jan(8), parentId = "p")
        val b = MonarchGanttTimelineItem("b", "B", start = jan(10), end = jan(20), parentId = "p")
        val undated = MonarchGanttTimelineUndated.List
        val lines = GanttTimelineRules.lines(listOf(parent, a, b), emptyList(), emptySet(), undated)
        val row = lines.lines.first() as GanttTimelineLine.Item
        assertEquals(GanttTimelineRules.span(jan(6), jan(20)), row.rollup)
        val loopA = MonarchGanttTimelineItem("x", "X", start = jan(6), end = jan(7), parentId = "y")
        val loopB = MonarchGanttTimelineItem("y", "Y", start = jan(6), end = jan(7), parentId = "x")
        val loop = GanttTimelineRules.lines(listOf(loopA, loopB), emptyList(), emptySet(), undated)
        assertTrue(loop.lines.isEmpty())
    }

    @Test
    fun theWindowStartsTheMondayAWeekBeforeTodayAndTicksFollowTheZoom() {
        val today = GanttTimelineDays.day(jan(14))
        val monday = GanttTimelineDays.day(Jan5)
        assertEquals(monday, GanttTimelineRules.anchor(null, today, emptyList()))
        assertEquals(monday, GanttTimelineRules.anchor(null, null, listOf(Design)))
        assertEquals(GanttTimelineDays.day(jan(20)), GanttTimelineRules.anchor(jan(20) + Hour, today, emptyList()))
        assertEquals(1, GanttTimelineDays.weekday(monday))

        val month = GanttTimelineRules.ticks(monday, MonarchGanttTimelineZoom.Month)
        assertEquals(13, month.minor.size)
        assertTrue(month.major.first().withYear)
        assertEquals(4, month.major.size)
        val week = GanttTimelineRules.ticks(monday, MonarchGanttTimelineZoom.Week)
        assertEquals(28, week.minor.size)
        val year = GanttTimelineRules.ticks(monday, MonarchGanttTimelineZoom.Year)
        assertTrue(year.minor.isEmpty())
        assertEquals(9, year.major.size)
    }

    @Test
    fun dependencyArrowsTurnBetweenBarsAndEndAtTheTarget() {
        val x: (Long) -> Float = { it * 10f }
        val a = GanttTimelineSpan(0, 1)
        val b = GanttTimelineSpan(5, 8)
        val forward = GanttTimelineRules.dependencyPath(
            MonarchGanttTimelineDependencyKind.FinishToStart, a, b, 0f, 40f, x, 10f, gap = 5f, tip = 1f,
        )
        assertEquals(4, forward.size)
        assertEquals(20f, forward.first().x)
        assertEquals(49f, forward.last().x)
        assertEquals(40f, forward.last().y)
        val backward = GanttTimelineRules.dependencyPath(
            MonarchGanttTimelineDependencyKind.FinishToStart, b, a, 0f, 40f, x, 10f, gap = 5f, tip = 1f,
        )
        assertEquals(6, backward.size)
        assertEquals(-1f, backward.last().x)
        val finish = GanttTimelineRules.dependencyPath(
            MonarchGanttTimelineDependencyKind.FinishToFinish, a, b, 0f, 40f, x, 10f, gap = 5f, tip = 1f,
        )
        assertEquals(91f, finish.last().x)
        assertTrue(finish[finish.size - 2].x > finish.last().x)
        val start = GanttTimelineRules.dependencyPath(
            MonarchGanttTimelineDependencyKind.StartToStart, a, b, 0f, 40f, x, 10f, gap = 5f, tip = 1f,
        )
        assertEquals(0f, start.first().x)
    }

    @Test
    fun aDragNearAnEndChangesThatEndAndElsewhereMovesTheBar() {
        assertEquals(MonarchGanttTimelineShift.Start, GanttTimelineRules.dragShift(4f, 100f, 12f, point = false))
        assertEquals(MonarchGanttTimelineShift.End, GanttTimelineRules.dragShift(95f, 100f, 12f, point = false))
        assertEquals(MonarchGanttTimelineShift.Move, GanttTimelineRules.dragShift(50f, 100f, 12f, point = false))
        assertEquals(MonarchGanttTimelineShift.Move, GanttTimelineRules.dragShift(2f, 30f, 12f, point = false))
        assertEquals(MonarchGanttTimelineShift.Move, GanttTimelineRules.dragShift(2f, 100f, 12f, point = true))
        assertEquals(2, GanttTimelineRules.dragDays(35f, 16f))
        assertEquals(-1, GanttTimelineRules.dragDays(-10f, 16f))
        assertEquals(0, GanttTimelineRules.dragDays(7f, 16f))
    }

    @Test
    fun coloursComeFromRolesInEveryTheme() {
        AllThemes.forEach { tokens ->
            val colors = MonarchColors(tokens.colors)
            val accent = MonarchGanttTimelineTokens.barColors(colors, null, conflict = false)
            assertEquals(colors.accentSubtle, accent.container)
            assertEquals(colors.accentSolid, accent.border)
            assertEquals(colors.textDefault, accent.content)
            assertFalse(accent.dashed)
            val neutral = MonarchGanttTimelineTokens.barColors(colors, MonarchTone.Neutral, conflict = false)
            assertEquals(colors.bgMuted, neutral.container)
            assertEquals(colors.borderStrong, neutral.border)
            val success = MonarchGanttTimelineTokens.barColors(colors, MonarchTone.Success, conflict = false)
            assertEquals(colors.successSubtle, success.container)
            assertEquals(colors.successSolid, success.progress)
            val conflict = MonarchGanttTimelineTokens.barColors(colors, MonarchTone.Success, conflict = true)
            assertEquals(colors.dangerSubtle, conflict.container)
            assertEquals(colors.dangerSolid, conflict.border)
            assertTrue(conflict.dashed)
            val done = MonarchGanttTimelineTokens.milestoneColors(colors, done = true)
            val open = MonarchGanttTimelineTokens.milestoneColors(colors, done = false)
            assertEquals(colors.accentSolid, done.fill)
            assertEquals(colors.bgSurface, open.fill)
            assertNotEquals(done.border, open.border)
            assertEquals(colors.dangerStrong, MonarchGanttTimelineTokens.dependencyColor(colors, conflict = true))
            assertEquals(colors.textSubtle, MonarchGanttTimelineTokens.dependencyColor(colors, conflict = false))
            assertEquals(colors.accentSolid, MonarchGanttTimelineTokens.todayColor(colors))
            assertEquals(colors.accentOnSolid, MonarchGanttTimelineTokens.onTodayColor(colors))
        }
    }

    @Test
    fun aBarIsReadWithItsDatesProgressAndConflictAndOpensWhenTapped() {
        var opened: String? = null
        rule.setMonarchContent { Timeline(items = listOf(Design, Build), onOpen = { opened = it.id }) }
        rule.onNodeWithContentDescription(designBar).assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        rule.onNodeWithContentDescription(
            "Build, Jan 21, 2026 to Feb 13, 2026. Conflict: Starts before Design ends",
        ).assertExists()
        rule.onNodeWithContentDescription(designBar).performSemanticsAction(SemanticsActions.OnClick)
        assertEquals("design", opened)
    }

    @Test
    fun aReadOnlyBarHasNoClickAndNoActions() {
        rule.setMonarchContent { Timeline() }
        rule.onNodeWithContentDescription(designBar)
            .assertHasNoClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
    }

    @Test
    fun talkBackActionsMoveTheBarAndEitherEndAndAnnounceTheNewDates() {
        val changes = mutableListOf<MonarchGanttTimelineDates>()
        rule.setMonarchContent { Timeline(onChange = { _, dates -> changes += dates }) }
        val actions = rule.onNodeWithContentDescription(designBar).fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
        assertEquals(
            listOf(
                "Move a day earlier",
                "Move a day later",
                "Start a day earlier",
                "Start a day later",
                "End a day earlier",
                "End a day later",
            ),
            actions.map { it.label },
        )
        actions[1].action()
        actions[2].action()
        actions[5].action()
        assertEquals(
            listOf(
                MonarchGanttTimelineDates(jan(13), jan(24)),
                MonarchGanttTimelineDates(jan(11), jan(23)),
                MonarchGanttTimelineDates(jan(12), jan(24)),
            ),
            changes,
        )
        rule.waitForIdle()
        rule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Polite,
            ) and hasContentDescription("Design: Jan 12, 2026 to Jan 24, 2026"),
        ).assertExists()
    }

    @Test
    fun aMarkerOffersOnlyMoves() {
        rule.setMonarchContent { Timeline(items = listOf(Launch), onChange = { _, _ -> }) }
        val actions = rule.onNodeWithContentDescription("Launch, Feb 20, 2026").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
        assertEquals(listOf("Move a day earlier", "Move a day later"), actions.map { it.label })
    }

    @Test
    fun aLockedItemCannotBeChanged() {
        val locked = MonarchGanttTimelineItem("l", "Contract", start = jan(6), end = jan(7), editable = false)
        rule.setMonarchContent { Timeline(items = listOf(locked), onChange = { _, _ -> }) }
        rule.onNodeWithContentDescription("Contract, Jan 6, 2026 to Jan 7, 2026")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
    }

    @Test
    fun barsAndParentTogglesReserve48dp() {
        rule.setMonarchContent {
            Timeline(items = listOf(Build, Api), onOpen = {}, zoom = MonarchGanttTimelineZoom.Year)
        }
        rule.onNodeWithContentDescription("API changes, Jan 26, 2026 to Feb 4, 2026")
            .assertTouchTargetAtLeast48dp(rule.density)
        rule.onNode(hasText("Build") and SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            .assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun aParentTogglesItsChildren() {
        val state = MonarchGanttTimelineState()
        rule.setMonarchContent { Timeline(items = listOf(Build, Api), state = state) }
        val toggle = rule.onNode(hasText("Build") and hasClickAction())
        assertEquals("Hide 1 sub-item", toggle.clickLabel())
        assertEquals("Expanded", toggle.stateDescription())
        rule.onNodeWithContentDescription("API changes, Jan 26, 2026 to Feb 4, 2026").assertExists()
        toggle.performClick()
        assertEquals(setOf("build"), state.collapsed)
        rule.onNodeWithContentDescription("API changes, Jan 26, 2026 to Feb 4, 2026").assertDoesNotExist()
        assertEquals("Show 1 sub-item", toggle.clickLabel())
        assertEquals("Collapsed", toggle.stateDescription())
    }

    @Test
    fun rowLabelsStayAtTheStartRightToLeft() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { Timeline() }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Design", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithContentDescription(designBar), direction)
        }
    }

    @Test
    fun longLabelsWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Timeline(
                items = listOf(
                    MonarchGanttTimelineItem("l", "Migrate workspaces to billing", start = jan(6), end = jan(7)),
                ),
            )
        }
        rule.onNodeWithText("Migrate workspaces to billing", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun builtInStringsHaveDefaultsAndOverrides() {
        val labels = MonarchGanttTimelineLabels(
            timeline = "Roadmap",
            bar = { label, start, end, _ -> "$label from $start until $end" },
            moveLater = "Push back",
        )
        rule.setMonarchContent {
            Column {
                Timeline(items = listOf(Brief))
                Timeline(items = listOf(Design), labels = labels, onChange = { _, _ -> })
            }
        }
        rule.onNodeWithContentDescription("Timeline").assertExists()
        rule.onNodeWithContentDescription("Roadmap").assertExists()
        rule.onNodeWithContentDescription("Write the brief, Jan 5, 2026 to Jan 9, 2026, 100% complete").assertExists()
        val node = rule.onNodeWithContentDescription("Design from Jan 12, 2026 until Jan 23, 2026").fetchSemanticsNode()
        assertTrue(node.config[SemanticsActions.CustomActions].any { it.label == "Push back" })
    }

    @Test
    fun theListWritesEveryFactAndChangesDatesWithTheDateRangePicker() {
        var opened: String? = null
        rule.setMonarchContent {
            Timeline(
                items = listOf(Design, Build),
                dependencies = listOf(MonarchGanttTimelineDependency("d", "design", "build")),
                onChange = { _, _ -> },
                onOpen = { opened = it.id },
                view = MonarchGanttTimelineView.List,
            )
        }
        rule.onNodeWithText("Jan 12, 2026 – Jan 23, 2026", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("12 days · 60% complete", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Depends on Design", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Conflict: Starts before Design ends", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Design", useUnmergedTree = true).performClick()
        assertEquals("design", opened)
        rule.onAllNodesWithText("Change dates").assertCountEquals(2)
        rule.onAllNodesWithText("Change dates")[0].performClick()
        rule.onNodeWithText("Save").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Save").assertDoesNotExist()
    }

    @Test
    fun theToolbarMovesTheWindowZoomsAndSwitchesToTheList() {
        val state = MonarchGanttTimelineState()
        rule.setMonarchContent { Timeline(items = listOf(Brief), state = state, toolbar = true) }
        rule.onNodeWithText("Jan 5, 2026 – Apr 5, 2026").assertExists()
        rule.onNodeWithContentDescription("Later").performClick()
        assertEquals(jan(5) + 30 * Day, state.start)
        rule.onNodeWithText("Feb 4, 2026 – May 5, 2026").assertExists()
        rule.onNodeWithText("Today").performClick()
        assertEquals(Jan5, state.start)
        rule.onNodeWithContentDescription("Earlier").performClick()
        assertEquals(jan(5) - 30 * Day, state.start)
        rule.onNodeWithText("Years").performClick()
        assertEquals(MonarchGanttTimelineZoom.Year, state.zoom)
        rule.onNodeWithText("List").performClick()
        assertEquals(MonarchGanttTimelineView.List, state.view)
    }

    @Test
    fun theStateSaverKeepsEveryValue() {
        val state = MonarchGanttTimelineState(
            start = jan(12) + Hour,
            zoom = MonarchGanttTimelineZoom.Quarter,
            view = MonarchGanttTimelineView.List,
            collapsed = setOf("build"),
        )
        assertEquals(jan(12), state.start)
        val saver = MonarchGanttTimelineState.Saver
        val scope = SaverScope { true }
        val saved = with(saver) { scope.save(state) }
        val restored = saver.restore(checkNotNull(saved))
        assertEquals(jan(12), restored?.start)
        assertEquals(MonarchGanttTimelineZoom.Quarter, restored?.zoom)
        assertEquals(MonarchGanttTimelineView.List, restored?.view)
        assertEquals(setOf("build"), restored?.collapsed)
        val empty = saver.restore(checkNotNull(with(saver) { scope.save(MonarchGanttTimelineState()) }))
        assertNull(empty?.start)
        state.toggle("build")
        assertTrue(state.collapsed.isEmpty())
    }

    @Test
    fun labelsCanBeCopiedWithAFewChanged() {
        val base = MonarchGanttTimelineLabels(today = "Today", save = "Save")
        val copy = base.copy(save = "Apply")
        assertEquals("Today", copy.today)
        assertEquals("Apply", copy.save)
        assertNull(copy.cancel)
    }

    @Test
    fun theMetaSlotsShowUnderTheNamesInTheChartAndTheList() {
        var view by mutableStateOf(MonarchGanttTimelineView.Chart)
        rule.setMonarchContent {
            Timeline(
                items = listOf(Design.inGroup("g")),
                groups = listOf(MonarchGanttTimelineGroup("g", "Website")),
                state = remember(view) {
                    MonarchGanttTimelineState(zoom = MonarchGanttTimelineZoom.Month, view = view)
                },
                itemMeta = { MonarchText("Owner ${it.label}") },
                groupMeta = { MonarchText("Group ${it.label}") },
            )
        }
        rule.onNodeWithText("Owner Design", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Group Website", useUnmergedTree = true).assertExists()
        view = MonarchGanttTimelineView.List
        rule.waitForIdle()
        rule.onNodeWithText("Owner Design", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Group Website", useUnmergedTree = true).assertExists()
    }
}

/** The label of the click action of the node: what TalkBack announces for a double tap. */
private fun SemanticsNodeInteraction.clickLabel(): String? =
    fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)?.label
