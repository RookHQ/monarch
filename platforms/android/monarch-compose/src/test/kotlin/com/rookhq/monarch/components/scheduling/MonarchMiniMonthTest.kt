package com.rookhq.monarch.components.scheduling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.scheduling.minimonth.MiniMonthDayState
import com.rookhq.monarch.components.scheduling.minimonth.MonarchMiniMonth
import com.rookhq.monarch.components.scheduling.minimonth.MonarchMiniMonthDayInfo
import com.rookhq.monarch.components.scheduling.minimonth.MonarchMiniMonthLabels
import com.rookhq.monarch.components.scheduling.minimonth.MonarchMiniMonthTokens
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.scheduling.MonarchCalendarDates
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import kotlin.test.Test
import kotlin.test.assertEquals

private const val Day = 86_400_000L

/** 1 November 2026, a Sunday, UTC midnight. */
private const val November1 = 1_793_491_200_000L
private const val November5 = November1 + 4 * Day
private const val November14 = November1 + 13 * Day
private const val December1 = November1 + 30 * Day

@Composable
private fun TestMiniMonth(
    selection: Long? = November14,
    onSelectionChange: (Long) -> Unit = {},
    visibleMonth: Long? = November1,
    onVisibleMonthChange: ((Long) -> Unit)? = null,
    minimumDate: Long? = null,
    maximumDate: Long? = null,
    showWeekNumbers: Boolean = false,
    showOutsideDays: Boolean = true,
    fixedWeeks: Boolean = true,
    enabled: Boolean = true,
    isDateDisabled: ((Long) -> Boolean)? = null,
    dayInfo: ((Long) -> MonarchMiniMonthDayInfo?)? = null,
) {
    MonarchMiniMonth(
        selection = selection,
        onSelectionChange = onSelectionChange,
        previousMonthIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
        nextMonthIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
        visibleMonth = visibleMonth,
        onVisibleMonthChange = onVisibleMonthChange,
        minimumDate = minimumDate,
        maximumDate = maximumDate,
        today = November5,
        firstDayOfWeek = Calendar.SUNDAY,
        showWeekNumbers = showWeekNumbers,
        showOutsideDays = showOutsideDays,
        fixedWeeks = fixedWeeks,
        enabled = enabled,
        isDateDisabled = isDateDisabled,
        dayInfo = dayInfo,
    )
}

internal object MonarchMiniMonthFixtures : ComponentFixtures {
    override val id = "mini-month"
    override val fixtures = listOf(
        fixture("a selection and today") { TestMiniMonth() },
        fixture("bounds, disabled days, dots and week numbers") {
            TestMiniMonth(
                minimumDate = November5,
                maximumDate = November1 + 24 * Day,
                showWeekNumbers = true,
                isDateDisabled = { MonarchCalendarDates.dayOfWeek(it) == Calendar.SATURDAY },
                dayInfo = { day ->
                    when (MonarchCalendarDates.dayOfMonth(day) % 3) {
                        0 -> MonarchMiniMonthDayInfo(dotCount = 5, description = "Five events")
                        1 -> MonarchMiniMonthDayInfo(listOf(MonarchTone.Danger, MonarchTone.Neutral), "Two events")
                        else -> null
                    }
                },
            )
        },
        fixture("only the month's weeks, no outside days, disabled") {
            TestMiniMonth(selection = null, showOutsideDays = false, fixedWeeks = false, enabled = false)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchMiniMonthTest {
    @get:Rule
    val rule = createComposeRule()

    private fun day(text: String) = rule.onNodeWithContentDescription(text, substring = true)

    @Test
    fun aDayIsReadAsItsDateAndChoosingItReportsItsUtcMidnight() {
        var chosen: Long? = null
        rule.setMonarchContent { TestMiniMonth(onSelectionChange = { chosen = it }) }
        day("November 14, 2026").assertIsSelected()
        day("November 20, 2026").assertIsNotSelected().assertIsEnabled().performClick()
        assertEquals(November1 + 19 * Day, chosen)
        day("Today, ").assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(dateOf(5))))
    }

    @Test
    fun daysOutsideTheBoundsAndDisabledDaysCannotBeChosen() {
        var chosen: Long? = null
        rule.setMonarchContent {
            TestMiniMonth(
                onSelectionChange = { chosen = it },
                minimumDate = November5,
                maximumDate = November1 + 24 * Day,
                isDateDisabled = { it == November1 + 9 * Day },
            )
        }
        day("November 4, 2026").assertIsNotEnabled().performClick()
        day("November 26, 2026").assertIsNotEnabled()
        day("November 10, 2026").assertIsNotEnabled().performClick()
        assertEquals(null, chosen)
        day("November 5, 2026").assertIsEnabled()
        rule.onNodeWithContentDescription("Previous month").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Next month").assertIsNotEnabled()
    }

    @Test
    fun theMonthButtonsMoveAMonthAndReportIt() {
        var month by mutableStateOf<Long?>(November1)
        val reported = mutableListOf<Long>()
        rule.setMonarchContent {
            TestMiniMonth(visibleMonth = month, onVisibleMonthChange = { reported += it; month = it })
        }
        rule.onNodeWithText("November 2026").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithContentDescription("Next month").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("December 2026").assertExists()
        rule.onNodeWithContentDescription("Previous month").performClick()
        rule.onNodeWithText("November 2026").assertExists()
        assertEquals(listOf(December1, November1), reported)
    }

    @Test
    fun theMonthTitleIsAnnouncedWhenItChanges() {
        rule.setMonarchContent { TestMiniMonth() }
        rule.onNodeWithText("November 2026")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun theDescriptionOfTheDotsIsReadWithTheDayAndWeekNumbersAreNamed() {
        rule.setMonarchContent {
            TestMiniMonth(
                showWeekNumbers = true,
                dayInfo = { day ->
                    if (day == November14) MonarchMiniMonthDayInfo(dotCount = 2, description = "2 events") else null
                },
            )
        }
        day("November 14, 2026, 2 events").assertExists()
        rule.onNodeWithContentDescription("Week 45").assertExists()
    }

    @Test
    fun aDayReservesA48dpTargetAndTheNumberIsNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) { TestMiniMonth() }
        day("November 1, 2026").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("14", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun rightToLeftPutsThePreviousButtonAndTheFirstDayOfTheWeekAtTheRight() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { TestMiniMonth() }
        rule.onNodeWithContentDescription("Previous month")
            .assertIsBefore(rule.onNodeWithContentDescription("Next month"), LayoutDirection.Rtl)
        day("November 1, 2026").assertIsBefore(day("November 2, 2026"), LayoutDirection.Rtl)
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchMiniMonth(
                selection = null,
                onSelectionChange = {},
                previousMonthIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                nextMonthIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                visibleMonth = November1,
                today = November5,
                showWeekNumbers = true,
                labels = MonarchMiniMonthLabels(previousMonth = "Zurück", today = "Heute", weekNumber = { "KW $it" }),
            )
        }
        rule.onNodeWithContentDescription("Zurück").assertExists()
        day("Heute, ").assertExists()
        rule.onNodeWithContentDescription("KW 45").assertExists()
    }

    @Test
    fun theCalendarArithmeticIsInUtc() {
        assertEquals(November1, MonarchCalendarDates.monthOf(November14 + 13 * 3_600_000L))
        assertEquals(December1, MonarchCalendarDates.addMonths(November14, 1))
        assertEquals(6, MonarchCalendarDates.weeks(November1, Calendar.SUNDAY, fixedWeeks = true).size)
        assertEquals(5, MonarchCalendarDates.weeks(November1, Calendar.SUNDAY, fixedWeeks = false).size)
        assertEquals(November1 - 6 * Day, MonarchCalendarDates.weeks(November1, Calendar.MONDAY, true).first().first())
        val firstWeek = MonarchCalendarDates.weeks(November1, Calendar.SUNDAY, true).first()
        assertEquals(45, MonarchCalendarDates.isoWeek(firstWeek))
        assertEquals(November1, MonarchMiniMonthTokens.clampMonth(December1, null, November14))
        assertEquals(December1, MonarchMiniMonthTokens.clampMonth(November1, December1, null))
        assertEquals(November5, MonarchMiniMonthTokens.latest(November5, November1))
    }

    @Test
    fun theDayStatesAndColoursComeFromTheRolesInEveryTheme() {
        val tokens = MonarchMiniMonthTokens
        val selected = tokens.dayState(November14, November1, November14, November5, null, null, true, false)
        assertEquals(
            MiniMonthDayState(inMonth = true, available = true, selected = true, today = false, disabledByApp = false),
            selected,
        )
        val outside = tokens.dayState(December1, November1, December1, November5, null, null, true, false)
        assertEquals(false, outside.selected)
        assertEquals(false, outside.inMonth)
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.accentSolid, tokens.dayColors(selected, colors).container, theme.id)
            assertEquals(colors.accentOnSolid, tokens.dayColors(selected, colors).content, theme.id)
            val today = selected.copy(selected = false, today = true)
            assertEquals(colors.accentBorder, tokens.dayColors(today, colors).border, theme.id)
            assertEquals(colors.accentStrong, tokens.dayColors(today, colors).content, theme.id)
            val unavailable = selected.copy(selected = false, available = false)
            assertEquals(colors.textDisabled, tokens.dayColors(unavailable, colors).content, theme.id)
            assertEquals(Color.Transparent, tokens.dayColors(unavailable, colors).container, theme.id)
            assertEquals(colors.textDisabled, tokens.dayColors(outside, colors).content, theme.id)
            assertEquals(colors.accentSolid, tokens.dotColor(null, colors), theme.id)
            assertEquals(colors.textSubtle, tokens.dotColor(MonarchTone.Neutral, colors), theme.id)
            assertEquals(colors.dangerSolid, tokens.dotColor(MonarchTone.Danger, colors), theme.id)
        }
    }

    private fun dateOf(day: Int): String = "Today, Thursday, November $day, 2026"
}
