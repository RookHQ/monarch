package com.rookhq.monarch.components.feedback

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgress
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgressVariant
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

/** The web ProgressCircle is the circular form of MonarchProgress, determinate or not. */
internal object ProgressCircleFixtures : ComponentFixtures {
    override val id = "progress-circle"
    override val fixtures = listOf(
        fixture("determinate, empty, part and complete") {
            Column {
                listOf(0f, 0.42f, 1f).forEach { value ->
                    MonarchProgress(progress = value, variant = MonarchProgressVariant.Circular, label = "Uploading")
                }
            }
        },
        fixture("indeterminate") { MonarchProgress(variant = MonarchProgressVariant.Circular) },
        fixture("out of range") {
            MonarchProgress(progress = 1.7f, variant = MonarchProgressVariant.Circular, label = "Storage used")
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class ProgressCircleTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aDeterminateRingReportsItsFractionAndLabel() {
        rule.setMonarchContent {
            MonarchProgress(
                modifier = Modifier.testTag("ring"),
                progress = 0.42f,
                variant = MonarchProgressVariant.Circular,
                label = "Uploading report.pdf",
            )
        }
        rule.onNodeWithTag("ring")
            .assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.42f, 0f..1f)))
            .assertContentDescriptionEquals("Uploading report.pdf")
    }

    @Test
    fun valuesOutsideTheRangeAreClamped() {
        rule.setMonarchContent {
            Column {
                MonarchProgress(
                    modifier = Modifier.testTag("over"),
                    progress = 1.7f,
                    variant = MonarchProgressVariant.Circular,
                )
                MonarchProgress(
                    modifier = Modifier.testTag("under"),
                    progress = -0.5f,
                    variant = MonarchProgressVariant.Circular,
                )
            }
        }
        rule.onNodeWithTag("over").assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(1f, 0f..1f)))
        rule.onNodeWithTag("under").assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(0f, 0f..1f)))
    }

    @Test
    fun aDeterminateRingWithoutALabelHasNoBuiltInName() {
        rule.setMonarchContent {
            MonarchProgress(
                modifier = Modifier.testTag("ring"),
                progress = 0.5f,
                variant = MonarchProgressVariant.Circular,
            )
        }
        rule.onNodeWithTag("ring").assert(hasContentDescription("Loading").not())
    }

    @Test
    fun anIndeterminateRingIsNamedLoading() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchProgress(modifier = Modifier.testTag("ring"), variant = MonarchProgressVariant.Circular)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("ring")
            .assert(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertContentDescriptionEquals("Loading")
    }
}
