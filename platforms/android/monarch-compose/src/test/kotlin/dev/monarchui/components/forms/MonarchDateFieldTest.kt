package dev.monarchui.components.forms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.forms.datefield.MonarchDateField
import dev.monarchui.components.forms.datefield.MonarchDateFieldTokens
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
internal class MonarchDateFieldTest {
    @get:Rule
    val rule = createComposeRule()

    /** 2026-11-17 at midnight UTC, as the platform picker gives it. */
    private val nov17 = 1_794_873_600_000L

    private val field get() = rule.onNodeWithContentDescription("Due date")

    @Test
    fun theDateIsShownInTheLocaleMediumFormatAtUtc() {
        assertEquals("Nov 17, 2026", MonarchDateFieldTokens.format(nov17, Locale.US))
        // One millisecond before midnight UTC is still the 16th, whatever the device zone.
        assertEquals("Nov 16, 2026", MonarchDateFieldTokens.format(nov17 - 1, Locale.US))
    }

    @Test
    fun theFieldIsAButtonNamedByItsLabelWithTheDateAsItsState() {
        rule.setMonarchContent { MonarchDateField(value = nov17, onValueChange = {}, label = "Due date") }
        field
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
        assertEquals("Nov 17, 2026", field.stateDescription())
        val openLabel = field.fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)?.label
        assertEquals("choose a date", openLabel)
    }

    @Test
    fun withoutADateThePlaceholderIsShownAndIsTheState() {
        rule.setMonarchContent { MonarchDateField(value = null, onValueChange = {}, label = "Due date") }
        rule.onNodeWithText("Choose a date", useUnmergedTree = true).assertExists()
        assertEquals("Choose a date", field.stateDescription())
    }

    @Test
    fun aRequiredFieldSaysSoInItsName() {
        rule.setMonarchContent {
            MonarchDateField(value = null, onValueChange = {}, label = "Due date", required = true)
        }
        rule.onNodeWithContentDescription("Due date, required").assertExists()
    }

    @Test
    fun tappingOpensThePlatformPickerAndConfirmingSetsTheDate() {
        var date by mutableStateOf<Long?>(nov17)
        rule.setMonarchContent { MonarchDateField(value = date, onValueChange = { date = it }, label = "Due date") }
        field.performClick()
        rule.onNodeWithText("OK").assertExists().performClick()
        rule.waitForIdle()
        assertEquals(nov17, date)
        rule.onNodeWithText("OK").assertDoesNotExist()
    }

    @Test
    fun cancellingKeepsTheDate() {
        var changes = 0
        rule.setMonarchContent { MonarchDateField(value = nov17, onValueChange = { changes++ }, label = "Due date") }
        field.performClick()
        rule.onNodeWithText("Cancel").performClick()
        rule.waitForIdle()
        assertEquals(0, changes)
    }

    @Test
    fun theErrorIsTheFieldsError() {
        rule.setMonarchContent {
            MonarchDateField(value = null, onValueChange = {}, label = "Due date", errorText = "Choose a due date.")
        }
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Choose a due date."))
    }

    @Test
    fun aClearableDateHasA48dpClearButtonThatClearsIt() {
        var date by mutableStateOf<Long?>(nov17)
        rule.setMonarchContent {
            MonarchDateField(value = date, onValueChange = { date = it }, label = "Due date", clearable = true)
        }
        rule.onNodeWithContentDescription("Clear date").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertNull(date)
        rule.onNodeWithContentDescription("Clear date").assertDoesNotExist()
    }

    @Test
    fun aDisabledFieldDoesNotOpenAndHasNoClearButton() {
        rule.setMonarchContent {
            MonarchDateField(value = nov17, onValueChange = {}, label = "Due date", clearable = true, enabled = false)
        }
        field.assertIsNotEnabled().performClick()
        rule.onNodeWithText("OK").assertDoesNotExist()
        rule.onNodeWithContentDescription("Clear date").assertDoesNotExist()
    }

    @Test
    fun rightToLeftPutsTheClearButtonBeforeTheCalendarOnTheLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchDateField(value = nov17, onValueChange = {}, label = "Due date", clearable = true)
        }
        rule.onNodeWithText("Nov 17, 2026", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithContentDescription("Clear date"), LayoutDirection.Rtl)
    }

    @Test
    fun theDateIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchDateField(value = nov17, onValueChange = {}, label = "Due date", clearable = true)
        }
        rule.onNodeWithText("Nov 17, 2026", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Due date", useUnmergedTree = true).assertTextFullyVisible()
    }
}
