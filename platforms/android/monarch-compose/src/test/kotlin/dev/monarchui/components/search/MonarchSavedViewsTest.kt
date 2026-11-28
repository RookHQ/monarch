package dev.monarchui.components.search

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.search.savedviews.MonarchSavedView
import dev.monarchui.components.search.savedviews.MonarchSavedViews
import dev.monarchui.components.search.savedviews.MonarchSavedViewsLabels
import dev.monarchui.components.search.savedviews.MonarchSavedViewsTokens
import dev.monarchui.components.search.savedviews.SavedViewsNameError
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A field node whose TalkBack error is [message]. */
private fun hasError(message: String) = SemanticsMatcher.expectValue(SemanticsProperties.Error, message)

private val Views = listOf(
    MonarchSavedView("all", "All deals", count = 128, readOnly = true, group = "Built-in"),
    MonarchSavedView("open", "Open deals", count = 42, default = true, group = "Mine"),
    MonarchSavedView("q4", "Closing this quarter", shared = true, group = "Mine"),
)

internal object MonarchSavedViewsFixtures : ComponentFixtures {
    override val id = "saved-views"
    override val fixtures = listOf(
        fixture("selected, edited, none, sizes") {
            Column {
                MonarchSavedViews(views = Views, selectedId = "open", onSelect = {})
                MonarchSavedViews(views = Views, selectedId = "q4", onSelect = {}, modified = true)
                MonarchSavedViews(views = Views, selectedId = null, onSelect = {}, size = MonarchButtonSize.Sm)
                MonarchSavedViews(views = Views, selectedId = "all", onSelect = {}, size = MonarchButtonSize.Lg)
            }
        },
        fixture("disabled, long name") {
            Column {
                MonarchSavedViews(views = Views, selectedId = "open", onSelect = {}, enabled = false)
                MonarchSavedViews(
                    views = listOf(MonarchSavedView("long", "Deals closing this quarter in the northern region")),
                    selectedId = "long",
                    onSelect = {},
                )
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSavedViewsTest {
    @get:Rule
    val rule = createComposeRule()

    private fun menuItem(text: String) = rule.onNode(hasText(text) and hasAnyAncestor(isPopup()))

    @Test
    fun theButtonIsNamedViewWithTheSelectedViewAndEditedAsItsState() {
        rule.setMonarchContent { MonarchSavedViews(views = Views, selectedId = "open", onSelect = {}, modified = true) }
        val button = rule.onNodeWithContentDescription("View")
        assertEquals("Open deals, Edited", button.stateDescription())
        button.assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun withoutASelectionTheButtonReadsAllItems() {
        rule.setMonarchContent { MonarchSavedViews(views = Views, selectedId = null, onSelect = {}) }
        assertEquals("All items", rule.onNodeWithContentDescription("View").stateDescription())
    }

    @Test
    fun choosingAViewCallsBackAndTheSelectedOneIsMarked() {
        var chosen: String? = null
        rule.setMonarchContent { MonarchSavedViews(views = Views, selectedId = "open", onSelect = { chosen = it }) }
        rule.onNodeWithContentDescription("View").performClick()
        menuItem("Open deals").assertIsSelected()
        menuItem("Built-in").assertExists()
        menuItem("42 · Default view").assertExists()
        menuItem("Shared").assertExists()
        menuItem("Closing this quarter").assertIsNotSelected().performClick()
        assertEquals("q4", chosen)
    }

    @Test
    fun aDisabledButtonDoesNotOpen() {
        rule.setMonarchContent { MonarchSavedViews(views = Views, selectedId = "open", onSelect = {}, enabled = false) }
        rule.onNodeWithContentDescription("View").assertIsNotEnabled().performClick()
        menuItem("All deals").assertDoesNotExist()
    }

    @Test
    fun onlyActionsWithACallbackAreOfferedAndReadOnlyViewsCannotBeChanged() {
        val tokens = MonarchSavedViewsTokens
        val readOnly = tokens.actions(Views[0], true, true, true, true, true, true, true)
        assertFalse(readOnly.saveChanges || readOnly.rename || readOnly.delete)
        assertTrue(readOnly.saveAsNew && readOnly.discard && readOnly.setDefault)
        val default = tokens.actions(Views[1], false, true, true, true, true, true, true)
        assertFalse(default.setDefault || default.saveChanges || default.discard)
        val none = tokens.actions(Views[2], true, false, false, false, false, false, false)
        assertFalse(none.any)
    }

    @Test
    fun namesAreTrimmedAndMustBeNewIgnoringCase() {
        val tokens = MonarchSavedViewsTokens
        assertEquals(SavedViewsNameError.Required, tokens.validate("   ", Views, null))
        assertEquals(SavedViewsNameError.Taken, tokens.validate(" open DEALS ", Views, null))
        assertNull(tokens.validate("OPEN DEALS", Views, renamingId = "open"))
        assertNull(tokens.validate("Won deals", Views, null))
    }

    @Test
    fun saveAsNewChecksTheNameAndPassesItTrimmed() {
        var saved: String? = null
        rule.setMonarchContent {
            MonarchSavedViews(views = Views, selectedId = "open", onSelect = {}, onSaveNew = { saved = it })
        }
        rule.onNodeWithContentDescription("View").performClick()
        menuItem("Save as new view…").performClick()
        rule.onNodeWithText("Save").performClick()
        // The label is above the box and names the field; the error is on the field.
        rule.onNode(hasContentDescription("Name") and hasError("Enter a name.")).assertExists()
        rule.onNodeWithContentDescription("Name").performTextInput("open deals")
        rule.onNodeWithText("Save").performClick()
        rule.onNode(hasContentDescription("Name") and hasError("A view with this name already exists.")).assertExists()
        rule.onNodeWithText("open deals").performTextClearance()
        rule.onNodeWithContentDescription("Name").performTextInput("  Won deals ")
        rule.onNodeWithText("Save").performClick()
        rule.waitForIdle()
        assertEquals("Won deals", saved)
        rule.onNodeWithText("Save view").assertDoesNotExist()
    }

    @Test
    fun aFailedSaveKeepsTheDialogOpenWithAMessage() {
        rule.setMonarchContent {
            MonarchSavedViews(
                views = Views,
                selectedId = "q4",
                onSelect = {},
                onRename = { _, _ -> error("offline") },
            )
        }
        rule.onNodeWithContentDescription("View").performClick()
        menuItem("Rename…").performClick()
        rule.onNodeWithText("Rename view").assertExists()
        rule.onNodeWithText("Save").performClick()
        rule.waitForIdle()
        rule.onNode(hasError("The view could not be saved. Try again.")).assertExists()
        rule.onNodeWithText("Rename view").assertExists()
    }

    @Test
    fun deletingAsksFirstAndPassesTheId() {
        var deleted: String? = null
        rule.setMonarchContent {
            MonarchSavedViews(views = Views, selectedId = "q4", onSelect = {}, onDelete = { deleted = it })
        }
        rule.onNodeWithContentDescription("View").performClick()
        menuItem("Delete view…").performClick()
        rule.onNodeWithText("Delete “Closing this quarter”?").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        assertNull(deleted)
        rule.onNodeWithContentDescription("View").performClick()
        menuItem("Delete view…").performClick()
        rule.onNodeWithText("Delete view").performClick()
        rule.waitForIdle()
        assertEquals("q4", deleted)
    }

    @Test
    fun saveChangesDiscardAndSetDefaultCallBack() {
        val calls = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchSavedViews(
                views = Views,
                selectedId = "q4",
                onSelect = {},
                modified = true,
                onSaveChanges = { calls += "save $it" },
                onDiscardChanges = { calls += "discard" },
                onSetDefault = { calls += "default $it" },
            )
        }
        listOf("Save changes to “Closing this quarter”", "Discard changes", "Set as default").forEach {
            rule.onNodeWithContentDescription("View").performClick()
            menuItem(it).performClick()
        }
        assertEquals(listOf("save q4", "discard", "default q4"), calls)
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchSavedViews(
                views = Views,
                selectedId = null,
                onSelect = {},
                modified = true,
                labels = MonarchSavedViewsLabels(view = "Vista", noView = "Todo", modified = "Editada"),
            )
        }
        assertEquals("Todo, Editada", rule.onNodeWithContentDescription("Vista").stateDescription())
    }

    @Test
    fun theNameComesBeforeTheEditedBadgeRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchSavedViews(views = Views, selectedId = "q4", onSelect = {}, modified = true)
        }
        rule.onNode(hasText("Closing this quarter"), useUnmergedTree = true)
            .assertIsBefore(rule.onNode(hasText("Edited"), useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun theButtonTextIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchSavedViews(views = Views, selectedId = "open", onSelect = {})
        }
        rule.onNode(hasText("Open deals"), useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.dangerStrong, MonarchSavedViewsTokens.dangerText(colors))
            assertEquals(colors.textMuted, MonarchSavedViewsTokens.secondaryText(colors))
        }
    }
}
