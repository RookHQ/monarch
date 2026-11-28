package com.rookhq.monarch.components.files

import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.files.columnsblock.MonarchColumnsBlock
import com.rookhq.monarch.components.files.columnsblock.MonarchColumnsBlockTokens
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal object MonarchColumnsBlockFixtures : ComponentFixtures {
    override val id = "columns-block"
    override val fixtures = listOf(
        fixture("two columns at a ratio") {
            MonarchColumnsBlock(ratio = 0.35f) {
                column { MonarchText(text = "Before", style = MonarchTextStyle.BodyStrong) }
                column { MonarchText(text = "Sign-ups doubled after the launch.") }
            }
        },
        fixture("three equal columns") {
            MonarchColumnsBlock {
                column { MonarchText(text = "Plan") }
                column { MonarchText(text = "Build") }
                column { MonarchText(text = "Ship") }
            }
        },
        fixture("one column") { MonarchColumnsBlock { column { MonarchText(text = "Only one column") } } },
        fixture("side by side, wider than the window") {
            MonarchColumnsBlock(modifier = Modifier.requiredWidth(WIDE.dp)) {
                column { MonarchText(text = "Left column with a long sentence that wraps.") }
                column { MonarchText(text = "Right") }
            }
        },
    )

    const val WIDE = 600
}

@RunWith(RobolectricTestRunner::class)
class MonarchColumnsBlockTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theRatioIsKeptBetweenATenthAndFourFifthsAndANonNumberIsAHalf() {
        assertEquals(0.2f, MonarchColumnsBlockTokens.clampRatio(0.05f))
        assertEquals(0.8f, MonarchColumnsBlockTokens.clampRatio(1.5f))
        assertEquals(0.35f, MonarchColumnsBlockTokens.clampRatio(0.35f))
        assertEquals(0.5f, MonarchColumnsBlockTokens.clampRatio(Float.NaN))
        assertEquals(0.5f, MonarchColumnsBlockTokens.clampRatio(Float.POSITIVE_INFINITY))
    }

    @Test
    fun twoColumnsShareTheWidthAtTheRatioAndOthersAreEqual() {
        assertEquals(listOf(95, 285), MonarchColumnsBlockTokens.widths(2, 400, 20, 0.25f))
        assertEquals(listOf(120, 120, 120), MonarchColumnsBlockTokens.widths(3, 400, 20, 0.25f))
        assertEquals(listOf(400), MonarchColumnsBlockTokens.widths(1, 400, 20, 0.25f))
        assertEquals(emptyList(), MonarchColumnsBlockTokens.widths(0, 400, 20, 0.5f))
    }

    @Test
    fun columnsStackBelow480dpAndTheThresholdGrowsWithTheFontSize() {
        assertTrue(MonarchColumnsBlockTokens.stacks(479.dp, 1f))
        assertFalse(MonarchColumnsBlockTokens.stacks(480.dp, 1f))
        assertFalse(MonarchColumnsBlockTokens.stacks(600.dp, 0.85f))
        assertTrue(MonarchColumnsBlockTokens.stacks(600.dp, LargestFontScale))
    }

    @Test
    fun onANarrowScreenTheColumnsStackInOrder() {
        rule.setMonarchContent {
            MonarchColumnsBlock {
                column { MonarchText(text = "First") }
                column { MonarchText(text = "Second") }
            }
        }
        val first = rule.onNodeWithText("First").getUnclippedBoundsInRoot()
        val second = rule.onNodeWithText("Second").getUnclippedBoundsInRoot()
        assertTrue(first.bottom <= second.top)
        assertEquals(first.left, second.left)
    }

    @Test
    fun onAWideBlockTheFirstColumnIsAtTheStartInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchColumnsBlock(modifier = Modifier.requiredWidth(MonarchColumnsBlockFixtures.WIDE.dp)) {
                column { MonarchText(text = "First") }
                column { MonarchText(text = "Second") }
            }
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("First").assertIsBefore(rule.onNodeWithText("Second"), direction)
            val first = rule.onNodeWithText("First").getUnclippedBoundsInRoot()
            val second = rule.onNodeWithText("Second").getUnclippedBoundsInRoot()
            assertEquals(first.top, second.top, "top-aligned")
        }
    }

    @Test
    fun eachColumnIsATraversalGroupSoItIsReadBeforeTheNext() {
        rule.setMonarchContent {
            MonarchColumnsBlock(modifier = Modifier.testTag("block")) {
                column { MonarchText(text = "First") }
                column { MonarchText(text = "Second") }
            }
        }
        val columns = rule.onNodeWithTag("block").fetchSemanticsNode().children
        assertEquals(2, columns.size)
        columns.forEach { assertEquals(true, it.config.getOrNull(SemanticsProperties.IsTraversalGroup)) }
    }

    @Test
    fun atTheLargestFontSizeTextIsNotCut() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchColumnsBlock {
                column { MonarchText(text = "A long sentence in the first column") }
                column { MonarchText(text = "Second") }
            }
        }
        rule.onNodeWithText("A long sentence in the first column").assertTextFullyVisible()
    }
}
