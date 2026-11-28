package dev.monarchui.components.collaboration

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.collaboration.slashmenu.MonarchSlashMenu
import dev.monarchui.components.collaboration.slashmenu.MonarchSlashMenuDefaults
import dev.monarchui.components.collaboration.slashmenu.MonarchSlashMenuItem
import dev.monarchui.components.collaboration.slashmenu.MonarchSlashMenuLabels
import dev.monarchui.components.collaboration.slashmenu.MonarchSlashMenuQuery
import dev.monarchui.components.collaboration.slashmenu.MonarchSlashMenuTokens
import dev.monarchui.components.collaboration.slashmenu.MonarchSlashMenuTriggerPosition
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val IconAndPadding = 40.dp

private val Open = MonarchSlashMenuQuery('/', "", 0, 1)

private val Commands = listOf(
    MonarchSlashMenuItem(id = "heading", label = "Heading", description = "Large section title", group = "Text"),
    MonarchSlashMenuItem(id = "bullets", label = "Bulleted list", description = "A simple list", group = "Lists"),
    MonarchSlashMenuItem(
        id = "checklist",
        label = "Checklist",
        description = "Items to tick off",
        group = "Lists",
        keywords = listOf("todo"),
    ),
)
private val Disabled =
    MonarchSlashMenuItem(id = "table", label = "Table", description = "Not in replies", enabled = false)

internal object MonarchSlashMenuFixtures : ComponentFixtures {
    override val id = "slash-menu"
    override val fixtures = listOf(
        fixture("grouped") { MonarchSlashMenu(query = Open, items = Commands, onSelect = {}) },
        fixture("ungrouped with a disabled entry") {
            MonarchSlashMenu(query = Open, items = listOf(Commands[0], Disabled).map(::ungrouped), onSelect = {})
        },
        fixture("with icons") {
            val icon = rememberVectorPainter(TestIcon)
            MonarchSlashMenu(
                query = Open,
                items = listOf(MonarchSlashMenuItem(id = "quote", label = "Quote", icon = icon)),
                onSelect = {},
            )
        },
        fixture("nothing matches") {
            MonarchSlashMenu(query = MonarchSlashMenuQuery.find("/zzz"), items = Commands, onSelect = {})
        },
        fixture("long label and description") {
            MonarchSlashMenu(
                query = Open,
                items = listOf(
                    MonarchSlashMenuItem(
                        id = "callout",
                        label = "Callout with a coloured background",
                        description = "Draws attention to a warning or a tip in a long document",
                    ),
                ),
                onSelect = {},
            )
        },
    )

    private fun ungrouped(item: MonarchSlashMenuItem) = MonarchSlashMenuItem(
        id = item.id,
        label = item.label,
        description = item.description,
        enabled = item.enabled,
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchSlashMenuTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun entriesAreButtonsAndTheCountIsAnnounced() {
        var chosen: String? = null
        rule.setMonarchContent { MonarchSlashMenu(query = Open, items = Commands, onSelect = { chosen = it.id }) }
        val strip = rule.onNodeWithContentDescription("Insert block")
        strip.assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        assertEquals("3 options", strip.stateDescription())
        rule.onNodeWithText("Heading")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("heading", chosen)
    }

    @Test
    fun aDisabledEntryCannotBeChosen() {
        var chosen: String? = null
        rule.setMonarchContent {
            MonarchSlashMenu(query = Open, items = listOf(Commands[0], Disabled), onSelect = { chosen = it.id })
        }
        rule.onNodeWithText("Table").assertIsNotEnabled().performClick()
        assertNull(chosen)
    }

    @Test
    fun theQueryFiltersTheEntries() {
        rule.setMonarchContent {
            MonarchSlashMenu(query = MonarchSlashMenuQuery.find("/todo"), items = Commands, onSelect = {})
        }
        rule.onNodeWithText("Checklist").assertExists()
        rule.onNodeWithText("Heading").assertDoesNotExist()
        assertEquals("1 option", rule.onNodeWithContentDescription("Insert block").stateDescription())
    }

    @Test
    fun groupsAreHeadingsInTheGivenOrder() {
        rule.setMonarchContent {
            MonarchSlashMenu(query = Open, items = Commands, onSelect = {}, groupOrder = listOf("Lists", "Text"))
        }
        rule.onNodeWithText("Lists").assert(isHeading())
        rule.onNodeWithText("Text").assert(isHeading())
        val lists = rule.onNodeWithText("Lists").fetchSemanticsNode().boundsInRoot
        val text = rule.onNodeWithText("Text").fetchSemanticsNode().boundsInRoot
        assertTrue(lists.top < text.top, "Lists is listed before Text")
    }

    @Test
    fun ungroupedEntriesHaveNoHeading() {
        rule.setMonarchContent { MonarchSlashMenu(query = Open, items = listOf(Disabled), onSelect = {}) }
        rule.onAllNodes(isHeading()).assertCountEquals(0)
    }

    @Test
    fun nothingMatchingSaysWhatWasTyped() {
        rule.setMonarchContent {
            MonarchSlashMenu(query = MonarchSlashMenuQuery.find("/zzz"), items = Commands, onSelect = {})
        }
        rule.onNodeWithText("Nothing matches “/zzz”").assertExists()
        assertEquals("Nothing matches “/zzz”", rule.onNodeWithContentDescription("Insert block").stateDescription())
    }

    @Test
    fun noQueryDrawsNothing() {
        rule.setMonarchContent {
            Box(Modifier.testTag("host")) { MonarchSlashMenu(query = null, items = Commands, onSelect = {}) }
        }
        assertEquals(0, rule.onNodeWithTag("host").fetchSemanticsNode().children.size)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchSlashMenu(
                query = MonarchSlashMenuQuery.find("/xy"),
                items = Commands,
                onSelect = {},
                labels = MonarchSlashMenuLabels(label = "Insertar", empty = { "Nada para $it" }),
            )
        }
        assertEquals("Nada para /xy", rule.onNodeWithContentDescription("Insertar").stateDescription())
        rule.onNodeWithText("Nada para /xy").assertExists()
    }

    @Test
    fun theCountCanBeReplaced() {
        rule.setMonarchContent {
            MonarchSlashMenu(
                query = Open,
                items = Commands,
                onSelect = {},
                labels = MonarchSlashMenuLabels(count = { "$it opciones" }),
            )
        }
        assertEquals("3 opciones", rule.onNodeWithContentDescription("Insert block").stateDescription())
    }

    @Test
    fun theIconComesBeforeTheLabelInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            val icon = rememberVectorPainter(TestIcon)
            MonarchSlashMenu(
                query = Open,
                items = listOf(MonarchSlashMenuItem(id = "quote", label = "Quote", icon = icon)),
                onSelect = {},
            )
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            val row = rule.onNodeWithText("Quote").fetchSemanticsNode().boundsInRoot
            val label = rule.onNodeWithText("Quote", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            // The start padding and the 24dp icon come before the label, on the start side.
            val inset = with(rule.density) { IconAndPadding.toPx() }
            val fromStart = if (direction == LayoutDirection.Ltr) label.left - row.left else row.right - label.right
            assertTrue(fromStart >= inset, "$direction: label starts $fromStart px from the start")
        }
    }

    @Test
    fun longDescriptionsWrapAtTheLargestFontScale() {
        val description = "Draws attention to a warning or a tip in a long document"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchSlashMenu(
                query = Open,
                items = listOf(MonarchSlashMenuItem(id = "c", label = "Callout", description = description)),
                onSelect = {},
            )
        }
        rule.onNodeWithText(description, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun findsTheCommandLikeTheWeb() {
        assertEquals(MonarchSlashMenuQuery('/', "", 0, 1), MonarchSlashMenuQuery.find("/"))
        assertEquals(MonarchSlashMenuQuery('/', "he", 7, 10), MonarchSlashMenuQuery.find("Agenda\n/he"))
        assertEquals(MonarchSlashMenuQuery('/', "to-do", 0, 6), MonarchSlashMenuQuery.find("/to-do"))
        assertNull(MonarchSlashMenuQuery.find("1/2"))
        assertNull(MonarchSlashMenuQuery.find("Note /he"))
        assertEquals(
            MonarchSlashMenuQuery('/', "he", 5, 8),
            MonarchSlashMenuQuery.find("Note /he", position = MonarchSlashMenuTriggerPosition.WordStart),
        )
        assertNull(MonarchSlashMenuQuery.find("1/2", position = MonarchSlashMenuTriggerPosition.WordStart))
        assertNull(MonarchSlashMenuQuery.find("/he "))
        assertNull(MonarchSlashMenuQuery.find("/he."))
        assertNull(MonarchSlashMenuQuery.find("/" + "a".repeat(25)))
        val longest = "a".repeat(24)
        assertEquals(MonarchSlashMenuQuery('/', longest, 0, 25), MonarchSlashMenuQuery.find("/$longest"))
        assertEquals(MonarchSlashMenuQuery('/', "h", 0, 2), MonarchSlashMenuQuery.find("/heading", caret = 2))
        assertEquals(MonarchSlashMenuQuery('!', "x", 0, 2), MonarchSlashMenuQuery.find("!x", trigger = '!'))
        assertNull(MonarchSlashMenuQuery.find("/x", caret = 9))
        val value = TextFieldValue("/b", TextRange(2))
        assertEquals(MonarchSlashMenuQuery('/', "b", 0, 2), MonarchSlashMenuQuery.find(value))
        assertNull(MonarchSlashMenuQuery.find(TextFieldValue("/b", TextRange(0, 2))))
    }

    @Test
    fun replacingRemovesTheCommandAndPlacesTheCaret() {
        val text = "Agenda\n/he\nNext"
        val query = MonarchSlashMenuQuery.find(text, caret = 10)!!
        assertEquals("Agenda\n## \nNext", query.replacing(text, "## "))
        assertEquals("Agenda\n\nNext", query.replacing(text))
        val value = query.replacing(TextFieldValue(text, TextRange(10)), "## ")
        assertEquals("Agenda\n## \nNext", value.text)
        assertEquals(TextRange(10), value.selection)
        assertEquals("short", query.replacing("short", "x"))
        assertEquals(TextFieldValue("x"), query.replacing(TextFieldValue("x"), "y"))
    }

    @Test
    fun theFilterRanksLikeTheWeb() {
        val items = listOf(
            MonarchSlashMenuItem(id = "quote", label = "Quote", description = "A list of words"),
            MonarchSlashMenuItem(id = "bullets", label = "Bulleted list"),
            MonarchSlashMenuItem(id = "list", label = "List"),
            MonarchSlashMenuItem(id = "todo", label = "Checklist", keywords = listOf("tasks")),
        )
        val ranked = MonarchSlashMenuDefaults.filter(items, " LIST").map { it.id }
        assertEquals(listOf("list", "bullets", "todo", "quote"), ranked)
        assertEquals(listOf("todo"), MonarchSlashMenuDefaults.filter(items, "task").map { it.id })
        assertEquals(items, MonarchSlashMenuDefaults.filter(items, ""))
        assertEquals(emptyList(), MonarchSlashMenuDefaults.filter(items, "zzz"))
    }

    @Test
    fun groupsFollowTheOrderThenFirstAppearance() {
        val items = listOf(
            MonarchSlashMenuItem(id = "a", label = "A", group = "Media"),
            MonarchSlashMenuItem(id = "b", label = "B"),
            MonarchSlashMenuItem(id = "c", label = "C", group = "Text"),
            MonarchSlashMenuItem(id = "d", label = "D", group = "Media"),
        )
        val groups = MonarchSlashMenuDefaults.group(items, listOf("Text", "Lists"))
        assertEquals(listOf("Text", "Media", null), groups.map { it.first })
        assertEquals(listOf("a", "d"), groups[1].second.map { it.id })
    }

    @Test
    fun theColoursComeFromTheirRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchSlashMenuTokens
            assertEquals(c.bgSurface, t.background(c), theme.id)
            assertEquals(c.borderSubtle, t.border(c), theme.id)
            assertEquals(c.textMuted, t.noteColor(c), theme.id)
            assertEquals(c.textDefault, t.rowColors(true, c).label, theme.id)
            assertEquals(c.textMuted, t.rowColors(true, c).description, theme.id)
            assertEquals(c.textMuted, t.rowColors(true, c).icon, theme.id)
            assertEquals(c.textDisabled, t.rowColors(false, c).label, theme.id)
            assertEquals(c.textDisabled, t.rowColors(false, c).description, theme.id)
            assertEquals(c.textDisabled, t.rowColors(false, c).icon, theme.id)
        }
    }
}
