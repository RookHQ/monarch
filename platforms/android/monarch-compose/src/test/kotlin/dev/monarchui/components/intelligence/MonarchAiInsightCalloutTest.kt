package dev.monarchui.components.intelligence

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.intelligence.aiinsightcallout.MonarchAiInsightCallout
import dev.monarchui.components.intelligence.aiinsightcallout.MonarchAiInsightCalloutLabels
import dev.monarchui.components.intelligence.aiinsightcallout.MonarchAiInsightCalloutTokens
import dev.monarchui.components.intelligence.aiinsightcallout.MonarchAiInsightOrigin
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchAiInsightCalloutFixtures : ComponentFixtures {
    override val id = "ai-insight-callout"
    override val fixtures = buildList {
        MonarchTone.entries.forEach { tone ->
            add(
                fixture("tone ${tone.name}, AI") {
                    MonarchAiInsightCallout(
                        title = "Renewal at risk",
                        why = "Usage fell 40% in 30 days.",
                        origin = MonarchAiInsightOrigin.Ai,
                        tone = tone,
                        icon = { MonarchIcon(TestIcon, null) },
                        dismissIcon = null,
                    )
                },
            )
        }
        add(
            fixture("rule, action and icon dismiss") {
                MonarchAiInsightCallout(
                    title = "Invoice overdue",
                    why = "Unpaid for 30 days.",
                    origin = MonarchAiInsightOrigin.Rule,
                    tone = MonarchTone.Danger,
                    actionLabel = "Send reminder",
                    onAction = {},
                    onDismiss = {},
                    dismissIcon = { MonarchIcon(TestIcon, null) },
                )
            },
        )
        add(
            fixture("evidence and text dismiss") {
                MonarchAiInsightCallout(
                    title = "Upsell signal",
                    why = "Seats are 95% used.",
                    origin = MonarchAiInsightOrigin.Ai,
                    tone = MonarchTone.Success,
                    onDismiss = {},
                    originIcon = { MonarchIcon(TestIcon, null) },
                    dismissIcon = null,
                ) { MonarchText(text = "Seats: 95 of 100") }
            },
        )
        add(
            fixture("long text") {
                MonarchAiInsightCallout(
                    title = "Three enterprise renewals are due before the end of the quarter",
                    why = "Each account has an open support escalation and lower weekly activity than last month.",
                    origin = MonarchAiInsightOrigin.Ai,
                    tone = MonarchTone.Warning,
                    dismissIcon = null,
                )
            },
        )
    }
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiInsightCalloutTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTitleIsReadWithTheToneNameFirstAndCanBeAHeading() {
        rule.setMonarchContent {
            MonarchAiInsightCallout(
                title = "Renewal at risk",
                why = "Usage fell.",
                origin = MonarchAiInsightOrigin.Ai,
                tone = MonarchTone.Warning,
                heading = true,
                dismissIcon = null,
            )
        }
        rule.onNodeWithContentDescription("Warning: Renewal at risk")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Usage fell.").assertExists()
    }

    @Test
    fun theNeutralToneAndAnEmptyToneLabelReadTheTitleAlone() {
        rule.setMonarchContent {
            Column {
                MonarchAiInsightCallout(
                    "Plain",
                    "Why.",
                    MonarchAiInsightOrigin.Rule,
                    tone = MonarchTone.Neutral,
                    dismissIcon = null,
                )
                MonarchAiInsightCallout(
                    "Quiet",
                    "Why.",
                    MonarchAiInsightOrigin.Rule,
                    labels = MonarchAiInsightCalloutLabels(tone = ""),
                    dismissIcon = null,
                )
            }
        }
        rule.onNodeWithContentDescription("Plain").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Heading))
        rule.onNodeWithContentDescription("Quiet").assertExists()
        assertNull(MonarchAiInsightCalloutTokens.toneResource(MonarchTone.Neutral))
        assertFalse(MonarchAiInsightCalloutTokens.namesTone(""))
        assertTrue(MonarchAiInsightCalloutTokens.namesTone("Info"))
    }

    @Test
    fun theOriginIsAlwaysShownAndCanBeRenamed() {
        rule.setMonarchContent {
            Column {
                MonarchAiInsightCallout("A", "Why.", MonarchAiInsightOrigin.Ai, dismissIcon = null)
                MonarchAiInsightCallout("B", "Why.", MonarchAiInsightOrigin.Rule, dismissIcon = null)
                MonarchAiInsightCallout(
                    "C",
                    "Why.",
                    MonarchAiInsightOrigin.Ai,
                    labels = MonarchAiInsightCalloutLabels(ai = "Analyse IA"),
                    dismissIcon = null,
                )
            }
        }
        rule.onNodeWithText("AI insight").assertExists()
        rule.onNodeWithText("Rule-based").assertExists()
        rule.onNodeWithText("Analyse IA").assertExists()
    }

    @Test
    fun theActionAndTheDismissButtonCallBackWith48dpTargets() {
        var acted = 0
        var dismissed = 0
        rule.setMonarchContent {
            MonarchAiInsightCallout(
                title = "Invoice overdue",
                why = "Unpaid for 30 days.",
                origin = MonarchAiInsightOrigin.Rule,
                actionLabel = "Send reminder",
                onAction = { acted++ },
                onDismiss = { dismissed++ },
                dismissIcon = { MonarchIcon(TestIcon, null) },
            )
        }
        rule.onNodeWithText("Send reminder").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("Dismiss insight")
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, acted)
        assertEquals(1, dismissed)
    }

    @Test
    fun anActionNeedsBothALabelAndACallback() {
        rule.setMonarchContent {
            MonarchAiInsightCallout(
                "Title",
                "Why.",
                MonarchAiInsightOrigin.Ai,
                actionLabel = "Plan outreach",
                dismissIcon = null,
            )
        }
        rule.onNodeWithText("Plan outreach").assertDoesNotExist()
    }

    @Test
    fun theEvidenceIsCollapsedUntilTheDisclosureIsOpened() {
        rule.setMonarchContent {
            MonarchAiInsightCallout("Title", "Why.", MonarchAiInsightOrigin.Ai, dismissIcon = null) {
                MonarchText(text = "Weekly active users: 120 to 72")
            }
        }
        rule.onNodeWithText("Weekly active users: 120 to 72").assertDoesNotExist()
        rule.onNodeWithText("Show evidence").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Weekly active users: 120 to 72").assertExists()
    }

    @Test
    fun theDismissButtonIsAtTheEndInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiInsightCallout("Title", "Why.", MonarchAiInsightOrigin.Ai, onDismiss = {}, dismissIcon = null)
        }
        rule.onNodeWithText("Why.").assertIsBefore(rule.onNodeWithText("Dismiss insight"), LayoutDirection.Rtl)
    }

    @Test
    fun theReasonWrapsAtTheLargestFontScale() {
        val why = "Each account has an open support escalation and lower weekly activity than last month."
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiInsightCallout("Renewals due", why, MonarchAiInsightOrigin.Ai, dismissIcon = null)
        }
        rule.onNodeWithText(why, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun toneAndOriginColoursComeFromTheirRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val warning = MonarchAiInsightCalloutTokens.surface(MonarchTone.Warning, colors)
            assertEquals(colors.warningSubtle, warning.fill, theme.id)
            assertEquals(colors.warningBorder, warning.border, theme.id)
            assertEquals(colors.warningStrong, warning.accent, theme.id)
            val ai = MonarchAiInsightCalloutTokens.origin(MonarchAiInsightOrigin.Ai, colors)
            val rule = MonarchAiInsightCalloutTokens.origin(MonarchAiInsightOrigin.Rule, colors)
            assertEquals(colors.aiStrong, ai.content, theme.id)
            assertEquals(colors.aiSubtle, ai.container, theme.id)
            assertEquals(colors.textMuted, rule.content, theme.id)
            assertEquals(colors.borderDefault, rule.border, theme.id)
        }
    }
}
