package com.rookhq.monarch.components.intelligence

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.rookhq.monarch.components.intelligence.aithinkingindicator.MonarchAiThinkingIndicator
import com.rookhq.monarch.components.intelligence.aithinkingindicator.MonarchAiThinkingIndicatorTokens
import com.rookhq.monarch.components.intelligence.aithinkingindicator.MonarchAiThinkingIndicatorVariant
import com.rookhq.monarch.components.intelligence.aithinkingindicator.ThinkingIndicator
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchAiThinkingIndicatorFixtures : ComponentFixtures {
    override val id = "ai-thinking-indicator"
    override val fixtures = listOf(
        fixture("shimmer") { MonarchAiThinkingIndicator() },
        fixture("mark with the typing dots") {
            MonarchAiThinkingIndicator(variant = MonarchAiThinkingIndicatorVariant.Mark)
        },
        fixture("mark with an icon, elapsed") {
            MonarchAiThinkingIndicator(
                variant = MonarchAiThinkingIndicatorVariant.Mark,
                startedAt = System.currentTimeMillis() - 65_000L,
                icon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        },
        fixture("long label, not announced") {
            MonarchAiThinkingIndicator(
                label = "Searching your files, your calendar and the shared drive for last quarter’s numbers…",
                startedAt = System.currentTimeMillis() - 12_000L,
                announce = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiThinkingIndicatorTest {
    @get:Rule
    val rule = createComposeRule()

    /** A clock the test moves by hand. */
    private var now = START

    private fun show(
        label: String? = null,
        startedAt: Long? = null,
        showAfter: Long = MonarchAiThinkingIndicatorTokens.DEFAULT_SHOW_ELAPSED_AFTER_MILLIS,
        announce: Boolean = true,
        variant: MonarchAiThinkingIndicatorVariant = MonarchAiThinkingIndicatorVariant.Mark,
        fontScale: Float = 1f,
        elapsedLabel: ((Int) -> String)? = null,
    ) {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(fontScale = fontScale) {
            ThinkingIndicator(
                modifier = Modifier.testTag("indicator"),
                variant = variant,
                label = label,
                startedAt = startedAt,
                showElapsedAfterMillis = showAfter,
                announce = announce,
                icon = null,
                elapsedLabel = elapsedLabel,
                clock = { now },
            )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
    }

    /** Moves the test clock and the composition clock on by [millis]. */
    private fun advance(millis: Long) {
        now += millis
        rule.mainClock.advanceTimeBy(millis)
        rule.mainClock.advanceTimeByFrame()
    }

    private fun label() = rule.onNode(
        SemanticsMatcher("has the label and no live region") {
            it.config.getOrNull(SemanticsProperties.ContentDescription) != null &&
                it.config.getOrNull(SemanticsProperties.LiveRegion) == null
        },
        useUnmergedTree = true,
    )

    private fun liveRegion() = rule.onNode(
        SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
        useUnmergedTree = true,
    )

    @Test
    fun theLabelIsOneElementReadAsTheBuiltInString() {
        show()
        label().assertContentDescriptionEquals("Thinking…")
    }

    @Test
    fun theLabelCanBeReplaced() {
        show(label = "Searching your files…")
        label().assertContentDescriptionEquals("Searching your files…")
        rule.onNodeWithText("Searching your files…", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theLabelIsAnnouncedThroughAPoliteLiveRegion() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            ThinkingIndicator(
                modifier = Modifier, variant = MonarchAiThinkingIndicatorVariant.Mark, label = null, startedAt = null,
                showElapsedAfterMillis = 3_000L, announce = true, icon = null, elapsedLabel = null, clock = { now },
            )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        liveRegion().assertContentDescriptionEquals("Thinking…")
    }

    @Test
    fun theLiveRegionNeverHoldsTheElapsedTime() {
        show()
        advance(5_000L)
        liveRegion().assertContentDescriptionEquals("Thinking…")
        assertNull(liveRegion().stateDescription())
    }

    @Test
    fun anIndicatorThatIsNotAnnouncedHasNoLiveRegion() {
        show(announce = false)
        rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion), useUnmergedTree = true)
            .fetchSemanticsNodes().let { assertTrue(it.isEmpty()) }
    }

    @Test
    fun theElapsedTimeAppearsAfterThreeSecondsAndTicks() {
        show()
        assertNull(label().stateDescription())
        advance(2_000L)
        assertNull(label().stateDescription())
        advance(1_000L)
        assertEquals("3s", label().stateDescription())
        rule.onNodeWithText("3s", useUnmergedTree = true).assertExists()
        advance(62_000L)
        assertEquals("1m 5s", label().stateDescription())
    }

    @Test
    fun theElapsedTimeCountsFromStartedAt() {
        show(startedAt = START - 125_000L)
        assertEquals("2m 5s", label().stateDescription())
    }

    @Test
    fun theDelayCanBeChangedAndTheFormatReplaced() {
        show(startedAt = START - 10_000L, showAfter = Long.MAX_VALUE)
        assertNull(label().stateDescription())
    }

    @Test
    fun theFormatCanBeReplaced() {
        show(startedAt = START - 7_000L, showAfter = 0L, elapsedLabel = { "$it seconds" })
        assertEquals("7 seconds", label().stateDescription())
    }

    @Test
    fun aStartInTheFutureCountsAsZero() {
        assertEquals(0, MonarchAiThinkingIndicatorTokens.elapsedSeconds(startedAt = 10_000L, now = 4_000L))
        assertEquals(6, MonarchAiThinkingIndicatorTokens.elapsedSeconds(startedAt = 0L, now = 6_999L))
        assertEquals(1_000L, MonarchAiThinkingIndicatorTokens.untilNextSecond(startedAt = 0L, now = 5_000L))
        assertEquals(250L, MonarchAiThinkingIndicatorTokens.untilNextSecond(startedAt = 0L, now = 5_750L))
    }

    @Test
    fun theElapsedTimeIsTheNarrowUnitFormat() {
        assertEquals("0s", MonarchAiThinkingIndicatorTokens.elapsedText(0, Locale.US))
        assertEquals("59s", MonarchAiThinkingIndicatorTokens.elapsedText(59, Locale.US))
        assertEquals("1m", MonarchAiThinkingIndicatorTokens.elapsedText(60, Locale.US))
        assertEquals("1m 5s", MonarchAiThinkingIndicatorTokens.elapsedText(65, Locale.US))
        assertTrue(MonarchAiThinkingIndicatorTokens.showsElapsed(3, 3_000L))
        assertFalse(MonarchAiThinkingIndicatorTokens.showsElapsed(2, 3_000L))
    }

    @Test
    fun aChangedLabelIsAnnouncedAgain() {
        var text by mutableStateOf("Thinking…")
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            Column {
                ThinkingIndicator(
                    modifier = Modifier, variant = MonarchAiThinkingIndicatorVariant.Mark, label = text,
                    startedAt = null, showElapsedAfterMillis = 3_000L, announce = true, icon = null,
                    elapsedLabel = null, clock = { now },
                )
                Text("other")
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        text = "Reading the report…"
        rule.mainClock.advanceTimeByFrame()
        liveRegion().assertContentDescriptionEquals("Reading the report…")
    }

    @Test
    fun aLongLabelWrapsAtTheLargestFontScale() {
        show(
            label = "Searching your files, your calendar and the shared drive for last quarter’s numbers…",
            fontScale = LargestFontScale,
            variant = MonarchAiThinkingIndicatorVariant.Shimmer,
        )
        rule.onNodeWithText("Searching your files", substring = true, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun thePublicIndicatorShowsTheLabel() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchAiThinkingIndicator(modifier = Modifier.testTag("indicator")) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Thinking…", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theColoursUseTheMutedAndAssistantRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchAiThinkingIndicatorTokens.labelColor(colors), theme.id)
            assertEquals(colors.aiStrong, MonarchAiThinkingIndicatorTokens.shimmerColor(colors), theme.id)
            assertEquals(colors.aiStrong, MonarchAiThinkingIndicatorTokens.markColor(colors), theme.id)
            assertEquals(colors.textSubtle, MonarchAiThinkingIndicatorTokens.elapsedColor(colors), theme.id)
        }
    }

    @Test
    fun theShimmerSweepsFromTheStartEdgeInBothDirections() {
        val tokens = MonarchAiThinkingIndicatorTokens
        assertEquals(-100f, tokens.bandStart(0f, 100f, rightToLeft = false))
        assertEquals(100f, tokens.bandStart(1f, 100f, rightToLeft = false))
        assertEquals(100f, tokens.bandStart(0f, 100f, rightToLeft = true))
    }

    private companion object {
        const val START = 1_700_000_000_000L
    }
}
