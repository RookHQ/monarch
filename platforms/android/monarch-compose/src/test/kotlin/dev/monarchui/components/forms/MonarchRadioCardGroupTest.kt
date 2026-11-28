package dev.monarchui.components.forms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.forms.radiocardgroup.MonarchRadioCardGroup
import dev.monarchui.components.forms.radiocardgroup.MonarchRadioCardGroupTokens
import dev.monarchui.components.forms.radiocardgroup.MonarchRadioCardOption
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
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
import kotlin.test.assertNotEquals

private val plans = listOf(
    MonarchRadioCardOption("starter", "Starter", description = "For one person.", meta = "1 seat"),
    MonarchRadioCardOption("team", "Team", description = "Shared projects.", meta = "50 seats"),
)

internal object MonarchRadioCardGroupFixtures : ComponentFixtures {
    override val id = "radio-card-group"
    override val fixtures = listOf(
        fixture("nothing chosen, with a label") {
            MonarchRadioCardGroup(options = plans, selected = null, onSelect = {}, label = "Plan")
        },
        fixture("chosen, with an icon") {
            MonarchRadioCardGroup(
                options = listOf(
                    MonarchRadioCardOption(
                        "team",
                        "Team",
                        meta = "50 seats",
                        icon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                    ),
                ),
                selected = "team",
                onSelect = {},
            )
        },
        fixture("one card disabled") {
            MonarchRadioCardGroup(
                options = listOf(
                    MonarchRadioCardOption("a", "Standard"),
                    MonarchRadioCardOption("b", "Express", enabled = false),
                ),
                selected = "a",
                onSelect = {},
            )
        },
        fixture("group disabled") {
            MonarchRadioCardGroup(options = plans, selected = "team", onSelect = {}, enabled = false)
        },
        fixture("three columns requested") {
            MonarchRadioCardGroup(
                options = listOf(MonarchRadioCardOption(1, "One"), MonarchRadioCardOption(2, "Two")),
                selected = 1,
                onSelect = {},
                columns = 3,
            )
        },
        fixture("long text") {
            MonarchRadioCardGroup(
                options = listOf(
                    MonarchRadioCardOption(
                        "x",
                        "A long title that wraps onto a second line",
                        description = "A description that also needs more than one line.",
                        meta = "Most chosen",
                    ),
                ),
                selected = "x",
                onSelect = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchRadioCardGroupTest {
    @get:Rule
    val rule = createComposeRule()

    private val isRadio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    @Test
    fun eachCardIsARadioButtonNamedByItsTitleAndSelectsOnTap() {
        var selected by mutableStateOf<String?>(null)
        rule.setMonarchContent {
            MonarchRadioCardGroup(options = plans, selected = selected, onSelect = { selected = it }, label = "Plan")
        }
        val team = rule.onNodeWithText("Team", substring = true)
        team.assert(isRadio).assertIsNotSelected()
        team.assertTextContains("Shared projects.").assertTextContains("50 seats")
        team.performClick()
        assertEquals("team", selected)
        team.assertIsSelected()
        rule.onNodeWithText("Starter", substring = true).assertIsNotSelected()
    }

    @Test
    fun theCardsAreOneSelectableGroup() {
        rule.setMonarchContent {
            MonarchRadioCardGroup(options = plans, selected = null, onSelect = {}, modifier = Modifier.testTag("group"))
        }
        val groups = rule.onNodeWithTag("group").fetchSemanticsNode().children
            .flatMap { listOf(it) + it.children }
            .filter { it.config.contains(SemanticsProperties.SelectableGroup) }
        assertEquals(1, groups.size)
    }

    @Test
    fun aDisabledCardCannotBeChosen() {
        var selected by mutableStateOf<String?>("a")
        rule.setMonarchContent {
            MonarchRadioCardGroup(
                options = listOf(
                    MonarchRadioCardOption("a", "Standard"),
                    MonarchRadioCardOption("b", "Express", enabled = false),
                ),
                selected = selected,
                onSelect = { selected = it },
            )
        }
        rule.onNodeWithText("Express").assertIsNotEnabled().performClick()
        assertEquals("a", selected)
    }

    @Test
    fun aDisabledGroupDisablesEveryCard() {
        rule.setMonarchContent {
            MonarchRadioCardGroup(options = plans, selected = null, onSelect = {}, enabled = false)
        }
        rule.onNodeWithText("Starter", substring = true).assertIsNotEnabled()
        rule.onNodeWithText("Team", substring = true).assertIsNotEnabled()
    }

    @Test
    fun cardsAreAtLeast48dpAndTheMarkIsAtTheStartInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchRadioCardGroup(options = plans, selected = "team", onSelect = {})
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Team", substring = true).assertTouchTargetAtLeast48dp(rule.density)
            rule.onNodeWithText("Team", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("50 seats", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun textWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchRadioCardGroup(options = plans, selected = null, onSelect = {}, columns = 2)
        }
        rule.onNodeWithText("Shared projects.", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Team", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun columnsCollapseToOneOnNarrowWindowsAndLargeText() {
        val t = MonarchRadioCardGroupTokens
        assertEquals(1, t.columns(3, 320.dp, 1f))
        assertEquals(3, t.columns(3, 800.dp, 1f))
        assertEquals(1, t.columns(3, 800.dp, LargestFontScale))
        assertEquals(3, t.columns(7, 800.dp, 1f))
        assertEquals(1, t.columns(0, 800.dp, 1f))
    }

    @Test
    fun coloursComeFromTheThemeRolesAndSelectionIsAFillAndAnEdge() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val t = MonarchRadioCardGroupTokens
            val resting = t.card(selected = false, enabled = true, colors = colors)
            val chosen = t.card(selected = true, enabled = true, colors = colors)
            val disabled = t.card(selected = false, enabled = false, colors = colors)
            assertEquals(colors.bgSurface, resting.container, theme.id)
            assertEquals(colors.borderDefault, resting.border, theme.id)
            assertEquals(colors.accentSubtle, chosen.container, theme.id)
            assertEquals(colors.accentStrong, chosen.border, theme.id)
            assertEquals(colors.bgSubtle, disabled.container, theme.id)
            assertEquals(colors.textDisabled, disabled.content, theme.id)
            assertEquals(colors.accentStrong, t.icon(selected = true, enabled = true, colors = colors), theme.id)
            assertEquals(colors.textMuted, t.muted(enabled = true, colors = colors), theme.id)
            assertNotEquals(chosen.container, resting.container)
            assertEquals(t.BorderWidth, t.borderWidth(true))
        }
    }
}
