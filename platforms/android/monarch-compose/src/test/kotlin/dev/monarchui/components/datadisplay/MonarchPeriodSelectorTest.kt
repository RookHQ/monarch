package dev.monarchui.components.datadisplay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.datadisplay.periodselector.MonarchPeriodPreset
import dev.monarchui.components.datadisplay.periodselector.MonarchPeriodRange
import dev.monarchui.components.datadisplay.periodselector.MonarchPeriodSelector
import dev.monarchui.components.datadisplay.periodselector.MonarchPeriodSelectorDefaults
import dev.monarchui.components.datadisplay.periodselector.MonarchPeriodSelectorLabels
import dev.monarchui.components.datadisplay.periodselector.MonarchPeriodSelectorTokens
import dev.monarchui.components.datadisplay.periodselector.MonarchPeriodSelectorVariant
import dev.monarchui.components.datadisplay.periodselector.MonarchPeriodValue
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
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val Day = 86_400_000L

/** UTC midnight of a date; [month] counts from 1. */
private fun day(year: Int, month: Int, date: Int): Long =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month - 1, date)
    }.timeInMillis

/** Sunday 15 November 2026. */
private val Today = day(2026, 11, 15)

internal object MonarchPeriodSelectorFixtures : ComponentFixtures {
    override val id = "period-selector"
    override val fixtures = listOf(
        fixture("segmented, default presets") {
            MonarchPeriodSelector(
                value = MonarchPeriodValue(MonarchPeriodPreset.Last7Days),
                onValueChange = {},
                today = Today,
                locale = Locale.US,
            )
        },
        fixture("segmented with the dates") {
            MonarchPeriodSelector(
                value = MonarchPeriodValue(MonarchPeriodPreset.ThisQuarter),
                onValueChange = {},
                showRange = true,
                today = Today,
                locale = Locale.US,
            )
        },
        fixture("disabled") {
            MonarchPeriodSelector(
                value = MonarchPeriodValue(MonarchPeriodPreset.Today),
                onValueChange = {},
                enabled = false,
                showRange = true,
                today = Today,
                locale = Locale.US,
            )
        },
        fixture("select") {
            MonarchPeriodSelector(
                value = MonarchPeriodValue(MonarchPeriodPreset.LastMonth),
                onValueChange = {},
                presets = MonarchPeriodPreset.entries,
                variant = MonarchPeriodSelectorVariant.Select,
                showRange = true,
                today = Today,
                locale = Locale.US,
            )
        },
        fixture("custom with dates") {
            MonarchPeriodSelector(
                value = MonarchPeriodValue(MonarchPeriodPreset.Custom, MonarchPeriodRange(day(2026, 10, 3), Today)),
                onValueChange = {},
                today = Today,
                locale = Locale.US,
            )
        },
        fixture("custom, no dates yet") {
            MonarchPeriodSelector(
                value = MonarchPeriodValue(MonarchPeriodPreset.Custom),
                onValueChange = {},
                variant = MonarchPeriodSelectorVariant.Select,
                today = Today,
                locale = Locale.US,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchPeriodSelectorTest {
    @get:Rule
    val rule = createComposeRule()

    private fun resolve(preset: MonarchPeriodPreset, firstDay: Int = Calendar.MONDAY) =
        MonarchPeriodSelectorDefaults.resolve(MonarchPeriodValue(preset), Today, firstDay)

    @Test
    fun rollingPeriodsEndTodayAndCountToday() {
        assertEquals(MonarchPeriodRange(day(2026, 11, 9), Today), resolve(MonarchPeriodPreset.Last7Days))
        assertEquals(MonarchPeriodRange(day(2026, 10, 17), Today), resolve(MonarchPeriodPreset.Last30Days))
        assertEquals(MonarchPeriodRange(day(2025, 11, 16), Today), resolve(MonarchPeriodPreset.Last365Days))
        assertEquals(MonarchPeriodRange(Today, Today), resolve(MonarchPeriodPreset.Today))
        val yesterday = day(2026, 11, 14)
        assertEquals(MonarchPeriodRange(yesterday, yesterday), resolve(MonarchPeriodPreset.Yesterday))
    }

    @Test
    fun calendarPeriodsFollowTheCalendar() {
        assertEquals(MonarchPeriodRange(day(2026, 11, 9), Today), resolve(MonarchPeriodPreset.ThisWeek))
        assertEquals(MonarchPeriodRange(Today, Today), resolve(MonarchPeriodPreset.ThisWeek, Calendar.SUNDAY))
        assertEquals(MonarchPeriodRange(day(2026, 11, 1), Today), resolve(MonarchPeriodPreset.ThisMonth))
        assertEquals(MonarchPeriodRange(day(2026, 10, 1), day(2026, 10, 31)), resolve(MonarchPeriodPreset.LastMonth))
        assertEquals(MonarchPeriodRange(day(2026, 10, 1), Today), resolve(MonarchPeriodPreset.ThisQuarter))
        assertEquals(MonarchPeriodRange(day(2026, 7, 1), day(2026, 9, 30)), resolve(MonarchPeriodPreset.LastQuarter))
        assertEquals(MonarchPeriodRange(day(2026, 1, 1), Today), resolve(MonarchPeriodPreset.ThisYear))
        val march = day(2026, 3, 31)
        assertEquals(
            MonarchPeriodRange(day(2026, 2, 1), day(2026, 2, 28)),
            MonarchPeriodSelectorDefaults.resolve(MonarchPeriodValue(MonarchPeriodPreset.LastMonth), march),
        )
        assertEquals(
            MonarchPeriodRange(day(2025, 10, 1), day(2025, 12, 31)),
            MonarchPeriodSelectorDefaults.resolve(MonarchPeriodValue(MonarchPeriodPreset.LastQuarter), march),
        )
    }

    @Test
    fun aCustomPeriodIsItsRangeOrNothing() {
        assertNull(resolve(MonarchPeriodPreset.Custom))
        val range = MonarchPeriodRange(day(2026, 1, 5), day(2026, 1, 9))
        assertEquals(
            range,
            MonarchPeriodSelectorDefaults.resolve(MonarchPeriodValue(MonarchPeriodPreset.Custom, range), Today),
        )
    }

    @Test
    fun todayIsTheCalendarDayInTheZone() {
        val lateEvening = Today + 23 * 3_600_000L + 30 * 60_000L
        assertEquals(Today, MonarchPeriodSelectorDefaults.today(lateEvening, "UTC"))
        assertEquals(Today + Day, MonarchPeriodSelectorDefaults.today(lateEvening, "Asia/Tokyo"))
        assertEquals(Calendar.SUNDAY, MonarchPeriodSelectorDefaults.firstDayOfWeek(Locale.US))
        assertEquals(Calendar.MONDAY, MonarchPeriodSelectorDefaults.firstDayOfWeek(Locale.UK))
    }

    @Test
    fun theRulesOfTheCustomPicker() {
        val t = MonarchPeriodSelectorTokens
        val shown = MonarchPeriodRange(day(2026, 11, 9), Today)
        val picked = MonarchPeriodRange(day(2026, 1, 1), day(2026, 1, 2))
        assertEquals(
            MonarchPeriodValue(MonarchPeriodPreset.Custom, shown),
            t.next(MonarchPeriodValue(MonarchPeriodPreset.Last7Days), MonarchPeriodPreset.Custom, shown),
        )
        assertEquals(
            MonarchPeriodValue(MonarchPeriodPreset.Custom, picked),
            t.next(MonarchPeriodValue(MonarchPeriodPreset.Last7Days, picked), MonarchPeriodPreset.Custom, shown),
        )
        assertEquals(
            MonarchPeriodValue(MonarchPeriodPreset.Today),
            t.next(MonarchPeriodValue(MonarchPeriodPreset.Custom, picked), MonarchPeriodPreset.Today, shown),
        )
        assertFalse(t.accepts(null, null, null))
        assertTrue(t.accepts(Today, null, 1))
        assertTrue(t.accepts(Today - 29 * Day, Today, 30))
        assertFalse(t.accepts(Today - 30 * Day, Today, 30))
        assertTrue(t.tooLong(Today - 30 * Day, Today, 30))
        assertTrue(t.selectable(Today, null, Today))
        assertFalse(t.selectable(Today + Day, null, Today))
        assertFalse(t.selectable(Today - Day, Today, Today))
    }

    @Test
    fun theRangeIsWrittenForTheLocale() {
        val t = MonarchPeriodSelectorTokens
        val week = t.rangeText(MonarchPeriodRange(day(2026, 11, 9), Today), Locale.US)
        assertTrue(week.startsWith("Nov 9") && week.endsWith("15, 2026"), week)
        assertEquals("Nov 15, 2026", t.rangeText(MonarchPeriodRange(Today, Today), Locale.US))
        val french = t.rangeText(MonarchPeriodRange(Today, Today), Locale.FRANCE)
        assertTrue(french.contains("nov"), french)
    }

    @Test
    fun eachPresetIsASelectableSegmentAndPickingOneReportsIt() {
        var value by mutableStateOf(MonarchPeriodValue(MonarchPeriodPreset.Last7Days))
        var calls = 0
        rule.setMonarchContent {
            MonarchPeriodSelector(
                value = value,
                onValueChange = {
                    calls++
                    value = it
                },
                today = Today,
                locale = Locale.US,
            )
        }
        rule.onNodeWithText("Last 7 days").assertIsSelected()
        rule.onNodeWithText("Today").assertIsNotSelected().assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(MonarchPeriodValue(MonarchPeriodPreset.Today), value)
        rule.onNodeWithText("Today").assertIsSelected()
        rule.onNodeWithText("Today").performClick()
        assertEquals(1, calls, "picking the chosen preset again changes nothing")
        rule.onNode(hasContentDescription("Period")).assertExists()
    }

    @Test
    fun aDisabledSelectorCannotChange() {
        rule.setMonarchContent {
            MonarchPeriodSelector(
                value = MonarchPeriodValue(MonarchPeriodPreset.Today),
                onValueChange = { error("disabled") },
                enabled = false,
                today = Today,
            )
        }
        rule.onNodeWithText("Last 30 days").assertIsNotEnabled()
    }

    @Test
    fun theDatesShowUnderAPreset() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchPeriodSelector(
                value = MonarchPeriodValue(MonarchPeriodPreset.Last7Days),
                onValueChange = {},
                showRange = true,
                today = Today,
                locale = Locale.US,
            )
        }
        rule.onNodeWithText("15, 2026", substring = true).assertTextFullyVisible()
    }

    @Test
    fun choosingCustomOpensThePickerAndSavingReportsTheDates() {
        var value by mutableStateOf(MonarchPeriodValue(MonarchPeriodPreset.Last7Days))
        rule.setMonarchContent {
            MonarchPeriodSelector(
                value = value,
                onValueChange = { value = it },
                presets = listOf(MonarchPeriodPreset.Last7Days, MonarchPeriodPreset.Custom),
                today = Today,
                locale = Locale.US,
            )
        }
        rule.onNodeWithText("Custom").performClick()
        val week = MonarchPeriodRange(day(2026, 11, 9), Today)
        assertEquals(MonarchPeriodValue(MonarchPeriodPreset.Custom, week), value)
        rule.onNode(hasText("Save") and hasClickAction()).assertIsEnabled().performClick()
        rule.onNode(hasText("Save") and hasClickAction()).assertDoesNotExist()
        assertEquals(week, value.range)
        rule.onNode(hasContentDescription("Custom period", substring = true) and hasClickAction())
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNode(hasText("Cancel") and hasClickAction()).performClick()
        rule.onNode(hasText("Cancel") and hasClickAction()).assertDoesNotExist()
    }

    @Test
    fun aRangeLongerThanAllowedCannotBeSaved() {
        rule.setMonarchContent {
            MonarchPeriodSelector(
                value = MonarchPeriodValue(MonarchPeriodPreset.Last7Days),
                onValueChange = {},
                presets = listOf(MonarchPeriodPreset.Last30Days, MonarchPeriodPreset.Custom),
                today = Today,
                maxDays = 3,
            )
        }
        rule.onNodeWithText("Custom").performClick()
        rule.onNodeWithText("Choose 3 days or fewer").assertExists()
        rule.onNode(hasText("Save") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun theSelectVariantIsALabelledField() {
        rule.setMonarchContent {
            MonarchPeriodSelector(
                value = MonarchPeriodValue(MonarchPeriodPreset.ThisQuarter),
                onValueChange = {},
                variant = MonarchPeriodSelectorVariant.Select,
                today = Today,
            )
        }
        // The label above the field names it for TalkBack; the field shows the chosen period.
        rule.onNode(hasContentDescription("Period") and hasText("This quarter")).assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchPeriodSelector(
                value = MonarchPeriodValue(MonarchPeriodPreset.Custom),
                onValueChange = {},
                today = Today,
                labels = MonarchPeriodSelectorLabels(
                    label = "Période",
                    preset = { if (it == MonarchPeriodPreset.Today) "Aujourd’hui" else it.name },
                    customRange = "Période personnalisée",
                    chooseDates = "Choisir les dates",
                ),
            )
        }
        rule.onNodeWithText("Aujourd’hui").assertExists()
        rule.onNode(hasContentDescription("Période")).assertExists()
        rule.onNode(hasContentDescription("Période personnalisée, Choisir les dates")).assertExists()
    }

    @Test
    fun theFirstSegmentIsAtTheRightInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchPeriodSelector(
                value = MonarchPeriodValue(MonarchPeriodPreset.Today),
                onValueChange = {},
                presets = listOf(MonarchPeriodPreset.Today, MonarchPeriodPreset.Yesterday),
                today = Today,
            )
        }
        rule.onNodeWithText("Today").assertIsBefore(rule.onNodeWithText("Yesterday"), LayoutDirection.Rtl)
    }

    @Test
    fun theColoursComeFromTheThemeRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.textMuted, MonarchPeriodSelectorTokens.rangeColor(true, c), theme.id)
            assertEquals(c.textDisabled, MonarchPeriodSelectorTokens.rangeColor(false, c), theme.id)
            assertEquals(c.dangerStrong, MonarchPeriodSelectorTokens.errorColor(c), theme.id)
        }
    }

    @Test
    fun theSelectionFollowsTheHoistedValue() {
        rule.setMonarchContent {
            var value by remember { mutableStateOf(MonarchPeriodValue(MonarchPeriodPreset.Today)) }
            MonarchPeriodSelector(
                value = value,
                onValueChange = { value = it },
                presets = listOf(MonarchPeriodPreset.Today, MonarchPeriodPreset.Last30Days),
                today = Today,
                locale = Locale.US,
            )
        }
        rule.onNodeWithText("Last 30 days").performClick()
        rule.onNodeWithText("Last 30 days").assertIsSelected()
    }
}
