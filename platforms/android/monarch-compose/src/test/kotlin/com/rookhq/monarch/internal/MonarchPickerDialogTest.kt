package com.rookhq.monarch.internal

import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimePickerColors
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
class MonarchPickerDialogTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theRangeDialogHasMonarchButtonsAndConfirmWaitsForItsCondition() {
        var confirmed = 0
        var dismissed = 0
        rule.setMonarchContent {
            val state = rememberDateRangePickerState(initialDisplayMode = DisplayMode.Input)
            MonarchPickerDialog(
                onDismissRequest = { dismissed++ },
                confirmLabel = "Save",
                onConfirm = { confirmed++ },
                cancelLabel = "Cancel",
                confirmEnabled = state.selectedStartDateMillis != null,
            ) {
                Text("Up to 31 days")
                MonarchDateRangePicker(state = state, title = { Text("Launch") })
            }
        }
        val save = rule.onNode(hasText("Save") and hasAnyAncestor(isDialog()))
        save.assertTouchTargetAtLeast48dp(rule.density).assertIsNotEnabled()
        rule.onNode(hasText("Launch") and hasAnyAncestor(isDialog())).assertExists()
        rule.onNode(hasText("Up to 31 days") and hasAnyAncestor(isDialog())).assertExists()
        rule.onNode(hasText("Cancel") and hasAnyAncestor(isDialog()))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, dismissed)
        assertEquals(0, confirmed)
    }

    @Test
    fun theTimeDialogConfirmsAndCancelsWithMonarchButtons() {
        var confirmed = 0
        rule.setMonarchContent {
            val state = rememberTimePickerState(initialHour = 9, initialMinute = 30, is24Hour = true)
            MonarchTimePickerDialog(
                onDismissRequest = {},
                title = { Text("Start time") },
                confirmLabel = "Use time",
                onConfirm = { confirmed++ },
                cancelLabel = "Cancel",
            ) { MonarchTimeInput(state) }
        }
        rule.onNode(hasText("Start time") and hasAnyAncestor(isDialog())).assertExists()
        rule.onNode(hasText("Use time") and hasAnyAncestor(isDialog()))
            .assertTouchTargetAtLeast48dp(rule.density)
            .assertIsEnabled()
            .performClick()
        assertEquals(1, confirmed)
    }

    @Test
    fun timePickerColoursComeFromTheThemeInEveryTheme() {
        val pickers = mutableMapOf<String, TimePickerColors>()
        rule.setContent {
            AllThemes.forEach { theme ->
                MonarchTheme(tokens = theme) { pickers[theme.id] = monarchTimePickerColors(MonarchTheme.colors) }
            }
        }
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val picker = pickers.getValue(theme.id)
            assertEquals(colors.bgRaised, picker.containerColor, theme.id)
            assertEquals(colors.accentSolid, picker.selectorColor, theme.id)
            assertEquals(colors.accentOnSolid, picker.clockDialSelectedContentColor, theme.id)
            assertEquals(colors.accentStrong, picker.timeSelectorSelectedContentColor, theme.id)
            assertEquals(colors.textMuted, picker.periodSelectorUnselectedContentColor, theme.id)
        }
    }
}
