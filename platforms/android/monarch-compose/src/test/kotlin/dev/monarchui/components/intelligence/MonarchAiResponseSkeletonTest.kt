package dev.monarchui.components.intelligence

import android.provider.Settings
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import dev.monarchui.components.intelligence.airesponseskeleton.MonarchAiResponseSkeleton
import dev.monarchui.components.intelligence.airesponseskeleton.MonarchAiResponseSkeletonShape
import dev.monarchui.components.intelligence.airesponseskeleton.MonarchAiResponseSkeletonTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object MonarchAiResponseSkeletonFixtures : ComponentFixtures {
    override val id = "ai-response-skeleton"
    override val fixtures = listOf(
        fixture("text") { MonarchAiResponseSkeleton() },
        fixture("one text line") { MonarchAiResponseSkeleton(lines = 1) },
        fixture("cards") { MonarchAiResponseSkeleton(shape = MonarchAiResponseSkeletonShape.Cards, lines = 2) },
        fixture("table") { MonarchAiResponseSkeleton(shape = MonarchAiResponseSkeletonShape.Table, lines = 3) },
        fixture("table, many columns, custom label") {
            MonarchAiResponseSkeleton(
                shape = MonarchAiResponseSkeletonShape.Table,
                lines = 2,
                columns = 6,
                label = "Building the comparison table",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiResponseSkeletonTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(
        shape: MonarchAiResponseSkeletonShape = MonarchAiResponseSkeletonShape.Text,
        label: String? = null,
    ) {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiResponseSkeleton(modifier = Modifier.testTag("skeleton"), shape = shape, label = label)
        }
        rule.mainClock.advanceTimeByFrame()
    }

    @Test
    fun thePlaceholderIsOneElementReadAsTheBuiltInLabelAndBusy() {
        show()
        rule.onNodeWithTag("skeleton")
            .assertContentDescriptionEquals("Generating response…")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo.Indeterminate,
                ),
            )
    }

    @Test
    fun theLabelCanBeReplaced() {
        show(shape = MonarchAiResponseSkeletonShape.Cards, label = "Writing the summary")
        rule.onNodeWithTag("skeleton").assertContentDescriptionEquals("Writing the summary")
    }

    @Test
    fun theBarsAreNotRead() {
        show(shape = MonarchAiResponseSkeletonShape.Table)
        val node = rule.onNodeWithTag("skeleton").fetchSemanticsNode()
        assertTrue(node.children.isEmpty(), "The bars must not be separate TalkBack elements")
    }

    @Test
    fun theShapeDecidesTheHeight() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            androidx.compose.foundation.layout.Column {
                MonarchAiResponseSkeleton(modifier = Modifier.testTag("one"), lines = 1)
                MonarchAiResponseSkeleton(
                    modifier = Modifier.testTag("cards"),
                    shape = MonarchAiResponseSkeletonShape.Cards,
                    lines = 1,
                )
            }
        }
        rule.mainClock.advanceTimeByFrame()
        val line = rule.onNodeWithTag("one").fetchSemanticsNode().size.height
        val card = rule.onNodeWithTag("cards").fetchSemanticsNode().size.height
        assertTrue(line > 0 && card > line, "A card ($card) is taller than one line ($line)")
    }

    @Test
    fun theBarsStandStillWhenAnimationsAreOff() {
        val resolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        try {
            // With the shimmer running, waiting for idle would never return.
            rule.setMonarchContent { MonarchAiResponseSkeleton(modifier = Modifier.testTag("skeleton")) }
            rule.waitForIdle()
            rule.onNodeWithTag("skeleton").assertContentDescriptionEquals("Generating response…")
        } finally {
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }
    }

    @Test
    fun countsBelowOneDrawOneAndDefaultsFollowTheWeb() {
        val tokens = MonarchAiResponseSkeletonTokens
        assertEquals(3, tokens.count(MonarchAiResponseSkeletonShape.Text, null))
        assertEquals(3, tokens.count(MonarchAiResponseSkeletonShape.Cards, null))
        assertEquals(4, tokens.count(MonarchAiResponseSkeletonShape.Table, null))
        assertEquals(1, tokens.count(MonarchAiResponseSkeletonShape.Text, 0))
        assertEquals(1, tokens.count(MonarchAiResponseSkeletonShape.Table, -3))
        assertEquals(1, tokens.columns(0))
        assertEquals(5, tokens.columns(5))
    }

    @Test
    fun aSingleLineIsFullWidthAndTheLastOfSeveralIsShorter() {
        val tokens = MonarchAiResponseSkeletonTokens
        assertEquals(1f, tokens.lineWidth(0, 1))
        assertEquals(0.62f, tokens.lineWidth(2, 3))
        assertTrue(tokens.lineWidth(1, 3) > tokens.lineWidth(2, 3))
        for (row in 0 until 5) {
            for (column in 0 until 6) {
                val width = tokens.cellWidth(row, column)
                assertTrue(width > 0f && width <= 1f)
            }
        }
    }

    @Test
    fun cardsUseTheSurfaceAndTheSubtleBorderInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val card = MonarchAiResponseSkeletonTokens.cardColors(colors)
            assertEquals(colors.bgSurface, card.container, theme.id)
            assertEquals(colors.borderSubtle, card.border, theme.id)
        }
    }
}
