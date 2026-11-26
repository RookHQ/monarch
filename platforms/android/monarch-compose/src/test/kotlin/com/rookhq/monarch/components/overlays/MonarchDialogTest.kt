package com.rookhq.monarch.components.overlays

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.espresso.Espresso
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.components.overlays.dialog.MonarchDialogBody
import com.rookhq.monarch.components.overlays.dialog.MonarchDialogTokens
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.foundation.MonarchElevation
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal object MonarchDialogFixtures : ComponentFixtures {
    override val id = "dialog"
    override val fixtures = listOf(
        fixture("confirmation with a destructive action") {
            MonarchDialog(
                onDismissRequest = {},
                title = "Delete project?",
                description = "Atlas and its 214 tasks are deleted for everyone in the workspace.",
                actions = {
                    MonarchButton(text = "Cancel", onClick = {}, variant = MonarchButtonVariant.Ghost)
                    MonarchButton(text = "Delete project", onClick = {}, variant = MonarchButtonVariant.Danger)
                },
            )
        },
        fixture("content, not dismissible") {
            MonarchDialog(
                onDismissRequest = {},
                title = "Rename board",
                dismissible = false,
                actions = {
                    MonarchButton(text = "Save", onClick = {}, variant = MonarchButtonVariant.Primary)
                },
            ) {
                MonarchText(text = "Everyone on the board sees the new name.")
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchDialogTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theDialogIsAWindowNamedByItsTitleWithTheTitleAsAHeading() {
        rule.setMonarchContent {
            MonarchDialog(onDismissRequest = {}, title = "Delete project?", description = "It cannot be undone.")
        }
        rule.onNode(isDialog()).assertExists()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Delete project?")).assertExists()
        rule.onNode(hasText("Delete project?") and isHeading() and hasAnyAncestor(isDialog())).assertIsDisplayed()
        rule.onNodeWithText("It cannot be undone.").assertIsDisplayed()
    }

    @Test
    fun theActionsAreButtonsWith48dpTargets() {
        var deleted = 0
        rule.setMonarchContent {
            MonarchDialog(
                onDismissRequest = {},
                title = "Delete project?",
                actions = {
                    MonarchButton(text = "Cancel", onClick = {}, variant = MonarchButtonVariant.Ghost)
                    MonarchButton(text = "Delete", onClick = { deleted++ }, variant = MonarchButtonVariant.Danger)
                },
            )
        }
        rule.onNodeWithText("Cancel").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Delete").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, deleted)
    }

    @Test
    fun backDismissesADismissibleDialog() {
        var dismissed = 0
        rule.setMonarchContent {
            MonarchDialog(onDismissRequest = { dismissed++ }, title = "Rename board")
        }
        rule.onNode(isDialog()).assertExists()
        Espresso.pressBack()
        rule.waitForIdle()
        assertEquals(1, dismissed)
    }

    @Test
    fun aDialogWithoutATitleIsNamedByItsPaneTitleAndShowsNoEmptyHeading() {
        rule.setMonarchContent {
            MonarchDialog(onDismissRequest = {}, title = null, paneTitle = "Milestone reached") {
                MonarchText(text = "You shipped 100 releases")
            }
        }
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Milestone reached")).assertExists()
        rule.onNode(isHeading() and hasAnyAncestor(isDialog())).assertDoesNotExist()
        rule.onNodeWithText("You shipped 100 releases").assertIsDisplayed()
    }

    @Test
    fun backStillDismissesADialogThatIgnoresTapsOutside() {
        var dismissed = 0
        rule.setMonarchContent {
            MonarchDialog(onDismissRequest = { dismissed++ }, title = "Name the view", dismissOnClickOutside = false)
        }
        Espresso.pressBack()
        rule.waitForIdle()
        assertEquals(1, dismissed)
    }

    @Test
    fun backDoesNotDismissADialogThatIsNotDismissible() {
        var dismissed = 0
        rule.setMonarchContent {
            MonarchDialog(onDismissRequest = { dismissed++ }, title = "Saving", dismissible = false)
        }
        Espresso.pressBack()
        rule.waitForIdle()
        assertEquals(0, dismissed)
        rule.onNode(isDialog()).assertExists()
    }

    @Test
    fun closingRemovesTheWindowAndLeavesTheScreen() {
        rule.setMonarchContent {
            var open by remember { mutableStateOf(false) }
            MonarchButton(text = "Delete project", onClick = { open = true })
            if (open) {
                MonarchDialog(
                    onDismissRequest = { open = false },
                    title = "Delete project?",
                    actions = { MonarchButton(text = "Cancel", onClick = { open = false }) },
                )
            }
        }
        rule.onNode(isDialog()).assertDoesNotExist()
        rule.onNodeWithText("Delete project").performClick()
        rule.onNode(isDialog()).assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNode(isDialog()).assertDoesNotExist()
        rule.onNodeWithText("Delete project").assertIsDisplayed()
    }

    @Test
    fun theTitleAndDescriptionWrapAtTheLargestFontSize() {
        val title = "Move the project to another workspace?"
        val description = "Members of the other workspace can see every task and file of the project."
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchDialog(onDismissRequest = {}, title = title, description = description)
        }
        rule.onNodeWithText(title).assertTextFullyVisible()
        rule.onNodeWithText(description).assertTextFullyVisible()
    }

    @Test
    fun theActionsFollowTheReadingDirectionRightToLeft() {
        // The body is tested outside the window: in unit tests a dialog window ignores the composition's
        // direction, because the test manifest does not declare supportsRtl.
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchDialogBody(
                title = "Rename",
                description = null,
                actions = {
                    MonarchButton(text = "Cancel", onClick = {}, variant = MonarchButtonVariant.Ghost)
                    MonarchButton(text = "Rename it", onClick = {}, variant = MonarchButtonVariant.Primary)
                },
                content = null,
            )
        }
        rule.onNodeWithText("Cancel").assertIsBefore(rule.onNodeWithText("Rename it"), LayoutDirection.Rtl)
    }

    @Test
    fun thePanelScrimAndShadowComeFromTheTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val panel = MonarchDialogTokens.colors(colors, highContrast = false)
            assertEquals(colors.bgRaised, panel.container, theme.id)
            assertEquals(colors.textDefault, panel.content, theme.id)
            assertEquals(colors.borderSubtle, panel.border, theme.id)
            assertEquals(colors.borderStrong, MonarchDialogTokens.colors(colors, highContrast = true).border, theme.id)
            assertEquals(colors.overlayScrim.alpha, MonarchDialogTokens.dimAmount(colors), theme.id)
            val elevation = MonarchElevation.from(theme.shadows)
            assertNull(MonarchDialogTokens.shadow(elevation, dark = true), theme.id)
            assertNotNull(MonarchDialogTokens.shadow(elevation, dark = false), theme.id)
        }
    }
}
