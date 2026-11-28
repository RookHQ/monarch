package com.rookhq.monarch.components.layout

import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.layout.marketingsection.MonarchCtaBand
import com.rookhq.monarch.components.layout.marketingsection.MonarchFaq
import com.rookhq.monarch.components.layout.marketingsection.MonarchFaqItem
import com.rookhq.monarch.components.layout.marketingsection.MonarchFeatureGrid
import com.rookhq.monarch.components.layout.marketingsection.MonarchFeatureGridItem
import com.rookhq.monarch.components.layout.marketingsection.MonarchMarketingAlign
import com.rookhq.monarch.components.layout.marketingsection.MonarchMarketingSection
import com.rookhq.monarch.components.layout.marketingsection.MonarchMarketingSectionTokens
import com.rookhq.monarch.components.layout.marketingsection.MonarchMarketingSectionTone
import com.rookhq.monarch.components.layout.marketingsection.MonarchProductFrame
import com.rookhq.monarch.components.layout.marketingsection.MonarchStatStrip
import com.rookhq.monarch.components.layout.marketingsection.MonarchStatStripItem
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private val Features = listOf(
    MonarchFeatureGridItem("Slash commands", "Tables, lists and callouts, one keystroke away."),
    MonarchFeatureGridItem("Version history", "Named snapshots you can restore safely."),
)

internal object MonarchMarketingSectionFixtures : ComponentFixtures {
    override val id = "marketing-section-blocks"
    override val fixtures = listOf(
        fixture("section with features") {
            MonarchMarketingSection(
                title = "Every idea gets the right shape.",
                eyebrow = "The editor",
                description = "Type a slash and pick a block.",
            ) { MonarchFeatureGrid(Features) }
        },
        fixture("subtle, centred, three columns") {
            MonarchMarketingSection(
                title = "Why teams switch",
                align = MonarchMarketingAlign.Center,
                tone = MonarchMarketingSectionTone.Subtle,
            ) { MonarchFeatureGrid(Features + Features.take(1), columns = 3) }
        },
        fixture("product frame and stats") {
            MonarchProductFrame(label = "Share dialog with a copyable link", title = "example.rookhq.com/share") {
                MonarchText("Anyone with the link can view")
            }
            MonarchStatStrip(
                listOf(MonarchStatStripItem("3", "Export formats"), MonarchStatStripItem("13", "Interface languages")),
            )
        },
        fixture("FAQ and call to action") {
            MonarchFaq(listOf(MonarchFaqItem("Which formats can I export?", "HTML, Markdown and PDF.")))
            MonarchCtaBand(title = "Start with a blank page.", description = "Free for small teams.") {
                MonarchButton("Start writing", {}, variant = MonarchButtonVariant.Primary)
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchMarketingSectionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun titlesAreHeadingsAndEachFeatureIsOneElement() {
        rule.setMonarchContent {
            MonarchMarketingSection(title = "Every idea gets the right shape.", eyebrow = "The editor") {
                MonarchFeatureGrid(Features)
            }
        }
        rule.onNodeWithText("Every idea gets the right shape.").assert(isHeading())
        rule.onNodeWithText("Slash commands", substring = true)
            .assert(hasText("Tables, lists and callouts, one keystroke away.", substring = true))
    }

    @Test
    fun aStatIsReadAsTheValueThenTheLabel() {
        rule.setMonarchContent { MonarchStatStrip(listOf(MonarchStatStripItem("13", "Interface languages"))) }
        rule.onNodeWithText("13").assert(hasText("Interface languages"))
    }

    @Test
    fun theProductFrameIsReadAsItsLabelOnly() {
        rule.setMonarchContent {
            MonarchProductFrame(label = "Share dialog", title = "example.rookhq.com/share") { MonarchText("Inside") }
        }
        rule.onNodeWithContentDescription("Share dialog").assertExists()
        rule.onNodeWithText("Inside").assertDoesNotExist()
    }

    @Test
    fun aQuestionExpandsToShowItsAnswer() {
        rule.setMonarchContent { MonarchFaq(
            listOf(MonarchFaqItem("Which formats can I export?", "HTML, Markdown and PDF.")),
        ) }
        val question = rule.onNodeWithText("Which formats can I export?")
        question.assertHasClickAction().assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("HTML, Markdown and PDF.").assertDoesNotExist()
        val before = question.stateDescription()
        question.performClick()
        rule.onNodeWithText("HTML, Markdown and PDF.").assertExists()
        assertNotEquals(before, rule.onNodeWithText("Which formats can I export?").stateDescription())
    }

    @Test
    fun theCallToActionKeepsItsButton() {
        var started = 0
        rule.setMonarchContent { MonarchCtaBand(title = "Start with a blank page.") { MonarchButton(
            "Start writing",
            { started++ },
        ) } }
        rule.onNodeWithText("Start with a blank page.").assert(isHeading())
        rule.onNodeWithText("Start writing").performClick()
        assertEquals(1, started)
    }

    @Test
    fun cardsFollowTheLayoutDirection() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MonarchFeatureGrid(Features, Modifier.requiredWidth(400.dp)) }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Slash commands", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Version history", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun textWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchFeatureGrid(Features) }
        rule.onNodeWithText(
            "Tables, lists and callouts, one keystroke away.",
            useUnmergedTree = true,
        ).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.accentStrong, MonarchMarketingSectionTokens.eyebrow(colors), theme.id)
            assertEquals(
                Color.Transparent,
                MonarchMarketingSectionTokens.sectionFill(MonarchMarketingSectionTone.Default, colors),
            )
            assertEquals(
                colors.bgSubtle,
                MonarchMarketingSectionTokens.sectionFill(MonarchMarketingSectionTone.Subtle, colors),
            )
            assertEquals(colors.accentSubtle, MonarchMarketingSectionTokens.ctaFill(colors), theme.id)
            assertEquals(colors.accentBorder, MonarchMarketingSectionTokens.ctaBorder(colors), theme.id)
        }
    }
}
