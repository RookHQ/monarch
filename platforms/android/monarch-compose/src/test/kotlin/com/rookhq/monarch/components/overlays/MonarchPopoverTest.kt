package com.rookhq.monarch.components.overlays

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.checkboxrow.MonarchCheckboxRow
import com.rookhq.monarch.components.overlays.popover.MonarchPopover
import com.rookhq.monarch.components.overlays.popover.MonarchPopoverPositionProvider
import com.rookhq.monarch.components.overlays.popover.MonarchPopoverSide
import com.rookhq.monarch.components.overlays.popover.MonarchPopoverTokens
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object MonarchPopoverFixtures : ComponentFixtures {
    override val id = "popover"
    override val fixtures = MonarchPopoverSide.entries.map { side ->
        fixture("open, side ${side.name}") {
            Box {
                MonarchButton(text = "Filter", onClick = {})
                MonarchPopover(expanded = true, onDismissRequest = {}, label = "Filter", side = side) {
                    MonarchText(text = "Filter", style = MonarchTextStyle.Subheading, heading = true)
                    MonarchCheckboxRow(checked = true, onCheckedChange = {}, label = "Only unpaid")
                }
            }
        }
    } + listOf(
        fixture("closed") {
            Box {
                MonarchButton(text = "Filter", onClick = {})
                MonarchPopover(expanded = false, onDismissRequest = {}, label = "Filter") {
                    MonarchText(text = "Hidden")
                }
            }
        },
        fixture("long text") {
            Box {
                MonarchButton(text = "Why?", onClick = {})
                MonarchPopover(expanded = true, onDismissRequest = {}, label = "Why this price") {
                    MonarchText(
                        text = "The price includes the seats added this month, prorated to the day they were " +
                            "added, and the tax of the billing country.",
                    )
                }
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchPopoverTest {
    @get:Rule
    val rule = createComposeRule()

    private val provider = MonarchPopoverPositionProvider(MonarchPopoverSide.Bottom, gap = 6, margin = 16)
    private val window = IntSize(400, 800)
    private val panel = IntSize(200, 100)

    @Test
    fun thePanelOpensBelowTheStartOfTheAnchor() {
        val anchor = IntRect(left = 40, top = 100, right = 140, bottom = 140)
        assertEquals(IntOffset(40, 146), provider.calculatePosition(anchor, window, LayoutDirection.Ltr, panel))
    }

    @Test
    fun inRightToLeftThePanelIsAlignedWithTheRightEdgeOfTheAnchor() {
        val anchor = IntRect(left = 260, top = 100, right = 360, bottom = 140)
        assertEquals(IntOffset(160, 146), provider.calculatePosition(anchor, window, LayoutDirection.Rtl, panel))
    }

    @Test
    fun thePanelMovesAboveWhenThereIsNoRoomBelow() {
        val anchor = IntRect(left = 40, top = 700, right = 140, bottom = 740)
        assertEquals(IntOffset(40, 594), provider.calculatePosition(anchor, window, LayoutDirection.Ltr, panel))
    }

    @Test
    fun theTopSideMovesBelowWhenThereIsNoRoomAbove() {
        val top = provider.copy(side = MonarchPopoverSide.Top)
        val near = IntRect(left = 40, top = 20, right = 140, bottom = 60)
        assertEquals(IntOffset(40, 66), top.calculatePosition(near, window, LayoutDirection.Ltr, panel))
        val far = IntRect(left = 40, top = 400, right = 140, bottom = 440)
        assertEquals(IntOffset(40, 294), top.calculatePosition(far, window, LayoutDirection.Ltr, panel))
    }

    @Test
    fun thePanelStaysInsideTheWindowMargin() {
        val anchor = IntRect(left = 300, top = 100, right = 390, bottom = 140)
        val position = provider.calculatePosition(anchor, window, LayoutDirection.Ltr, panel)
        assertEquals(184, position.x)
        val leftEdge = IntRect(left = 0, top = 100, right = 50, bottom = 140)
        assertEquals(16, provider.calculatePosition(leftEdge, window, LayoutDirection.Rtl, panel).x)
    }

    @Test
    fun thePanelColoursComeFromTheTheme() {
        for (theme in AllThemes) {
            val colors = MonarchColors(theme.colors)
            val surface = MonarchPopoverTokens.colors(colors, theme.isHighContrast)
            assertEquals(colors.bgRaised, surface.container)
            assertEquals(colors.textDefault, surface.content)
            val border = if (theme.isHighContrast) colors.borderStrong else colors.borderSubtle
            assertEquals(border, surface.border, theme.id)
        }
    }

    @Test
    fun openingShowsANamedPaneAndDismissingClosesIt() {
        rule.setMonarchContent {
            var open by remember { mutableStateOf(false) }
            Box {
                MonarchButton(text = "Filter", onClick = { open = true })
                MonarchPopover(expanded = open, onDismissRequest = { open = false }, label = "Filter invoices") {
                    MonarchButton(text = "Done", onClick = { open = false }, variant = MonarchButtonVariant.Primary)
                }
            }
        }
        rule.onNodeWithText("Done").assertDoesNotExist()
        rule.onNodeWithText("Filter").performClick()
        rule.onNodeWithText("Done").assertIsDisplayed().assertTouchTargetAtLeast48dp(rule.density)
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Filter invoices")).assertExists()
        rule.onNodeWithText("Done").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Done").assertDoesNotExist()
    }

    @Test
    fun theWidthIsLimitedAndLongTextWrapsAtTheLargestFontScale() {
        val text = "The price includes the seats added this month, prorated to the day they were added."
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Box {
                MonarchButton(text = "Why?", onClick = {})
                MonarchPopover(
                    expanded = true,
                    onDismissRequest = {},
                    label = "Why this price",
                    modifier = Modifier.testTag("panel"),
                ) { MonarchText(text = text) }
            }
        }
        rule.onNodeWithText(text, useUnmergedTree = true).assertTextFullyVisible()
        val width = with(rule.density) { rule.onNodeWithTag("panel").fetchSemanticsNode().size.width.toDp() }
        assertTrue(width <= MonarchPopoverTokens.MaxWidth, "The panel is $width wide")
    }

    @Test
    fun theContentKeepsItsOwnWidthWhenItIsNarrow() {
        rule.setMonarchContent {
            Box {
                MonarchButton(text = "Info", onClick = {})
                MonarchPopover(
                    expanded = true,
                    onDismissRequest = {},
                    label = "Info",
                    modifier = Modifier.testTag("panel"),
                ) { MonarchText(text = "Short", modifier = Modifier.width(80.dp)) }
            }
        }
        val width = with(rule.density) { rule.onNodeWithTag("panel").fetchSemanticsNode().size.width.toDp() }
        assertTrue(width < MonarchPopoverTokens.MaxWidth, "The panel is $width wide")
    }
}
