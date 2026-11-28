package dev.monarchui.components.overlays

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.overlays.unsavedchangesguard.MonarchUnsavedChangesGuard
import dev.monarchui.components.overlays.unsavedchangesguard.MonarchUnsavedChangesGuardChoices
import dev.monarchui.components.overlays.unsavedchangesguard.MonarchUnsavedChangesGuardLabels
import dev.monarchui.components.overlays.unsavedchangesguard.MonarchUnsavedChangesGuardLeaveReason
import dev.monarchui.components.overlays.unsavedchangesguard.MonarchUnsavedChangesGuardTokens
import dev.monarchui.components.typography.text.MonarchText
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
import kotlinx.coroutines.awaitCancellation
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal object MonarchUnsavedChangesGuardFixtures : ComponentFixtures {
    override val id = "unsaved-changes-guard"
    override val fixtures = listOf(
        fixture("discard or keep editing") {
            MonarchUnsavedChangesGuard(hasChanges = true, open = true, onOpenChange = {}, onLeave = {})
        },
        fixture("with save and details") {
            MonarchUnsavedChangesGuard(
                hasChanges = true,
                open = true,
                onOpenChange = {},
                onLeave = {},
                onSave = {},
                details = { MonarchText(text = "Unsaved: Name, Email") },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchUnsavedChangesGuardTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var back: OnBackPressedDispatcher

    private fun guard(
        hasChanges: Boolean,
        onSave: (suspend () -> Unit)? = null,
        leaves: MutableList<MonarchUnsavedChangesGuardLeaveReason> = mutableListOf(),
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        fontScale: Float = 1f,
    ): () -> Boolean {
        var shown = false
        rule.setMonarchContent(fontScale = fontScale, layoutDirection = layoutDirection) {
            back = checkNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
            var open by remember { mutableStateOf(false) }
            shown = open
            MonarchUnsavedChangesGuard(
                hasChanges = hasChanges,
                open = open,
                onOpenChange = { open = it },
                onLeave = { leaves += it },
                onSave = onSave,
            )
        }
        return { shown }
    }

    private fun pressBack() {
        rule.runOnIdle { back.onBackPressed() }
        rule.waitForIdle()
    }

    @Test
    fun backAsksWhenThereAreChanges() {
        val shown = guard(hasChanges = true)
        pressBack()
        assertTrue(shown())
        rule.onNodeWithText("Discard unsaved changes?").assertExists()
        rule.onNodeWithText("Your changes have not been saved. If you leave now, they will be lost.").assertExists()
    }

    @Test
    fun backIsNotInterceptedWithoutChanges() {
        val shown = guard(hasChanges = false)
        rule.runOnIdle { assertFalse(back.hasEnabledCallbacks()) }
        assertFalse(shown())
    }

    @Test
    fun keepEditingStays() {
        val leaves = mutableListOf<MonarchUnsavedChangesGuardLeaveReason>()
        val shown = guard(hasChanges = true, leaves = leaves)
        pressBack()
        rule.onNodeWithText("Keep editing").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertFalse(shown())
        assertEquals(emptyList(), leaves)
    }

    @Test
    fun discardLeavesAtOnce() {
        val leaves = mutableListOf<MonarchUnsavedChangesGuardLeaveReason>()
        guard(hasChanges = true, leaves = leaves)
        pressBack()
        rule.onNodeWithText("Save and leave").assertDoesNotExist()
        rule.onNodeWithText("Discard changes").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(listOf(MonarchUnsavedChangesGuardLeaveReason.Discard), leaves)
    }

    @Test
    fun saveAndLeaveSavesThenLeaves() {
        val leaves = mutableListOf<MonarchUnsavedChangesGuardLeaveReason>()
        var saves = 0
        val shown = guard(hasChanges = true, onSave = { saves++ }, leaves = leaves)
        pressBack()
        rule.onNodeWithText("Save and leave").performClick()
        rule.waitForIdle()
        assertEquals(1, saves)
        assertEquals(listOf(MonarchUnsavedChangesGuardLeaveReason.Save), leaves)
        assertFalse(shown())
    }

    @Test
    fun whileSavingTheOtherChoicesAndBackAreBlocked() {
        val leaves = mutableListOf<MonarchUnsavedChangesGuardLeaveReason>()
        val shown = guard(hasChanges = true, onSave = { awaitCancellation() }, leaves = leaves)
        pressBack()
        rule.onNodeWithText("Save and leave").performClick()
        rule.waitForIdle()
        assertEquals("Loading", rule.onNodeWithText("Save and leave").stateDescription())
        rule.onNodeWithText("Discard changes").assertIsNotEnabled()
        rule.onNodeWithText("Keep editing").assertIsNotEnabled()
        pressBack()
        assertTrue(shown())
        assertEquals(emptyList(), leaves)
    }

    @Test
    fun aFailedSaveIsAnnouncedAndThePersonStays() {
        val leaves = mutableListOf<MonarchUnsavedChangesGuardLeaveReason>()
        val shown = guard(hasChanges = true, onSave = { throw IOException("offline") }, leaves = leaves)
        pressBack()
        rule.onNodeWithText("Save and leave").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Your changes could not be saved. Try again.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        assertTrue(shown())
        assertEquals(emptyList(), leaves)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchUnsavedChangesGuard(
                hasChanges = true,
                open = true,
                onOpenChange = {},
                onLeave = {},
                onSave = {},
                labels = MonarchUnsavedChangesGuardLabels(
                    title = "¿Descartar cambios?",
                    message = "No se han guardado.",
                    keepEditing = "Seguir editando",
                    discard = "Descartar",
                    save = "Guardar y salir",
                ),
            )
        }
        listOf("¿Descartar cambios?", "No se han guardado.", "Seguir editando", "Descartar", "Guardar y salir")
            .forEach { rule.onNodeWithText(it).assertExists() }
    }

    @Test
    fun discardComesBeforeSaveRightToLeft() {
        // The choices are tested outside the dialog: in unit tests a dialog window ignores the composition's
        // direction, because the test manifest does not declare supportsRtl.
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchUnsavedChangesGuardChoices(
                saving = false,
                canSave = true,
                onDiscard = {},
                onSave = {},
                labels = MonarchUnsavedChangesGuardLabels(),
            )
        }
        rule.onNodeWithText("Discard changes")
            .assertIsBefore(rule.onNodeWithText("Save and leave"), LayoutDirection.Rtl)
    }

    @Test
    fun theMessageWrapsAtTheLargestFontSize() {
        guard(hasChanges = true, fontScale = LargestFontScale)
        pressBack()
        rule.onNodeWithText("Discard unsaved changes?").assertTextFullyVisible()
    }

    @Test
    fun theLogicOfBackAndTheColours() {
        val t = MonarchUnsavedChangesGuardTokens
        assertTrue(t.interceptsBack(guardsBack = true, hasChanges = true, saving = false))
        assertFalse(t.interceptsBack(guardsBack = false, hasChanges = true, saving = false))
        assertTrue(t.interceptsBack(guardsBack = false, hasChanges = false, saving = true))
        assertFalse(t.backAsks(saving = true))
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.dangerStrong, t.errorColor(c), theme.id)
            assertEquals(c.textMuted, t.messageColor(c), theme.id)
        }
    }
}
