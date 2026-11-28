package dev.monarchui.components.files

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.files.filecard.MonarchFileCard
import dev.monarchui.components.files.filecard.MonarchFileCardLabels
import dev.monarchui.components.files.filecard.MonarchFileCardLayout
import dev.monarchui.components.files.filecard.MonarchFileCardTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

internal object MonarchFileCardFixtures : ComponentFixtures {
    override val id = "file-card"
    override val fixtures = listOf(
        fixture("card with selection, badge and actions") {
            MonarchFileCard(
                name = "Q3 report.pdf",
                modifier = Modifier.width(180.dp),
                sizeBytes = 2_400_000,
                meta = "Edited yesterday",
                badges = listOf("Shared"),
                onSelectedChange = {},
                onOpen = {},
            ) {
                IconButton(onClick = {}) { Icon(TestIcon, contentDescription = "Actions for Q3 report.pdf") }
            }
        },
        fixture("card selected, with a picture") {
            MonarchFileCard(
                name = "beach.jpg",
                modifier = Modifier.width(180.dp),
                image = ColorPainter(MonarchTheme.colors.accentSubtle),
                selected = true,
                onSelectedChange = {},
                onOpen = {},
            )
        },
        fixture("card disabled") {
            MonarchFileCard(
                name = "archive.zip",
                modifier = Modifier.width(180.dp),
                sizeBytes = 9_800_000,
                enabled = false,
                onSelectedChange = {},
                onOpen = {},
            )
        },
        fixture("rows") {
            Column {
                MonarchFileCard(name = "notes.txt", sizeBytes = 900, layout = MonarchFileCardLayout.Row, onOpen = {})
                MonarchFileCard(
                    name = "budget-2026-final-revised-for-the-board.xlsx",
                    meta = "Shared by Sam",
                    layout = MonarchFileCardLayout.Row,
                    selected = true,
                    onSelectedChange = {},
                    onOpen = {},
                ) {
                    IconButton(onClick = {}) { Icon(TestIcon, contentDescription = "Actions") }
                }
                MonarchFileCard(name = "slides.key", layout = MonarchFileCardLayout.Row)
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchFileCardTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun thePictureNameAndMetaAreOneButtonThatOpensTheFile() {
        var opened = 0
        rule.setMonarchContent {
            MonarchFileCard(
                name = "Q3 report.pdf",
                sizeBytes = 2_400_000,
                meta = "Edited yesterday",
                badges = listOf("Shared"),
                onOpen = { opened++ },
            )
        }
        val open = rule.onNode(isButton and hasText("Q3 report.pdf") and hasText("Shared"))
        open.assert(hasText("PDF document", substring = true))
        open.assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun withoutOnOpenItIsNotAButton() {
        rule.setMonarchContent { MonarchFileCard(name = "notes.txt") }
        rule.onNode(hasText("notes.txt") and hasClickAction()).assertDoesNotExist()
    }

    @Test
    fun theMetaLineHasTheTypeTheSizeAndTheText() {
        rule.setMonarchContent {
            MonarchFileCard(name = "Q3 report.pdf", sizeBytes = 2_400_000, meta = "Edited yesterday")
        }
        val meta = rule.onNodeWithText("PDF document · ", substring = true, useUnmergedTree = true)
        meta.assert(hasText(" · Edited yesterday", substring = true))
        meta.assert(hasText("MB", substring = true))
    }

    @Test
    fun theCheckboxSelectsTheFileAndIsNamedAfterIt() {
        var selected by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchFileCard(name = "Q3 report.pdf", selected = selected, onSelectedChange = { selected = it })
        }
        val checkbox = rule.onNodeWithContentDescription("Select Q3 report.pdf")
        checkbox.assertIsOff().assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertTrue(selected)
        checkbox.assertIsOn()
    }

    @Test
    fun noCheckboxWithoutOnSelectedChange() {
        rule.setMonarchContent { MonarchFileCard(name = "a.pdf", selected = true) }
        rule.onNodeWithContentDescription("Select a.pdf").assertDoesNotExist()
    }

    @Test
    fun disabledBlocksOpeningAndSelecting() {
        var opened = 0
        rule.setMonarchContent {
            MonarchFileCard(name = "a.pdf", enabled = false, onSelectedChange = {}, onOpen = { opened++ })
        }
        rule.onNodeWithContentDescription("Select a.pdf").assertIsNotEnabled()
        rule.onNode(isButton and hasText("a.pdf")).assertIsNotEnabled().performClick()
        assertEquals(0, opened)
    }

    @Test
    fun theActionsAreASeparateElement() {
        var menu = 0
        rule.setMonarchContent {
            MonarchFileCard(name = "a.pdf", onOpen = {}) {
                IconButton(onClick = { menu++ }) { Icon(TestIcon, contentDescription = "Actions for a.pdf") }
            }
        }
        rule.onNodeWithContentDescription("Actions for a.pdf").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, menu)
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchFileCard(
                name = "a.pdf",
                onSelectedChange = {},
                labels = MonarchFileCardLabels(select = { "Choose $it" }, type = "Contract"),
            )
        }
        rule.onNodeWithContentDescription("Choose a.pdf").assertExists()
        rule.onNodeWithText("Contract", useUnmergedTree = true).assertExists()
    }

    @Test
    fun inARowTheCheckboxComesFirstRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchFileCard(name = "a.pdf", layout = MonarchFileCardLayout.Row, onSelectedChange = {})
        }
        rule.onNodeWithContentDescription("Select a.pdf")
            .assertIsBefore(rule.onNodeWithText("a.pdf", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun theNameWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchFileCard(name = "Q3 report.pdf", meta = "Edited yesterday")
        }
        rule.onNodeWithText("Q3 report.pdf", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Edited yesterday", substring = true, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theRulesOfTheCard() {
        val t = MonarchFileCardTokens
        assertEquals("PDF · 2 MB · Mine", t.meta("PDF", "2 MB", "Mine", " · "))
        assertEquals("PDF", t.meta("PDF", null, " ", " · "))
        assertEquals(2, t.nameLines(MonarchFileCardLayout.Card))
        assertEquals(1, t.nameLines(MonarchFileCardLayout.Row))
        assertTrue(t.borderWidth(selected = true, enabled = true) > t.borderWidth(selected = false, enabled = true))
        assertEquals(t.borderWidth(false, true), t.borderWidth(selected = true, enabled = false))
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        val t = MonarchFileCardTokens
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val rest = t.colors(selected = false, enabled = true, pressed = false, colors = c)
            assertEquals(c.bgSurface, rest.container, theme.id)
            assertEquals(c.borderSubtle, rest.border, theme.id)
            val selected = t.colors(selected = true, enabled = true, pressed = true, colors = c)
            assertEquals(c.accentSubtle, selected.container, theme.id)
            assertEquals(c.accentBorder, selected.border, theme.id)
            val pressed = t.colors(selected = false, enabled = true, pressed = true, colors = c)
            assertEquals(c.bgSubtle, pressed.container, theme.id)
            assertEquals(c.borderDefault, pressed.border, theme.id)
            val disabled = t.colors(selected = true, enabled = false, pressed = false, colors = c)
            assertEquals(c.bgSubtle, disabled.container, theme.id)
            assertEquals(c.textDisabled, disabled.content, theme.id)
            assertEquals(c.textMuted, t.metaColor(enabled = true, colors = c), theme.id)
            assertEquals(c.textDisabled, t.metaColor(enabled = false, colors = c), theme.id)
            assertEquals(c.bgSubtle, t.placeholder(c), theme.id)
            assertEquals(c.bgSurface, t.checkboxBackdrop(c), theme.id)
        }
    }
}
