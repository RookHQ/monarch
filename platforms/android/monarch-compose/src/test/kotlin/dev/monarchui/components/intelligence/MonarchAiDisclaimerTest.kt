package dev.monarchui.components.intelligence

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.R
import dev.monarchui.components.intelligence.aidisclaimer.MonarchAiDisclaimer
import dev.monarchui.components.intelligence.aidisclaimer.MonarchAiDisclaimerRisk
import dev.monarchui.components.intelligence.aidisclaimer.MonarchAiDisclaimerTokens
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val LowText = "AI can make mistakes. Check important info."
private const val MediumText = "AI can make mistakes. Check important details before you rely on them."
private const val HighText =
    "AI-generated content can be wrong. Have a qualified person review it before you act on it."

internal object MonarchAiDisclaimerFixtures : ComponentFixtures {
    override val id = "ai-disclaimer"
    override val fixtures = listOf(
        fixture("low") { MonarchAiDisclaimer() },
        fixture("low with icon") {
            MonarchAiDisclaimer(icon = { MonarchIcon(TestIcon, contentDescription = null, size = MonarchIconSize.Sm) })
        },
        fixture("medium with icon") {
            MonarchAiDisclaimer(
                risk = MonarchAiDisclaimerRisk.Medium,
                icon = { MonarchIcon(TestIcon, contentDescription = null, size = MonarchIconSize.Sm) },
            )
        },
        fixture("high with icon") {
            MonarchAiDisclaimer(
                risk = MonarchAiDisclaimerRisk.High,
                icon = { MonarchIcon(TestIcon, contentDescription = null, size = MonarchIconSize.Sm) },
            )
        },
        fixture("long custom text") {
            MonarchAiDisclaimer(
                text = "Answers come from the documents in this workspace and may be out of date or incomplete. " +
                    "Check the sources before you share them.",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiDisclaimerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachRiskHasItsOwnBuiltInWording() {
        rule.setMonarchContent {
            androidx.compose.foundation.layout.Column {
                MonarchAiDisclaimer()
                MonarchAiDisclaimer(risk = MonarchAiDisclaimerRisk.Medium)
                MonarchAiDisclaimer(risk = MonarchAiDisclaimerRisk.High)
            }
        }
        rule.onNodeWithText(LowText, useUnmergedTree = true).assertExists()
        rule.onNodeWithText(MediumText, useUnmergedTree = true).assertExists()
        rule.onNodeWithText(HighText, useUnmergedTree = true).assertExists()
    }

    @Test
    fun theTextReplacesTheWordingAndBlankTextKeepsIt() {
        rule.setMonarchContent {
            androidx.compose.foundation.layout.Column {
                MonarchAiDisclaimer(text = "Las respuestas pueden contener errores.")
                MonarchAiDisclaimer(risk = MonarchAiDisclaimerRisk.Medium, text = "  ")
            }
        }
        rule.onNodeWithText("Las respuestas pueden contener errores.", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithText(LowText, useUnmergedTree = true).assertCountEquals(0)
        rule.onNodeWithText(MediumText, useUnmergedTree = true).assertExists()
    }

    @Test
    fun theIconAndTextAreOneElementReadAsTheText() {
        rule.setMonarchContent {
            MonarchAiDisclaimer(
                modifier = Modifier.testTag("disclaimer"),
                icon = { MonarchIcon(TestIcon, contentDescription = "Bot") },
            )
        }
        val node = rule.onNodeWithTag("disclaimer").fetchSemanticsNode()
        assertEquals(listOf(LowText), node.config[SemanticsProperties.Text].map { it.text })
        assertNull(node.config.getOrNull(SemanticsProperties.ContentDescription), "The icon is decorative")
    }

    @Test
    fun theHighRiskIsReadWithTheWarningToneFirst() {
        rule.setMonarchContent { MonarchAiDisclaimer(risk = MonarchAiDisclaimerRisk.High) }
        val merged = rule.onNodeWithText(HighText).fetchSemanticsNode()
        val texts = merged.config[SemanticsProperties.Text].map { it.text }
        assertEquals("Warning", texts.first())
        assertTrue(HighText in texts)
    }

    @Test
    fun theMediumRiskStartsAtTheStart() {
        rule.setMonarchContent {
            MonarchAiDisclaimer(
                modifier = Modifier.testTag("disclaimer"),
                risk = MonarchAiDisclaimerRisk.Medium,
                icon = { MonarchIcon(TestIcon, contentDescription = null, modifier = Modifier.testTag("icon")) },
            )
        }
        val row = rule.onNodeWithTag("disclaimer").getUnclippedBoundsInRoot()
        val icon = rule.onNodeWithTag("icon", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(row.left, icon.left)
    }

    @Test
    fun theIconComesBeforeTheTextRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiDisclaimer(
                risk = MonarchAiDisclaimerRisk.Medium,
                icon = { MonarchIcon(TestIcon, contentDescription = null, modifier = Modifier.testTag("icon")) },
            )
        }
        rule.onNodeWithTag("icon", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText(MediumText, useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun theTextWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiDisclaimer(risk = MonarchAiDisclaimerRisk.Medium)
        }
        rule.onNodeWithText(MediumText, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theWordingResourcesFollowTheRisk() {
        assertEquals(R.string.monarch_aidisclaimer_low, MonarchAiDisclaimerTokens.message(MonarchAiDisclaimerRisk.Low))
        assertEquals(
            R.string.monarch_aidisclaimer_high,
            MonarchAiDisclaimerTokens.message(MonarchAiDisclaimerRisk.High),
        )
        assertNull(MonarchAiDisclaimerTokens.textOrNull(""))
        assertEquals("Custom", MonarchAiDisclaimerTokens.textOrNull("Custom"))
    }

    @Test
    fun theColoursComeFromTheRiskRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val medium = MonarchAiDisclaimerRisk.Medium
            assertEquals(c.textMuted, MonarchAiDisclaimerTokens.textColor(MonarchAiDisclaimerRisk.Low, c), theme.id)
            assertEquals(c.textDefault, MonarchAiDisclaimerTokens.textColor(medium, c), theme.id)
            assertEquals(c.aiStrong, MonarchAiDisclaimerTokens.iconColor(MonarchAiDisclaimerRisk.Low, c), theme.id)
            assertEquals(c.infoStrong, MonarchAiDisclaimerTokens.iconColor(medium, c), theme.id)
            val high = MonarchAiDisclaimerTokens.iconColor(MonarchAiDisclaimerRisk.High, c)
            assertEquals(c.warningStrong, high, theme.id)
        }
    }
}
