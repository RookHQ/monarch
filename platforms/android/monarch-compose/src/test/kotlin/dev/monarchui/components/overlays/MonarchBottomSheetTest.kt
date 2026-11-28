package dev.monarchui.components.overlays

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.test.espresso.Espresso
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheetTokens
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchShapes
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

internal object MonarchBottomSheetFixtures : ComponentFixtures {
    override val id = "sheet"
    override val fixtures = listOf(
        fixture("title, description and a primary action") {
            MonarchBottomSheet(
                onDismissRequest = {},
                title = "Add a note",
                description = "Notes are visible to everyone on the ticket.",
            ) {
                MonarchText(text = "The customer asked for a refund by Friday.")
                MonarchButton(text = "Save note", onClick = { dismiss() }, variant = MonarchButtonVariant.Primary)
            }
        },
        fixture("no handle, not dismissible") {
            MonarchBottomSheet(
                onDismissRequest = {},
                title = "Uploading files",
                dismissible = false,
                showHandle = false,
            ) {
                MonarchText(text = "3 of 8 files uploaded.")
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchBottomSheetTest {
    @get:Rule
    val rule = createComposeRule()

    private val handle = "Drag handle"

    @Test
    fun theTitleIsThePaneTitleAndAHeading() {
        rule.setMonarchContent {
            MonarchBottomSheet(onDismissRequest = {}, title = "Share report", description = "Anyone with the link") {
                MonarchText(text = "Copy link")
            }
        }
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Share report")).assertExists()
        rule.onNode(hasText("Share report") and isHeading()).assertIsDisplayed()
        rule.onNodeWithText("Anyone with the link").assertIsDisplayed()
        rule.onNodeWithText("Copy link").assertIsDisplayed()
    }

    @Test
    fun theHandleIsANamed48dpTargetWithADismissActionForTalkBack() {
        var dismissed = 0
        rule.setMonarchContent {
            MonarchBottomSheet(onDismissRequest = { dismissed++ }, title = "Share report") {
                MonarchText(text = "Copy link")
            }
        }
        rule.onNodeWithContentDescription(handle, substring = true)
            .assertTouchTargetAtLeast48dp(rule.density)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Dismiss)
        rule.waitForIdle()
        assertEquals(1, dismissed)
    }

    @Test
    fun dismissInTheContentSlidesTheSheetAwayAndReportsIt() {
        var dismissed = 0
        rule.setMonarchContent {
            MonarchBottomSheet(onDismissRequest = { dismissed++ }, title = "Add a note", dismissible = false) {
                MonarchButton(text = "Save note", onClick = { dismiss() }, variant = MonarchButtonVariant.Primary)
            }
        }
        rule.onNodeWithText("Save note").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(1, dismissed)
    }

    @Test
    fun aSheetThatIsNotDismissibleIgnoresBackAndOffersNoDismissAction() {
        var dismissed = 0
        rule.setMonarchContent {
            MonarchBottomSheet(onDismissRequest = { dismissed++ }, title = "Uploading", dismissible = false) {
                MonarchText(text = "3 of 8 files uploaded.")
            }
        }
        rule.onNodeWithContentDescription(handle, substring = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Dismiss))
        Espresso.pressBack()
        rule.waitForIdle()
        assertEquals(0, dismissed)
        rule.onNodeWithText("3 of 8 files uploaded.").assertIsDisplayed()
    }

    @Test
    fun backDismissesADismissibleSheet() {
        var dismissed = 0
        rule.setMonarchContent {
            MonarchBottomSheet(onDismissRequest = { dismissed++ }, title = "Share report") {
                MonarchText(text = "Copy link")
            }
        }
        rule.onNodeWithText("Copy link").assertIsDisplayed()
        Espresso.pressBack()
        rule.waitForIdle()
        assertEquals(1, dismissed)
    }

    @Test
    fun theTitleWrapsAtTheLargestFontSize() {
        val title = "Move the project to another workspace"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchBottomSheet(onDismissRequest = {}, title = title) { MonarchText(text = "Body") }
        }
        rule.onNodeWithText(title).assertTextFullyVisible()
    }

    @Test
    fun theSheetHasRoundedTopCornersAndThemeColours() {
        val shape = MonarchBottomSheetTokens.shape(MonarchShapes.Default)
        val size = Size(400f, 300f)
        val density = Density(1f)
        assertEquals(MonarchShapes.Default.xl.topStart.toPx(size, density), shape.topStart.toPx(size, density))
        assertEquals(CornerSize(0).toPx(size, density), shape.bottomEnd.toPx(size, density))
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val sheet = MonarchBottomSheetTokens.colors(colors)
            assertEquals(colors.bgRaised, sheet.container, theme.id)
            assertEquals(colors.textDefault, sheet.content, theme.id)
            assertEquals(colors.textSubtle, MonarchBottomSheetTokens.handleColor(colors), theme.id)
        }
    }
}
