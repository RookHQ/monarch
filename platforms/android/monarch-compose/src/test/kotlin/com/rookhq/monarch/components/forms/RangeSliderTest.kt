package com.rookhq.monarch.components.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.components.forms.rangeslider.MonarchRangeSlider
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object RangeSliderFixtures : ComponentFixtures {
    override val id = "range-slider"
    override val fixtures = listOf(
        fixture("continuous") { MonarchRangeSlider(value = 0.2f..0.8f, onValueChange = {}) },
        fixture("stepped, thumbs together") {
            MonarchRangeSlider(value = 50f..50f, onValueChange = {}, valueRange = 0f..100f, steps = 9)
        },
        fixture("at both ends") { MonarchRangeSlider(value = 0f..1f, onValueChange = {}) },
        fixture("disabled") { MonarchRangeSlider(value = 0.3f..0.6f, onValueChange = {}, enabled = false) },
        fixture("with the range shown") {
            Column {
                MonarchRangeSlider(
                    value = 20f..150f,
                    onValueChange = {},
                    valueRange = 0f..200f,
                    label = "Price",
                    valueLabel = { "€${it.toInt()}" },
                )
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class RangeSliderTest {
    @get:Rule
    val rule = createComposeRule()

    private val thumbs: SemanticsNodeInteractionCollection
        get() = rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))

    @Test
    fun eachThumbExposesItsValueAndTheThumbsDoNotCross() {
        var range by mutableStateOf(20f..80f)
        rule.setMonarchContent {
            MonarchRangeSlider(value = range, onValueChange = { range = it }, valueRange = 0f..100f, steps = 9)
        }
        thumbs.assertCountEquals(2)
        val start = thumbs[0].fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
        val end = thumbs[1].fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
        assertEquals(20f, start.current)
        assertEquals(80f, end.current)
        thumbs[0].performSemanticsAction(SemanticsActions.SetProgress) { it(43f) }
        // Nine steps make ten intervals of 10, so 43 snaps to 40.
        assertEquals(40f..80f, range)
        thumbs[0].performSemanticsAction(SemanticsActions.SetProgress) { it(95f) }
        assertTrue(range.start <= 80f && range.endInclusive == 80f, "the start thumb stops at the end thumb: $range")
    }

    @Test
    fun aDisabledRangeSliderIsReadAsDisabled() {
        rule.setMonarchContent { MonarchRangeSlider(value = 0.2f..0.4f, onValueChange = {}, enabled = false) }
        thumbs[0].assertIsNotEnabled()
        thumbs[1].assertIsNotEnabled()
    }

    @Test
    fun theRangeSliderIs48dpHighAndTheLowerThumbIsAtTheStartInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchRangeSlider(value = 0.2f..0.8f, onValueChange = {}, modifier = Modifier.testTag("slider"))
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithTag("slider").assertHeightIsAtLeast(MonarchTouchTarget.Minimum)
            thumbs[0].assertIsBefore(thumbs[1], direction)
        }
    }

    @Test
    fun theThumbsAreNamedRangeStartAndRangeEnd() {
        rule.setMonarchContent { MonarchRangeSlider(value = 0.2f..0.8f, onValueChange = {}) }
        thumbs[0].assert(hasContentDescription("Range start"))
        thumbs[1].assert(hasContentDescription("Range end"))
    }

    @Test
    fun theRangeIsShownAsStartAndEndNextToTheLabel() {
        rule.setMonarchContent {
            MonarchRangeSlider(
                value = 20f..150f,
                onValueChange = {},
                valueRange = 0f..200f,
                label = "Price",
                valueLabel = { "${it.toInt()}" },
            )
        }
        rule.onNodeWithText("20 – 150", useUnmergedTree = true).assertExists()
    }
}
