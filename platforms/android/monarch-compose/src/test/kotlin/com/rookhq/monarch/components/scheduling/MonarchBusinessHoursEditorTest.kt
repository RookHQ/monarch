package com.rookhq.monarch.components.scheduling

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.scheduling.businesshourseditor.BusinessHoursEditorRows
import com.rookhq.monarch.components.scheduling.businesshourseditor.MonarchBusinessHoursEditor
import com.rookhq.monarch.components.scheduling.businesshourseditor.MonarchBusinessHoursEditorLabels
import com.rookhq.monarch.components.scheduling.businesshourseditor.MonarchBusinessHoursEditorTokens as Tokens
import com.rookhq.monarch.components.scheduling.businesshourseditor.MonarchBusinessHoursErrors
import com.rookhq.monarch.components.scheduling.businesshourseditor.MonarchBusinessHoursException as Holiday
import com.rookhq.monarch.components.scheduling.businesshourseditor.MonarchBusinessHoursExceptionError as ExceptionError
import com.rookhq.monarch.components.scheduling.businesshourseditor.MonarchBusinessHoursInterval
import com.rookhq.monarch.components.scheduling.businesshourseditor.MonarchBusinessHoursValue
import com.rookhq.monarch.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursError
import com.rookhq.monarch.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursInterval
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** A field node whose TalkBack error is [message]. */
private fun hasError(message: String) = SemanticsMatcher.expectValue(SemanticsProperties.Error, message)

/** ICU puts a narrow no-break space before AM and PM in en-US. */
private fun clock(text: String) = text.replace(" AM", " AM").replace(" PM", " PM")

private const val NewYear = 1_798_761_600_000L // 2027-01-01
private const val Christmas = 1_798_156_800_000L // 2026-12-25
private const val ChristmasEve = 1_798_070_400_000L // 2026-12-24

private val Weekdays = (1..5).map { MonarchWeeklyHoursInterval(it, 540, 1020) }

private val Support = MonarchBusinessHoursValue(
    timeZone = "Europe/London",
    hours = Weekdays,
    exceptions = listOf(
        Holiday(Christmas, "Christmas Day"),
        Holiday(ChristmasEve, "Christmas Eve", listOf(MonarchBusinessHoursInterval(600, 960))),
    ),
    name = "Support hours",
)

internal object MonarchBusinessHoursEditorFixtures : ComponentFixtures {
    override val id = "business-hours-editor"
    override val fixtures = listOf(
        // The editor is meant for a scrolling screen; sections below the test window are not measured.
        fixture("a new calendar") {
            MonarchBusinessHoursEditor(
                value = MonarchBusinessHoursValue(timeZone = "UTC"),
                onValueChange = {},
                modifier = Modifier.verticalScroll(rememberScrollState()),
                is24Hour = false,
            )
        },
        fixture("holidays and special hours") {
            MonarchBusinessHoursEditor(
                value = Support,
                onValueChange = {},
                modifier = Modifier.verticalScroll(rememberScrollState()),
                is24Hour = false,
            )
        },
        fixture("every error shown") {
            MonarchBusinessHoursEditor(
                value = MonarchBusinessHoursValue(
                    timeZone = "UTC",
                    exceptions = listOf(Holiday(null, ""), Holiday(Christmas, "A"), Holiday(Christmas, "B")),
                    effectiveFrom = NewYear,
                    effectiveTo = Christmas,
                ),
                onValueChange = {},
                modifier = Modifier.verticalScroll(rememberScrollState()),
                showEffectiveDates = true,
                requireName = true,
                requireOpen = true,
                showErrors = true,
                is24Hour = true,
            )
        },
        fixture("effective dates and default") {
            MonarchBusinessHoursEditor(
                value = Support.copy(effectiveFrom = Christmas, default = true),
                onValueChange = {},
                modifier = Modifier.verticalScroll(rememberScrollState()),
                showName = false,
                showDefault = true,
                showEffectiveDates = true,
                is24Hour = false,
            )
        },
        fixture("disabled") {
            MonarchBusinessHoursEditor(
                value = Support,
                onValueChange = {},
                modifier = Modifier.verticalScroll(rememberScrollState()),
                enabled = false,
                is24Hour = false,
            )
        },
        fixture("read-only") {
            MonarchBusinessHoursEditor(
                value = Support,
                onValueChange = {},
                modifier = Modifier.verticalScroll(rememberScrollState()),
                readOnly = true,
                is24Hour = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchBusinessHoursEditorTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(
        start: MonarchBusinessHoursValue,
        onChange: (MonarchBusinessHoursValue) -> Unit = {},
        showErrors: Boolean = false,
        labels: MonarchBusinessHoursEditorLabels = MonarchBusinessHoursEditorLabels(),
    ) {
        rule.setMonarchContent {
            var value by remember { mutableStateOf(start) }
            MonarchBusinessHoursEditor(
                value = value,
                onValueChange = {
                    value = it
                    onChange(it)
                },
                modifier = Modifier.verticalScroll(rememberScrollState()),
                showDefault = true,
                showEffectiveDates = true,
                requireName = true,
                showErrors = showErrors,
                is24Hour = false,
                labels = labels,
            )
        }
    }

    @Test
    fun typingANameChangesTheValueAndAnEmptyNameShowsItsErrorOnceTouched() {
        var result: MonarchBusinessHoursValue? = null
        show(MonarchBusinessHoursValue(timeZone = "UTC"), { result = it })
        rule.onNode(hasError("Enter a name.")).assertDoesNotExist()
        // The label sits above the box; the field is named by it.
        rule.onNodeWithContentDescription("Name, required").performTextInput("Sales")
        assertEquals("Sales", result?.name)
    }

    @Test
    fun showErrorsShowsEveryProblemPolitely() {
        show(
            MonarchBusinessHoursValue(
                timeZone = "UTC",
                exceptions = listOf(Holiday(Christmas, "A"), Holiday(Christmas, "B")),
                effectiveFrom = NewYear,
                effectiveTo = Christmas,
            ),
            showErrors = true,
        )
        // A field's error is part of the field's TalkBack element and is announced by its polite live region.
        rule.onNode(hasContentDescription("Name, required") and hasError("Enter a name.")).assertExists()
        rule.onNodeWithContentDescription("Enter a name.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNode(hasError("The end date must be on or after the start date.")).assertExists()
        rule.onNodeWithText("This date is listed twice.").performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun aRowsErrorWaitsUntilTheRowIsChanged() {
        show(MonarchBusinessHoursValue(timeZone = "UTC", exceptions = listOf(Holiday(null, ""))))
        rule.onNodeWithText("Choose a date.").assertDoesNotExist()
        rule.onNodeWithContentDescription("Name of date 1").performScrollTo().performTextInput("Founders Day")
        rule.onNodeWithText("Choose a date.").performScrollTo().assertExists()
    }

    @Test
    fun addDateAppendsAnEmptyRow() {
        var result: MonarchBusinessHoursValue? = null
        show(Support, { result = it })
        rule.onNodeWithText("Add date").performScrollTo().assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(Support.exceptions + Holiday(null, ""), result?.exceptions)
    }

    @Test
    fun removeIsNamedByTheDateAndDropsIt() {
        var result: MonarchBusinessHoursValue? = null
        show(Support, { result = it })
        rule.onNodeWithContentDescription("Remove Christmas Day").performScrollTo()
            .assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf(Support.exceptions[1]), result?.exceptions)
    }

    @Test
    fun openingADateAtOtherTimesGivesTheDefaultHours() {
        var result: MonarchBusinessHoursValue? = null
        show(Support, { result = it })
        rule.onNodeWithText("Closed all day").performScrollTo().performClick()
        rule.onAllNodesWithText("Open at other times")
            // The menu row, not the trigger of date 2 that already shows it.
            .filterToOne(!SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList))
            .performClick()
        assertEquals(listOf(MonarchBusinessHoursInterval(600, 960)), result?.exceptions?.first()?.intervals)
    }

    @Test
    fun theDefaultIsACheckbox() {
        var result: MonarchBusinessHoursValue? = null
        show(Support, { result = it })
        rule.onNodeWithText("Default").performScrollTo().performClick()
        assertEquals(true, result?.default)
        rule.onNodeWithText("Default").assertIsOn()
    }

    @Test
    fun readOnlyShowsTextWithoutControls() {
        rule.setMonarchContent {
            MonarchBusinessHoursEditor(
                value = Support,
                onValueChange = { error("read-only") },
                modifier = Modifier.verticalScroll(rememberScrollState()),
                readOnly = true,
                is24Hour = false,
            )
        }
        rule.onNodeWithText("Add date").assertDoesNotExist()
        rule.onNodeWithContentDescription("Remove Christmas Day").assertDoesNotExist()
        rule.onNodeWithText("12/25/2026").performScrollTo().assertExists()
        rule.onAllNodesWithText("Europe/London").onFirst().assertExists()
    }

    @Test
    fun disabledControlsCannotChange() {
        rule.setMonarchContent {
            MonarchBusinessHoursEditor(
                value = Support,
                onValueChange = { error("disabled") },
                modifier = Modifier.verticalScroll(rememberScrollState()),
                enabled = false,
                is24Hour = false,
            )
        }
        rule.onNodeWithText("Add date").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithContentDescription("Remove Christmas Day").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun theOpeningTimeComesFirstRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchBusinessHoursEditor(
                value = Support.copy(hours = emptyList()),
                onValueChange = {},
                modifier = Modifier.verticalScroll(rememberScrollState()),
                is24Hour = false,
            )
        }
        rule.onNodeWithText(clock("10:00 AM")).performScrollTo()
            .assertIsBefore(rule.onNodeWithText(clock("4:00 PM")), LayoutDirection.Rtl)
    }

    @Test
    fun theHeadingIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchBusinessHoursEditor(
                value = Support,
                onValueChange = {},
                modifier = Modifier.verticalScroll(rememberScrollState()),
                is24Hour = false,
            )
        }
        rule.onNodeWithText("Holidays and exceptions").performScrollTo().assertTextFullyVisible()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        show(
            Support,
            labels = MonarchBusinessHoursEditorLabels(
                addException = "Neues Datum",
                removeException = { position, _ -> "Datum $position entfernen" },
            ),
        )
        rule.onNodeWithText("Neues Datum").performScrollTo().assertExists()
        rule.onNodeWithContentDescription("Datum 2 entfernen").performScrollTo().assertExists()
    }

    @Test
    fun validateFollowsTheWebRules() {
        assertTrue(Support.validate().valid)
        val broken = MonarchBusinessHoursValue(
            timeZone = "UTC",
            hours = listOf(MonarchWeeklyHoursInterval(1, 600, 540)),
            exceptions = listOf(
                Holiday(null, "No date"),
                Holiday(Christmas, " "),
                Holiday(NewYear, "A"),
                Holiday(NewYear, "B"),
                Holiday(ChristmasEve, "Eve", listOf(MonarchBusinessHoursInterval(900, 600))),
            ),
            effectiveFrom = NewYear,
            effectiveTo = Christmas,
        )
        assertEquals(
            MonarchBusinessHoursErrors(
                nameRequired = true,
                hours = mapOf(1 to MonarchWeeklyHoursError.Order),
                noOpenHours = true,
                exceptions = mapOf(
                    0 to ExceptionError.Date,
                    1 to ExceptionError.Name,
                    3 to ExceptionError.Duplicate,
                    4 to ExceptionError.Hours,
                ),
                effectiveOrder = true,
            ),
            broken.validate(requireName = true, requireOpen = true),
        )
        assertTrue(MonarchBusinessHoursValue(timeZone = "UTC").validate(requireOpen = true).noOpenHours)
        assertFalse(MonarchBusinessHoursValue(timeZone = "UTC").validate().noOpenHours)
    }

    @Test
    fun tokensEditTheValueAndKeepRowIds() {
        val closed = Holiday(Christmas, "Day")
        assertEquals(listOf(Tokens.SpecialHoursDefault), Tokens.withSpecialHours(closed, true).intervals)
        assertEquals(emptyList(), Tokens.withSpecialHours(Support.exceptions[1], false).intervals)
        assertEquals(Support.exceptions[1].intervals, Tokens.withSpecialHours(Support.exceptions[1], true).intervals)
        assertEquals(listOf(Support.exceptions[1]), Tokens.withoutException(Support, 0).exceptions)
        assertEquals(closed, Tokens.withException(Support, 1, closed).exceptions[1])
        assertEquals(listOf(0, 1, 2), Tokens.reconcile(emptyList(), 3, 0))
        assertEquals(listOf(0, 2), Tokens.reconcile(listOf(0, 1, 2), 2, 3, removed = 1))
        assertEquals(listOf(0, 2, 3), Tokens.reconcile(listOf(0, 2), 3, 3))
        val rows = BusinessHoursEditorRows()
        assertEquals(listOf(0, 1, 2), rows.sync(3))
        rows.remove(0)
        assertEquals(listOf(1, 2), rows.sync(2))
        assertEquals(listOf(1, 2, 3), rows.sync(3))
    }

    @Test
    fun datesAreFormattedInUtc() {
        assertEquals("01/01/2027", Tokens.date(NewYear, Locale.UK))
        assertEquals("25/12/2026", Tokens.date(Christmas, Locale.UK))
    }

    @Test
    fun colorsComeFromRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.dangerStrong, Tokens.errorColor(colors))
            assertEquals(colors.textMuted, Tokens.noteColor(colors))
        }
    }
}
