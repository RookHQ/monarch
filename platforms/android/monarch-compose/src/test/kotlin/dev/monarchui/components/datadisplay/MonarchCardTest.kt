package dev.monarchui.components.datadisplay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.datadisplay.card.MonarchCardTokens
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

internal object MonarchCardFixtures : ComponentFixtures {
    override val id = "card"
    override val fixtures = listOf(
        fixture("static") {
            MonarchCard {
                MonarchText(text = "Quarterly report", style = MonarchTextStyle.Subheading)
                MonarchText(text = "Updated two hours ago", style = MonarchTextStyle.Caption)
            }
        },
        fixture("selected and a danger tone") {
            Column {
                MonarchCard(onClick = {}, selected = true) { MonarchText(text = "Picked") }
                MonarchCard(tone = MonarchTone.Danger) { MonarchText(text = "Payment failed") }
            }
        },
        fixture("elevated") { MonarchCard(elevated = true) { MonarchText(text = "Elevated") } },
        fixture("tappable") { MonarchCard(onClick = {}) { MonarchText(text = "Tap to open") } },
        fixture("tappable, disabled and elevated") {
            MonarchCard(onClick = {}, enabled = false, elevated = true) { MonarchText(text = "Unavailable") }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchCardTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aStaticCardIsNotActionableAndSetsTheContentColour() {
        var contentColor = Color.Unspecified
        rule.setMonarchContent(tokens = MonarchTokens.dark) {
            MonarchCard(modifier = Modifier.testTag("card")) {
                contentColor = LocalContentColor.current
                MonarchText(text = "Report")
            }
        }
        rule.onNodeWithTag("card").assertHasNoClickAction()
        rule.onNodeWithText("Report").assertExists()
        assertEquals(Color(MonarchTokens.dark.colors.textDefault), contentColor)
    }

    @Test
    fun aTappableCardIsOneActionableElement() {
        var clicks = 0
        rule.setMonarchContent {
            MonarchCard(onClick = { clicks++ }, modifier = Modifier.testTag("card")) {
                MonarchText(text = "Report")
                MonarchText(text = "Updated today")
            }
        }
        rule.onNodeWithTag("card").assertHasClickAction().assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun aDisabledTappableCardIgnoresTaps() {
        var clicks = 0
        rule.setMonarchContent {
            MonarchCard(onClick = { clicks++ }, enabled = false, modifier = Modifier.testTag("card")) {
                MonarchText(text = "Report")
            }
        }
        rule.onNodeWithTag("card").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun elevationChangesTheSurfaceAndBorderRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.bgSurface, MonarchCardTokens.container(false, colors), theme.id)
            assertEquals(colors.bgRaised, MonarchCardTokens.container(true, colors), theme.id)
            val plain = MonarchCardTokens.border(false, false, null, colors)
            val raised = MonarchCardTokens.border(true, false, null, colors)
            assertEquals(colors.borderDefault, plain.brushColor(), theme.id)
            assertEquals(colors.borderSubtle, raised.brushColor(), theme.id)
        }
    }

    @Test
    fun aSelectedCardHasAnAccentEdgeAndTheSelectedState() {
        rule.setMonarchContent {
            MonarchCard(onClick = {}, selected = true, modifier = Modifier.testTag("picked")) { MonarchText("A") }
            MonarchCard(onClick = {}, modifier = Modifier.testTag("other")) { MonarchText("B") }
        }
        rule.onNodeWithTag("picked").assertIsSelected()
        rule.onNodeWithTag("other").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
    }

    @Test
    fun selectedThenToneThenDefaultDecideTheBorder() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val selected = MonarchCardTokens.border(false, true, MonarchTone.Danger, c)
            assertEquals(c.accentSolid, selected.brushColor(), theme.id)
            assertEquals(c.accentSubtle, MonarchCardTokens.container(false, true, c), theme.id)
            assertEquals(MonarchCardTokens.SelectedBorderWidth, selected.width, theme.id)
            val danger = MonarchCardTokens.border(false, false, MonarchTone.Danger, c)
            assertEquals(c.dangerBorder, danger.brushColor(), theme.id)
            assertEquals(MonarchCardTokens.BorderWidth, danger.width, theme.id)
            assertEquals(c.borderDefault, MonarchCardTokens.border(false, false, MonarchTone.Neutral, c).brushColor())
        }
    }

    private fun BorderStroke.brushColor(): Color = (brush as SolidColor).value
}
