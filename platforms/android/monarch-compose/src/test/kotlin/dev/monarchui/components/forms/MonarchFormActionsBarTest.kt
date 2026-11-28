package dev.monarchui.components.forms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.formactionsbar.MonarchFormActionsBar
import dev.monarchui.components.forms.formactionsbar.MonarchFormActionsBarLabels
import dev.monarchui.components.forms.formactionsbar.MonarchFormActionsBarTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
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
import kotlin.test.assertTrue

private val NoInsets = WindowInsets(0)

internal object MonarchFormActionsBarFixtures : ComponentFixtures {
    override val id = "form-actions-bar"
    override val fixtures = listOf(
        fixture("clean") { MonarchFormActionsBar(dirty = false, onSave = {}, onDiscard = {}, windowInsets = NoInsets) },
        fixture("dirty") { MonarchFormActionsBar(dirty = true, onSave = {}, onDiscard = {}, windowInsets = NoInsets) },
        fixture("pending") {
            MonarchFormActionsBar(dirty = true, onSave = {}, onDiscard = {}, pending = true, windowInsets = NoInsets)
        },
        fixture("error") {
            MonarchFormActionsBar(
                dirty = true,
                onSave = {},
                onDiscard = {},
                error = "Your changes could not be saved. Try again.",
                windowInsets = NoInsets,
            )
        },
        fixture("an extra action and a custom status") {
            MonarchFormActionsBar(
                dirty = false,
                onSave = {},
                status = { Text("Saved 2 minutes ago") },
                windowInsets = NoInsets,
                actions = { MonarchButton(text = "Preview", onClick = {}, variant = MonarchButtonVariant.Ghost) },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchFormActionsBarTest {
    @get:Rule
    val rule = createComposeRule()

    private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    @Test
    fun saveAndDiscardRespondOnlyWithUnsavedChanges() {
        var dirty by mutableStateOf(false)
        var saves = 0
        rule.setMonarchContent {
            MonarchFormActionsBar(dirty = dirty, onSave = { saves++ }, onDiscard = {}, windowInsets = NoInsets)
        }
        rule.onNodeWithText("Save changes").assertIsNotEnabled().performClick()
        rule.onNodeWithText("Discard").assertIsNotEnabled()
        assertEquals(0, saves)
        dirty = true
        rule.onNodeWithText("Save changes").assertIsEnabled().assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Discard").assertIsEnabled().assertTouchTargetAtLeast48dp(rule.density)
        assertEquals(1, saves)
    }

    @Test
    fun theUnsavedMessageIsInAPoliteLiveRegion() {
        var dirty by mutableStateOf(false)
        rule.setMonarchContent { MonarchFormActionsBar(dirty = dirty, onSave = {}, windowInsets = NoInsets) }
        rule.onNodeWithText("You have unsaved changes").assertDoesNotExist()
        dirty = true
        rule.onNodeWithText("You have unsaved changes").assert(polite)
    }

    @Test
    fun whilePendingSaveShowsProgressAndNothingResponds() {
        var saves = 0
        var discards = 0
        rule.setMonarchContent {
            MonarchFormActionsBar(
                dirty = true,
                onSave = { saves++ },
                onDiscard = { discards++ },
                pending = true,
                confirmDiscard = false,
                windowInsets = NoInsets,
            )
        }
        val save = rule.onNodeWithText("Save changes")
        assertEquals("Saving", save.stateDescription())
        save.performClick()
        rule.onNodeWithText("Discard").assertIsNotEnabled().performClick()
        assertEquals(0, saves)
        assertEquals(0, discards)
    }

    @Test
    fun anErrorReplacesTheStatusAndIsAnnounced() {
        rule.setMonarchContent {
            MonarchFormActionsBar(dirty = true, onSave = {}, error = "Could not save.", windowInsets = NoInsets)
        }
        rule.onNodeWithText("Could not save.").assert(polite)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        rule.onNodeWithText("You have unsaved changes").assertDoesNotExist()
    }

    @Test
    fun discardAsksFirstAndKeepEditingCancels() {
        var discards = 0
        rule.setMonarchContent {
            MonarchFormActionsBar(dirty = true, onSave = {}, onDiscard = { discards++ }, windowInsets = NoInsets)
        }
        rule.onNodeWithText("Discard").performClick()
        rule.onNode(isDialog()).assertExists()
        rule.onNodeWithText("Discard unsaved changes?").assertExists()
        rule.onNodeWithText("Your changes will be lost. This cannot be undone.").assertExists()
        rule.onNodeWithText("Keep editing").performClick()
        rule.onNode(isDialog()).assertDoesNotExist()
        assertEquals(0, discards)
        rule.onNodeWithText("Discard").performClick()
        rule.onNodeWithText("Discard changes").performClick()
        assertEquals(1, discards)
    }

    @Test
    fun withoutConfirmationDiscardActsAtOnce() {
        var discards = 0
        rule.setMonarchContent {
            MonarchFormActionsBar(
                dirty = true,
                onSave = {},
                onDiscard = { discards++ },
                confirmDiscard = false,
                windowInsets = NoInsets,
            )
        }
        rule.onNodeWithText("Discard").performClick()
        rule.onNode(isDialog()).assertDoesNotExist()
        assertEquals(1, discards)
    }

    @Test
    fun hideWhenCleanShowsNothingUntilThereAreChanges() {
        var dirty by mutableStateOf(false)
        rule.setMonarchContent {
            Box(Modifier.testTag("host")) {
                MonarchFormActionsBar(dirty = dirty, onSave = {}, hideWhenClean = true, windowInsets = NoInsets)
            }
        }
        rule.onNodeWithText("Save changes").assertDoesNotExist()
        dirty = true
        rule.onNodeWithText("Save changes").assertExists()
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchFormActionsBar(
                dirty = true,
                onSave = {},
                onDiscard = {},
                windowInsets = NoInsets,
                labels = MonarchFormActionsBarLabels(
                    unsaved = "Modifications non enregistrées",
                    save = "Enregistrer",
                    discard = "Annuler",
                    discardTitle = "Annuler les modifications ?",
                    discardDescription = "Elles seront perdues.",
                    discardConfirm = "Annuler les modifications",
                    keepEditing = "Continuer",
                ),
            )
        }
        rule.onNodeWithText("Modifications non enregistrées").assertExists()
        rule.onNodeWithText("Enregistrer").assertExists()
        rule.onNodeWithText("Annuler").performClick()
        rule.onNodeWithText("Annuler les modifications ?").assertExists()
        rule.onNodeWithText("Elles seront perdues.").assertExists()
        rule.onNodeWithText("Continuer").assertExists()
        rule.onNodeWithText("Annuler les modifications").assertExists()
    }

    @Test
    fun saveIsAtTheEndAfterDiscardInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchFormActionsBar(dirty = true, onSave = {}, onDiscard = {}, windowInsets = NoInsets)
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.onNodeWithText("Discard").assertIsBefore(rule.onNodeWithText("Save changes"), direction)
        }
    }

    @Test
    fun theMessageWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchFormActionsBar(dirty = true, onSave = {}, onDiscard = {}, windowInsets = NoInsets)
        }
        rule.onNodeWithText("You have unsaved changes", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Save changes", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Discard", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun availabilityAndColoursFollowTheState() {
        val tokens = MonarchFormActionsBarTokens
        val clean = tokens.availability(dirty = false, pending = false, hasError = false, hideWhenClean = true)
        assertFalse(clean.saveEnabled)
        assertFalse(clean.discardEnabled)
        assertTrue(clean.hidden)
        val pending = tokens.availability(dirty = true, pending = true, hasError = false, hideWhenClean = true)
        assertTrue(pending.saveEnabled)
        assertFalse(pending.discardEnabled)
        assertFalse(pending.hidden)
        assertFalse(tokens.availability(dirty = false, pending = false, hasError = true, hideWhenClean = true).hidden)
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val bar = tokens.colors(colors)
            assertEquals(colors.bgSurface, bar.container, theme.id)
            assertEquals(colors.borderSubtle, bar.divider, theme.id)
            assertEquals(colors.textMuted, bar.status, theme.id)
            assertEquals(colors.dangerStrong, bar.error, theme.id)
        }
    }
}
