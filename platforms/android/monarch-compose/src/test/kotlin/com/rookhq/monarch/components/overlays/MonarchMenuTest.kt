package com.rookhq.monarch.components.overlays

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuDivider
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.components.overlays.menu.MonarchMenuLabel
import com.rookhq.monarch.components.overlays.menu.MonarchMenuTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object MonarchMenuFixtures : ComponentFixtures {
    override val id = "dropdown-menu"
    override val fixtures = listOf(
        fixture("icons, shortcut, choice, disabled and destructive") {
            Box {
                MonarchButton(text = "Actions", onClick = {})
                MonarchMenu(expanded = true, onDismissRequest = {}) {
                    MonarchMenuItem(
                        text = "Rename",
                        onClick = {},
                        leadingIcon = { MonarchIcon(Icons.Outlined.Edit, contentDescription = null) },
                    )
                    MonarchMenuItem(text = "Duplicate", onClick = {}, shortcut = "Ctrl+D")
                    MonarchMenuItem(text = "Move", onClick = {}, enabled = false)
                    MonarchMenuDivider()
                    MonarchMenuLabel(text = "Sort by")
                    MonarchMenuItem(text = "Newest first", onClick = {}, selected = true)
                    MonarchMenuItem(text = "Oldest first", onClick = {}, selected = false)
                    MonarchMenuDivider()
                    MonarchMenuItem(
                        text = "Delete",
                        onClick = {},
                        destructive = true,
                        leadingIcon = { MonarchIcon(Icons.Outlined.Delete, contentDescription = null) },
                    )
                }
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchMenuTest {
    @get:Rule
    val rule = createComposeRule()

    private fun menu(onRename: () -> Unit = {}) {
        rule.setMonarchContent {
            var open by remember { mutableStateOf(false) }
            Box {
                MonarchButton(text = "Actions", onClick = { open = true })
                MonarchMenu(expanded = open, onDismissRequest = { open = false }) {
                    MonarchMenuItem(text = "Rename", onClick = { onRename(); open = false })
                    MonarchMenuItem(text = "Move", onClick = {}, enabled = false)
                    MonarchMenuLabel(text = "Sort by")
                    MonarchMenuItem(text = "Newest first", onClick = {}, selected = true)
                    MonarchMenuItem(text = "Oldest first", onClick = {}, selected = false)
                    MonarchMenuItem(text = "Delete", onClick = {}, destructive = true)
                }
            }
        }
        rule.onNodeWithText("Actions").performClick()
    }

    @Test
    fun theMenuOpensInAPopupAndAnItemRunsItsActionAndCloses() {
        var renamed = 0
        menu(onRename = { renamed++ })
        rule.onNode(hasText("Rename") and hasAnyAncestor(isPopup())).assertIsDisplayed()
        rule.onNodeWithText("Rename").assertHasClickAction().performClick()
        rule.waitForIdle()
        assertEquals(1, renamed)
        rule.onNodeWithText("Rename").assertDoesNotExist()
    }

    @Test
    fun everyRowHasA48dpTarget() {
        menu()
        listOf("Rename", "Newest first", "Oldest first", "Delete").forEach {
            rule.onNodeWithText(it).assertTouchTargetAtLeast48dp(rule.density)
        }
    }

    @Test
    fun optionsSaySelectedAndDisabledRowsAreNotEnabled() {
        menu()
        rule.onNodeWithText("Newest first").assertIsSelected()
        rule.onNodeWithText("Oldest first").assertIsNotSelected()
        rule.onNodeWithText("Move").assertIsNotEnabled()
        rule.onNode(hasText("Sort by") and isHeading()).assertExists()
    }

    @Test
    fun aRowWithItsOwnContentReadsAsOneSelectableItemInTheRowStyle() {
        var chosen = ""
        rule.setMonarchContent {
            var open by remember { mutableStateOf(true) }
            Box {
                MonarchMenu(expanded = open, onDismissRequest = { open = false }) {
                    MonarchMenuItem(
                        onClick = { chosen = "acme"; open = false },
                        selected = true,
                        trailingContent = { Text("12") },
                    ) {
                        Text("Acme")
                        Text("@acme")
                    }
                    MonarchMenuItem(onClick = {}, enabled = false) { Text("Archived") }
                }
            }
        }
        rule.onNode(hasText("Acme") and hasText("@acme") and hasText("12")).assertIsSelected()
            .assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Archived").assertIsNotEnabled()
        rule.onNodeWithText("Acme").performClick()
        rule.waitForIdle()
        assertEquals("acme", chosen)
        rule.onNodeWithText("Acme").assertDoesNotExist()
    }

    @Test
    fun backClosesTheMenu() {
        menu()
        rule.onNodeWithText("Rename").assertIsDisplayed()
        Espresso.pressBack()
        rule.waitForIdle()
        rule.onNodeWithText("Rename").assertDoesNotExist()
    }

    @Test
    fun longTextWrapsAtTheLargestFontSize() {
        val text = "Move to another workspace"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Box {
                MonarchMenu(expanded = true, onDismissRequest = {}) {
                    MonarchMenuItem(text = text, onClick = {}, shortcut = "Ctrl+M")
                }
            }
        }
        rule.onNodeWithText(text).assertTextFullyVisible()
    }

    @Test
    fun theShortcutFollowsTheTextRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchMenuItem(
                text = "Rename",
                onClick = {},
                shortcut = "Ctrl+R",
            )
        }
        rule.onNodeWithText("Rename", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Ctrl+R", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun rowColoursFollowTheStateAndTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val rest = MonarchMenuTokens.itemColors(colors, enabled = true, destructive = false, highlighted = false)
            assertEquals(colors.textDefault, rest.text, theme.id)
            assertEquals(colors.textMuted, rest.icon, theme.id)
            assertEquals(Color.Transparent, rest.highlight, theme.id)
            val pressed = MonarchMenuTokens.itemColors(colors, enabled = true, destructive = false, highlighted = true)
            assertEquals(colors.bgMuted, pressed.highlight, theme.id)
            val danger = MonarchMenuTokens.itemColors(colors, enabled = true, destructive = true, highlighted = true)
            assertEquals(colors.dangerStrong, danger.text, theme.id)
            assertEquals(colors.dangerSubtle, danger.highlight, theme.id)
            val disabled = MonarchMenuTokens.itemColors(colors, enabled = false, destructive = true, highlighted = true)
            assertEquals(colors.textDisabled, disabled.text, theme.id)
            assertEquals(Color.Transparent, disabled.highlight, theme.id)
            assertEquals(colors.borderSubtle, MonarchMenuTokens.border(colors, dark = false, highContrast = false))
            assertEquals(colors.borderDefault, MonarchMenuTokens.border(colors, dark = true, highContrast = false))
            assertEquals(colors.borderStrong, MonarchMenuTokens.border(colors, dark = true, highContrast = true))
        }
        assertTrue(MonarchMenuTokens.RowTarget - MonarchMenuTokens.Inset * 2 == 40.dp)
    }
}
