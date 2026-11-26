@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.scheduling

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.scheduling.shiftroster.MonarchShiftRoster
import com.rookhq.monarch.components.scheduling.shiftroster.MonarchShiftRosterAssignment
import com.rookhq.monarch.components.scheduling.shiftroster.MonarchShiftRosterChange
import com.rookhq.monarch.components.scheduling.shiftroster.MonarchShiftRosterLabels
import com.rookhq.monarch.components.scheduling.shiftroster.MonarchShiftRosterLayout
import com.rookhq.monarch.components.scheduling.shiftroster.MonarchShiftRosterMember
import com.rookhq.monarch.components.scheduling.shiftroster.MonarchShiftRosterShift
import com.rookhq.monarch.components.scheduling.shiftroster.MonarchShiftRosterTokens
import com.rookhq.monarch.components.scheduling.shiftroster.ShiftRosterRules
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
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

/** Monday 2 November 2026, 00:00 UTC. */
private const val Monday = 1_793_577_600_000L
private const val Day = 86_400_000L
private const val Hour = 3_600_000L
private val Utc: TimeZone = TimeZone.getTimeZone("UTC")

private val Morning = MonarchShiftRosterShift("m", "Morning", start = 6 * 60, end = 14 * 60, shortName = "M")
private val Night = MonarchShiftRosterShift("n", "Night", start = 22 * 60, end = 6 * 60, shortName = "N")
private val Shifts = listOf(Morning, Night)
private val Alex = MonarchShiftRosterMember("a", "Alex Kim", description = "Support")
private val Sam = MonarchShiftRosterMember("b", "Sam Lee")

private val SampleValue = mapOf(
    "a" to mapOf(
        Monday to MonarchShiftRosterAssignment.Shift("m"),
        Monday + Day to MonarchShiftRosterAssignment.Off,
    ),
    "b" to mapOf(Monday to MonarchShiftRosterAssignment.Shift("n")),
)

@androidx.compose.runtime.Composable
private fun Roster(
    members: List<MonarchShiftRosterMember> = listOf(Alex),
    value: Map<String, Map<Long, MonarchShiftRosterAssignment>> = SampleValue,
    onValueChange: (Map<String, Map<Long, MonarchShiftRosterAssignment>>, MonarchShiftRosterChange) -> Unit =
        { _, _ -> },
    days: Int = 3,
    layout: MonarchShiftRosterLayout = MonarchShiftRosterLayout.ByDay,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    showTotals: Boolean = true,
    onMemberClick: ((MonarchShiftRosterMember) -> Unit)? = null,
    labels: MonarchShiftRosterLabels = MonarchShiftRosterLabels(),
) {
    MonarchShiftRoster(
        members = members,
        shifts = Shifts,
        from = Monday,
        value = value,
        onValueChange = onValueChange,
        days = days,
        layout = layout,
        enabled = enabled,
        readOnly = readOnly,
        showTotals = showTotals,
        today = Monday + 9 * Hour,
        timeZone = Utc.id,
        is24Hour = true,
        onMemberClick = onMemberClick,
        labels = labels,
    )
}

internal object MonarchShiftRosterFixtures : ComponentFixtures {
    override val id = "shift-roster"
    override val fixtures = listOf(
        fixture("by day, a shift, today") { Roster() },
        fixture("by day, a day off, member button") {
            Roster(value = mapOf("a" to mapOf(Monday to MonarchShiftRosterAssignment.Off)), onMemberClick = {})
        },
        fixture("by day, not assigned, disabled, no totals") {
            Roster(value = emptyMap(), enabled = false, showTotals = false)
        },
        fixture("by day, read only, overnight") { Roster(members = listOf(Sam), readOnly = true) },
        fixture("by person, one day") { Roster(days = 1, layout = MonarchShiftRosterLayout.ByPerson) },
        fixture("grid, one day, two people") {
            Roster(members = listOf(Alex, Sam), days = 1, layout = MonarchShiftRosterLayout.Grid, showTotals = false)
        },
        fixture("long names") {
            Roster(
                members = listOf(
                    MonarchShiftRosterMember(
                        "a",
                        "Maria Fernanda Albuquerque de Oliveira",
                        description = "Customer support, second line, weekend rota",
                    ),
                ),
            )
        },
        fixture("loading") { LoadingRoster() },
        fixture("error") {
            MonarchShiftRoster(
                members = listOf(Alex),
                shifts = Shifts,
                from = Monday,
                value = emptyMap(),
                onValueChange = { _, _ -> },
                error = "The roster could not be loaded.",
            )
        },
        fixture("empty") {
            MonarchShiftRoster(
                members = emptyList(),
                shifts = Shifts,
                from = Monday,
                value = emptyMap(),
                onValueChange = { _, _ -> },
            )
        },
    )
}

@androidx.compose.runtime.Composable
private fun LoadingRoster() {
    MonarchShiftRoster(
        members = listOf(Alex),
        shifts = Shifts,
        from = Monday,
        value = emptyMap(),
        onValueChange = { _, _ -> },
        loading = true,
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchShiftRosterTest {
    @get:Rule
    val rule = createComposeRule()

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

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
    fun rangeStartsOnTheDateOfFromAndIsBounded() {
        assertEquals(listOf(Monday, Monday + Day, Monday + 2 * Day), ShiftRosterRules.range(Monday + 15 * Hour, 3))
        assertEquals(1, ShiftRosterRules.range(Monday, 0).size)
        assertEquals(ShiftRosterRules.MAX_DAYS, ShiftRosterRules.range(Monday, 10_000).size)
        assertEquals(Monday - Day, ShiftRosterRules.dayKey(Monday - 1))
    }

    @Test
    fun todayIsReadInTheTimeZoneAndOpensFirst() {
        val tokyo = TimeZone.getTimeZone("Asia/Tokyo")
        assertEquals(Monday, ShiftRosterRules.localDay(Monday + 20 * Hour, Utc))
        assertEquals(Monday + Day, ShiftRosterRules.localDay(Monday + 20 * Hour, tokyo))
        val range = ShiftRosterRules.range(Monday, 3)
        assertEquals(1, MonarchShiftRosterTokens.initialIndex(range, Monday + Day))
        assertEquals(0, MonarchShiftRosterTokens.initialIndex(range, Monday + 9 * Day))
        assertEquals(0, MonarchShiftRosterTokens.initialIndex(range, null))
    }

    @Test
    fun assignSetsReplacesAndClearsOneDayAndKeepsTheRest() {
        val value = mapOf("a" to mapOf(Monday + 5 * Hour to MonarchShiftRosterAssignment.Off))
        assertEquals(MonarchShiftRosterAssignment.Off, ShiftRosterRules.assignmentOf(value, "a", Monday))
        val set = ShiftRosterRules.assign(value, "a", Monday, MonarchShiftRosterAssignment.Shift("m"))
        assertEquals(mapOf(Monday to MonarchShiftRosterAssignment.Shift("m")), set["a"])
        val other = ShiftRosterRules.assign(set, "b", Monday + Day, MonarchShiftRosterAssignment.Off)
        assertEquals(set["a"], other["a"])
        val cleared = ShiftRosterRules.assign(other, "a", Monday, null)
        assertEquals(emptyMap(), cleared["a"])
        assertNull(ShiftRosterRules.assignmentOf(cleared, "a", Monday))
    }

    @Test
    fun hoursCountOvernightShiftsAndIgnoreOffAndUnknownShifts() {
        val byId = Shifts.associateBy { it.id }
        assertEquals(8 * 60, ShiftRosterRules.durationMinutes(Morning))
        assertEquals(8 * 60, ShiftRosterRules.durationMinutes(Night))
        assertTrue(ShiftRosterRules.overnight(Night))
        assertFalse(ShiftRosterRules.overnight(Morning))
        assertEquals(24 * 60, ShiftRosterRules.durationMinutes(MonarchShiftRosterShift("d", "Day", 480, 480)))
        val value = mapOf(
            "a" to mapOf(
                Monday to MonarchShiftRosterAssignment.Shift("m"),
                Monday + Day to MonarchShiftRosterAssignment.Shift("n"),
                Monday + 2 * Day to MonarchShiftRosterAssignment.Off,
                Monday + 3 * Day to MonarchShiftRosterAssignment.Shift("gone"),
            ),
        )
        val range = ShiftRosterRules.range(Monday, 7)
        assertEquals(16.0, ShiftRosterRules.hours(value, "a", range, byId))
        assertEquals(1, ShiftRosterRules.working(value, listOf(Alex, Sam), Monday, byId))
        assertEquals(0, ShiftRosterRules.working(value, listOf(Alex, Sam), Monday + 2 * Day, byId))
        assertEquals(0, ShiftRosterRules.working(value, listOf(Alex, Sam), Monday + 3 * Day, byId))
    }

    @Test
    fun automaticShowsTheGridOnlyWhenItFitsAndTheTextIsNotEnlarged() {
        val tokens = MonarchShiftRosterTokens
        assertTrue(tokens.fitsGrid(1100.dp, 7, totals = true, fontScale = 1f))
        assertFalse(tokens.fitsGrid(400.dp, 7, totals = true, fontScale = 1f))
        assertFalse(tokens.fitsGrid(1100.dp, 7, totals = true, fontScale = LargestFontScale))
        assertEquals(MonarchShiftRosterLayout.Grid, tokens.resolve(MonarchShiftRosterLayout.Automatic, fits = true))
        assertEquals(MonarchShiftRosterLayout.ByDay, tokens.resolve(MonarchShiftRosterLayout.Automatic, fits = false))
        assertEquals(MonarchShiftRosterLayout.ByPerson, tokens.resolve(MonarchShiftRosterLayout.ByPerson, fits = true))
    }

    @Test
    fun slotColoursComeFromRolesInEveryTheme() {
        AllThemes.forEach { tokens ->
            val colors = MonarchColors(tokens.colors)
            val off = MonarchShiftRosterTokens.slotColors(colors, off = true)
            assertEquals(colors.bgMuted, off.container)
            assertEquals(colors.textMuted, off.content)
            assertNull(off.border)
            val empty = MonarchShiftRosterTokens.slotColors(colors, off = false)
            assertEquals(colors.borderDefault, empty.border)
            assertNotEquals(off.container, empty.container)
            // A day's name is neutral, today included; the Today badge marks it.
            assertEquals(colors.textDefault, MonarchShiftRosterTokens.dayColor(colors))
        }
    }

    @Test
    fun aCellIsOneButtonNamedByPersonDayAndShift() {
        rule.setMonarchContent { Roster() }
        rule.onNodeWithContentDescription("Alex Kim, Monday, November 2, Today: Morning, 06:00 to 14:00")
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun pickingAShiftFromTheMenuReportsTheValueAndTheChange() {
        var result: Map<String, Map<Long, MonarchShiftRosterAssignment>>? = null
        var change: MonarchShiftRosterChange? = null
        rule.setMonarchContent {
            Roster(onValueChange = { v, c ->
                result = v
                change = c
            })
        }
        rule.onNode(hasContentDescription("Alex Kim, Monday", substring = true)).performClick()
        rule.onNodeWithText("Alex Kim, Monday, November 2").assert(isHeading)
        rule.onNodeWithContentDescription("Morning, 06:00 to 14:00").assertIsSelected()
        rule.onNodeWithContentDescription("Night, 06:00 to 14:00", substring = true).assertDoesNotExist()
        rule.onNodeWithContentDescription("Night, 22:00 to 06:00, overnight").performClick()
        assertEquals(MonarchShiftRosterChange("a", Monday, MonarchShiftRosterAssignment.Shift("n")), change)
        assertEquals(MonarchShiftRosterAssignment.Shift("n"), result?.get("a")?.get(Monday))
        assertEquals(MonarchShiftRosterAssignment.Off, result?.get("a")?.get(Monday + Day))
    }

    @Test
    fun clearIsOfferedOnlyForAnAssignedDayAndRemovesIt() {
        var change: MonarchShiftRosterChange? = null
        rule.setMonarchContent { Roster(onValueChange = { _, c -> change = c }) }
        rule.onNode(hasContentDescription("Alex Kim, Monday", substring = true)).performClick()
        rule.onNodeWithText("Clear").performClick()
        assertEquals(MonarchShiftRosterChange("a", Monday, null), change)
    }

    @Test
    fun anUnassignedDayOffersOffButNotClear() {
        var change: MonarchShiftRosterChange? = null
        rule.setMonarchContent { Roster(value = emptyMap(), onValueChange = { _, c -> change = c }) }
        rule.onNodeWithContentDescription("Alex Kim, Monday, November 2, Today: Not assigned").performClick()
        rule.onNodeWithText("Clear").assertDoesNotExist()
        rule.onNodeWithContentDescription("Off").performClick()
        assertEquals(MonarchShiftRosterChange("a", Monday, MonarchShiftRosterAssignment.Off), change)
    }

    @Test
    fun readOnlyCellsAreTextAndDisabledCellsAreNotEnabled() {
        rule.setMonarchContent {
            androidx.compose.foundation.layout.Column {
                Roster(members = listOf(Sam), readOnly = true)
                Roster(members = listOf(Alex), enabled = false, showTotals = false)
            }
        }
        rule.onNodeWithContentDescription("Sam Lee, Monday, November 2, Today: Night, 22:00 to 06:00, overnight")
            .assertHasNoClickAction()
        rule.onNodeWithContentDescription("Alex Kim, Monday, November 2, Today: Morning, 06:00 to 14:00")
            .assertIsNotEnabled()
    }

    @Test
    fun theDayTabsSwitchTheDayAndTodayIsMarked() {
        rule.setMonarchContent { Roster() }
        rule.onNodeWithContentDescription("Monday, November 2, Today").assertIsSelected()
        rule.onNodeWithText("Monday, November 2").assert(isHeading)
        // The day tab is a rich face: "Today" in place of "Mon" over the day number, in the accent colour.
        rule.onAllNodesWithText("Today", useUnmergedTree = true)[0].assertExists()
        rule.onNodeWithText("Mon", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("Tue", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Tuesday, November 3").performClick().assertIsSelected()
        rule.onNodeWithText("Tuesday, November 3").assert(isHeading)
        rule.onNodeWithContentDescription("Alex Kim, Tuesday, November 3: Off").assertHasClickAction()
    }

    @Test
    fun theWorkingCountIsAPoliteLiveRegionAndThePersonReadsTheirHours() {
        rule.setMonarchContent { Roster(members = listOf(Alex, Sam)) }
        rule.onNodeWithText("2 working").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        rule.onNodeWithContentDescription("Alex Kim, Support, Hours: 8 h").assertHasNoClickAction()
        rule.onNodeWithContentDescription("Sam Lee, Hours: 8 h").assertExists()
    }

    @Test
    fun aPersonIsAButtonWithOnMemberClick() {
        var opened: String? = null
        rule.setMonarchContent { Roster(onMemberClick = { opened = it.id }) }
        rule.onNodeWithContentDescription("Alex Kim, Support, Hours: 8 h")
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("a", opened)
    }

    @Test
    fun byPersonShowsOneRowPerDayForThePickedPerson() {
        rule.setMonarchContent { Roster(members = listOf(Alex, Sam), layout = MonarchShiftRosterLayout.ByPerson) }
        rule.onNodeWithContentDescription("Alex Kim").assertIsSelected()
        rule.onNodeWithContentDescription("Alex Kim, Tuesday, November 3: Off").assertExists()
        rule.onNodeWithContentDescription("Sam Lee").performClick()
        rule.onNodeWithContentDescription("Sam Lee, Monday, November 2, Today: Night, 22:00 to 06:00, overnight")
            .assertHasClickAction()
        rule.onNodeWithContentDescription("Sam Lee, Wednesday, November 4: Not assigned").assertExists()
    }

    @Test
    fun theGridIsACollectionWithShortNamesAndCounts() {
        rule.setMonarchContent {
            Roster(members = listOf(Alex, Sam), days = 1, layout = MonarchShiftRosterLayout.Grid)
        }
        rule.onNodeWithContentDescription("Shift roster")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionInfo))
        rule.onNodeWithContentDescription("Sam Lee, Monday, November 2, Today: Night, N, 22:00 to 06:00, overnight")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionItemInfo))
        rule.onNodeWithContentDescription("Monday, November 2, Today").assert(isHeading)
        rule.onNodeWithContentDescription("Monday, November 2: 2 working").assertExists()
        rule.onAllNodes(hasText("Hours")).assertCountEquals(1)
    }

    @Test
    fun rightToLeftPutsThePersonBeforeTheCell() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { Roster() }
        rule.onNodeWithText("Alex Kim", useUnmergedTree = true).assertIsBefore(
            rule.onNodeWithText("Morning", useUnmergedTree = true),
            LayoutDirection.Rtl,
        )
    }

    @Test
    fun atTheLargestFontScaleTheCellGoesUnderThePersonAndNothingIsCut() {
        rule.setMonarchContent(fontScale = LargestFontScale) { Roster() }
        val name = rule.onNodeWithText("Alex Kim", useUnmergedTree = true).assertTextFullyVisible()
        val shift = rule.onNodeWithText("Morning", useUnmergedTree = true).assertTextFullyVisible()
        assertTrue(name.getUnclippedBoundsInRoot().bottom <= shift.getUnclippedBoundsInRoot().top)
        rule.onNodeWithText("Support · 8 h", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun loadingErrorAndEmptyReplaceTheRoster() {
        rule.setMonarchContent {
            androidx.compose.foundation.layout.Column {
                LoadingRoster()
                MonarchShiftRoster(
                    members = listOf(Alex),
                    shifts = Shifts,
                    from = Monday,
                    value = emptyMap(),
                    onValueChange = { _, _ -> },
                    error = "The roster could not be loaded.",
                )
                Box {
                    MonarchShiftRoster(
                        members = emptyList(),
                        shifts = Shifts,
                        from = Monday,
                        value = emptyMap(),
                        onValueChange = { _, _ -> },
                    )
                }
            }
        }
        rule.onNodeWithContentDescription("Loading roster").assertExists()
        rule.onNodeWithText("The roster could not be loaded.").assertExists()
        rule.onNodeWithText("No one to schedule").assertExists()
        rule.onNodeWithText("Alex Kim").assertDoesNotExist()
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            Roster(
                value = emptyMap(),
                labels = MonarchShiftRosterLabels(
                    unassigned = "Sin asignar",
                    today = "Hoy",
                    cell = { name, day, assignment -> "$assignment · $name · $day" },
                    hoursValue = { "$it horas" },
                    workingValue = { "$it trabajando" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Sin asignar · Alex Kim · Monday, November 2, Hoy").assertHasClickAction()
        rule.onNodeWithText("0 trabajando").assertExists()
        rule.onNodeWithText("Support · 0 horas", useUnmergedTree = true).assertExists()
    }
}
