package com.rookhq.monarch.components.billing

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.billing.usagemeter.MonarchUsageLevel
import com.rookhq.monarch.components.billing.usagemeter.MonarchUsageMeter
import com.rookhq.monarch.components.billing.usagemeter.MonarchUsageMeterItem
import com.rookhq.monarch.components.billing.usagemeter.MonarchUsageMeterLabels
import com.rookhq.monarch.components.billing.usagemeter.MonarchUsageMeterTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

private val Usage = listOf(
    MonarchUsageMeterItem("Requests", used = 4200.0, limit = 10000.0),
    MonarchUsageMeterItem("Storage", used = 42.0, limit = 50.0, unit = "GB"),
    MonarchUsageMeterItem("Minutes", used = 1300.0, limit = 1000.0, description = "Calls and recordings"),
    MonarchUsageMeterItem("Seats", used = 4.0),
)

internal object MonarchUsageMeterFixtures : ComponentFixtures {
    override val id = "usage-meter"
    override val fixtures = listOf(
        fixture("every level, with a header") {
            MonarchUsageMeter(items = Usage, title = "Usage", period = "This month", resetsAtMillis = 1_796_083_200_000)
        },
        fixture("no header") { MonarchUsageMeter(items = Usage.take(1)) },
        fixture("announcing thresholds") { MonarchUsageMeter(items = Usage, announceThresholds = true) },
        fixture("loading") { MonarchUsageMeter(items = Usage, title = "Usage", loading = true) },
        fixture("error with retry") {
            MonarchUsageMeter(items = Usage, error = "Usage could not be loaded.", onRetry = {})
        },
        fixture("empty") { MonarchUsageMeter(items = emptyList(), title = "Usage") },
        fixture("long label in a narrow width") {
            MonarchUsageMeter(
                items = listOf(MonarchUsageMeterItem("Outbound transactional email messages", 12.0, 100.0)),
                modifier = Modifier.width(180.dp),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchUsageMeterTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun onlyItemsThatRiseToAThresholdAreReportedAndNotOnTheFirstShow() {
        val storage = MonarchUsageMeterItem(label = "Storage", used = 50.0, limit = 100.0)
        val seats = MonarchUsageMeterItem(label = "Seats", used = 9.0, limit = 10.0)
        val tokens = MonarchUsageMeterTokens
        assertEquals(emptyList(), tokens.crossings(emptyMap(), listOf(storage, seats), 0.8))
        val before = mapOf("Storage" to MonarchUsageLevel.Normal, "Seats" to MonarchUsageLevel.Warning)
        val after = listOf(
            MonarchUsageMeterItem(label = "Storage", used = 85.0, limit = 100.0),
            MonarchUsageMeterItem(label = "Seats", used = 11.0, limit = 10.0),
        )
        assertEquals(
            listOf("Storage" to MonarchUsageLevel.Warning, "Seats" to MonarchUsageLevel.Over),
            tokens.crossings(before, after, 0.8).map { (item, level) -> item.label to level },
        )
        val falling = mapOf("Storage" to MonarchUsageLevel.Over)
        assertEquals(emptyList(), tokens.crossings(falling, listOf(storage), 0.8))
    }

    @Test
    fun theMeterAnnouncesTheItemsThatCrossedAThresholdOnce() {
        var items by mutableStateOf(listOf(MonarchUsageMeterItem(label = "Storage", used = 50.0, limit = 100.0)))
        rule.setMonarchContent { MonarchUsageMeter(items = items, announceThresholds = true) }
        val announced = SemanticsMatcher("announces") { node ->
            node.config.getOrNull(SemanticsProperties.LiveRegion) == LiveRegionMode.Polite &&
                node.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
                    ?.startsWith("Storage: Near the limit") == true
        }
        assertEquals(0, rule.onAllNodes(announced).fetchSemanticsNodes().size)
        rule.runOnUiThread { items = listOf(MonarchUsageMeterItem(label = "Storage", used = 90.0, limit = 100.0)) }
        rule.waitForIdle()
        assertEquals(1, rule.onAllNodes(announced).fetchSemanticsNodes().size)
    }

    @Test
    fun eachItemIsOneElementWithItsFiguresAndLevel() {
        rule.setMonarchContent { MonarchUsageMeter(items = Usage) }
        rule.onNodeWithText("Requests").assert(hasText("4,200 of 10,000")).assert(hasText("42% used"))
        rule.onNodeWithText("Storage").assert(hasText("42 of 50 GB")).assert(hasText("Near the limit · 84% used"))
        rule.onNodeWithText("Minutes").assert(hasText("Over the limit · 130% used"))
            .assert(hasText("Calls and recordings"))
        rule.onNodeWithText("Seats").assert(hasText("4"))
    }

    @Test
    fun theTitleIsAHeadingAndThePeriodAndResetAreWritten() {
        rule.setMonarchContent {
            MonarchUsageMeter(items = Usage, title = "Usage", period = "This month", resetsAtMillis = 0L)
        }
        rule.onNodeWithText("Usage").assert(isHeading())
        rule.onNodeWithText("This month · Resets", substring = true).assertExists()
    }

    @Test
    fun levelsFollowTheLimitAndTheWarningShare() {
        assertEquals(MonarchUsageLevel.Normal, MonarchUsageMeterTokens.level(10.0, 100.0, 0.8))
        assertEquals(MonarchUsageLevel.Warning, MonarchUsageMeterTokens.level(80.0, 100.0, 0.8))
        assertEquals(MonarchUsageLevel.Warning, MonarchUsageMeterTokens.level(100.0, 100.0, 0.8))
        assertEquals(MonarchUsageLevel.Over, MonarchUsageMeterTokens.level(100.5, 100.0, 0.8))
        assertEquals(MonarchUsageLevel.Unlimited, MonarchUsageMeterTokens.level(5.0, 0.0, 0.8))
        assertEquals(MonarchUsageLevel.Unlimited, MonarchUsageMeterTokens.level(5.0, -1.0, 0.8))
        assertEquals(MonarchUsageLevel.Unlimited, MonarchUsageMeterTokens.level(5.0, Double.NaN, 0.8))
        assertEquals(1f, MonarchUsageMeterTokens.fraction(130.0, 100.0))
        assertEquals(0.5f, MonarchUsageMeterTokens.fraction(50.0, 100.0))
    }

    @Test
    fun theErrorWinsOverLoadingAndOffersARetry() {
        var retries = 0
        rule.setMonarchContent {
            MonarchUsageMeter(
                items = Usage,
                loading = true,
                error = "Usage could not be loaded.",
                onRetry = { retries++ },
            )
        }
        rule.onNodeWithText("Usage could not be loaded.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithText("Loading usage").assertDoesNotExist()
        rule.onNodeWithText("Requests").assertDoesNotExist()
        rule.onNodeWithText("Try again").assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, retries)
    }

    @Test
    fun loadingReplacesTheItems() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchUsageMeter(items = Usage, loading = true) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Loading usage").assertExists()
        rule.onNodeWithText("Requests").assertDoesNotExist()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchUsageMeter(
                items = Usage,
                labels = MonarchUsageMeterLabels(
                    amount = { used, limit -> "$used / $limit" },
                    percent = { "$it" },
                    nearLimit = "Almost full",
                    overLimit = "Full",
                ),
            )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Storage").assert(hasText("42 / 50 GB")).assert(hasText("Almost full · 84%"))
        rule.onNodeWithText("Minutes").assert(hasText("Full · 130%"))
    }

    @Test
    fun itemsAnnounceChangesOnlyWhenAsked() {
        rule.setMonarchContent { MonarchUsageMeter(items = Usage.take(1), announceChanges = true) }
        rule.onNodeWithText("Requests")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun theLabelWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchUsageMeter(
                items = listOf(MonarchUsageMeterItem("Outbound transactional email", 12.0, 100.0)),
                modifier = Modifier.width(200.dp),
            )
        }
        rule.onNodeWithText("Outbound transactional email", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("12% used", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun coloursFollowTheLevelInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val meter = { level: MonarchUsageLevel -> MonarchUsageMeterTokens.meterColor(level, colors) }
            val status = { level: MonarchUsageLevel -> MonarchUsageMeterTokens.statusColor(level, colors) }
            assertEquals(colors.accentSolid, meter(MonarchUsageLevel.Normal), theme.id)
            assertEquals(colors.warningSolid, meter(MonarchUsageLevel.Warning), theme.id)
            assertEquals(colors.dangerSolid, meter(MonarchUsageLevel.Over), theme.id)
            assertEquals(colors.textMuted, status(MonarchUsageLevel.Normal), theme.id)
            assertEquals(colors.warningStrong, status(MonarchUsageLevel.Warning), theme.id)
            assertEquals(colors.dangerStrong, status(MonarchUsageLevel.Over), theme.id)
            assertEquals(colors.bgEmphasis, MonarchUsageMeterTokens.trackColor(colors), theme.id)
        }
    }
}
