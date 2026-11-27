package com.rookhq.monarch.components.files

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.files.editortoolbar.EditorToolbarItem
import com.rookhq.monarch.components.files.editortoolbar.MonarchEditorTool
import com.rookhq.monarch.components.files.editortoolbar.MonarchEditorToolbar
import com.rookhq.monarch.components.files.editortoolbar.MonarchEditorToolbarLabels
import com.rookhq.monarch.components.files.editortoolbar.MonarchEditorToolbarTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.onNodeWithTextOrDescription
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

internal object MonarchEditorToolbarFixtures : ComponentFixtures {
    override val id = "editor-toolbar"
    override val fixtures = listOf(
        fixture("default groups with active and disabled tools") {
            MonarchEditorToolbar(
                onAction = {},
                active = setOf(MonarchEditorTool.Paragraph, MonarchEditorTool.Bold),
                disabled = setOf(MonarchEditorTool.Redo),
            )
        },
        fixture("inline tools with icons") {
            val icon = rememberVectorPainter(TestIcon)
            MonarchEditorToolbar(
                onAction = {},
                groups = MonarchEditorTool.InlineGroups + listOf(listOf(MonarchEditorTool.Undo)),
                active = setOf(MonarchEditorTool.Italic),
                icons = (MonarchEditorTool.InlineGroups.flatten() + MonarchEditorTool.Undo).associateWith { icon },
                onLink = {},
            )
        },
        fixture("disabled") {
            MonarchEditorToolbar(onAction = {}, groups = MonarchEditorTool.InlineGroups, enabled = false)
        },
        fixture("custom tools") {
            MonarchEditorToolbar(
                onAction = {},
                groups = listOf(
                    listOf(MonarchEditorTool.heading(1), MonarchEditorTool.heading(2)),
                    listOf(MonarchEditorTool.custom("mention", "Mention someone")),
                ),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchEditorToolbarTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun formatsAreTogglesThatCallBack() {
        val pressed = mutableListOf<MonarchEditorTool>()
        rule.setMonarchContent {
            MonarchEditorToolbar(
                onAction = { pressed += it },
                groups = MonarchEditorTool.InlineGroups,
                active = setOf(MonarchEditorTool.Bold),
            )
        }
        rule.onNodeWithContentDescription("Formatting").assertExists()
        rule.onNodeWithTextOrDescription("Bold").assertIsOn().assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithTextOrDescription("Italic").assertIsOff().performClick()
        assertEquals(listOf(MonarchEditorTool.Bold, MonarchEditorTool.Italic), pressed)
    }

    @Test
    fun aToolWithAnIconIsAnIconToggleNamedByTheTool() {
        var pressed: MonarchEditorTool? = null
        rule.setMonarchContent {
            val icon = rememberVectorPainter(TestIcon)
            MonarchEditorToolbar(
                onAction = { pressed = it },
                groups = listOf(listOf(MonarchEditorTool.Bold, MonarchEditorTool.Undo)),
                active = setOf(MonarchEditorTool.Bold),
                icons = mapOf(MonarchEditorTool.Bold to icon, MonarchEditorTool.Undo to icon),
            )
        }
        rule.onNodeWithContentDescription("Bold").assertIsOn().assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithContentDescription("Undo").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(MonarchEditorTool.Undo, pressed)
    }

    @Test
    fun disabledToolsAndADisabledToolbarCannotBeUsed() {
        var enabled by mutableStateOf(true)
        var pressed = 0
        rule.setMonarchContent {
            MonarchEditorToolbar(
                onAction = { pressed++ },
                groups = listOf(listOf(MonarchEditorTool.Undo, MonarchEditorTool.Redo, MonarchEditorTool.Bold)),
                disabled = setOf(MonarchEditorTool.Redo),
                enabled = enabled,
            )
        }
        rule.onNodeWithTextOrDescription("Redo").assertIsNotEnabled().performClick()
        rule.onNodeWithTextOrDescription("Undo").assertIsEnabled()
        enabled = false
        rule.onNodeWithTextOrDescription("Undo").assertIsNotEnabled()
        rule.onNodeWithTextOrDescription("Bold").assertIsNotEnabled().performClick()
        assertEquals(0, pressed)
    }

    @Test
    fun neighbouringTextStylesAreOneMenuWithTheCurrentStyle() {
        var pressed: MonarchEditorTool? = null
        rule.setMonarchContent {
            MonarchEditorToolbar(
                onAction = { pressed = it },
                groups = listOf(
                    listOf(MonarchEditorTool.Paragraph, MonarchEditorTool.heading(1), MonarchEditorTool.heading(2)),
                ),
                active = setOf(MonarchEditorTool.heading(2)),
                textStyleMenu = true,
            )
        }
        val menu = rule.onNodeWithContentDescription("Text style")
        assertEquals("Heading 2", menu.stateDescription())
        menu.assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNode(hasText("Heading 2") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected))
            .assertIsSelected()
        rule.onNodeWithTextOrDescription("Heading 1").assertIsNotSelected().performClick()
        assertEquals(MonarchEditorTool.heading(1), pressed)
        rule.onNodeWithTextOrDescription("Paragraph").assertDoesNotExist()
    }

    @Test
    fun theLinkFormAppliesOnlyASafeAddress() {
        var link: String? = "unset"
        var actions = 0
        rule.setMonarchContent {
            MonarchEditorToolbar(
                onAction = { actions++ },
                groups = MonarchEditorTool.InlineGroups,
                onLink = { link = it },
            )
        }
        rule.onNodeWithTextOrDescription("Link").performClick()
        val apply = rule.onNodeWithTextOrDescription("Apply")
        apply.assertIsNotEnabled()
        rule.onNode(hasSetTextAction()).performTextInput("javascript:alert(1)")
        apply.assertIsNotEnabled()
        rule.onNodeWithTextOrDescription("Remove link").assertDoesNotExist()
        rule.onNodeWithTextOrDescription("Cancel").performClick()
        rule.onNodeWithTextOrDescription("Apply").assertDoesNotExist()
        rule.onNodeWithTextOrDescription("Link").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("acme.com")
        rule.onNodeWithTextOrDescription("Apply").assertIsEnabled().performClick()
        assertEquals("https://acme.com", link)
        assertEquals(0, actions)
    }

    @Test
    fun anActiveLinkCanBeRemoved() {
        var link: String? = "unset"
        rule.setMonarchContent {
            MonarchEditorToolbar(
                onAction = {},
                groups = listOf(listOf(MonarchEditorTool.Link)),
                active = setOf(MonarchEditorTool.Link),
                onLink = { link = it },
            )
        }
        rule.onNodeWithTextOrDescription("Link").performClick()
        rule.onNodeWithTextOrDescription("Remove link").performClick()
        assertNull(link)
    }

    @Test
    fun withoutOnLinkTheLinkToolIsAnAction() {
        var pressed: MonarchEditorTool? = null
        rule.setMonarchContent {
            MonarchEditorToolbar(onAction = { pressed = it }, groups = listOf(listOf(MonarchEditorTool.Link)))
        }
        rule.onNodeWithTextOrDescription("Link").performClick()
        assertEquals(MonarchEditorTool.Link, pressed)
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchEditorToolbar(
                onAction = {},
                groups = listOf(listOf(MonarchEditorTool.Bold, MonarchEditorTool.custom("mention", "Mention"))),
                labels = MonarchEditorToolbarLabels(
                    toolbar = "Text tools",
                    tools = mapOf(MonarchEditorTool.Bold to "Strong"),
                ),
            )
        }
        rule.onNodeWithContentDescription("Text tools").assertExists()
        rule.onNodeWithTextOrDescription("Strong").assertExists()
        rule.onNodeWithTextOrDescription("Mention").assertExists()
    }

    @Test
    fun theToolsMirrorRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchEditorToolbar(onAction = {}, groups = listOf(listOf(MonarchEditorTool.Undo, MonarchEditorTool.Redo)))
        }
        rule.onNodeWithTextOrDescription("Undo")
            .assertIsBefore(rule.onNodeWithTextOrDescription("Redo"), LayoutDirection.Rtl)
    }

    @Test
    fun namesAreNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchEditorToolbar(onAction = {}, groups = listOf(listOf(MonarchEditorTool.Bold, MonarchEditorTool.Undo)))
        }
        // The tools are icon buttons named by their description, so there is no text to cut off.
        rule.onNodeWithTextOrDescription("Bold").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithTextOrDescription("Undo").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theRulesOfTheToolbar() {
        val t = MonarchEditorToolbarTokens
        val items = t.items(
            listOf(
                emptyList(),
                listOf(
                    MonarchEditorTool.Paragraph,
                    MonarchEditorTool.heading(1),
                    MonarchEditorTool.Bold,
                    MonarchEditorTool.heading(2),
                ),
            ),
        )
        assertEquals(1, items.size)
        assertEquals(
            listOf(
                EditorToolbarItem.StyleMenu(listOf(MonarchEditorTool.Paragraph, MonarchEditorTool.heading(1))),
                EditorToolbarItem.Single(MonarchEditorTool.Bold),
                EditorToolbarItem.StyleMenu(listOf(MonarchEditorTool.heading(2))),
            ),
            items.single(),
        )
        assertEquals(6, MonarchEditorTool.heading(9).level)
        assertEquals(MonarchEditorTool.heading(2), MonarchEditorTool.heading(2))
        assertEquals("custom:mention", MonarchEditorTool.custom("mention", "Mention").id)
        assertTrue(t.isToggle(MonarchEditorTool.CodeBlock))
        assertEquals("https://acme.com", t.safeLink(" acme.com "))
        assertEquals("https://acme.com/docs?a=1", t.safeLink("https://acme.com/docs?a=1"))
        assertEquals("mailto:sam@acme.com", t.safeLink("sam@acme.com"))
        assertEquals("http://acme.com", t.safeLink("http://acme.com"))
        assertNull(t.safeLink("javascript:alert(1)"))
        assertNull(t.safeLink("data:text/html,hi"))
        assertNull(t.safeLink("ftp://acme.com"))
        assertNull(t.safeLink("not a link"))
        assertNull(t.safeLink("https://"))
        assertNull(t.safeLink(""))
        assertNull(t.safeLink("acme"))
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.bgSurface, MonarchEditorToolbarTokens.container(colors), theme.id)
            assertEquals(colors.borderSubtle, MonarchEditorToolbarTokens.divider(colors), theme.id)
        }
    }
}
