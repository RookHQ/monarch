package com.rookhq.monarch.components.intelligence

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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rookhq.monarch.components.billing.usagemeter.MonarchUsageLevel
import com.rookhq.monarch.components.billing.usagemeter.MonarchUsageMeterLabels
import com.rookhq.monarch.components.intelligence.aiusagemeter.MonarchAiUsageMeter
import com.rookhq.monarch.components.intelligence.aiusagemeter.MonarchAiUsageMeterLabels
import com.rookhq.monarch.components.intelligence.aiusagemeter.MonarchAiUsageMeterTokens
import com.rookhq.monarch.components.intelligence.aiusagemeter.MonarchAiUsageScope
import com.rookhq.monarch.components.intelligence.aiusagemeter.MonarchAiUsageUnit
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

internal object MonarchAiUsageMeterFixtures : ComponentFixtures {
    override val id = "ai-usage-meter"
    override val fixtures = listOf(
        fixture("turn, conversation and month") {
            MonarchAiUsageMeter(
                scopes = listOf(
                    MonarchAiUsageScope("This turn", used = 1_850.0),
                    MonarchAiUsageScope("Conversation", used = 42_000.0, limit = 50_000.0),
                ),
            )
        },
        fixture("over the limit, credits, titled") {
            MonarchAiUsageMeter(
                scopes = listOf(
                    MonarchAiUsageScope(
                        "This month",
                        used = 1_020.0,
                        limit = 1_000.0,
                        unit = MonarchAiUsageUnit.Credits,
                    ),
                ),
                title = "AI usage",
                period = "November",
            )
        },
        fixture("loading") { MonarchAiUsageMeter(scopes = emptyList(), loading = true) },
        fixture("error with retry") {
            MonarchAiUsageMeter(scopes = emptyList(), error = "Usage could not be loaded.", onRetry = {})
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiUsageMeterTest {
    @get:Rule
    val rule = createComposeRule()

    private val liveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    @Test
    fun theMeterIsAGroupNamedAiUsageWithoutATitle() {
        rule.setMonarchContent {
            MonarchAiUsageMeter(
                scopes = listOf(MonarchAiUsageScope("This turn", used = 10.0)),
                modifier = Modifier.testTag("meter"),
            )
        }
        rule.onNodeWithTag("meter")
            .assertContentDescriptionEquals("AI usage")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
    }

    @Test
    fun theTitleNamesTheGroupAndTheBuiltInNameCanBeReplaced() {
        rule.setMonarchContent {
            androidx.compose.foundation.layout.Column {
                MonarchAiUsageMeter(
                    scopes = emptyList(),
                    modifier = Modifier.testTag("titled"),
                    title = "Assistant quota",
                )
                MonarchAiUsageMeter(
                    scopes = emptyList(),
                    modifier = Modifier.testTag("renamed"),
                    labels = MonarchAiUsageMeterLabels(label = "Usage of the assistant"),
                )
            }
        }
        rule.onNodeWithTag("titled").assertContentDescriptionEquals("Assistant quota")
        rule.onNodeWithTag("renamed").assertContentDescriptionEquals("Usage of the assistant")
    }

    @Test
    fun theUnitIsWrittenAndSingularOnlyForExactlyOne() {
        rule.setMonarchContent {
            androidx.compose.foundation.layout.Column {
                MonarchAiUsageMeter(scopes = listOf(MonarchAiUsageScope("Turn", used = 1.0)))
                MonarchAiUsageMeter(
                    scopes = listOf(
                        MonarchAiUsageScope("Month", used = 3.0, limit = 10.0, unit = MonarchAiUsageUnit.Credits),
                    ),
                )
            }
        }
        rule.onNodeWithText("1 token", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("3 of 10 credits", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theUnitCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiUsageMeter(
                scopes = listOf(MonarchAiUsageScope("Turn", used = 5.0)),
                labels = MonarchAiUsageMeterLabels(
                    unit = { unit, _ -> if (unit == MonarchAiUsageUnit.Tokens) "jetons" else "crédits" },
                ),
            )
        }
        rule.onNodeWithText("5 jetons", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theLevelIsWrittenSoItDoesNotDependOnColour() {
        rule.setMonarchContent {
            MonarchAiUsageMeter(
                scopes = listOf(
                    MonarchAiUsageScope("Month", used = 1_200.0, limit = 1_000.0, unit = MonarchAiUsageUnit.Credits),
                ),
            )
        }
        rule.onNodeWithText("Over the limit", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun theFirstStateIsNotAnnouncedButCrossingAThresholdIs() {
        var used by mutableStateOf(500.0)
        rule.setMonarchContent {
            MonarchAiUsageMeter(scopes = listOf(MonarchAiUsageScope("This month", used = used, limit = 1_000.0)))
        }
        rule.onNode(liveRegion, useUnmergedTree = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
        used = 850.0
        rule.waitForIdle()
        rule.onNode(
            liveRegion,
            useUnmergedTree = true,
        ).assertContentDescriptionEquals("This month: 85% of the limit used")
        used = 1_100.0
        rule.waitForIdle()
        rule.onNode(liveRegion, useUnmergedTree = true).assertContentDescriptionEquals("This month: over the limit")
    }

    @Test
    fun aMeterThatStartsNearTheLimitIsNotAnnouncedOnAppearance() {
        rule.setMonarchContent {
            MonarchAiUsageMeter(scopes = listOf(MonarchAiUsageScope("This month", used = 950.0, limit = 1_000.0)))
        }
        rule.onNode(liveRegion, useUnmergedTree = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }

    @Test
    fun theAnnouncementsCanBeReplaced() {
        var used by mutableStateOf(0.0)
        rule.setMonarchContent {
            MonarchAiUsageMeter(
                scopes = listOf(MonarchAiUsageScope("Month", used = used, limit = 10.0)),
                labels = MonarchAiUsageMeterLabels(overLimit = { "$it is used up" }),
            )
        }
        used = 11.0
        rule.waitForIdle()
        rule.onNode(liveRegion, useUnmergedTree = true).assertContentDescriptionEquals("Month is used up")
    }

    @Test
    fun theErrorShowsARetryButtonThatCallsBack() {
        var retries = 0
        rule.setMonarchContent {
            MonarchAiUsageMeter(
                scopes = emptyList(),
                error = "Usage could not be loaded.",
                onRetry = { retries++ },
                labels = MonarchAiUsageMeterLabels(meter = MonarchUsageMeterLabels(retry = "Load again")),
            )
        }
        rule.onNodeWithText("Load again").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun scopesWrapAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiUsageMeter(
                scopes = listOf(
                    MonarchAiUsageScope("Image generation this billing period", used = 32.0, limit = 100.0),
                ),
            )
        }
        rule.onNodeWithText("Image generation this billing period", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theRulesFollowTheSharedMeter() {
        val tokens = MonarchAiUsageMeterTokens
        val near = MonarchAiUsageScope("A", used = 80.0, limit = 100.0)
        val over = MonarchAiUsageScope("B", used = 101.0, limit = 100.0)
        val atLimit = MonarchAiUsageScope("C", used = 100.0, limit = 100.0)
        val unlimited = MonarchAiUsageScope("D", used = -5.0)
        assertEquals(MonarchUsageLevel.Warning, tokens.level(near, 0.8))
        assertEquals(MonarchUsageLevel.Over, tokens.level(over, 0.8))
        assertEquals(MonarchUsageLevel.Warning, tokens.level(atLimit, 0.8))
        assertEquals(MonarchUsageLevel.Unlimited, tokens.level(unlimited, 0.8))
        assertEquals(0.0, tokens.used(unlimited))
        assertNull(tokens.limit(MonarchAiUsageScope("E", used = 1.0, limit = 0.0)))
        assertSame(over, tokens.worst(listOf(near, over, atLimit), 0.8))
        assertSame(near, tokens.worst(listOf(near, atLimit), 0.8))
        assertNull(tokens.worst(listOf(unlimited), 0.8))
    }

    @Test
    fun theUnitAgreesWithTheLimitOrTheAmount() {
        val tokens = MonarchAiUsageMeterTokens
        assertEquals(1.0, tokens.unitCount(MonarchAiUsageScope("A", used = 0.0, limit = 1.0)))
        assertEquals(3.0, tokens.unitCount(MonarchAiUsageScope("B", used = 3.0)))
        assertEquals(1, tokens.quantity(1.0))
        assertEquals(0, tokens.quantity(0.0))
        assertEquals(2, tokens.quantity(1.5))
        assertEquals(42_000, tokens.quantity(42_000.0))
    }
}
