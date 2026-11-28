package dev.monarchui.components.files

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.files.pagetree.MonarchPageTree
import dev.monarchui.components.files.pagetree.MonarchPageTreeGroup
import dev.monarchui.components.files.pagetree.MonarchPageTreeLabels
import dev.monarchui.components.files.pagetree.MonarchPageTreeMove
import dev.monarchui.components.files.pagetree.MonarchPageTreeMoveKind
import dev.monarchui.components.files.pagetree.MonarchPageTreePage
import dev.monarchui.components.files.pagetree.MonarchPageTreeTokens
import dev.monarchui.components.files.pagetree.pageTreeCanDeleteGroup
import dev.monarchui.components.files.pagetree.pageTreeCanDeletePage
import dev.monarchui.components.files.pagetree.pageTreeMoveGroup
import dev.monarchui.components.files.pagetree.pageTreeMovePageTo
import dev.monarchui.components.files.pagetree.pageTreeMovePageWithin
import dev.monarchui.components.files.pagetree.pageTreeNumber
import dev.monarchui.components.files.pagetree.pageTreeNumbers
import dev.monarchui.components.files.pagetree.pageTreeRenamed
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

private val treeGroups = listOf(
    MonarchPageTreeGroup(
        id = "basics",
        label = "Basics",
        pages = listOf(MonarchPageTreePage("welcome", "Welcome"), MonarchPageTreePage("setup", "Set up")),
    ),
    MonarchPageTreeGroup(id = "guides", label = "Guides", pages = listOf(MonarchPageTreePage("billing", "Billing"))),
    MonarchPageTreeGroup(id = "faq", label = "FAQ", pages = emptyList()),
)

internal object MonarchPageTreeFixtures : ComponentFixtures {
    override val id = "page-tree"
    override val fixtures = listOf(
        fixture("navigation") { MonarchPageTree(groups = treeGroups, selected = "setup", onSelect = {}) },
        fixture("without numbers, disabled") {
            MonarchPageTree(
                groups = treeGroups.take(2),
                selected = "welcome",
                onSelect = {},
                numbered = false,
                enabled = false,
            )
        },
        fixture("organiser") {
            MonarchPageTree(
                groups = treeGroups.take(1),
                selected = "welcome",
                onSelect = {},
                onReorder = { _, _ -> },
                onAddPage = {},
                onRenameGroup = { _, _ -> },
                onDeletePage = {},
            )
        },
        fixture("empty group with add, add group") {
            MonarchPageTree(
                groups = treeGroups.drop(2),
                selected = null,
                onSelect = {},
                onAddPage = {},
                onAddGroup = {},
            )
        },
        fixture("untitled and long titles") {
            MonarchPageTree(
                groups = listOf(
                    MonarchPageTreeGroup(
                        id = "g",
                        label = "Appendix with a long name for the group",
                        pages = listOf(
                            MonarchPageTreePage("u", " "),
                            MonarchPageTreePage("l", "A page whose title is long enough to wrap onto two lines"),
                        ),
                    ),
                ),
                selected = "l",
                onSelect = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchPageTreeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun choosingAPageSelectsIt() {
        var page by mutableStateOf<String?>("welcome")
        rule.setMonarchContent { MonarchPageTree(groups = treeGroups, selected = page, onSelect = { page = it }) }
        rule.onNodeWithText("Welcome").assertIsSelected()
        rule.onNodeWithText("Billing").assertIsNotSelected().assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("billing", page)
        rule.onNodeWithText("Billing").assertIsSelected()
        rule.onNodeWithText("Welcome").assertIsNotSelected()
    }

    @Test
    fun groupsAreHeadingsAndTheTreeIsNamed() {
        rule.setMonarchContent { MonarchPageTree(groups = treeGroups, selected = null, onSelect = {}) }
        rule.onNode(isHeading() and hasText("Basics")).assertExists()
        rule.onNodeWithContentDescription("Pages").assertExists()
        rule.onNodeWithText("No pages yet.").assertExists()
    }

    @Test
    fun pagesAreNumberedAcrossGroupsAndTheNumberIsNotRead() {
        rule.setMonarchContent { MonarchPageTree(groups = treeGroups, selected = null, onSelect = {}) }
        rule.onNodeWithText("03", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility))
    }

    @Test
    fun aDisabledTreeCannotBeUsed() {
        var chosen = 0
        rule.setMonarchContent {
            MonarchPageTree(groups = treeGroups, selected = null, onSelect = { chosen++ }, enabled = false)
        }
        rule.onNodeWithText("Welcome").assertIsNotEnabled().performClick()
        assertEquals(0, chosen)
    }

    @Test
    fun thePageMenuMovesAPageToAnotherGroupAndAnnouncesIt() {
        var groups by mutableStateOf(treeGroups)
        var move: MonarchPageTreeMove? = null
        rule.setMonarchContent {
            MonarchPageTree(
                groups = groups,
                selected = null,
                onSelect = {},
                onReorder = { next, m ->
                    groups = next
                    move = m
                },
            )
        }
        rule.onNodeWithContentDescription("Actions for Welcome").assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithText("Move to Guides").performClick()
        assertEquals(MonarchPageTreeMove(MonarchPageTreeMoveKind.Page, "welcome", "basics", "guides", 1), move)
        assertEquals(listOf("billing", "welcome"), groups[1].pages.map { it.id })
        rule.onNode(polite and hasContentDescription("Welcome moved to Guides.")).assertExists()
    }

    @Test
    fun theGroupMenuMovesAGroupAndAnnouncesIt() {
        var groups by mutableStateOf(treeGroups)
        rule.setMonarchContent {
            MonarchPageTree(groups = groups, selected = null, onSelect = {}, onReorder = { next, _ -> groups = next })
        }
        rule.onNodeWithContentDescription("Actions for Basics").performClick()
        rule.onNodeWithText("Move group up").assertDoesNotExist()
        rule.onNodeWithText("Move group down").performClick()
        assertEquals(listOf("guides", "basics", "faq"), groups.map { it.id })
        rule.onNode(polite and hasContentDescription("Basics moved to position 2 of 3.")).assertExists()
    }

    @Test
    fun theHandleMovesAPageWithinItsGroup() {
        var groups by mutableStateOf(treeGroups)
        rule.setMonarchContent {
            MonarchPageTree(groups = groups, selected = null, onSelect = {}, onReorder = { next, _ -> groups = next })
        }
        val handle = rule.onNodeWithContentDescription("Reorder Welcome")
        val down = handle.fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .first { it.label == "Move down" }
        rule.runOnIdle { down.action() }
        assertEquals(listOf("setup", "welcome"), groups[0].pages.map { it.id })
    }

    @Test
    fun addingAPageAndAGroup() {
        val added = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchPageTree(
                groups = treeGroups,
                selected = null,
                onSelect = {},
                onAddPage = { added += "page:$it" },
                onAddGroup = { added += "group:$it" },
            )
        }
        rule.onNodeWithContentDescription("Add page to Guides").assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithText("Add first page").performClick()
        rule.onNodeWithText("Add group").performClick()
        rule.onNodeWithText("Add a group").assertExists()
        rule.onNode(hasSetTextAction()).performTextReplacement("  Reference  ")
        rule.onNode(hasText("Add group") and hasAnyAncestor(isDialog())).performClick()
        assertEquals(listOf("page:guides", "page:faq", "group:Reference"), added)
    }

    @Test
    fun renamingNeedsADifferentName() {
        val renamed = mutableListOf<Pair<String, String>>()
        rule.setMonarchContent {
            MonarchPageTree(
                groups = treeGroups,
                selected = null,
                onSelect = {},
                onRenameGroup = { id, label -> renamed += id to label },
            )
        }
        rule.onNodeWithContentDescription("Actions for Guides").performClick()
        rule.onNodeWithText("Rename group").performClick()
        rule.onNodeWithText("Rename").assertIsNotEnabled()
        rule.onNode(hasSetTextAction()).performTextReplacement("How-to guides")
        rule.onNodeWithText("Rename").assertIsEnabled().performClick()
        assertEquals(listOf("guides" to "How-to guides"), renamed)
    }

    @Test
    fun deletingAGroupWithPagesAsksForItsName() {
        val deleted = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchPageTree(groups = treeGroups, selected = null, onSelect = {}, onDeleteGroup = { deleted += it })
        }
        rule.onNodeWithContentDescription("Actions for Basics").performClick()
        rule.onNodeWithText("Delete group").performClick()
        rule.onNodeWithText("Delete the “Basics” group?").assertExists()
        rule.onNodeWithText("Its 2 pages are deleted too").assertExists()
        rule.onNodeWithText("Delete group").assertIsNotEnabled()
        rule.onNode(hasSetTextAction()).performTextInput("Basics")
        rule.onNodeWithText("Delete group").assertIsEnabled().performClick()
        assertEquals(listOf("basics"), deleted)
    }

    @Test
    fun deletingAPageIsConfirmedAndTheLastPageCannotBeDeleted() {
        val deleted = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchPageTree(groups = treeGroups, selected = null, onSelect = {}, onDeletePage = { deleted += it })
        }
        rule.onNodeWithContentDescription("Actions for Set up").performClick()
        rule.onNodeWithText("Delete page").performClick()
        rule.onNodeWithText("Delete “Set up”?").assertExists()
        rule.onNodeWithText("This cannot be undone.").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(emptyList(), deleted)
        rule.onNodeWithContentDescription("Actions for Set up").performClick()
        rule.onNodeWithText("Delete page").performClick()
        rule.onNodeWithText("Delete page").performClick()
        assertEquals(listOf("setup"), deleted)
        assertEquals(false, pageTreeCanDeletePage(listOf(treeGroups[1])))
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchPageTree(
                groups = treeGroups,
                selected = null,
                onSelect = {},
                labels = MonarchPageTreeLabels(
                    label = "Chapters",
                    number = { "#$it" },
                    noPages = "Empty",
                    pageActions = { "Menu $it" },
                ),
                onDeletePage = {},
            )
        }
        rule.onNodeWithContentDescription("Chapters").assertExists()
        rule.onNodeWithText("#3", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Empty").assertExists()
        rule.onNodeWithContentDescription("Menu Welcome").assertExists()
    }

    @Test
    fun theNumberComesBeforeTheTitleRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchPageTree(groups = treeGroups, selected = null, onSelect = {})
        }
        rule.onNodeWithText("01", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Welcome", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun longTitlesWrapAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchPageTree(
                groups = listOf(
                    MonarchPageTreeGroup(
                        id = "g",
                        label = "G",
                        pages = listOf(MonarchPageTreePage("l", "A page whose title is long enough to wrap")),
                    ),
                ),
                selected = null,
                onSelect = {},
            )
        }
        rule.onNodeWithText("A page whose title is long enough to wrap", useUnmergedTree = true)
            .assertTextFullyVisible()
    }

    @Test
    fun aBlankTitleIsUntitled() {
        rule.setMonarchContent {
            MonarchPageTree(
                groups = listOf(MonarchPageTreeGroup("g", "G", listOf(MonarchPageTreePage("u", "  ")))),
                selected = null,
                onSelect = {},
            )
        }
        rule.onNodeWithText("Untitled page").assertExists()
    }

    @Test
    fun theReorderRules() {
        assertEquals(mapOf("welcome" to 1, "setup" to 2, "billing" to 3), pageTreeNumbers(treeGroups))
        assertEquals("07", pageTreeNumber(7, Locale.US))
        assertEquals("12", pageTreeNumber(12, Locale.US))
        val within = pageTreeMovePageWithin(treeGroups, "basics", 1, 0)!!
        assertEquals(listOf("setup", "welcome"), within.groups[0].pages.map { it.id })
        assertEquals(MonarchPageTreeMove(MonarchPageTreeMoveKind.Page, "setup", "basics", "basics", 0), within.move)
        assertNull(pageTreeMovePageWithin(treeGroups, "basics", 0, 0))
        assertNull(pageTreeMovePageTo(treeGroups, "welcome", "basics"))
        val group = pageTreeMoveGroup(treeGroups, "faq", -2)!!
        assertEquals(listOf("faq", "basics", "guides"), group.groups.map { it.id })
        assertEquals(MonarchPageTreeMove(MonarchPageTreeMoveKind.Group, "faq", null, null, 0), group.move)
        assertNull(pageTreeMoveGroup(treeGroups, "basics", -1))
        assertEquals("New", pageTreeRenamed(" New ", "Old"))
        assertNull(pageTreeRenamed("Old", "Old"))
        assertNull(pageTreeRenamed("  ", "Old"))
        assertEquals(false, pageTreeCanDeleteGroup(treeGroups.take(1)))
    }

    @Test
    fun theOpenPageUsesTheAccentRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val t = MonarchPageTreeTokens
            assertEquals(colors.accentSubtle, t.rowFill(true, colors), theme.id)
            assertEquals(Color.Transparent, t.rowFill(false, colors), theme.id)
            assertEquals(colors.accentSolid, t.currentBar(true, colors), theme.id)
            assertEquals(colors.accentStrong, t.titleColor(current = true, enabled = true, colors = colors), theme.id)
            assertEquals(colors.textDefault, t.titleColor(current = false, enabled = true, colors = colors), theme.id)
            assertEquals(colors.textDisabled, t.titleColor(current = true, enabled = false, colors = colors), theme.id)
            assertEquals(colors.textSubtle, t.numberColor(true, colors), theme.id)
            assertEquals(colors.textMuted, t.headingColor(true, colors), theme.id)
        }
    }
}
