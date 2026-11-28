package com.rookhq.monarch.components.datadisplay

import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.datadisplay.insightstrip.MonarchInsight
import com.rookhq.monarch.components.datadisplay.insightstrip.MonarchInsightOrigin
import com.rookhq.monarch.components.datadisplay.insightstrip.MonarchInsightStrip
import com.rookhq.monarch.components.datadisplay.insightstrip.MonarchInsightStripLabels
import com.rookhq.monarch.components.datadisplay.insightstrip.MonarchInsightStripLayout
import com.rookhq.monarch.components.datadisplay.insightstrip.MonarchInsightStripTokens
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val backup = MonarchInsight(
    id = "backup",
    title = "Backup failed",
    why = "The disk is full.",
    origin = MonarchInsightOrigin.Rule,
    tone = MonarchTone.Danger,
)
private val usage = MonarchInsight(
    id = "usage",
    title = "Usage is up",
    why = "More seats are active.",
    origin = MonarchInsightOrigin.Ai,
    tone = MonarchTone.Success,
)
private val renewals = MonarchInsight(
    id = "renewals",
    title = "3 domains expire in 30 days",
    why = "Auto-renew is off for all three.",
    origin = MonarchInsightOrigin.Rule,
    tone = MonarchTone.Warning,
    evidence = "example.com, example.org, example.net",
    actionLabel = "Review renewals",
    onAction = {},
)
private val all = listOf(usage, renewals, backup)

private fun short(tone: MonarchTone, origin: MonarchInsightOrigin = MonarchInsightOrigin.Rule) = MonarchInsight(
    id = tone.name,
    title = "${tone.name} insight",
    why = "Raised by a rule.",
    origin = origin,
    tone = tone,
)

// The test window is 320 by 470dp and fixtures do not scroll, so each fixture is short enough to fit at
// font scale 2.0.
internal object MonarchInsightStripFixtures : ComponentFixtures {
    override val id = "insight-strip"
    override val fixtures = MonarchTone.entries.map { tone ->
        fixture("tone ${tone.name}") {
            MonarchInsightStrip(insights = listOf(short(tone)), layout = MonarchInsightStripLayout.Stack)
        }
    } + listOf(
        fixture("scrolling row with a title and dismiss") {
            MonarchInsightStrip(insights = all, title = "Needs attention", onDismiss = {})
        },
        fixture("stack, AI and rule") {
            MonarchInsightStrip(
                insights = listOf(short(MonarchTone.Danger, MonarchInsightOrigin.Ai), short(MonarchTone.Info)),
                layout = MonarchInsightStripLayout.Stack,
            )
        },
        fixture("grid") {
            MonarchInsightStrip(
                insights = listOf(short(MonarchTone.Warning), short(MonarchTone.Success)),
                layout = MonarchInsightStripLayout.Grid,
            )
        },
        fixture("evidence, action and dismiss") {
            MonarchInsightStrip(
                insights = listOf(
                    MonarchInsight(
                        id = "renewals",
                        title = "Renewals",
                        why = "Auto-renew is off.",
                        origin = MonarchInsightOrigin.Rule,
                        tone = MonarchTone.Warning,
                        evidence = "example.com",
                        actionLabel = "Review",
                        onAction = {},
                    ),
                ),
                layout = MonarchInsightStripLayout.Stack,
                onDismiss = {},
            )
        },
        fixture("limited") {
            MonarchInsightStrip(
                insights = listOf(short(MonarchTone.Danger), short(MonarchTone.Info)),
                layout = MonarchInsightStripLayout.Stack,
                limit = 1,
            )
        },
        fixture("loading") { MonarchInsightStrip(insights = emptyList(), loading = true) },
        fixture("loading, stack") {
            MonarchInsightStrip(insights = emptyList(), layout = MonarchInsightStripLayout.Stack, loading = true)
        },
        fixture("error with retry") {
            MonarchInsightStrip(insights = all, errorText = "Insights could not be loaded.", onRetry = {})
        },
        fixture("empty") { MonarchInsightStrip(insights = emptyList(), title = "Needs attention") },
        fixture("neutral note") {
            MonarchInsightStrip(
                insights = listOf(
                    MonarchInsight(
                        id = "note",
                        title = "A note with a long title that wraps onto more than one line",
                        why = "Written by the workspace rules.",
                        origin = MonarchInsightOrigin.Rule,
                        tone = MonarchTone.Neutral,
                    ),
                ),
                layout = MonarchInsightStripLayout.Stack,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchInsightStripTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theCardIsAsDeepUnderTheActionAsAboveItsBadgesAndTheActionKeepsA48dpTarget() {
        var taps = 0
        val one = MonarchInsight(
            id = "renewals",
            title = "3 domains expire in 30 days",
            why = "Auto-renew is off for all three.",
            origin = MonarchInsightOrigin.Rule,
            tone = MonarchTone.Warning,
            actionLabel = "Review renewals",
            onAction = { taps++ },
        )
        rule.setMonarchContent {
            MonarchInsightStrip(insights = listOf(one), modifier = Modifier.testTag("strip"))
        }
        val strip = rule.onNodeWithTag("strip").getUnclippedBoundsInRoot()
        val action = rule.onNodeWithText("Review renewals").assertTouchTargetAtLeast48dp(rule.density)
        val target = action.getUnclippedBoundsInRoot()
        // A 32dp button centred in its 48dp target: the visible bottom is 8dp above the target's bottom.
        val visibleBottom = target.bottom - 8.dp
        val badgeTop = rule.onNodeWithText("Warning", useUnmergedTree = true).getUnclippedBoundsInRoot().top
        val under = strip.bottom - visibleBottom
        val over = badgeTop - strip.top
        assertEquals(20f, under.value, 1f, "padding under the action")
        // The badge's own padding sits between the card padding and its text.
        assertTrue(over.value in 20f..26f, "padding above the badge text: $over")
        // A tap in the reserved space under the visible button still presses it.
        action.performTouchInput { click(Offset(centerX, height - 2f)) }
        assertEquals(1, taps)
    }

    @Test
    fun theTitleIsLargerAndHeavierThanTheReason() {
        val typography = MonarchTypography.Default
        val title = MonarchInsightStripTokens.titleStyle(typography)
        val reason = MonarchInsightStripTokens.reasonStyle(typography)
        assertTrue(title.fontSize.value > reason.fontSize.value)
        assertTrue(title.fontWeight!!.weight > reason.fontWeight!!.weight)
    }

    @Test
    fun theTitleIsAHeadingAndEachInsightIsOneElementWithToneAndOrigin() {
        rule.setMonarchContent {
            MonarchInsightStrip(insights = all, title = "Needs attention", layout = MonarchInsightStripLayout.Stack)
        }
        rule.onNodeWithText("Needs attention").assert(isHeading())
        rule.onNode(hasText("Backup failed"))
            .assert(hasText("Critical"))
            .assert(hasText("Rule-based"))
            .assert(hasText("The disk is full."))
        rule.onNode(hasText("Usage is up"))
            .assert(hasText("Positive"))
            .assert(hasText("AI insight"))
    }

    @Test
    fun insightsAreSortedMostSevereFirstUnlessAskedNotTo() {
        rule.setMonarchContent { MonarchInsightStrip(insights = all, layout = MonarchInsightStripLayout.Stack) }
        val critical = rule.onNodeWithText("Backup failed", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val warning = rule.onNodeWithText("3 domains expire in 30 days", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val positive = rule.onNodeWithText("Usage is up", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(critical.top < warning.top && warning.top < positive.top)
    }

    @Test
    fun aLimitHidesTheRestBehindAButton() {
        rule.setMonarchContent {
            MonarchInsightStrip(insights = all, layout = MonarchInsightStripLayout.Stack, limit = 1)
        }
        rule.onNodeWithText("Backup failed", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithText("Usage is up", useUnmergedTree = true).assertCountEquals(0)
        // The toggle is below the 470dp test window once all insights show, so it is activated by its action.
        rule.onNodeWithText("Show 2 more").performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithText("Usage is up", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Show fewer").performSemanticsAction(SemanticsActions.OnClick)
        rule.onAllNodesWithText("Usage is up", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun theActionAndTheDismissButtonCallBackWithTheInsight() {
        var acted = 0
        var dismissed: String? = null
        val insight = MonarchInsight(
            id = "renewals",
            title = "Renewals",
            why = "Off",
            origin = MonarchInsightOrigin.Rule,
            actionLabel = "Review renewals",
            onAction = { acted++ },
        )
        rule.setMonarchContent {
            MonarchInsightStrip(insights = listOf(insight), onDismiss = { dismissed = it })
        }
        rule.onNodeWithText("Review renewals").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("Dismiss").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, acted)
        assertEquals("renewals", dismissed)
    }

    @Test
    fun theEvidenceIsACollapsedSection() {
        rule.setMonarchContent { MonarchInsightStrip(insights = listOf(renewals)) }
        rule.onAllNodesWithText("example.com, example.org, example.net").assertCountEquals(0)
        rule.onNodeWithText("Show evidence").performClick()
        rule.onNodeWithText("example.com, example.org, example.net").assertExists()
    }

    @Test
    fun whileLoadingThePlaceholdersAreOneNamedElement() {
        rule.setMonarchContent { MonarchInsightStrip(insights = all, loading = true) }
        rule.onNodeWithContentDescription("Loading insights").assertExists()
        rule.onAllNodesWithText("Backup failed", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun anErrorReplacesTheInsightsAndOffersRetry() {
        var retries = 0
        rule.setMonarchContent {
            MonarchInsightStrip(insights = all, errorText = "Could not load.", onRetry = { retries++ })
        }
        rule.onNode(hasText("Could not load."))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        rule.onAllNodesWithText("Backup failed", useUnmergedTree = true).assertCountEquals(0)
        rule.onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchInsightStrip(
                insights = listOf(backup),
                labels = MonarchInsightStripLabels(
                    ruleOrigin = "Regel",
                    toneLabel = { "Kritisch" },
                    dismiss = "Ausblenden",
                ),
                onDismiss = {},
            )
        }
        rule.onNode(hasText("Backup failed")).assert(hasText("Regel")).assert(hasText("Kritisch"))
        rule.onNodeWithContentDescription("Ausblenden").assertExists()
    }

    @Test
    fun theEmptyTextCanBeReplaced() {
        rule.setMonarchContent {
            MonarchInsightStrip(insights = emptyList(), labels = MonarchInsightStripLabels(empty = "Alles erledigt."))
        }
        rule.onNodeWithText("Alles erledigt.").assertExists()
    }

    @Test
    fun theToneBadgeComesBeforeTheOriginInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchInsightStrip(insights = listOf(backup), layout = MonarchInsightStripLayout.Stack)
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Critical", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Rule-based", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun textWrapsInsideTheCardsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchInsightStrip(insights = listOf(renewals), layout = MonarchInsightStripLayout.Stack)
        }
        rule.onNodeWithText("3 domains expire in 30 days", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Auto-renew is off for all three.", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theOrderTheLimitAndTheColumnsFollowTheRules() {
        val tokens = MonarchInsightStripTokens
        assertEquals(listOf("backup", "renewals", "usage"), tokens.ordered(all, sortBySeverity = true).map { it.id })
        assertEquals(listOf("usage", "renewals", "backup"), tokens.ordered(all, sortBySeverity = false).map { it.id })
        val ranks = listOf(MonarchTone.Danger, MonarchTone.Warning, MonarchTone.Info, MonarchTone.Neutral)
            .plus(MonarchTone.Success)
            .map(tokens::rank)
        assertEquals(ranks.sorted(), ranks)
        assertFalse(tokens.limits(limit = 0, count = 3))
        assertFalse(tokens.limits(limit = 3, count = 3))
        assertTrue(tokens.limits(limit = 2, count = 3))
        assertEquals(1, tokens.shown(all, limit = 1, expanded = false).size)
        assertEquals(3, tokens.shown(all, limit = 1, expanded = true).size)
        assertEquals(3, tokens.shown(all, limit = -1, expanded = false).size)
        assertEquals(1, tokens.columns(200.dp))
        assertEquals(2, tokens.columns(600.dp))
        assertEquals(3, tokens.columns(2000.dp))
    }

    @Test
    fun placeholdersAndTheEmptyTextUseMutedRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.bgMuted, MonarchInsightStripTokens.placeholderColor(colors), theme.id)
            assertEquals(colors.textMuted, MonarchInsightStripTokens.emptyTextColor(colors), theme.id)
        }
    }

    @Test
    fun aGridShowsSeveralColumnsWhenThereIsRoom() {
        // Wider than the 320dp test window on purpose: two columns need 576dp.
        rule.setMonarchContent {
            MonarchInsightStrip(
                insights = all,
                modifier = Modifier.requiredWidth(MonarchInsightStripTokens.MinCardWidth * 2),
                layout = MonarchInsightStripLayout.Grid,
            )
        }
        val first = rule.onNodeWithText("Backup failed", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val second = rule.onNodeWithText("3 domains expire in 30 days", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val third = rule.onNodeWithText("Usage is up", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(first.top, second.top, "the first two share a row")
        assertTrue(third.top > first.top, "the third starts a new row")
    }
}
