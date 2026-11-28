package com.rookhq.monarch.components.interaction

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandle
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleAxis
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleDirection
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleSize
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleState
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.minimumTouchSide
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchDragHandleFixtures : ComponentFixtures {
    override val id = "drag-handle"
    override val fixtures = listOf(
        fixture("every state") {
            Row {
                MonarchDragHandleState.entries.forEach { MonarchDragHandle(label = "Reorder", state = it, onMove = {}) }
            }
        },
        fixture("every axis, small") {
            Row {
                MonarchDragHandleAxis.entries.forEach {
                    MonarchDragHandle(label = "Move", axis = it, size = MonarchDragHandleSize.Sm, onMove = {})
                }
            }
        },
        fixture("disabled") { MonarchDragHandle(label = "Reorder", enabled = false, onMove = {}) },
        fixture("no actions") { MonarchDragHandle(label = "Reorder") },
    )
}

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class MonarchDragHandleTest {
    @get:Rule
    val rule = createComposeRule()

    private fun actionNames(): List<String> =
        rule.onNodeWithContentDescription("Reorder Design review").fetchSemanticsNode()
            .config.getOrElse(SemanticsActions.CustomActions) { emptyList() }.map { it.label }

    @Test
    fun theHandleIsOneElementNamedByItsLabel() {
        rule.setMonarchContent { MonarchDragHandle(label = "Reorder Design review") }
        rule.onNodeWithContentDescription("Reorder Design review").assertExists()
        assertNull(rule.onNodeWithContentDescription("Reorder Design review").stateDescription())
    }

    @Test
    fun theGrabbedAndDroppedStatesAreSpoken() {
        rule.setMonarchContent {
            Row {
                MonarchDragHandle(label = "A", state = MonarchDragHandleState.Grabbed)
                MonarchDragHandle(label = "B", state = MonarchDragHandleState.Dropped)
            }
        }
        assertEquals("Picked up", rule.onNodeWithContentDescription("A").stateDescription())
        assertEquals("Dropped", rule.onNodeWithContentDescription("B").stateDescription())
    }

    @Test
    fun eachAxisOffersItsMoveActions() {
        rule.setMonarchContent { MonarchDragHandle(label = "Reorder Design review", onMove = {}) }
        assertEquals(listOf("Move up", "Move down"), actionNames())
        assertEquals(
            listOf(MonarchDragHandleDirection.Backward, MonarchDragHandleDirection.Forward),
            MonarchDragHandleTokens.directions(MonarchDragHandleAxis.Horizontal),
        )
        assertEquals(MonarchDragHandleDirection.entries, MonarchDragHandleTokens.directions(MonarchDragHandleAxis.Both))
    }

    @Test
    fun anActionMovesTheItem() {
        val moves = mutableListOf<MonarchDragHandleDirection>()
        rule.setMonarchContent { MonarchDragHandle(label = "Reorder Design review", onMove = { moves += it }) }
        val node = rule.onNodeWithContentDescription("Reorder Design review").fetchSemanticsNode()
        node.config[SemanticsActions.CustomActions].first { it.label == "Move down" }.action()
        assertEquals(listOf(MonarchDragHandleDirection.Down), moves)
    }

    @Test
    fun withoutOnMoveOrWhenDisabledThereAreNoActions() {
        rule.setMonarchContent { MonarchDragHandle(label = "Reorder Design review", enabled = false, onMove = {}) }
        rule.onNodeWithContentDescription("Reorder Design review").assertIsNotEnabled()
        assertEquals(emptyList(), actionNames())
    }

    @Test
    fun theActionNamesAndStatesCanBeReplaced() {
        rule.setMonarchContent {
            MonarchDragHandle(
                label = "Reorder Design review",
                state = MonarchDragHandleState.Grabbed,
                onMove = {},
                actionLabels = mapOf(MonarchDragHandleDirection.Up to "Monter", MonarchDragHandleDirection.Down to ""),
                stateLabels = mapOf(MonarchDragHandleState.Grabbed to "Saisi"),
            )
        }
        assertEquals(listOf("Monter", "Move down"), actionNames())
        assertEquals("Saisi", rule.onNodeWithContentDescription("Reorder Design review").stateDescription())
    }

    private fun arrowKeys(direction: LayoutDirection): List<MonarchDragHandleDirection> {
        val moves = mutableListOf<MonarchDragHandleDirection>()
        val focus = FocusRequester()
        rule.setMonarchContent(layoutDirection = direction) {
            MonarchDragHandle(
                label = "Move card",
                modifier = Modifier.focusRequester(focus),
                axis = MonarchDragHandleAxis.Both,
                onMove = { moves += it },
            )
        }
        rule.runOnIdle { focus.requestFocus() }
        rule.onNodeWithContentDescription("Move card").performKeyInput {
            pressKey(Key.DirectionLeft)
            pressKey(Key.DirectionDown)
        }
        return moves
    }

    @Test
    fun arrowKeysMoveTheFocusedHandleLeftToRight() {
        assertEquals(
            listOf(MonarchDragHandleDirection.Backward, MonarchDragHandleDirection.Down),
            arrowKeys(LayoutDirection.Ltr),
        )
    }

    @Test
    fun theLeftArrowMovesForwardRightToLeft() {
        assertEquals(
            listOf(MonarchDragHandleDirection.Forward, MonarchDragHandleDirection.Down),
            arrowKeys(LayoutDirection.Rtl),
        )
    }

    @Test
    fun aVerticalHandleIgnoresTheSideArrows() {
        val moves = mutableListOf<MonarchDragHandleDirection>()
        val focus = FocusRequester()
        rule.setMonarchContent {
            MonarchDragHandle(label = "Move", modifier = Modifier.focusRequester(focus), onMove = { moves += it })
        }
        rule.runOnIdle { focus.requestFocus() }
        rule.onNodeWithContentDescription("Move").performKeyInput {
            pressKey(Key.DirectionLeft)
            pressKey(Key.DirectionUp)
        }
        assertEquals(listOf(MonarchDragHandleDirection.Up), moves)
    }

    @Test
    fun bothSizesKeepA48dpTarget() {
        rule.setMonarchContent {
            Row {
                MonarchDragHandle(label = "Small", size = MonarchDragHandleSize.Sm, onMove = {})
                MonarchDragHandle(label = "Medium", onMove = {})
            }
        }
        rule.onNodeWithContentDescription("Small").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithContentDescription("Medium").assertTouchTargetAtLeast48dp(rule.density)
        assertTrue(rule.onNodeWithContentDescription("Small").minimumTouchSide(rule.density).value >= 48f - 0.5f)
    }

    @Test
    fun theGripHasTheWebPatternForEachAxis() {
        assertEquals(6, MonarchDragHandleTokens.dots(MonarchDragHandleAxis.Vertical).size)
        assertEquals(6, MonarchDragHandleTokens.dots(MonarchDragHandleAxis.Horizontal).size)
        assertEquals(9, MonarchDragHandleTokens.dots(MonarchDragHandleAxis.Both).size)
        assertTrue(MonarchDragHandleTokens.dots(MonarchDragHandleAxis.Vertical).all { it.x == 6f || it.x == 10f })
    }

    @Test
    fun theColoursOfEachStateInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val idle = MonarchDragHandleTokens.colors(MonarchDragHandleState.Idle, true, colors)
            val grabbed = MonarchDragHandleTokens.colors(MonarchDragHandleState.Grabbed, true, colors)
            val dropped = MonarchDragHandleTokens.colors(MonarchDragHandleState.Dropped, true, colors)
            val disabled = MonarchDragHandleTokens.colors(MonarchDragHandleState.Grabbed, false, colors)
            assertEquals(colors.textMuted, idle.content, theme.id)
            assertNull(idle.border, theme.id)
            assertEquals(colors.accentSubtle, grabbed.container, theme.id)
            assertEquals(colors.accentStrong, grabbed.content, theme.id)
            assertEquals(colors.accentBorder, grabbed.border, theme.id)
            assertEquals(colors.successSubtle, dropped.container, theme.id)
            assertEquals(colors.successStrong, dropped.content, theme.id)
            assertEquals(colors.textDisabled, disabled.content, theme.id)
        }
    }
}
