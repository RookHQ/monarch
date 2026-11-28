package dev.monarchui.components.typography

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import dev.monarchui.components.typography.kbd.MonarchKbd
import dev.monarchui.components.typography.kbd.MonarchKbdLabels
import dev.monarchui.components.typography.kbd.MonarchKbdTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

internal object MonarchKbdFixtures : ComponentFixtures {
    override val id = "kbd"
    override val fixtures = listOf(
        fixture("glyphs and words, always visible") {
            Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchKbd(keys = "⌘K", alwaysVisible = true)
                MonarchKbd(keys = "⇧⌘P", alwaysVisible = true)
                MonarchKbd(keys = "Esc", alwaysVisible = true)
            }
        },
        fixture("with a hardware keyboard") { WithKeyboard(attached = true) { MonarchKbd(keys = "Ctrl+K") } },
        fixture("empty keys") { MonarchKbd(keys = "", alwaysVisible = true) },
    )
}

/** Provides a configuration with or without an attached hardware keyboard. */
@Composable
internal fun WithKeyboard(attached: Boolean, content: @Composable () -> Unit) {
    val configuration = Configuration(LocalConfiguration.current).apply {
        keyboard = if (attached) Configuration.KEYBOARD_QWERTY else Configuration.KEYBOARD_NOKEYS
        hardKeyboardHidden =
            if (attached) Configuration.HARDKEYBOARDHIDDEN_NO else Configuration.HARDKEYBOARDHIDDEN_YES
    }
    CompositionLocalProvider(LocalConfiguration provides configuration, content = content)
}

@RunWith(RobolectricTestRunner::class)
class MonarchKbdTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theCapIsDrawnOnlyWhileAHardwareKeyboardIsAttached() {
        rule.setMonarchContent {
            Row {
                WithKeyboard(attached = false) { MonarchKbd(keys = "A", modifier = Modifier.testTag("none")) }
                WithKeyboard(attached = true) { MonarchKbd(keys = "B", modifier = Modifier.testTag("attached")) }
                WithKeyboard(attached = false) {
                    MonarchKbd(keys = "C", modifier = Modifier.testTag("always"), alwaysVisible = true)
                }
            }
        }
        rule.onNodeWithTag("none").assertDoesNotExist()
        rule.onNodeWithTag("attached").assertExists()
        rule.onNodeWithTag("always").assertExists()
    }

    @Test
    fun keyGlyphsAreReadByNameAndIsNotActionable() {
        rule.setMonarchContent {
            MonarchKbd(keys = "⇧⌘P", modifier = Modifier.testTag("kbd"), alwaysVisible = true)
        }
        rule.onNodeWithTag("kbd").assertContentDescriptionEquals("Shift Command P").assertHasNoClickAction()
    }

    @Test
    fun wordsAreReadAsWrittenAndLabelsReplaceTheBuiltInNames() {
        val names = mapOf('⌘' to "Cmd", '⇧' to "Shift")
        assertEquals("Ctrl+K", MonarchKbdTokens.spokenKeys("Ctrl+K", names))
        assertEquals("Esc", MonarchKbdTokens.spokenKeys("Esc", names))
        assertEquals("Cmd K", MonarchKbdTokens.spokenKeys("⌘K", names))
        assertEquals("", MonarchKbdTokens.spokenKeys("", names))
        rule.setMonarchContent {
            MonarchKbd(
                keys = "⌘↩",
                modifier = Modifier.testTag("kbd"),
                alwaysVisible = true,
                labels = MonarchKbdLabels(command = "Befehl"),
            )
        }
        rule.onNodeWithTag("kbd").assertContentDescriptionEquals("Befehl Return")
    }

    @Test
    fun theTextIsNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchKbd(keys = "⇧⌘P", alwaysVisible = true) }
        rule.onNodeWithText("⇧⌘P", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheSurfaceRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val cap = MonarchKbdTokens.colors(colors)
            assertEquals(colors.bgSubtle, cap.container, theme.id)
            assertEquals(colors.textMuted, cap.content, theme.id)
            assertEquals(colors.borderDefault, cap.border, theme.id)
        }
    }
}
