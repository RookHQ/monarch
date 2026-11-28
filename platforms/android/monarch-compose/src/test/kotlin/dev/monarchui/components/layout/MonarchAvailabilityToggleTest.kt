package dev.monarchui.components.layout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.layout.availabilitytoggle.MonarchAvailabilityToggle
import dev.monarchui.components.layout.availabilitytoggle.MonarchAvailabilityToggleLabels
import dev.monarchui.components.layout.availabilitytoggle.MonarchAvailabilityToggleTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal object MonarchAvailabilityToggleFixtures : ComponentFixtures {
    override val id = "availability-toggle"
    override val fixtures = listOf(
        fixture("available") { MonarchAvailabilityToggle(available = true, onAvailableChange = {}) },
        fixture("away") { MonarchAvailabilityToggle(available = false, onAvailableChange = {}) },
        fixture("available with hint") {
            MonarchAvailabilityToggle(available = true, onAvailableChange = {}, showHint = true)
        },
        fixture("away with hint") {
            MonarchAvailabilityToggle(available = false, onAvailableChange = {}, showHint = true)
        },
        fixture("pending") { MonarchAvailabilityToggle(available = true, onAvailableChange = {}, pending = true) },
        fixture("disabled") { MonarchAvailabilityToggle(available = false, onAvailableChange = {}, enabled = false) },
        fixture("long labels") {
            MonarchAvailabilityToggle(
                available = true,
                onAvailableChange = {},
                showHint = true,
                labels = MonarchAvailabilityToggleLabels(
                    available = "Taking new conversations from the support queue",
                    availableHint = "Round robin assignment includes you until you switch this off.",
                ),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAvailabilityToggleTest {
    @get:Rule
    val rule = createComposeRule()

    private fun toggle() = rule.onNodeWithContentDescription("Available for new work")

    @Test
    fun theRowIsOneSwitchNamedForItsEffectWithTheStatusWordAsItsState() {
        var available by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchAvailabilityToggle(available = available, onAvailableChange = { available = it })
        }
        toggle()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsOn()
            .assertIsEnabled()
            .assertTouchTargetAtLeast48dp(rule.density)
        assertEquals("Available", toggle().stateDescription())
        // The status word is the state, so it is not a second element.
        rule.onAllNodesWithText("Available").assertCountEquals(0)
        toggle().performClick()
        assertFalse(available)
        toggle().assertIsOff()
        assertEquals("Away", toggle().stateDescription())
    }

    @Test
    fun whilePendingTheSwitchCannotBeToggledAndSavingIsRead() {
        rule.mainClock.autoAdvance = false
        var changes = 0
        rule.setMonarchContent {
            MonarchAvailabilityToggle(available = false, onAvailableChange = { changes++ }, pending = true)
        }
        rule.mainClock.advanceTimeByFrame()
        toggle().assertIsNotEnabled().performClick()
        assertEquals(0, changes)
        assertEquals("Away, Saving", toggle().stateDescription())
    }

    @Test
    fun aDisabledToggleIgnoresTaps() {
        var changes = 0
        rule.setMonarchContent {
            MonarchAvailabilityToggle(available = true, onAvailableChange = { changes++ }, enabled = false)
        }
        toggle().assertIsNotEnabled().performClick()
        assertEquals(0, changes)
    }

    @Test
    fun theHintFollowsTheStateAndIsReadWithTheSwitch() {
        var available by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchAvailabilityToggle(available = available, onAvailableChange = { available = it }, showHint = true)
        }
        rule.onNodeWithText("Automatic assignment can give you new work.").assertExists()
        toggle().performClick()
        rule.onNodeWithText("Automatic assignment skips you. You can still work on what you have.").assertExists()
    }

    @Test
    fun theHintIsHiddenByDefault() {
        rule.setMonarchContent { MonarchAvailabilityToggle(available = true, onAvailableChange = {}) }
        rule.onAllNodesWithText("Automatic assignment can give you new work.", useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @Test
    fun everyBuiltInStringCanBeReplaced() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAvailabilityToggle(
                available = true,
                onAvailableChange = {},
                pending = true,
                showHint = true,
                labels = MonarchAvailabilityToggleLabels(
                    label = "Disponible",
                    available = "Oui",
                    availableHint = "Indice",
                    pending = "Enregistrement",
                ),
            )
        }
        rule.mainClock.advanceTimeByFrame()
        val node = rule.onNodeWithContentDescription("Disponible")
        assertEquals("Oui, Enregistrement", node.stateDescription())
        rule.onNodeWithText("Indice").assertExists()
    }

    @Test
    fun theHintWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAvailabilityToggle(available = false, onAvailableChange = {}, showHint = true)
        }
        val hint = "Automatic assignment skips you. You can still work on what you have."
        rule.onNodeWithText(hint, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theTextIsAtTheStartAndTheSwitchAtTheEndInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchAvailabilityToggle(available = true, onAvailableChange = {}, showHint = true)
        }
        val hintText = "Automatic assignment can give you new work."
        fun hint() = rule.onNodeWithText(hintText, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        fun row() = toggle().fetchSemanticsNode().boundsInRoot
        assertEquals(row().left, hint().left)
        assertTrue(hint().right < row().right)
        environment.layoutDirection = LayoutDirection.Rtl
        rule.waitForIdle()
        assertEquals(row().right, hint().right)
        assertTrue(hint().left > row().left)
    }

    @Test
    fun theMarkIsAFilledGreenDotOrAYellowRingInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.successSolid, MonarchAvailabilityToggleTokens.markColor(true, colors), theme.id)
            assertEquals(colors.warningSolid, MonarchAvailabilityToggleTokens.markColor(false, colors), theme.id)
            assertEquals(colors.textDefault, MonarchAvailabilityToggleTokens.statusColor(true, colors), theme.id)
            assertEquals(colors.textDisabled, MonarchAvailabilityToggleTokens.statusColor(false, colors), theme.id)
            assertEquals(colors.textMuted, MonarchAvailabilityToggleTokens.hintColor(true, colors), theme.id)
            assertEquals(colors.textDisabled, MonarchAvailabilityToggleTokens.hintColor(false, colors), theme.id)
        }
        assertTrue(MonarchAvailabilityToggleTokens.markFilled(true))
        assertFalse(MonarchAvailabilityToggleTokens.markFilled(false))
        assertTrue(MonarchAvailabilityToggleTokens.interactive(enabled = true, pending = false))
        assertFalse(MonarchAvailabilityToggleTokens.interactive(enabled = true, pending = true))
        assertFalse(MonarchAvailabilityToggleTokens.interactive(enabled = false, pending = false))
    }
}

