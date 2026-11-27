package com.rookhq.monarch.sample

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.rookhq.monarch.tokens.MonarchTokens
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

/** Launches the real activity: appearance pickers, and navigation from family to page and back. */
@RunWith(RobolectricTestRunner::class)
// A screen large enough to show the pickers and every family without scrolling.
@Config(qualifiers = "w1280dp-h2400dp")
class CatalogueTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun settle() = rule.mainClock.advanceTimeBy(SETTLE_MS)

    @Test
    fun theThemeAndTheAccentCanBeSwitched() {
        rule.mainClock.autoAdvance = false
        settle()
        rule.onNodeWithText("Monarch catalogue").assertExists()
        rule.onNodeWithText(CatalogueTheme.Light.label).assertIsSelected()
        CatalogueTheme.entries.forEach { theme ->
            rule.onNodeWithText(theme.label).performClick()
            settle()
            rule.onNodeWithText(theme.label).assertIsSelected()
        }
        // The accent is a MonarchSelect: open it, choose, and the field shows the choice.
        val accent = MonarchTokens.Accents.neutral.label
        rule.mainClock.autoAdvance = true
        rule.onNodeWithText(MonarchTokens.Accents.rook.label).performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithText(accent).performClick()
        rule.waitForIdle()
        rule.onNodeWithText(accent).assertExists()
        rule.onNodeWithText(MonarchTokens.Accents.rook.label).assertDoesNotExist()
    }

    /** No page on this path animates forever, so the clock runs by itself here. */
    @Test
    fun aFamilyOpensItsPagesAndBackReturns() {
        rule.onNodeWithText("Actions").performClick()
        rule.onNodeWithText("Button").performClick()
        rule.onNodeWithText("Variants").assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Variants").assertDoesNotExist()
        rule.onNodeWithText("Button").assertExists()
        // The system back gesture goes up one more level instead of closing the app.
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithText("Families").assertExists()
    }

    private companion object {
        const val SETTLE_MS = 500L
    }
}
