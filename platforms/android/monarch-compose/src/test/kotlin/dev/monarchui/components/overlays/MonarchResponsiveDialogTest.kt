package dev.monarchui.components.overlays

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.window.core.layout.WindowSizeClass
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.overlays.responsivedialog.MonarchResponsiveDialog
import dev.monarchui.components.overlays.responsivedialog.MonarchResponsiveDialogBody
import dev.monarchui.components.overlays.responsivedialog.MonarchResponsiveDialogBreakpoint
import dev.monarchui.components.overlays.responsivedialog.MonarchResponsiveDialogTokens
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal object MonarchResponsiveDialogFixtures : ComponentFixtures {
    override val id = "responsive-dialog"
    override val fixtures = listOf(
        fixture("title, description and actions") {
            MonarchResponsiveDialog(
                onDismissRequest = {},
                title = "Rename project",
                description = "Everyone in the workspace sees the new name.",
                actions = {
                    MonarchButton(text = "Cancel", onClick = {}, variant = MonarchButtonVariant.Ghost)
                    MonarchButton(text = "Rename", onClick = {}, variant = MonarchButtonVariant.Primary)
                },
            ) { MonarchText(text = "Monarch for Android") }
        },
        fixture("not dismissible, no handle") {
            MonarchResponsiveDialog(
                onDismissRequest = {},
                title = "Accept the terms",
                dismissible = false,
                showHandle = false,
            ) {
                MonarchText(text = "Read and accept the updated terms to continue.")
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchResponsiveDialogTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun onAPhoneItIsASheetWithAHandleAndTheTitleAsPaneTitle() {
        rule.setMonarchContent {
            MonarchResponsiveDialog(onDismissRequest = {}, title = "Rename project") { MonarchText(text = "Body") }
        }
        rule.onNodeWithText("Body").assertIsDisplayed()
        rule.onNodeWithText("Rename project").assertIsDisplayed()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Rename project")).assertExists()
        rule.onNodeWithContentDescription("Drag handle", substring = true).assertExists()
    }

    @Test
    fun theHandleCanBeHidden() {
        rule.setMonarchContent {
            MonarchResponsiveDialog(onDismissRequest = {}, title = "T", showHandle = false) {
                MonarchText(text = "Body")
            }
        }
        rule.onNodeWithText("Body").assertIsDisplayed()
        rule.onNodeWithContentDescription("Drag handle", substring = true).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w900dp-h700dp")
    fun onALargeWindowItIsADialogWithoutAHandle() {
        rule.setMonarchContent {
            MonarchResponsiveDialog(onDismissRequest = {}, title = "Rename project") { MonarchText(text = "Body") }
        }
        rule.onNodeWithText("Body").assertIsDisplayed()
        rule.onNodeWithContentDescription("Drag handle", substring = true).assertDoesNotExist()
    }

    @Test
    fun theActionsAreButtonsAtTheEnd() {
        var renamed = 0
        rule.setMonarchContent {
            MonarchResponsiveDialog(
                onDismissRequest = {},
                title = "Rename",
                actions = {
                    MonarchButton(text = "Rename it", onClick = { renamed++ }, variant = MonarchButtonVariant.Primary)
                },
            ) { MonarchText(text = "Body") }
        }
        rule.onNodeWithText("Rename it").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, renamed)
    }

    @Test
    fun theActionsFollowTheReadingDirectionRightToLeft() {
        // The body is tested outside the window: in unit tests a dialog window ignores the composition's
        // direction, because the test manifest does not declare supportsRtl.
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchResponsiveDialogBody(
                title = "Rename",
                description = null,
                actions = {
                    MonarchButton(text = "Cancel", onClick = {}, variant = MonarchButtonVariant.Ghost)
                    MonarchButton(text = "Rename it", onClick = {}, variant = MonarchButtonVariant.Primary)
                },
            ) { MonarchText(text = "Body") }
        }
        rule.onNodeWithText("Cancel").assertIsBefore(rule.onNodeWithText("Rename it"), LayoutDirection.Rtl)
    }

    @Test
    fun theTitleWrapsAtTheLargestFontSize() {
        val title = "Move the project to another workspace"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchResponsiveDialog(onDismissRequest = {}, title = title) { MonarchText(text = "Body") }
        }
        rule.onNodeWithText(title).assertTextFullyVisible()
    }

    @Test
    fun theBreakpointsFollowTheWindowSizeClasses() {
        val t = MonarchResponsiveDialogTokens
        // currentWindowAdaptiveInfoV2 gives the lower bound of the class the window is in.
        val phone = WindowSizeClass(0, WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND)
        val foldable = WindowSizeClass(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND, 0)
        val tablet = WindowSizeClass(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND, 0)
        assertTrue(t.usesSheet(phone, MonarchResponsiveDialogBreakpoint.Medium))
        assertFalse(t.usesSheet(foldable, MonarchResponsiveDialogBreakpoint.Medium))
        assertTrue(t.usesSheet(foldable, MonarchResponsiveDialogBreakpoint.Expanded))
        assertFalse(t.usesSheet(tablet, MonarchResponsiveDialogBreakpoint.Expanded))
        val medium = t.minDialogWidthDp(MonarchResponsiveDialogBreakpoint.Medium)
        assertEquals(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND, medium)
    }
}
