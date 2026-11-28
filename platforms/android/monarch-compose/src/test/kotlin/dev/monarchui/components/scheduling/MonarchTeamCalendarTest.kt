@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.scheduling

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.components.scheduling.teamcalendar.MonarchTeamCalendar
import dev.monarchui.components.scheduling.teamcalendar.MonarchTeamCalendarEntry
import dev.monarchui.components.scheduling.teamcalendar.MonarchTeamCalendarHoliday
import dev.monarchui.components.scheduling.teamcalendar.MonarchTeamCalendarLabels
import dev.monarchui.components.scheduling.teamcalendar.MonarchTeamCalendarMember
import dev.monarchui.components.scheduling.teamcalendar.MonarchTeamCalendarPortion
import dev.monarchui.components.scheduling.teamcalendar.MonarchTeamCalendarTokens
import dev.monarchui.components.scheduling.teamcalendar.TeamCalendarFormats
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
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
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val Day = 86_400_000L

/** 1 November 2026, a Sunday, UTC midnight. */
private const val Nov1 = 1_793_491_200_000L
private fun nov(day: Int): Long = Nov1 + (day - 1) * Day

private val Members = listOf(
    MonarchTeamCalendarMember(id = "a", name = "Alex Kim", description = "Design"),
    MonarchTeamCalendarMember(id = "b", name = "Sam Lee", description = "Engineering"),
    MonarchTeamCalendarMember(id = "c", name = "Priya Nair"),
)

private val Leave =
    MonarchTeamCalendarEntry(id = "1", memberId = "a", start = nov(2), end = nov(5), label = "Annual leave")
private val Doctor = MonarchTeamCalendarEntry(
    id = "2",
    memberId = "b",
    start = nov(3),
    label = "Doctor",
    portion = MonarchTeamCalendarPortion.FirstHalf,
)
private val Conference = MonarchTeamCalendarEntry(
    id = "3",
    memberId = "c",
    start = nov(4),
    end = nov(6),
    label = "Conference",
    pending = true,
    portions = mapOf(nov(6) to MonarchTeamCalendarPortion.FirstHalf),
)
private val Entries = listOf(Leave, Doctor, Conference)

@Composable
private fun TestTeamCalendar(
    members: List<MonarchTeamCalendarMember> = Members,
    from: Long? = null,
    to: Long? = null,
    today: Long = nov(3),
    loading: Boolean = false,
    error: String? = null,
    showLegend: Boolean = true,
    labels: MonarchTeamCalendarLabels = MonarchTeamCalendarLabels(),
    onEntryClick: ((MonarchTeamCalendarEntry, MonarchTeamCalendarMember) -> Unit)? = { _, _ -> },
    onCellClick: ((MonarchTeamCalendarMember, Long) -> Unit)? = { _, _ -> },
    empty: (@Composable () -> Unit)? = null,
) {
    MonarchTeamCalendar(
        members = members,
        entries = Entries,
        modifier = Modifier.height(320.dp),
        month = nov(14),
        from = from,
        to = to,
        holidays = listOf(MonarchTeamCalendarHoliday(nov(11), "Veterans Day")),
        today = today,
        loading = loading,
        error = error,
        showLegend = showLegend,
        labels = labels,
        onEntryClick = onEntryClick,
        onCellClick = onCellClick,
        empty = empty,
    )
}

internal object MonarchTeamCalendarFixtures : ComponentFixtures {
    override val id = "team-calendar"
    override val fixtures = listOf(
        fixture("a month: bars, a half day, pending, a holiday, taps") { TestTeamCalendar() },
        fixture("a custom range, read only, no legend") {
            TestTeamCalendar(from = nov(2), to = nov(8), onEntryClick = null, onCellClick = null, showLegend = false)
        },
        fixture("today outside the range") { TestTeamCalendar(today = nov(1) + 40 * Day) },
        fixture("a long name") {
            TestTeamCalendar(
                members = listOf(
                    MonarchTeamCalendarMember(
                        id = "a",
                        name = "Maria Fernanda de los Santos Oliveira",
                        description = "Customer success, EMEA",
                    ),
                ),
            )
        },
        fixture("loading") { TestTeamCalendar(loading = true) },
        fixture("error") { TestTeamCalendar(error = "The calendar could not load.") },
        fixture("no members") { TestTeamCalendar(members = emptyList()) },
        fixture("no members, custom empty content") {
            TestTeamCalendar(members = emptyList()) { MonarchText(text = "Add people to your team") }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchTeamCalendarTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchTeamCalendarTokens

    @Test
    fun aBarIsOneButtonReadWithItsLabelPersonAndDays() {
        var tapped: Pair<String, String>? = null
        rule.setMonarchContent { TestTeamCalendar(onEntryClick = { entry, member -> tapped = entry.id to member.id }) }
        rule.onNodeWithContentDescription("Annual leave, Alex Kim, Monday, November 2 to Thursday, November 5")
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("1" to "a", tapped)
    }

    @Test
    fun halfDaysAndPendingEntriesAreSaidNotOnlyDrawn() {
        rule.setMonarchContent { TestTeamCalendar() }
        rule.onNodeWithContentDescription("Doctor, Sam Lee, Tuesday, November 3, first half")
            .assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithContentDescription(
            "Conference, Priya Nair, Wednesday, November 4 to Thursday, November 5, pending",
        ).assertExists()
        rule.onNodeWithContentDescription("Conference, Priya Nair, Friday, November 6, first half, pending")
            .assertExists()
    }

    @Test
    fun anEmptyDayReportsTheMemberAndTheUtcMidnightOfTheDay() {
        var tapped: Pair<String, Long>? = null
        rule.setMonarchContent { TestTeamCalendar(onCellClick = { member, day -> tapped = member.id to day }) }
        rule.onNodeWithContentDescription("Sam Lee, Wednesday, November 4")
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("b" to nov(4), tapped)
    }

    @Test
    fun withoutCallbacksBarsAreReadButTakeNoTapsAndEmptyDaysAreNotElements() {
        rule.setMonarchContent { TestTeamCalendar(onEntryClick = null, onCellClick = null) }
        rule.onNodeWithContentDescription("Annual leave, Alex Kim", substring = true).assertHasNoClickAction()
        val emptyDays = rule.onAllNodesWithContentDescription("Sam Lee, Wednesday", substring = true)
        assertTrue(emptyDays.fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun theCornerNamesTheGridAndDayHeadersSayTodayAndHolidays() {
        rule.setMonarchContent { TestTeamCalendar() }
        rule.onNodeWithContentDescription("Team calendar, November 2026")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithContentDescription("Today, Tuesday, November 3").assertExists()
        rule.onNodeWithContentDescription("Wednesday, November 11, Holiday: Veterans Day").assertExists()
        rule.onNode(hasText("Alex Kim")).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText(
            "Shaded columns are weekends and holidays. Half bars are half days; " +
                "faded bars are waiting for approval.",
        ).assertExists()
    }

    @Test
    fun aCustomRangeIsNamedByItsDaysAndShowsOnlyThem() {
        rule.setMonarchContent { TestTeamCalendar(from = nov(2), to = nov(8), showLegend = false) }
        rule.onNodeWithContentDescription("Team calendar, Nov 2 – Nov 8, 2026").assertExists()
        rule.onNodeWithContentDescription("Sunday, November 8").assertExists()
        val outside = rule.onAllNodesWithContentDescription("November 9", substring = true)
        assertTrue(outside.fetchSemanticsNodes().isEmpty())
        val legend = rule.onAllNodesWithText("Shaded columns", substring = true)
        assertTrue(legend.fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            TestTeamCalendar(
                labels = MonarchTeamCalendarLabels(
                    person = "Personne",
                    grid = { "Calendrier, $it" },
                    today = "Aujourd’hui",
                    pending = "en attente",
                    range = { a, b -> "$a – $b" },
                    legend = "Légende",
                ),
            )
        }
        rule.onNodeWithContentDescription("Calendrier, November 2026").assertExists()
        rule.onNodeWithContentDescription("Aujourd’hui, Tuesday, November 3").assertExists()
        rule.onNodeWithContentDescription(
            "Conference, Priya Nair, Wednesday, November 4 – Thursday, November 5, en attente",
        ).assertExists()
        rule.onNodeWithText("Légende").assertExists()
    }

    @Test
    fun loadingErrorAndEmptyReplaceTheGrid() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { TestTeamCalendar(loading = true) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Loading team calendar").assertExists()
        val grid = rule.onAllNodesWithContentDescription("Team calendar", substring = true)
        assertTrue(grid.fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun anErrorIsAnnouncedInADangerBanner() {
        rule.setMonarchContent { TestTeamCalendar(error = "The calendar could not load.") }
        rule.onNode(hasText("The calendar could not load.", substring = true)).assertExists()
        assertTrue(
            rule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
                .fetchSemanticsNodes().isNotEmpty(),
        )
    }

    @Test
    fun noMembersShowsTheEmptyStateOrTheAppsContent() {
        rule.setMonarchContent { TestTeamCalendar(members = emptyList()) }
        rule.onNodeWithText("Nobody to show").assertExists()
        rule.onNodeWithText("People and their time away appear here.").assertExists()
    }

    @Test
    fun aCustomEmptyContentReplacesTheBuiltInOne() {
        rule.setMonarchContent { TestTeamCalendar(members = emptyList()) { MonarchText(text = "Add people") } }
        rule.onNodeWithText("Add people").assertExists()
        assertTrue(rule.onAllNodesWithText("Nobody to show").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun aLongPressOnABarShowsItsLabelInATooltip() {
        rule.setMonarchContent { TestTeamCalendar() }
        rule.onNodeWithContentDescription("Doctor, Sam Lee", substring = true).performTouchInput { longClick() }
        rule.waitForIdle()
        rule.onNodeWithText("Doctor").assertExists()
    }

    @Test
    fun namesAreNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) { TestTeamCalendar() }
        rule.onNodeWithText("Alex Kim", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Engineering", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithContentDescription("Sam Lee, Wednesday, November 4").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun rightToLeftPutsPeopleAtTheRightAndEarlierDaysToTheRight() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { TestTeamCalendar() }
        rule.onNodeWithContentDescription("Team calendar, November 2026")
            .assertIsBefore(rule.onNodeWithContentDescription("Today, Tuesday, November 3"), LayoutDirection.Rtl)
        rule.onNodeWithContentDescription("Today, Tuesday, November 3")
            .assertIsBefore(rule.onNodeWithContentDescription("Wednesday, November 4"), LayoutDirection.Rtl)
    }

    @Test
    fun theRangeIsTheMonthOrTheCustomDaysCappedAtAYear() {
        assertEquals(nov(1) to nov(30), tokens.range(month = nov(14) + 5_000L, from = null, to = null, today = 0L))
        assertEquals(nov(1) to nov(30), tokens.range(month = null, from = nov(2), to = null, today = nov(20)))
        assertEquals(nov(2) to nov(8), tokens.range(month = nov(14), from = nov(2) + 1L, to = nov(8) + 1L, today = 0L))
        assertEquals(nov(8) to nov(8), tokens.range(month = null, from = nov(8), to = nov(2), today = 0L))
        val long = tokens.range(month = null, from = nov(1), to = nov(1) + 900 * Day, today = 0L)
        assertEquals(tokens.MAX_DAYS, tokens.days(long.first, long.second).size)
    }

    @Test
    fun fullDaysOfOneEntryMergeAndHalfDaysStandAlone() {
        val days = tokens.days(nov(1), nov(7))
        val conference = tokens.segments(tokens.cells(listOf(Conference), days), days)
        assertEquals(2, conference.size)
        assertEquals(listOf(3 to 2, 5 to 1), conference.map { it.first to it.count })
        assertEquals(MonarchTeamCalendarPortion.FirstHalf, conference[1].portion)
        assertTrue(conference[0].startsEntry && !conference[0].endsEntry)
        assertTrue(!conference[1].startsEntry && conference[1].endsEntry)
    }

    @Test
    fun anEntryCutByTheRangeHasNoEdgeThereAndALaterEntryWinsADay() {
        val days = tokens.days(nov(3), nov(4))
        val cut = tokens.segments(tokens.cells(listOf(Leave), days), days).single()
        assertEquals(0 to 2, cut.first to cut.count)
        assertTrue(!cut.startsEntry && !cut.endsEntry)

        val sick = MonarchTeamCalendarEntry(id = "9", memberId = "a", start = nov(4), label = "Sick")
        val both = tokens.segments(tokens.cells(listOf(Leave, sick), days), days)
        assertEquals(listOf("1", "9"), both.map { it.entry.id })
        val backwards = MonarchTeamCalendarEntry(id = "8", memberId = "a", start = nov(4), end = nov(1), label = "x")
        assertEquals(nov(4), tokens.lastDay(backwards))
    }

    @Test
    fun weekendsAndHolidaysAreShaded() {
        val holidays = mapOf(nov(11) to "Veterans Day")
        val weekend = setOf(java.util.Calendar.SATURDAY, java.util.Calendar.SUNDAY)
        assertTrue(tokens.shaded(nov(1), weekend, holidays))
        assertTrue(tokens.shaded(nov(11), weekend, holidays))
        assertTrue(!tokens.shaded(nov(2), weekend, holidays))
    }

    @Test
    fun coloursComeFromRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val accent = tokens.barColors(colors, Color.Unspecified, pending = false)
            assertEquals(colors.accentSolid, accent.stripe)
            assertNull(accent.border)
            assertEquals(colors.textDefault, accent.content)
            assertTrue(accent.container != colors.bgSurface, theme.id)
            val pending = tokens.barColors(colors, colors.warningSolid, pending = true)
            assertEquals(colors.bgSurface, pending.container)
            assertEquals(colors.warningSolid, pending.border)
            assertNull(pending.stripe)
            assertEquals(colors.bgMuted, tokens.columnColor(colors, shaded = true))
            assertEquals(Color.Transparent, tokens.columnColor(colors, shaded = false))
            val today = tokens.headerColors(colors, today = true)
            assertEquals(colors.accentStrong, today.content)
            assertEquals(colors.accentBorder, today.border)
            assertEquals(colors.textMuted, tokens.headerColors(colors, today = false).content)
        }
    }

    @Test
    fun formatsUseTheLocaleInUtc() {
        val formats = TeamCalendarFormats(Locale.US)
        assertEquals("November 2026", formats.month(nov(30) + Day - 1))
        assertEquals("Monday, November 2", formats.day(nov(2)))
        assertEquals("2", formats.number(nov(2)))
        assertEquals("M", formats.narrowWeekday(nov(2)))
        assertEquals("Nov 2 – Nov 8, 2026", formats.range(nov(2), nov(8)))
    }
}
