package com.rookhq.monarch.components.datadisplay

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.datadisplay.truncatedtext.MonarchTruncatedText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.textLayout
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val LongText =
    "The customer asked for the invoice to be split across two cost centers, with the travel costs on the " +
        "second one and the license fees on the first. Finance agreed on the condition that both parts " +
        "reference the original purchase order number, and that the split is noted on the account."

private const val MediumText =
    "Renewal is due on the first of the month. The account owner gets a reminder a week before it."

internal object MonarchTruncatedTextFixtures : ComponentFixtures {
    override val id = "truncated-text"
    override val fixtures = listOf(
        fixture("cut, three lines") { MonarchTruncatedText(text = LongText) },
        fixture("expanded") { MonarchTruncatedText(text = MediumText, expanded = true, onExpandedChange = {}) },
        fixture("one line") { MonarchTruncatedText(text = LongText, maxLines = 1) },
        fixture("not expandable") { MonarchTruncatedText(text = LongText, expandable = false) },
        fixture("fits") { MonarchTruncatedText(text = "Short text.") },
        fixture("caption") { MonarchTruncatedText(text = LongText, style = MonarchTextStyle.Caption) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchTruncatedTextTest {
    @get:Rule
    val rule = createComposeRule()

    private val narrow = Modifier.width(200.dp)

    @Test
    fun cutTextKeepsTheWholeTextForTalkBackAndOffersShowMore() {
        rule.setMonarchContent { MonarchTruncatedText(text = LongText, modifier = narrow) }
        val text = rule.onNodeWithText(LongText)
        text.assertTextEquals(LongText)
        val layout = text.textLayout()
        assertEquals(3, layout.lineCount)
        assertTrue(layout.isLineEllipsized(2))
        rule.onNodeWithText("Show more").assertHasClickAction().assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun showMoreExpandsAndShowLessCutsAgain() {
        rule.setMonarchContent { MonarchTruncatedText(text = LongText, modifier = narrow) }
        rule.onNodeWithText("Show more").performClick()
        rule.onNodeWithText(LongText).assertTextFullyVisible()
        rule.onNodeWithText("Show less").performClick()
        assertTrue(rule.onNodeWithText(LongText).textLayout().isLineEllipsized(2))
        rule.onNodeWithText("Show more").assertExists()
    }

    @Test
    fun textThatFitsHasNoButton() {
        rule.setMonarchContent { MonarchTruncatedText(text = "Short text.", modifier = narrow) }
        rule.onNodeWithText("Show more").assertDoesNotExist()
    }

    @Test
    fun anEmptyTextHasNoButton() {
        rule.setMonarchContent { MonarchTruncatedText(text = "", modifier = narrow) }
        rule.onNodeWithText("Show more").assertDoesNotExist()
    }

    @Test
    fun aTextThatIsNotExpandableHasNoButton() {
        rule.setMonarchContent { MonarchTruncatedText(text = LongText, modifier = narrow, expandable = false) }
        assertTrue(rule.onNodeWithText(LongText).textLayout().isLineEllipsized(2))
        rule.onNodeWithText("Show more").assertDoesNotExist()
    }

    @Test
    fun maxLinesBelowOneCountsAsOne() {
        rule.setMonarchContent { MonarchTruncatedText(text = LongText, modifier = narrow, maxLines = 0) }
        assertEquals(1, rule.onNodeWithText(LongText).textLayout().lineCount)
    }

    @Test
    fun theHoistedFormHandsTheNewStateToTheApp() {
        var expanded by mutableStateOf(false)
        val changes = mutableListOf<Boolean>()
        rule.setMonarchContent {
            MonarchTruncatedText(
                text = LongText,
                expanded = expanded,
                onExpandedChange = {
                    changes += it
                    expanded = it
                },
                modifier = narrow,
            )
        }
        rule.onNodeWithText("Show more").performClick()
        rule.onNodeWithText("Show less").performClick()
        assertEquals(listOf(true, false), changes)
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchTruncatedText(text = LongText, modifier = narrow, showMoreLabel = "Read all", showLessLabel = "Hide")
        }
        rule.onNodeWithText("Read all").performClick()
        rule.onNodeWithText("Hide").assertExists()
    }

    @Test
    fun theExpandedTextIsNotCutOffAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchTruncatedText(text = MediumText, expanded = true, onExpandedChange = {}, maxLines = 1)
        }
        rule.onNodeWithText(MediumText).assertTextFullyVisible()
        rule.onNodeWithText("Show less").assertTouchTargetAtLeast48dp(rule.density)
    }
}
