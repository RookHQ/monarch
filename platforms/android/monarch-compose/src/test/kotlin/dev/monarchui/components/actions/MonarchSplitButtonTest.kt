package dev.monarchui.components.actions

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.splitbutton.MonarchSplitButton
import dev.monarchui.components.actions.splitbutton.MonarchSplitButtonTokens
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val arrow: @Composable () -> Unit = { Icon(TestIcon, contentDescription = null) }

@Composable
private fun Save(
    variant: MonarchButtonVariant = MonarchButtonVariant.Secondary,
    size: MonarchButtonSize = MonarchButtonSize.Md,
    enabled: Boolean = true,
    loading: Boolean = false,
    menuEnabled: Boolean = true,
    text: String = "Save",
    onClick: () -> Unit = {},
    onDraft: () -> Unit = {},
) {
    MonarchSplitButton(
        text = text,
        onClick = onClick,
        arrowIcon = arrow,
        variant = variant,
        size = size,
        enabled = enabled,
        loading = loading,
        menuEnabled = menuEnabled,
    ) { close ->
        MonarchMenuItem(
            text = "Save as draft",
            onClick = {
                onDraft()
                close()
            },
        )
    }
}

internal object MonarchSplitButtonFixtures : ComponentFixtures {
    override val id = "split-button"
    override val fixtures = listOf(
        fixture("every variant") { Column { MonarchButtonVariant.entries.forEach { Save(variant = it) } } },
        fixture("every size") { Column { MonarchButtonSize.entries.forEach { Save(size = it) } } },
        fixture("disabled, loading and menu disabled") {
            Column {
                Save(enabled = false)
                Save(loading = true)
                Save(menuEnabled = false)
            }
        },
        fixture("long label") { Save(text = "Save and send to every approver on the list") },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSplitButtonTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theMainButtonActsAtOnceAndTheArrowOpensTheMenu() {
        var saves = 0
        var drafts = 0
        rule.setMonarchContent { Save(onClick = { saves++ }, onDraft = { drafts++ }) }
        rule.onNodeWithText("Save").performClick()
        assertEquals(1, saves)
        rule.onNodeWithText("Save as draft").assertDoesNotExist()
        rule.onNodeWithContentDescription("More options for Save").performClick()
        rule.onNodeWithText("Save as draft").performClick()
        assertEquals(1, drafts)
        assertEquals(1, saves)
        rule.onNodeWithText("Save as draft").assertDoesNotExist()
    }

    @Test
    fun theArrowSaysWhetherTheMenuIsOpenAndOffersExpandAndCollapse() {
        rule.setMonarchContent { Save() }
        val arrowButton = rule.onNodeWithContentDescription("More options for Save")
        assertEquals("Collapsed", arrowButton.stateDescription())
        arrowButton.performSemanticsAction(SemanticsActions.Expand)
        rule.onNodeWithText("Save as draft").assertExists()
        assertEquals("Expanded", arrowButton.stateDescription())
        arrowButton.performSemanticsAction(SemanticsActions.Collapse)
        rule.onNodeWithText("Save as draft").assertDoesNotExist()
    }

    @Test
    fun disablingTheMenuKeepsTheMainActionAndLoadingBlocksBoth() {
        var loading by mutableStateOf(false)
        var saves = 0
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            Column {
                Save(menuEnabled = false, text = "Send")
                Save(loading = loading, onClick = { saves++ })
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Send").assertIsEnabled()
        rule.onNodeWithContentDescription("More options for Send").assertIsNotEnabled()
        loading = true
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Save").performClick()
        assertEquals(0, saves)
        rule.onNodeWithContentDescription("More options for Save").assertIsNotEnabled()
    }

    @Test
    fun aDisabledPairTurnsOffBothButtons() {
        rule.setMonarchContent { Save(enabled = false) }
        rule.onNodeWithText("Save").assertIsNotEnabled()
        rule.onNodeWithContentDescription("More options for Save").assertIsNotEnabled()
    }

    @Test
    fun bothButtonsHaveFullTargetsAtEverySize() {
        rule.setMonarchContent { Column { MonarchButtonSize.entries.forEach { Save(size = it, text = it.name) } } }
        MonarchButtonSize.entries.forEach { size ->
            rule.onNodeWithText(size.name).assertTouchTargetAtLeast48dp(rule.density)
            rule.onNodeWithContentDescription("More options for ${size.name}")
                .assertTouchTargetAtLeast48dp(rule.density)
        }
    }

    @Test
    fun theArrowIsAfterTheMainButtonInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { Save() }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Save")
                .assertIsBefore(rule.onNodeWithContentDescription("More options for Save"), direction)
        }
    }

    @Test
    fun aLongLabelLeavesRoomForTheArrow() {
        rule.setMonarchContent { Save(text = "Save and send to every approver on the list before the end of the day") }
        rule.onNodeWithContentDescription("More options for", substring = true)
            .assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun aLongLabelWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) { Save(text = "Save and send to every approver") }
        rule.onNodeWithText("Save and send to every approver", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theNameOfTheArrowCanBeReplaced() {
        rule.setMonarchContent {
            MonarchSplitButton("Save", onClick = {}, arrowIcon = arrow, moreOptionsLabel = "Other ways to save") {}
        }
        rule.onNodeWithContentDescription("Other ways to save").assertExists()
    }

    @Test
    fun theArrowRule() {
        val t = MonarchSplitButtonTokens
        assertTrue(t.arrowEnabled(enabled = true, loading = false, menuEnabled = true))
        assertFalse(t.arrowEnabled(enabled = false, loading = false, menuEnabled = true))
        assertFalse(t.arrowEnabled(enabled = true, loading = true, menuEnabled = true))
        assertFalse(t.arrowEnabled(enabled = true, loading = false, menuEnabled = false))
    }
}
