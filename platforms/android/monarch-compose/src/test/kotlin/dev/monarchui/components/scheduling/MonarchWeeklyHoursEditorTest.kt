package dev.monarchui.components.scheduling

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursEditor
import dev.monarchui.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursEditorFormats
import dev.monarchui.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursEditorLabels
import dev.monarchui.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursEditorTokens as Tokens
import dev.monarchui.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursError
import dev.monarchui.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursInterval as Interval
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** ICU puts a narrow no-break space before AM and PM in en-US. */
private fun clock(text: String) = text.replace(" AM", "\u202FAM").replace(" PM", "\u202FPM")

private val Monday = listOf(Interval(1, 540, 1020))
private val Split = listOf(Interval(1, 540, 750), Interval(1, 810, 1050))

internal object MonarchWeeklyHoursEditorFixtures : ComponentFixtures {
    override val id = "weekly-hours-editor"
    override val fixtures = listOf(
        // The editor is meant for a scrolling screen; days below the test window are not measured.
        fixture("every day off") {
            MonarchWeeklyHoursEditor(
                value = emptyList(),
                onValueChange = {},
                modifier = Modifier.verticalScroll(rememberScrollState()),
                is24Hour = false,
            )
        },
        fixture("two intervals on Monday") {
            MonarchWeeklyHoursEditor(
                value = Split,
                onValueChange = {},
                modifier = Modifier.verticalScroll(rememberScrollState()),
                is24Hour = true,
            )
        },
        fixture("overlap error, at the limit") {
            MonarchWeeklyHoursEditor(
                value = listOf(Interval(1, 540, 720), Interval(1, 660, 900)),
                onValueChange = {},
                modifier = Modifier.verticalScroll(rememberScrollState()),
                is24Hour = false,
                maxIntervals = 2,
            )
        },
        fixture("disabled") {
            MonarchWeeklyHoursEditor(
                value = Monday,
                onValueChange = {},
                modifier = Modifier.verticalScroll(rememberScrollState()),
                is24Hour = false,
                enabled = false,
            )
        },
        fixture("read-only") {
            MonarchWeeklyHoursEditor(
                value = Split + Interval(2, 0, 1440),
                onValueChange = {},
                modifier = Modifier.verticalScroll(rememberScrollState()),
                is24Hour = false,
                readOnly = true,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchWeeklyHoursEditorTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(
        start: List<Interval>,
        onChange: (List<Interval>) -> Unit = {},
        labels: MonarchWeeklyHoursEditorLabels = MonarchWeeklyHoursEditorLabels(),
    ) {
        rule.setMonarchContent {
            var value by remember { mutableStateOf(start) }
            MonarchWeeklyHoursEditor(
                value = value,
                onValueChange = {
                    value = it
                    onChange(it)
                },
                modifier = Modifier.verticalScroll(rememberScrollState()),
                is24Hour = false,
                labels = labels,
            )
        }
    }

    @Test
    fun eachDayIsASwitchNamedByTheWeekday() {
        show(Monday)
        rule.onNodeWithText("Weekly hours").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNode(hasText("Monday") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsOn()
            .assertTouchTargetAtLeast48dp(rule.density)
        rule.onNode(hasText("Tuesday", substring = true) and hasText("Unavailable", substring = true))
            .assertIsOff()
    }

    @Test
    fun switchingADayOnAddsTheDefaultAndOffAndOnRestoresIt() {
        var result: List<Interval> = emptyList()
        show(Split, { result = it })
        rule.onNodeWithText("Tuesday").performScrollTo().performClick()
        assertEquals(Split + Interval(2, 540, 1020), result)
        rule.onNodeWithText("Monday").performScrollTo().performClick()
        assertEquals(listOf(Interval(2, 540, 1020)), result)
        rule.onNodeWithText("Monday").performScrollTo().performClick()
        assertEquals(Split + Interval(2, 540, 1020), result)
    }

    @Test
    fun addAppendsAnHourAfterTheLastAndRemoveDropsOne() {
        var result: List<Interval> = emptyList()
        show(Monday, { result = it })
        rule.onNodeWithContentDescription("Add interval to Monday").assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(Monday + Interval(1, 1080, 1140), result)
        rule.onNodeWithContentDescription("Remove Monday interval 1").performScrollTo().performClick()
        assertEquals(listOf(Interval(1, 1080, 1140)), result)
    }

    @Test
    fun choosingATimeChangesTheInterval() {
        var result: List<Interval> = emptyList()
        show(Monday, { result = it })
        rule.onNodeWithText(clock("9:00 AM")).performClick()
        rule.onNodeWithText(clock("9:15 AM")).performScrollTo().performClick()
        assertEquals(listOf(Interval(1, 555, 1020)), result)
    }

    @Test
    fun theAddButtonIsHiddenAtTheLimit() {
        rule.setMonarchContent {
            MonarchWeeklyHoursEditor(value = Split, onValueChange = {}, is24Hour = false, maxIntervals = 2,
                modifier = Modifier.verticalScroll(rememberScrollState()))
        }
        rule.onNodeWithContentDescription("Add interval to Monday").assertDoesNotExist()
    }

    @Test
    fun copyToAllCopiesTheDayAndAnnouncesIt() {
        var result: List<Interval> = emptyList()
        show(Split, { result = it })
        rule.onNodeWithContentDescription("Copy to all days from Monday").performScrollTo().performClick()
        assertEquals(14, result.size)
        assertEquals((1..7).toList(), result.map { it.day }.distinct())
        rule.onNodeWithContentDescription("Monday hours copied to every day.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun anOverlapShowsAPoliteMessageUnderTheDay() {
        show(listOf(Interval(1, 540, 720), Interval(1, 660, 900)))
        rule.onNodeWithText("Intervals must not overlap").performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun readOnlyShowsTheHoursAsTextWithMidnight() {
        rule.setMonarchContent {
            MonarchWeeklyHoursEditor(
                value = Split + Interval(2, 0, 1440),
                onValueChange = {},
                modifier = Modifier.verticalScroll(rememberScrollState()),
                is24Hour = false,
                readOnly = true,
            )
        }
        rule.onNodeWithText(clock("9:00 AM – 12:30 PM, 1:30 PM – 5:30 PM")).assertExists()
        rule.onNodeWithText(clock("12:00 AM – Midnight")).assertExists()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)).assertDoesNotExist()
    }

    @Test
    fun disabledEditorCannotChange() {
        rule.setMonarchContent {
            MonarchWeeklyHoursEditor(value = Monday, onValueChange = { error("changed") }, is24Hour = false,
                enabled = false, modifier = Modifier.verticalScroll(rememberScrollState()))
        }
        rule.onNodeWithText("Monday").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Copy to all days from Monday").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Remove Monday interval 1").assertIsNotEnabled()
    }

    private fun assertFirst(first: String, second: String) {
        var locale = ""
        rule.setMonarchContent {
            locale = LocalConfiguration.current.locales[0].toString()
            MonarchWeeklyHoursEditor(value = emptyList(), onValueChange = {}, is24Hour = false,
                modifier = Modifier.verticalScroll(rememberScrollState()))
        }
        val a = rule.onNodeWithText(first).fetchSemanticsNode().positionInRoot.y
        val b = rule.onNodeWithText(second).fetchSemanticsNode().positionInRoot.y
        assertTrue(a < b, "$first at $a, $second at $b, locale $locale")
    }

    @Test
    fun withoutFirstDayOfWeekAUsLocaleStartsOnSunday() {
        assertFirst("Sunday", "Monday")
    }

    @Test
    @Config(qualifiers = "de-rDE")
    fun withoutFirstDayOfWeekAGermanLocaleStartsOnMonday() {
        assertFirst("Montag", "Sonntag")
    }

    @Test
    fun theWeekStartsOnTheChosenDay() {
        rule.setMonarchContent {
            MonarchWeeklyHoursEditor(value = emptyList(), onValueChange = {}, is24Hour = false,
                firstDayOfWeek = Calendar.SUNDAY, modifier = Modifier.verticalScroll(rememberScrollState()))
        }
        val sunday = rule.onNodeWithText("Sunday").fetchSemanticsNode().boundsInRoot.top
        val monday = rule.onNodeWithText("Monday").fetchSemanticsNode().boundsInRoot.top
        assertTrue(sunday < monday)
    }

    @Test
    fun theStartMenuComesFirstRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchWeeklyHoursEditor(value = Monday, onValueChange = {}, is24Hour = false,
                modifier = Modifier.verticalScroll(rememberScrollState()))
        }
        rule.onNodeWithText(clock("9:00 AM"))
            .assertIsBefore(rule.onNodeWithText(clock("5:00 PM")), LayoutDirection.Rtl)
    }

    @Test
    fun theDayNameIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchWeeklyHoursEditor(value = Monday, onValueChange = {}, is24Hour = false,
                modifier = Modifier.verticalScroll(rememberScrollState()))
        }
        rule.onNodeWithText("Monday").assertTextFullyVisible()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        show(
            Monday,
            labels = MonarchWeeklyHoursEditorLabels(
                title = "Opening hours",
                unavailable = "Closed",
                copyToAllItem = { "Use $it for the whole week" },
            ),
        )
        rule.onNodeWithText("Opening hours").assertExists()
        rule.onNode(hasText("Tuesday", substring = true) and hasText("Closed", substring = true)).assertExists()
        rule.onNodeWithContentDescription("Use Monday for the whole week").assertExists()
    }

    @Test
    fun validateFollowsTheWebRules() {
        assertEquals(emptyMap(), Interval.validate(Split))
        assertEquals(mapOf(1 to MonarchWeeklyHoursError.Range), Interval.validate(listOf(Interval(1, -15, 60))))
        assertEquals(mapOf(2 to MonarchWeeklyHoursError.Order), Interval.validate(listOf(Interval(2, 600, 600))))
        assertEquals(
            mapOf(3 to MonarchWeeklyHoursError.Overlap),
            Interval.validate(listOf(Interval(3, 600, 720), Interval(3, 540, 660))),
        )
        assertEquals(emptyMap(), Interval.validate(listOf(Interval(4, 540, 600), Interval(4, 600, 660))))
    }

    @Test
    fun tokensComputeOrderOptionsAndTheNextInterval() {
        assertEquals(listOf(7, 1, 2, 3, 4, 5, 6), Tokens.order(Calendar.SUNDAY))
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), Tokens.order(Calendar.MONDAY))
        assertEquals(15, Tokens.step(0))
        assertEquals(60, Tokens.step(90))
        assertEquals(96, Tokens.startOptions(15, 540).size)
        assertEquals(1440, Tokens.endOptions(15, 1020).last())
        assertTrue(545 in Tokens.startOptions(15, 545))
        assertEquals(Interval(1, 1380, 1440), Tokens.nextInterval(listOf(Interval(1, 1200, 1400)), 1, 60))
        assertEquals(Interval(5, 540, 1020), Tokens.nextInterval(emptyList(), 5, 15))
        assertEquals(listOf(2, 3, 3), Tokens.withDay(listOf(Interval(3, 0, 60), Interval(3, 60, 120)), 2, Monday)
            .map { it.day })
    }

    @Test
    fun formatsFollowTheLocaleAndTheClock() {
        val twelve = MonarchWeeklyHoursEditorFormats(Locale.US, is24Hour = false, midnight = "Midnight")
        val twentyFour = MonarchWeeklyHoursEditorFormats(Locale.UK, is24Hour = true, midnight = "Midnight")
        assertEquals("Monday", twelve.day(1))
        assertEquals("Sunday", twelve.day(7))
        assertEquals(clock("1:30 PM"), twelve.time(810))
        assertEquals("Midnight", twelve.time(1440))
        assertEquals("13:30", twentyFour.time(810))
        assertEquals("24:00", twentyFour.time(1440))
    }

    @Test
    fun theErrorTakesTheDangerTextColourInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.dangerStrong, Tokens.errorColor(colors))
            assertEquals(colors.textMuted, Tokens.noteColor(colors))
        }
    }
}
