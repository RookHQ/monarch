package dev.monarchui.components.feedback

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import dev.monarchui.components.feedback.spinner.MonarchSpinner
import dev.monarchui.components.feedback.spinner.MonarchSpinnerSize
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

/** The web Spinner: a 2dp ring with one open quarter. */
internal object SpinnerFixtures : ComponentFixtures {
    override val id = "spinner"
    override val fixtures = listOf(
        fixture("default label") { MonarchSpinner() },
        fixture("own label") { MonarchSpinner(size = MonarchSpinnerSize.Lg, label = "Loading invoices") },
        fixture("small") { MonarchSpinner(size = MonarchSpinnerSize.Sm) },
    )
}

@RunWith(RobolectricTestRunner::class)
class SpinnerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theSpinnerIsAnIndeterminateProgressBarNamedLoading() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchSpinner(modifier = Modifier.testTag("spinner"))
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("spinner")
            .assertContentDescriptionEquals("Loading")
            .assert(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
    }

    @Test
    fun theLabelReplacesTheBuiltInString() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchSpinner(modifier = Modifier.testTag("spinner"), label = "Loading invoices")
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("spinner").assertContentDescriptionEquals("Loading invoices")
    }

    @Test
    fun theSizesAre14And16And24dp() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            androidx.compose.foundation.layout.Column {
                MonarchSpinnerSize.entries.forEach { MonarchSpinner(modifier = Modifier.testTag(it.name), size = it) }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("Sm").assertWidthIsEqualTo(14.dp).assertHeightIsEqualTo(14.dp)
        rule.onNodeWithTag("Md").assertWidthIsEqualTo(16.dp)
        rule.onNodeWithTag("Lg").assertWidthIsEqualTo(24.dp)
    }

    @Test
    fun anEmptyLabelHidesTheSpinnerFromTalkBack() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchSpinner(label = "") }
        rule.mainClock.advanceTimeByFrame()
        rule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
    }
}
