package com.rookhq.monarch.components.feedback

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingState
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingStateTokens
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingStateVariant
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

internal object MonarchLoadingStateFixtures : ComponentFixtures {
    override val id = "loading-state"
    override val fixtures = MonarchLoadingStateVariant.entries.map { variant ->
        fixture(variant.name) { MonarchLoadingState(label = "Loading tickets", variant = variant, rows = 2) }
    } + listOf(
        fixture("long label") {
            MonarchLoadingState(
                label = "Loading the invoices of every workspace you belong to",
                variant = MonarchLoadingStateVariant.Spinner,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchLoadingStateTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theLabelIsAnnouncedOnceThroughAPoliteLiveRegion() {
        rule.setMonarchContent { MonarchLoadingState(label = "Loading tickets") }
        rule.waitForIdle()
        rule.onNode(polite and hasContentDescription("Loading tickets")).assertExists()
        rule.onAllNodesWithContentDescription("Loading tickets").assertCountEquals(1)
    }

    @Test
    fun theLiveRegionIsEmptyInTheFirstFrame() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchLoadingState(label = "Loading tickets") }
        rule.onNode(polite and hasContentDescription("Loading tickets")).assertDoesNotExist()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.onNode(polite and hasContentDescription("Loading tickets")).assertExists()
    }

    @Test
    fun theSpinnerShowsTheLabelAndAnnouncesIt() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchLoadingState(label = "Loading reports", variant = MonarchLoadingStateVariant.Spinner)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Loading reports", useUnmergedTree = true).assertExists()
        rule.onNode(polite and hasContentDescription("Loading reports")).assertExists()
        rule.onAllNodesWithContentDescription("Loading", substring = true).assertCountEquals(1)
    }

    @Test
    fun theRowsAreDrawnForTheCountAndNoneForANegativeOne() {
        rule.setMonarchContent {
            MonarchLoadingState(
                label = "Loading",
                modifier = Modifier.testTag("four"),
                variant = MonarchLoadingStateVariant.Cards,
            )
        }
        val four = rule.onNodeWithTag("four").getUnclippedBoundsInRoot()
        assertTrue(four.bottom - four.top >= 96.dp * 4)
        assertEquals(0, MonarchLoadingStateTokens.rowCount(-2))
        assertEquals(3, MonarchLoadingStateTokens.rowCount(3))
    }

    @Test
    fun theLineWidthsVaryAsOnTheWeb() {
        assertEquals(0.55f to 0.30f, MonarchLoadingStateTokens.lineWidths(0))
        assertEquals(0.72f to 0.41f, MonarchLoadingStateTokens.lineWidths(1))
    }

    @Test
    fun theSpinnerLabelWrapsAtTheLargestFontSize() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchLoadingState(
                label = "Loading the invoices of every workspace",
                variant = MonarchLoadingStateVariant.Spinner,
            )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Loading the invoices", substring = true, useUnmergedTree = true).assertTextFullyVisible()
    }
}
