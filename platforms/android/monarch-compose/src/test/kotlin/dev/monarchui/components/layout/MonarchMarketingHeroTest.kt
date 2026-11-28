package dev.monarchui.components.layout

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.layout.marketinghero.MonarchMarketingHero
import dev.monarchui.components.layout.marketinghero.MonarchMarketingHeroTokens
import dev.monarchui.components.layout.marketingsection.MonarchMarketingAlign
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
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

internal object MonarchMarketingHeroFixtures : ComponentFixtures {
    override val id = "marketing-hero"
    override val fixtures = listOf(
        fixture("centred with two actions", tall = true) {
            MonarchMarketingHero(
                title = "Write it once. Find it whenever.",
                eyebrow = "Rook Example",
                lead = "Structured documents for teams whose decisions need to stay connected.",
                note = "Set up in under five minutes.",
            ) {
                MonarchButton("Start writing", {}, variant = MonarchButtonVariant.Primary, size = MonarchButtonSize.Lg)
                MonarchButton("See how it works", {}, size = MonarchButtonSize.Lg)
            }
        },
        fixture("start aligned without wash") {
            MonarchMarketingHero(title = "Plan the week", align = MonarchMarketingAlign.Start, wash = false)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchMarketingHeroTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTitleIsTheHeadingAndActionsStayButtons() {
        var started = 0
        rule.setMonarchContent {
            MonarchMarketingHero(title = "Write it once.", eyebrow = "Rook Example") {
                MonarchButton("Start writing", { started++ })
            }
        }
        rule.onNodeWithText("Write it once.").assert(isHeading())
        rule.onNodeWithText("Start writing")
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, started)
    }

    @Test
    fun actionsFollowTheLayoutDirection() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchMarketingHero(title = "Hero") {
                MonarchButton("First", {})
                MonarchButton("Second", {})
            }
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("First").assertIsBefore(rule.onNodeWithText("Second"), direction)
        }
    }

    @Test
    fun textWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchMarketingHero(
                title = "Write it once. Find it whenever.",
                lead = "Structured documents for teams whose decisions need to stay connected.",
            )
        }
        rule.onNodeWithText("Write it once. Find it whenever.").assertTextFullyVisible()
        rule.onNodeWithText("Structured documents for teams whose decisions need to stay connected.")
            .assertTextFullyVisible()
    }

    @Test
    fun theEyebrowTakesTheAccentInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.accentStrong, MonarchMarketingHeroTokens.eyebrow(colors), theme.id)
        }
    }
}
