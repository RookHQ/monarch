package dev.monarchui.components.datadisplay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.datadisplay.tag.MonarchTag
import dev.monarchui.components.datadisplay.tag.MonarchTagSize
import dev.monarchui.components.datadisplay.tag.MonarchTagTokens
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
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

internal object MonarchTagFixtures : ComponentFixtures {
    override val id = "tag"
    override val fixtures = MonarchTagSize.entries.flatMap { size ->
        listOf(
            fixture("read-only, every tone, ${size.name}") {
                Column {
                    MonarchTone.entries.forEach { tone -> MonarchTag(text = tone.name, tone = tone, size = size) }
                }
            },
            fixture("removable, every tone, ${size.name}") {
                Column {
                    MonarchTone.entries.forEach { tone ->
                        MonarchTag(
                            text = tone.name,
                            onRemove = {},
                            removeIcon = null,
                            tone = tone,
                            size = size,
                        )
                    }
                }
            },
            fixture("selectable, on and off, ${size.name}") {
                Column {
                    MonarchTag(text = "On", selected = true, onSelectedChange = {}, size = size)
                    MonarchTag(text = "Off", selected = false, onSelectedChange = {}, size = size)
                }
            },
        )
    } + listOf(
        fixture("disabled") {
            Column {
                MonarchTag(
                    text = "Removable",
                    onRemove = {},
                    removeIcon = null,
                    enabled = false,
                )
                MonarchTag(text = "Selected", selected = true, onSelectedChange = {}, enabled = false)
            }
        },
        fixture("icons") {
            Column {
                MonarchTag(text = "Design", leadingIcon = { MonarchIcon(TestIcon, null, size = MonarchIconSize.Sm) })
                MonarchTag(
                    text = "Open",
                    selected = true,
                    onSelectedChange = {},
                    selectedIcon = { MonarchIcon(TestIcon, null, size = MonarchIconSize.Sm) },
                )
            }
        },
        fixture("long text") {
            MonarchTag(text = "Waiting for a second approval from the finance team before the end of the month")
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchTagTest {
    @get:Rule
    val rule = createComposeRule()

    private val removeIcon = @Composable {
        MonarchIcon(TestIcon, contentDescription = null, size = MonarchIconSize.Sm)
    }

    @Test
    fun aReadOnlyTagIsReadAsItsTextAndIsNotActionable() {
        rule.setMonarchContent {
            MonarchTag(text = "Design", leadingIcon = { MonarchIcon(TestIcon, contentDescription = "Icon") })
        }
        rule.onNodeWithText("Design").assertHasNoClickAction()
        rule.onNodeWithContentDescription("Icon").assertDoesNotExist()
    }

    @Test
    fun aRemovableTagIsAButtonNamedRemoveAndTheText() {
        var removed = 0
        rule.setMonarchContent { MonarchTag(text = "Design", onRemove = { removed++ }, removeIcon = removeIcon) }
        rule.onNodeWithContentDescription("Remove Design")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, removed)
    }

    @Test
    fun theRemoveLabelReplacesTheBuiltInName() {
        rule.setMonarchContent {
            MonarchTag(text = "Design", onRemove = {}, removeIcon = removeIcon, removeLabel = "Delete label Design")
        }
        rule.onNodeWithContentDescription("Delete label Design").assertExists()
        rule.onNodeWithContentDescription("Remove Design").assertDoesNotExist()
    }

    @Test
    fun aDisabledRemovableTagDoesNotCallBack() {
        var removed = 0
        rule.setMonarchContent {
            MonarchTag(text = "Design", onRemove = { removed++ }, removeIcon = removeIcon, enabled = false)
        }
        rule.onNodeWithContentDescription("Remove Design").assertIsNotEnabled().performClick()
        assertEquals(0, removed)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun deleteOnAHardwareKeyboardRemovesAFocusedTag() {
        var removed = 0
        lateinit var inputMode: InputModeManager
        rule.setMonarchContent {
            inputMode = LocalInputModeManager.current
            MonarchTag(text = "Design", onRemove = { removed++ }, removeIcon = removeIcon)
        }
        // Clickable elements take focus only in keyboard mode, as on a device with a hardware keyboard.
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        val tag = rule.onNodeWithContentDescription("Remove Design")
        tag.requestFocus()
        tag.performKeyInput { pressKey(Key.Delete) }
        assertEquals(1, removed)
    }

    @Test
    fun aSelectableTagIsACheckboxThatToggles() {
        var selected = false
        rule.setMonarchContent { MonarchTag(text = "Open", selected = selected, onSelectedChange = { selected = it }) }
        rule.onNodeWithText("Open")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsNotSelected()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(true, selected)
    }

    @Test
    fun aSelectedTagIsSelectedAndHandsBackFalse() {
        var next: Boolean? = null
        rule.setMonarchContent { MonarchTag(text = "Open", selected = true, onSelectedChange = { next = it }) }
        rule.onNodeWithText("Open").assertIsSelected().performClick()
        assertEquals(false, next)
    }

    @Test
    fun aDisabledSelectableTagDoesNotCallBack() {
        var calls = 0
        rule.setMonarchContent {
            MonarchTag(text = "Open", selected = false, onSelectedChange = { calls++ }, enabled = false)
        }
        rule.onNodeWithText("Open").assertIsNotEnabled().performClick()
        assertEquals(0, calls)
    }

    @Test
    fun theIconIsBeforeTheTextInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchTag(text = "Design", leadingIcon = { Box(Modifier.testTag("icon").size(16.dp)) })
        }
        rule.onNodeWithTag("icon", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Design"), LayoutDirection.Rtl)
    }

    @Test
    fun longTextWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchTag(text = "Waiting for a second approval from the finance team")
        }
        rule.onNodeWithText("Waiting", substring = true).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheToneAndAccentRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val danger = MonarchTagTokens.colors(MonarchTone.Danger, colors)
            assertEquals(colors.dangerSubtle, danger.container, theme.id)
            assertEquals(colors.dangerStrong, danger.content, theme.id)
            assertEquals(colors.dangerBorder, danger.border, theme.id)
            val neutral = MonarchTagTokens.colors(MonarchTone.Neutral, colors)
            assertEquals(colors.bgMuted, neutral.container, theme.id)
            assertEquals(colors.textDefault, neutral.content, theme.id)
            val on = MonarchTagTokens.selectableColors(selected = true, enabled = true, colors = colors)
            assertEquals(colors.accentSubtle, on.container, theme.id)
            assertEquals(colors.accentStrong, on.content, theme.id)
            assertEquals(colors.accentSolid, on.border, theme.id)
            val off = MonarchTagTokens.selectableColors(selected = false, enabled = true, colors = colors)
            assertEquals(colors.bgSurface, off.container, theme.id)
            assertEquals(colors.borderDefault, off.border, theme.id)
            val disabled = MonarchTagTokens.selectableColors(selected = true, enabled = false, colors = colors)
            assertEquals(colors.textMuted, disabled.content, theme.id)
            assertEquals(colors.bgMuted, disabled.container, theme.id)
        }
    }
}
