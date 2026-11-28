package dev.monarchui.components.actions

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonTokens
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.actions.iconbutton.toggleLook
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MonarchIconButtonTest {
    @get:Rule
    val rule = createComposeRule()

    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    @Test
    fun theLabelIsTheTalkBackNameAndTheButtonRoleAndClickAreExposed() {
        var clicks = 0
        rule.setMonarchContent {
            MonarchIconButton(label = "Close", onClick = { clicks++ }) {
                MonarchIcon(imageVector = TestIcon, contentDescription = null)
            }
        }
        rule.onNodeWithContentDescription("Close").assert(isButton).assertHasClickAction().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun everySizeKeepsA48dpTouchTargetAroundItsSquare() {
        rule.setMonarchContent {
            Column {
                MonarchButtonSize.entries.forEach { size ->
                    MonarchIconButton(
                        label = size.name,
                        onClick = {},
                        size = size,
                        modifier = Modifier.testTag(size.name),
                    ) {
                        MonarchIcon(imageVector = TestIcon, contentDescription = null)
                    }
                }
            }
        }
        MonarchButtonSize.entries.forEach { rule.onNodeWithTag(it.name).assertTouchTargetAtLeast48dp(rule.density) }
        assertEquals(32f, MonarchButtonTokens.iconButtonSide(MonarchButtonSize.Sm).value)
        assertEquals(36f, MonarchButtonTokens.iconButtonSide(MonarchButtonSize.Md).value)
        assertEquals(40f, MonarchButtonTokens.iconButtonSide(MonarchButtonSize.Lg).value)
    }

    @Test
    fun aDisabledIconButtonIsDisabledAndIgnoresTaps() {
        var clicks = 0
        rule.setMonarchContent {
            MonarchIconButton(label = "Delete", onClick = { clicks++ }, enabled = false) {
                MonarchIcon(imageVector = TestIcon, contentDescription = null)
            }
        }
        rule.onNodeWithContentDescription("Delete").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun aLoadingIconButtonKeepsItsNameAnnouncesTheStateAndIgnoresTaps() {
        var clicks = 0
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchIconButton(label = "Refresh", onClick = { clicks++ }, loading = true) {
                MonarchIcon(imageVector = TestIcon, contentDescription = null)
            }
        }
        rule.mainClock.advanceTimeByFrame()
        val button = rule.onNodeWithContentDescription("Refresh")
        button.assertContentDescriptionEquals("Refresh")
        assertEquals("Loading", button.stateDescription())
        button.performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun aToggleIconButtonIsACheckboxThatSaysWhetherItIsOnAndKeepsTheTarget() {
        var muted by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchIconButton(label = "Mute", checked = muted, onCheckedChange = { muted = it }) {
                MonarchIcon(imageVector = TestIcon, contentDescription = null)
            }
        }
        val button = rule.onNodeWithContentDescription("Mute")
        button.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).assertIsOff()
        button.assertTouchTargetAtLeast48dp(rule.density)
        button.performClick()
        assertTrue(muted)
        button.assertIsOn()
        button.performClick()
        assertFalse(muted)
    }

    @Test
    fun aDisabledToggleIconButtonIgnoresTaps() {
        var changes = 0
        rule.setMonarchContent {
            MonarchIconButton(label = "Bold", checked = true, onCheckedChange = { changes++ }, enabled = false) {
                MonarchIcon(imageVector = TestIcon, contentDescription = null)
            }
        }
        rule.onNodeWithContentDescription("Bold").assertIsNotEnabled().assertIsOn().performClick()
        assertEquals(0, changes)
    }

    @Test
    fun theToggleOnLookFollowsTheToneAndTheOutlineAddsAHairline() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val accent = toggleLook(c, checked = true, enabled = true, highlighted = false)
            assertEquals(c.accentSubtle, accent.fill, theme.id)
            assertEquals(c.accentStrong, accent.content, theme.id)
            assertNull(accent.border, theme.id)
            val ai = toggleLook(
                c, checked = true, enabled = true, highlighted = false, tone = MonarchTone.Ai, outlined = true,
            )
            assertEquals(c.aiSubtle, ai.fill, theme.id)
            assertEquals(c.aiStrong, ai.content, theme.id)
            assertEquals(c.aiBorder, ai.border, theme.id)
            val off = toggleLook(c, checked = false, enabled = true, highlighted = false, outlined = true)
            assertEquals(c.bgSurface, off.fill, theme.id)
            assertEquals(c.textMuted, off.content, theme.id)
            assertEquals(c.borderDefault, off.border, theme.id)
            val disabled = toggleLook(c, checked = false, enabled = false, highlighted = false, outlined = true)
            assertEquals(c.textDisabled, disabled.content, theme.id)
            assertEquals(c.borderSubtle, disabled.border, theme.id)
        }
    }

    @Test
    fun anOutlinedAiToggleKeepsItsNameStateAndTarget() {
        rule.setMonarchContent {
            MonarchIconButton(
                label = "Assistant",
                checked = true,
                onCheckedChange = {},
                tone = MonarchTone.Ai,
                outlined = true,
                modifier = Modifier.testTag("ai"),
            ) { MonarchIcon(imageVector = TestIcon, contentDescription = null) }
        }
        rule.onNodeWithContentDescription("Assistant").assertIsOn()
        rule.onNodeWithTag("ai").assertTouchTargetAtLeast48dp(rule.density)
    }
}

internal object MonarchIconButtonFixtures : ComponentFixtures {
    override val id = "icon-button"
    override val fixtures = listOf(
        fixture("toggle icon buttons, outlined, accent and AI") {
            Column {
                MonarchIconButton(label = "Mute", checked = false, onCheckedChange = {}, outlined = true) {
                    MonarchIcon(imageVector = TestIcon, contentDescription = null)
                }
                MonarchIconButton(label = "Camera", checked = true, onCheckedChange = {}, outlined = true) {
                    MonarchIcon(imageVector = TestIcon, contentDescription = null)
                }
                MonarchIconButton(
                    label = "Assistant",
                    checked = true,
                    onCheckedChange = {},
                    tone = MonarchTone.Ai,
                    outlined = true,
                ) { MonarchIcon(imageVector = TestIcon, contentDescription = null) }
            }
        },
        fixture("icon buttons, every variant") {
            Column {
                MonarchButtonVariant.entries.forEach { variant ->
                    MonarchIconButton(label = variant.name, onClick = {}, variant = variant) {
                        MonarchIcon(imageVector = TestIcon, contentDescription = null)
                    }
                }
            }
        },
    )
}
