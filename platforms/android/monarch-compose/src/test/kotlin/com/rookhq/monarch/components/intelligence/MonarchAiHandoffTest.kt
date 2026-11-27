@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.components.intelligence.aihandoff.MonarchAiHandoff
import com.rookhq.monarch.components.intelligence.aihandoff.MonarchAiHandoffLabels
import com.rookhq.monarch.components.intelligence.aihandoff.MonarchAiHandoffLogic
import com.rookhq.monarch.components.intelligence.aihandoff.MonarchAiHandoffTarget
import com.rookhq.monarch.components.intelligence.aihandoff.MonarchAiHandoffTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private const val Now = 1_700_000_000_000L
private const val ThreeMinutes = 180_000L
private val Research = MonarchAiAgentRef(name = "Research agent", id = "research")
private val Writer = MonarchAiAgentRef(name = "Writing agent", id = "writer")

internal object MonarchAiHandoffFixtures : ComponentFixtures {
    override val id = "ai-handoff"
    override val fixtures = listOf(
        fixture("to an agent, with reason and time") {
            MonarchAiHandoff(
                from = Research,
                to = MonarchAiHandoffTarget.Agent(Writer),
                reason = "Sources are ready to draft from",
                dateMillis = Now - ThreeMinutes,
                nowMillis = Now,
            )
        },
        fixture("to a person") {
            MonarchAiHandoff(from = Research, to = MonarchAiHandoffTarget.Person("Sam Lee"))
        },
        fixture("with an arrow icon") {
            MonarchAiHandoff(
                from = Research,
                to = MonarchAiHandoffTarget.Person("Sam Lee"),
                arrowIcon = { MonarchIcon(TestIcon, contentDescription = null, size = MonarchIconSize.Sm) },
            )
        },
        fixture("long names and reason") {
            MonarchAiHandoff(
                from = MonarchAiAgentRef(name = "Quarterly revenue reconciliation agent"),
                to = MonarchAiHandoffTarget.Person("Maria Fernanda de los Santos Oliveira"),
                reason = "The refund is above the limit an agent may approve without a person in finance",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiHandoffTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theMarkerIsOneElementReadAsLabelSentenceReasonAndTime() {
        rule.setMonarchContent {
            MonarchAiHandoff(
                from = Research,
                to = MonarchAiHandoffTarget.Agent(Writer),
                reason = "Sources are ready",
                dateMillis = Now - ThreeMinutes,
                nowMillis = Now,
            )
        }
        rule.onNodeWithContentDescription(
            "Handoff. Research agent (AI agent) handed off to Writing agent (AI agent). " +
                "Reason: Sources are ready. 3 minutes ago",
        ).assertExists().assertHasNoClickAction()
    }

    @Test
    fun aPersonsNameIsNotTaggedAsAnAgent() {
        rule.setMonarchContent { MonarchAiHandoff(from = Research, to = MonarchAiHandoffTarget.Person("Sam Lee")) }
        rule.onNodeWithContentDescription("Handoff. Research agent (AI agent) handed off to Sam Lee")
            .assertExists()
        rule.onNodeWithText("Research agent handed off to Sam Lee", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theLabelsReplaceEveryBuiltInString() {
        rule.setMonarchContent {
            MonarchAiHandoff(
                from = Research,
                to = MonarchAiHandoffTarget.Person("Sam"),
                reason = "Approval",
                labels = MonarchAiHandoffLabels(
                    label = "Relais",
                    handedOff = { from, to, person -> "$to übernimmt von $from${if (person) " (Person)" else ""}" },
                    agentKind = "KI",
                    reason = "Grund:",
                ),
            )
        }
        rule.onNodeWithContentDescription("Relais. Sam übernimmt von Research agent (KI) (Person). Grund: Approval")
            .assertExists()
        rule.onNodeWithText("Sam übernimmt von Research agent (Person)", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aBlankReasonIsLeftOut() {
        rule.setMonarchContent {
            MonarchAiHandoff(from = Research, to = MonarchAiHandoffTarget.Person("Sam"), reason = "  ")
        }
        rule.onNodeWithContentDescription("Handoff. Research agent (AI agent) handed off to Sam").assertExists()
    }

    @Test
    fun theSentenceWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiHandoff(
                from = MonarchAiAgentRef(name = "Quarterly reconciliation agent"),
                to = MonarchAiHandoffTarget.Person("Maria Fernanda Oliveira"),
            )
        }
        val sentence = "Quarterly reconciliation agent handed off to Maria Fernanda Oliveira"
        rule.onNodeWithText(sentence, useUnmergedTree = true)
            .assertTextFullyVisible()
    }

    @Test
    fun thePicturesAreBeforeTheSentenceRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiHandoff(
                from = Research,
                to = MonarchAiHandoffTarget.Person("Sam"),
                arrowIcon = { MonarchIcon(TestIcon, contentDescription = "arrow") },
            )
        }
        rule.onNodeWithContentDescription("arrow", useUnmergedTree = true)
            .assertIsBefore(
                rule.onNodeWithText("Research agent handed off to Sam", useUnmergedTree = true),
                LayoutDirection.Rtl,
            )
    }

    @Test
    fun namesAreStrongWhereverTheTranslationPutsThem() {
        val text = MonarchAiHandoffLogic.emphasized(
            "Sam übernimmt von Research agent",
            listOf("Research agent", "Sam"),
        )
        val strong = text.spanStyles.filter { it.item.fontWeight == FontWeight(MonarchAiHandoffTokens.NAME_WEIGHT) }
        assertEquals(listOf(0 to 3, 18 to 32), strong.map { it.start to it.end }.sortedBy { it.first })
    }

    @Test
    fun theLinesAppearOnlyOnWideWindows() {
        assertFalse(MonarchAiHandoffTokens.showsLines(360.dp))
        assertTrue(MonarchAiHandoffTokens.showsLines(800.dp))
    }

    @Test
    fun theBorderSaysAgentOrPersonInEveryTheme() {
        for (theme in AllThemes) {
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.aiBorder, MonarchAiHandoffTokens.borderColor(toPerson = false, colors = colors))
            assertEquals(colors.borderDefault, MonarchAiHandoffTokens.borderColor(toPerson = true, colors = colors))
            assertNotEquals(MonarchAiHandoffTokens.container(colors), MonarchAiHandoffTokens.sentenceColor(colors))
            assertEquals(colors.textMuted, MonarchAiHandoffTokens.reasonColor(colors))
            assertEquals(colors.textSubtle, MonarchAiHandoffTokens.timeColor(colors))
        }
    }
}
