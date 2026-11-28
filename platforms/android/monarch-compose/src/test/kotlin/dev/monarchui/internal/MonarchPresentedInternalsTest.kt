package dev.monarchui.internal

import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

/** The Monarch theming of the presented surfaces the pickers share: the date picker dialog and the list sheet. */
@RunWith(RobolectricTestRunner::class)
class MonarchPresentedInternalsTest {
    @get:Rule
    val rule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun theDatePickerTakesMonarchColoursInEveryTheme() {
        val pickers = mutableMapOf<String, DatePickerColors>()
        rule.setContent {
            AllThemes.forEach { theme ->
                MonarchTheme(tokens = theme) { pickers[theme.id] = monarchDatePickerColors(MonarchTheme.colors) }
            }
        }
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val picker = pickers.getValue(theme.id)
            assertEquals(colors.bgRaised, picker.containerColor, theme.id)
            assertEquals(colors.accentSolid, picker.selectedDayContainerColor, theme.id)
            assertEquals(colors.accentOnSolid, picker.selectedDayContentColor, theme.id)
            assertEquals(colors.accentStrong, picker.todayDateBorderColor, theme.id)
            assertEquals(colors.textMuted, picker.weekdayContentColor, theme.id)
            assertEquals(colors.textDisabled, picker.disabledDayContentColor, theme.id)
            assertEquals(colors.borderSubtle, picker.dividerColor, theme.id)
        }
    }

    @Test
    fun theDatePickerIsADialogWithMonarchButtons() {
        rule.setMonarchContent {
            MonarchDatePickerDialog(null, null, "Use date", "Cancel", onPick = {}, onClose = {})
        }
        rule.onNode(hasText("Use date") and hasAnyAncestor(isDialog())).assertTouchTargetAtLeast48dp(rule.density)
        rule.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theListSheetHasTheMonarchHandleWithA48dpTarget() {
        rule.setMonarchContent {
            MonarchSearchListSheet(
                title = "Workspace",
                query = "",
                onQueryChange = {},
                onDismiss = {},
                searchPlaceholder = "Search",
                searching = false,
                buttons = {},
            ) {}
        }
        rule.onNodeWithContentDescription("Drag handle").assertTouchTargetAtLeast48dp(rule.density)
    }
}
