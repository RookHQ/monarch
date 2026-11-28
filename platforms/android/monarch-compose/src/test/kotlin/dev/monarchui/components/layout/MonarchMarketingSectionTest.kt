package dev.monarchui.components.layout

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
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.layout.marketingsection.MonarchCtaBand
import dev.monarchui.components.layout.marketingsection.MonarchFaq
import dev.monarchui.components.layout.marketingsection.MonarchFaqItem
import dev.monarchui.components.layout.marketingsection.MonarchFeatureGrid
import dev.monarchui.components.layout.marketingsection.MonarchFeatureGridItem
import dev.monarchui.components.layout.marketingsection.MonarchMarketingAlign
import dev.monarchui.components.layout.marketingsection.MonarchMarketingSection
import dev.monarchui.components.layout.marketingsection.MonarchMarketingSectionTokens
import dev.monarchui.components.layout.marketingsection.MonarchMarketingSectionTone
import dev.monarchui.components.layout.marketingsection.MonarchProductFrame
import dev.monarchui.components.layout.marketingsection.MonarchStatStrip
import dev.monarchui.components.layout.marketingsection.MonarchStatStripItem
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
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
