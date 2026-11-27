package com.rookhq.monarch.components.datadisplay

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.datadisplay.treeview.MonarchTreeView
import com.rookhq.monarch.components.datadisplay.treeview.MonarchTreeViewItem
import com.rookhq.monarch.components.datadisplay.treeview.MonarchTreeViewLabels
import com.rookhq.monarch.components.datadisplay.treeview.MonarchTreeViewSelectionMode
import com.rookhq.monarch.components.datadisplay.treeview.MonarchTreeViewTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val chevron: @Composable () -> Unit = { MonarchIcon(TestIcon, null, size = MonarchIconSize.Sm) }

private val tree = listOf(
    MonarchTreeViewItem(
        id = "eng",
        label = "Engineering",
        description = "12 people",
        children = listOf(
            MonarchTreeViewItem(id = "web", label = "Web"),
            MonarchTreeViewItem(id = "mobile", label = "Mobile", hasChildren = true),
        ),
    ),
    MonarchTreeViewItem(id = "sales", label = "Sales", children = emptyList()),
    MonarchTreeViewItem(id = "legal", label = "Legal", enabled = false),
)

internal object MonarchTreeViewFixtures : ComponentFixtures {
    override val id = "tree-view"
    override val fixtures = listOf(
        fixture("collapsed") { MonarchTreeView(tree, emptySet(), {}, chevron) },
        fixture("expanded with a selection") {
            MonarchTreeView(tree, setOf("eng"), {}, chevron, selected = setOf("web"), current = "eng")
        },
        fixture("loading children") {
            MonarchTreeView(tree.take(1), setOf("eng", "mobile"), {}, chevron, loading = setOf("mobile"))
        },
        fixture("multiple, with icons and meta") {
            MonarchTreeView(
                tree.take(2),
                setOf("eng"),
                {},
                chevron,
                selectionMode = MonarchTreeViewSelectionMode.Multiple,
                selected = setOf("web", "sales"),
                icon = { MonarchIcon(TestIcon, null, size = MonarchIconSize.Sm) },
                meta = { Text("3") },
            )
        },
        fixture("no selection") {
            MonarchTreeView(tree.take(2), emptySet(), {}, chevron, selectionMode = MonarchTreeViewSelectionMode.None)
        },
        fixture("long text") {
            MonarchTreeView(
                listOf(
                    MonarchTreeViewItem(
                        id = "a",
                        label = "Customer success and account management for enterprise customers",
                        description = "Department · 48 people in four offices",
                        children = emptyList(),
                    ),
                ),
                emptySet(),
                {},
                chevron,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchTreeViewTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTreeIsFlattenedInReadingOrderWithLevels() {
        val rows = MonarchTreeViewTokens.flatten(tree, setOf("eng", "mobile"), setOf("mobile"))
        assertEquals(listOf("eng", "web", "mobile", "loading:mobile", "sales", "legal"), rows.map { it.key })
        assertEquals(listOf(1, 2, 2, 3, 1, 1), rows.map { it.level })
        assertEquals("eng", rows[1].parentId)
        val collapsed = MonarchTreeViewTokens.flatten(tree.take(2), emptySet(), emptySet())
        assertEquals(listOf("eng", "sales"), collapsed.map { it.key })
        assertEquals(setOf("eng", "mobile", "sales"), MonarchTreeViewTokens.parentIds(tree))
    }

    @Test
    fun selectionReplacesOrToggles() {
        val t = MonarchTreeViewTokens
        assertEquals(setOf("b"), t.select(MonarchTreeViewSelectionMode.Single, setOf("a"), "b"))
        assertEquals(setOf("a", "b"), t.select(MonarchTreeViewSelectionMode.Multiple, setOf("a"), "b"))
        assertEquals(setOf("b"), t.select(MonarchTreeViewSelectionMode.Multiple, setOf("a", "b"), "a"))
        assertEquals(setOf("a"), t.select(MonarchTreeViewSelectionMode.None, setOf("a"), "b"))
    }

    @Test
    fun theExpandButtonOpensAParentAndIsNamedAfterIt() {
        var expanded by mutableStateOf(emptySet<String>())
        rule.setMonarchContent { MonarchTreeView(tree, expanded, { expanded = it }, chevron) }
        rule.onNodeWithText("Web").assertDoesNotExist()
        rule.onNodeWithContentDescription("Expand Engineering")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(setOf("eng"), expanded)
        rule.onNodeWithText("Web").assertExists()
        rule.onNodeWithContentDescription("Collapse Engineering").performClick()
        assertEquals(emptySet(), expanded)
    }

    @Test
    fun eachRowReadsItsStateAndLevelAndOffersExpandAndCollapse() {
        var expanded by mutableStateOf(emptySet<String>())
        rule.setMonarchContent { MonarchTreeView(tree, expanded, { expanded = it }, chevron) }
        val engineering = rule.onNodeWithText("Engineering")
        assertEquals("Collapsed, Level 1", engineering.stateDescription())
        engineering.performSemanticsAction(SemanticsActions.Expand)
        assertEquals(setOf("eng"), expanded)
        assertEquals("Expanded, Level 1", rule.onNodeWithText("Engineering").stateDescription())
        assertEquals("Level 2", rule.onNodeWithText("Web").stateDescription())
        rule.onNodeWithText("Engineering").performSemanticsAction(SemanticsActions.Collapse)
        assertEquals(emptySet(), expanded)
    }

    @Test
    fun tappingAnItemSelectsAndActivatesIt() {
        var selected by mutableStateOf(setOf("sales"))
        val activated = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchTreeView(
                tree,
                setOf("eng"),
                {},
                chevron,
                selected = selected,
                onSelectedChange = { selected = it },
                onActivate = { activated += it.id },
            )
        }
        rule.onNodeWithText("Sales").assertIsSelected()
        rule.onNodeWithText("Web").assertIsNotSelected().assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(setOf("web"), selected)
        assertEquals(listOf("web"), activated)
        rule.onNodeWithText("Web").assertIsSelected()
    }

    @Test
    fun multipleSelectionToggles() {
        var selected by mutableStateOf(setOf("sales"))
        rule.setMonarchContent {
            MonarchTreeView(
                tree,
                emptySet(),
                {},
                chevron,
                selectionMode = MonarchTreeViewSelectionMode.Multiple,
                selected = selected,
                onSelectedChange = { selected = it },
            )
        }
        rule.onNodeWithText("Engineering").performClick()
        rule.onNodeWithText("Sales").performClick()
        assertEquals(setOf("eng"), selected)
    }

    @Test
    fun withoutSelectionATapOnAParentOpensIt() {
        var expanded by mutableStateOf(emptySet<String>())
        rule.setMonarchContent {
            MonarchTreeView(
                tree,
                expanded,
                { expanded = it },
                chevron,
                selectionMode = MonarchTreeViewSelectionMode.None,
            )
        }
        rule.onNodeWithText("Engineering")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
            .performClick()
        assertEquals(setOf("eng"), expanded)
    }

    @Test
    fun aDisabledItemCannotBeSelected() {
        var selected by mutableStateOf(emptySet<String>())
        rule.setMonarchContent {
            MonarchTreeView(tree, emptySet(), {}, chevron, selected = selected, onSelectedChange = { selected = it })
        }
        rule.onNodeWithText("Legal").assertIsNotEnabled().performClick()
        assertEquals(emptySet(), selected)
    }

    @Test
    fun openingAnUnloadedParentAsksForItsChildrenAndShowsLoading() {
        var expanded by mutableStateOf(setOf("eng"))
        var loading by mutableStateOf(emptySet<String>())
        val asked = mutableListOf<String>()
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchTreeView(
                tree,
                expanded,
                { expanded = it },
                chevron,
                loading = loading,
                onLoadChildren = {
                    asked += it.id
                    loading = loading + it.id
                },
            )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Expand Mobile").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(listOf("mobile"), asked)
        rule.onNodeWithText("Loading", useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theArrowKeysOpenAndCloseAParent() {
        var expanded by mutableStateOf(emptySet<String>())
        lateinit var inputMode: InputModeManager
        rule.setMonarchContent {
            inputMode = LocalInputModeManager.current
            MonarchTreeView(tree, expanded, { expanded = it }, chevron)
        }
        // With a hardware keyboard the app leaves touch mode, where clickable elements are not focusable.
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        val engineering = rule.onNodeWithText("Engineering")
        engineering.requestFocus()
        engineering.assertIsFocused()
        engineering.performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(setOf("eng"), expanded)
        rule.onNodeWithText("Engineering").performKeyInput { pressKey(Key.DirectionLeft) }
        assertEquals(emptySet(), expanded)
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchTreeView(
                tree,
                emptySet(),
                {},
                chevron,
                labels = MonarchTreeViewLabels(
                    expand = { "Open $it" },
                    collapsed = "Closed",
                    level = { "Depth $it" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Open Engineering").assertExists()
        assertEquals("Closed, Depth 1", rule.onNodeWithText("Engineering").stateDescription())
    }

    @Test
    fun theExpandButtonComesFirstInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchTreeView(tree, emptySet(), {}, chevron)
        }
        rule.onNodeWithContentDescription("Expand Engineering")
            .assertIsBefore(rule.onNodeWithText("Engineering"), LayoutDirection.Rtl)
    }

    @Test
    fun longLabelsWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchTreeView(
                listOf(MonarchTreeViewItem(id = "a", label = "Customer success and account management")),
                emptySet(),
                {},
                chevron,
            )
        }
        rule.onNodeWithText("Customer success", substring = true, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun rowColoursComeFromTheRolesInEveryTheme() {
        val t = MonarchTreeViewTokens
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val selected = t.colors(selected = true, current = false, enabled = true, colors = colors)
            assertEquals(colors.accentSubtle, selected.container, theme.id)
            assertEquals(colors.accentStrong, selected.text, theme.id)
            assertEquals(colors.accentSolid, selected.bar, theme.id)
            val current = t.colors(selected = false, current = true, enabled = true, colors = colors)
            assertEquals(Color.Transparent, current.container, theme.id)
            assertEquals(colors.accentStrong, current.text, theme.id)
            val disabled = t.colors(selected = true, current = false, enabled = false, colors = colors)
            assertEquals(colors.textDisabled, disabled.text, theme.id)
            assertEquals(null, disabled.bar, theme.id)
            assertEquals(colors.textMuted, t.iconColor(enabled = true, colors = colors), theme.id)
        }
        assertTrue(t.needsLoad(tree[0].children!![1], emptySet()))
        assertFalse(t.needsLoad(tree[0], emptySet()))
    }
}
