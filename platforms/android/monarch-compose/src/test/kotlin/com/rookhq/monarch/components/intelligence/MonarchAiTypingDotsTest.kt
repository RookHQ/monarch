package com.rookhq.monarch.components.intelligence

import android.provider.Settings
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.rookhq.monarch.components.intelligence.aitypingdots.MonarchAiTypingDots
import com.rookhq.monarch.components.intelligence.aitypingdots.MonarchAiTypingDotsSize
import com.rookhq.monarch.components.intelligence.aitypingdots.MonarchAiTypingDotsTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object MonarchAiTypingDotsFixtures : ComponentFixtures {
    override val id = "ai-typing-dots"
    override val fixtures = listOf(
        fixture("medium") { MonarchAiTypingDots() },
        fixture("small") { MonarchAiTypingDots(size = MonarchAiTypingDotsSize.Sm) },
        fixture("not announced, custom label") { MonarchAiTypingDots(announce = false, label = "Writing a reply") },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiTypingDotsTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(announce: Boolean = true, label: String? = null, size: MonarchAiTypingDotsSize? = null) {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiTypingDots(
                modifier = Modifier.testTag("dots"),
                size = size ?: MonarchAiTypingDotsSize.Md,
                announce = announce,
                label = label,
            )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
    }

    @Test
    fun theDotsAreOneElementReadAsTheBuiltInLabel() {
        show()
        rule.onNodeWithTag("dots").assertContentDescriptionEquals("Assistant is typing")
    }

    @Test
    fun theLabelCanBeReplaced() {
        show(label = "Writing a reply")
        rule.onNodeWithTag("dots").assertContentDescriptionEquals("Writing a reply")
    }

    @Test
    fun anAnnouncedIndicatorIsAPoliteLiveRegionFilledAfterItAppears() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchAiTypingDots(modifier = Modifier.testTag("dots")) }
        val live = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        rule.onNodeWithTag("dots").assert(live)
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("dots").assert(live).assertContentDescriptionEquals("Assistant is typing")
    }

    @Test
    fun anIndicatorThatIsNotAnnouncedIsNoLiveRegion() {
        show(announce = false)
        rule.onNodeWithTag("dots")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
            .assertContentDescriptionEquals("Assistant is typing")
    }

    @Test
    fun theSmallDotsAreSmallerThanTheMediumOnes() {
        show(size = MonarchAiTypingDotsSize.Sm)
        val small = rule.onNodeWithTag("dots").fetchSemanticsNode().size
        assertTrue(MonarchAiTypingDotsTokens.dotSize(MonarchAiTypingDotsSize.Sm) <
            MonarchAiTypingDotsTokens.dotSize(MonarchAiTypingDotsSize.Md))
        assertTrue(small.width > 0 && small.height > 0)
    }

    @Test
    fun theDotsStandStillWhenAnimationsAreOff() {
        val resolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        try {
            // With an endless animation running, waiting for idle would never return.
            rule.setMonarchContent { MonarchAiTypingDots(modifier = Modifier.testTag("dots")) }
            rule.waitForIdle()
            rule.onNodeWithTag("dots").assertContentDescriptionEquals("Assistant is typing")
        } finally {
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }
    }

    @Test
    fun theDotsUseTheAssistantRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.aiStrong, MonarchAiTypingDotsTokens.activeColor(colors), theme.id)
            assertEquals(colors.textSubtle, MonarchAiTypingDotsTokens.restingColor(colors), theme.id)
        }
    }

    @Test
    fun theTimingFollowsTheWeb() {
        assertEquals(1200, MonarchAiTypingDotsTokens.CYCLE_MILLIS)
        assertEquals(200, MonarchAiTypingDotsTokens.DOT_DELAY_MILLIS)
    }
}
