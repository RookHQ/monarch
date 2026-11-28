package dev.monarchui.components.intelligence

import androidx.compose.material3.Text
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdown
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownAlign
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownBlock
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownEmphasis
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownInline
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownInlineParser
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownLabels
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownLinks
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownParser
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownTokens
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal object MonarchAiMarkdownFixtures : ComponentFixtures {
    override val id = "ai-markdown"
    override val fixtures = listOf(
        fixture("headings and inline styles") {
            MonarchAiMarkdown(
                // A followable inline link: the matrix exempts links in running text (WCAG 2.5.8).
                text = "# Summary\n\n**Revenue** grew *12%*, see `Q3`, [the report](https://example.com/q3) " +
                    "and ~~old~~ figures.",
                onOpenLink = {},
            )
        },
        fixture("lists and tasks") {
            MonarchAiMarkdown(text = "1. First\n2. Second\n   - nested\n\n- [x] Done\n- [ ] Open")
        },
        fixture("table") {
            MonarchAiMarkdown(
                text = "| Plan | Seats | Price |\n| :- | -: | :-: |\n| Team | 10 | €90 |\n| Business | 50 | €400 |",
            )
        },
        fixture("code block") {
            MonarchAiMarkdown(text = "```ts\nconst a = 1;\n```")
        },
        fixture("streaming, unclosed code") {
            MonarchAiMarkdown(text = "Run this:\n\n```bash\nnpm install", streaming = true)
        },
        fixture("quotation and rule") {
            MonarchAiMarkdown(text = "> Quoted advice\n> on two lines\n\n---\n\nAfter the rule.")
        },
        fixture("footnotes") {
            MonarchAiMarkdown(text = "A claim.[^1]\n\n[^1]: The source.")
        },
        fixture("untrusted input") {
            MonarchAiMarkdown(
                text = "<script>alert(1)</script> [bad](javascript:alert(1)) [data](data:text/html,x)",
            )
        },
        fixture("long text") {
            MonarchAiMarkdown(
                text = "The quarterly revenue grew across every region, with the strongest growth in the " +
                    "Nordic countries where the new pricing took effect in August.",
            )
        },
    )
}

private fun SemanticsNodeInteraction.annotated(): AnnotatedString =
    fetchSemanticsNode().config[SemanticsProperties.Text].first()

private fun AnnotatedString.links(): List<LinkAnnotation.Url> =
    getLinkAnnotations(0, length).map { it.item }.filterIsInstance<LinkAnnotation.Url>()

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiMarkdownTest {
    @get:Rule
    val rule = createComposeRule()

    private fun text(value: String) = MonarchAiMarkdownInline.Text(value)

    @Test
    fun theParserReadsEveryBlockConstructAsTheWebDoes() {
        val blocks = MonarchAiMarkdownParser.parse(
            listOf(
                "# Title", "", "Setext", "===", "", "A paragraph", "on two lines.", "", "> quoted", "",
                "```ts title=\"app.ts\"", "const a = 1;", "```", "", "    indented code", "", "***", "",
                "| a | b |", "| :- | -: |", "| 1 | 2 |", "", "[^n]: A note.",
            ).joinToString("\n"),
        )
        assertEquals(
            listOf("Heading", "Heading", "Paragraph", "Quote", "Code", "Code", "Rule", "Table", "FootnoteDefinition"),
            blocks.map { it::class.simpleName },
        )
        val code = assertIs<MonarchAiMarkdownBlock.Code>(blocks[4])
        assertEquals(listOf("ts", "app.ts", "const a = 1;"), listOf(code.lang, code.filename, code.code))
        assertTrue(code.closed)
        assertEquals("indented code", assertIs<MonarchAiMarkdownBlock.Code>(blocks[5]).code)
        val table = assertIs<MonarchAiMarkdownBlock.Table>(blocks[7])
        assertEquals(listOf(MonarchAiMarkdownAlign.Left, MonarchAiMarkdownAlign.Right), table.align)
        assertEquals(1, assertIs<MonarchAiMarkdownBlock.Heading>(blocks[1]).level)
        val paragraph = assertIs<MonarchAiMarkdownBlock.Paragraph>(blocks[2])
        assertEquals(listOf(text("A paragraph\non two lines.")), paragraph.content)
    }

    @Test
    fun theParserReadsNestedOrderedLooseAndTaskLists() {
        val list = assertIs<MonarchAiMarkdownBlock.ListBlock>(
            MonarchAiMarkdownParser.parse("3. one\n4. two\n   - nested\n     - deeper\n5. three").single(),
        )
        assertTrue(list.ordered)
        assertEquals(3, list.start)
        assertFalse(list.loose)
        assertEquals(3, list.items.size)
        val nested = assertIs<MonarchAiMarkdownBlock.ListBlock>(list.items[1].children[1])
        assertFalse(nested.ordered)
        assertIs<MonarchAiMarkdownBlock.ListBlock>(nested.items[0].children[1])
        val loose = assertIs<MonarchAiMarkdownBlock.ListBlock>(MonarchAiMarkdownParser.parse("- a\n\n- b").single())
        assertTrue(loose.loose)
        val tasks = assertIs<MonarchAiMarkdownBlock.ListBlock>(
            MonarchAiMarkdownParser.parse("- [x] done\n- [ ] open\n- plain").single(),
        )
        assertEquals(listOf(true, false, null), tasks.items.map { it.checked })
    }

    @Test
    fun anUnclosedFenceIsCodeThatIsNotClosed() {
        val code = assertIs<MonarchAiMarkdownBlock.Code>(MonarchAiMarkdownParser.parse("```py\nprint(1)").single())
        assertEquals("py", code.lang)
        assertEquals("print(1)", code.code)
        assertFalse(code.closed)
        val maths = assertIs<MonarchAiMarkdownBlock.Code>(MonarchAiMarkdownParser.parse("$$\nx_1 + y\n$$").single())
        assertEquals("math" to "x_1 + y", maths.lang to maths.code)
    }

    @Test
    fun theInlineParserFollowsTheWebRules() {
        val em = MonarchAiMarkdownEmphasis.Em
        val strong = MonarchAiMarkdownEmphasis.Strong
        assertEquals(
            listOf(
                MonarchAiMarkdownInline.Styled(em, listOf(text("em"))), text(" "),
                MonarchAiMarkdownInline.Styled(strong, listOf(text("strong"))), text(" "),
                MonarchAiMarkdownInline.Styled(MonarchAiMarkdownEmphasis.Del, listOf(text("del"))), text(" "),
                MonarchAiMarkdownInline.Code("x"), text(" "),
                MonarchAiMarkdownInline.Styled(
                    em,
                    listOf(MonarchAiMarkdownInline.Styled(strong, listOf(text("both")))),
                ),
            ),
            MonarchAiMarkdownInlineParser.parse("*em* **strong** ~~del~~ `x` ***both***"),
        )
        val plain = "snake_case_name and 2 * 3 * 4"
        assertEquals(listOf(text(plain)), MonarchAiMarkdownInlineParser.parse(plain))
        val parsedLink = MonarchAiMarkdownInlineParser.parse("[a *b*](https://example.com \"T\")")
        val link = assertIs<MonarchAiMarkdownInline.Link>(parsedLink[0])
        assertEquals("https://example.com" to "T", link.href to link.title)
        assertEquals(
            listOf("Text", "Break", "Text"),
            MonarchAiMarkdownInlineParser.parse("line  \nnext").map { it::class.simpleName },
        )
        val bare = MonarchAiMarkdownInlineParser.parse("see https://example.com/a_b. or www.example.org")
        assertEquals(text("see "), bare[0])
        assertEquals("https://example.com/a_b", assertIs<MonarchAiMarkdownInline.Link>(bare[1]).href)
        assertEquals(text(". or "), bare[2])
        assertEquals("https://www.example.org", assertIs<MonarchAiMarkdownInline.Link>(bare[3]).href)
        val escaped = MonarchAiMarkdownInlineParser.parse("<https://example.com> \\*not em\\* &amp;")
        assertTrue(assertIs<MonarchAiMarkdownInline.Link>(escaped[0]).autolink)
        assertEquals(text(" *not em* &"), escaped[1])
    }

    @Test
    fun incrementalParsingKeepsCompletedBlocks() {
        val first = MonarchAiMarkdownParser.parseIncremental("Para one.\n\nPara tw", null)
        val next = MonarchAiMarkdownParser.parseIncremental("Para one.\n\nPara two.\n\n- item", first)
        assertSame(first.blocks[0], next.blocks[0])
        assertEquals(listOf("Paragraph", "Paragraph", "ListBlock"), next.blocks.map { it::class.simpleName })
        val reset = MonarchAiMarkdownParser.parseIncremental("Other", next)
        assertEquals(1, reset.blocks.size)
        assertSame(next, MonarchAiMarkdownParser.parseIncremental(next.text, next))
    }

    @Test
    fun plainTextOfBlocks() {
        val blocks = MonarchAiMarkdownParser.parse("**Hi** [there](https://example.com).\n\n```\ncode\n```")
        assertEquals(listOf("Hi there.", "code"), blocks.map { MonarchAiMarkdownParser.blockText(it) })
    }

    @Test
    fun anEmptyTextRendersNothing() {
        rule.setMonarchContent { MonarchAiMarkdown(text = "") }
        assertTrue(MonarchAiMarkdownParser.parse("").isEmpty())
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)).assertDoesNotExist()
    }

    @Test
    fun headingsAreHeadingsAndRawHtmlStaysText() {
        rule.setMonarchContent {
            MonarchAiMarkdown(text = "# One\n\n## Two\n\n<b>bold</b> <script>x</script>")
        }
        rule.onNodeWithText("One").assert(isHeading())
        rule.onNodeWithText("Two").assert(isHeading())
        rule.onNodeWithText("<b>bold</b> <script>x</script>").assertExists()
    }

    @Test
    fun safeLinksAreLinksAndShowTheirHostUnsafeOnesStayText() {
        rule.setMonarchContent {
            MonarchAiMarkdown(
                text = "[Docs](https://example.com/docs) [bad](javascript:alert(1)) [data](data:text/html,x) " +
                    "[paypal.com](https://evil.test/login) [see example.com](https://www.example.com) " +
                    "[x](//evil.test/a) [Local](/help)",
            )
        }
        val paragraph = rule.onNodeWithText("Docs (example.com)", substring = true)
        val text = paragraph.annotated()
        assertEquals(
            "Docs (example.com) bad data paypal.com (evil.test) see example.com x Local",
            text.text,
        )
        assertEquals(
            listOf("https://example.com/docs", "https://evil.test/login", "https://www.example.com"),
            text.links().map { it.url },
        )
    }

    @Test
    fun relativeLinksAreLinksWhenTheAppOpensLinks() {
        val opened = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiMarkdown(
                text = "[Local](/help) [Docs](https://example.com)",
                showLinkHosts = false,
                onOpenLink = { opened += it },
            )
        }
        val links = rule.onNodeWithText("Local Docs").annotated().links()
        assertEquals(listOf("/help", "https://example.com"), links.map { it.url })
        links.forEach { link -> link.linkInteractionListener?.onClick(link) }
        assertEquals(listOf("/help", "https://example.com"), opened)
    }

    @Test
    fun footnotesAreNumberedByFirstReferenceAndListedAtTheEnd() {
        rule.setMonarchContent {
            MonarchAiMarkdown(
                text = "Claim[^a] and again[^a], other[^b], missing[^x].\n\n[^b]: Second.\n[^a]: First *note*.",
            )
        }
        rule.onNodeWithText("Claim1 and again1, other2, missing[^x].").assertExists()
        rule.onNodeWithContentDescription("Footnotes").assert(isHeading())
        val first = rule.onNodeWithText("First note.").fetchSemanticsNode().boundsInRoot
        val second = rule.onNodeWithText("Second.").fetchSemanticsNode().boundsInRoot
        assertTrue(first.bottom <= second.top)
    }

    @Test
    fun taskItemsReadTaskWithTheirState() {
        rule.setMonarchContent { MonarchAiMarkdown(text = "- [x] Done\n- [ ] Open") }
        val tasks = taskNodes()
        assertEquals(2, tasks.size)
        val states = tasks.map { it.config[SemanticsProperties.ToggleableState] }
        assertEquals(listOf(ToggleableState.On, ToggleableState.Off), states)
        assertTrue(tasks.all { it.config.getOrNull(SemanticsProperties.Role) == Role.Checkbox })
        assertTrue(tasks.none { it.config.getOrNull(SemanticsActions.OnClick) != null })
    }

    private fun taskNodes() = rule.onAllNodes(
        SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Task")),
        useUnmergedTree = true,
    ).fetchSemanticsNodes()

    @Test
    fun tablesGiveTheirSizeAndScrollInALabelledArea() {
        rule.setMonarchContent { MonarchAiMarkdown(text = "| a | b |\n| :- | -: |\n| 1 | 2 |\n| 3 | 4 |") }
        rule.onNodeWithContentDescription("Table").assertExists()
        val grid = rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionInfo)).fetchSemanticsNode()
        val info = grid.config[SemanticsProperties.CollectionInfo]
        assertEquals(3 to 2, info.rowCount to info.columnCount)
        val cell = rule.onNodeWithText("4").fetchSemanticsNode().config[SemanticsProperties.CollectionItemInfo]
        assertEquals(2 to 1, cell.rowIndex to cell.columnIndex)
    }

    @Test
    fun codeGoesToTheSlotWithItsLanguageAndCompleteness() {
        val seen = mutableListOf<Triple<String, String, Boolean>>()
        rule.setMonarchContent {
            MonarchAiMarkdown(
                text = "```mermaid\ngraph TD\n```\n\n```js\nlet a",
                streaming = true,
                codeBlock = { language, code, complete ->
                    seen += Triple(language, code, complete)
                    Text(code)
                },
            )
        }
        rule.onNodeWithText("graph TD").assertExists()
        assertEquals(listOf(Triple("mermaid", "graph TD", true), Triple("js", "let a", false)), seen.distinct())
    }

    @Test
    fun onlyASafeStandaloneImageGoesToTheImageSlot() {
        rule.setMonarchContent {
            MonarchAiMarkdown(
                text = "![A chart](https://cdn.test/c.png)\n\n![Pixel](//evil.test/p.png)\n\n" +
                    "Inline ![icon](https://cdn.test/i.png) here",
                image = { url, alt -> Text("image $alt from $url") },
            )
        }
        rule.onNodeWithText("image A chart from https://cdn.test/c.png").assertExists()
        rule.onNodeWithText("Pixel").assertExists()
        rule.onNodeWithText("Inline icon here").assertExists()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiMarkdown(
                text = "- [ ] Open[^1]\n\n[^1]: Note\n\n| a |\n| - |\n| b |\n\n[Docs](https://example.com)",
                labels = MonarchAiMarkdownLabels(
                    table = "Tableau",
                    footnotes = "Notes",
                    task = "Tâche",
                    linkHost = { "[$it]" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Tableau").assertExists()
        rule.onNodeWithContentDescription("Notes").assertExists()
        rule.onNodeWithContentDescription("Tâche", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Docs [example.com]").assertExists()
    }

    @Test
    fun theNumberIsAtTheStartInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { MonarchAiMarkdown(text = "1. one\n2. two") }
        rule.onNodeWithText("1.").assertIsBefore(rule.onNodeWithText("one"), LayoutDirection.Rtl)
    }

    @Test
    fun textWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiMarkdown(text = "The quarterly revenue grew across every region this year.")
        }
        rule.onNodeWithText("The quarterly revenue grew across every region this year.").assertTextFullyVisible()
    }

    @Test
    fun linkSafetyAndHostsFollowTheWebRules() {
        val t = MonarchAiMarkdownLinks
        assertTrue(t.safeLink("https://example.com", relativeAllowed = false))
        assertTrue(t.safeLink("mailto:a@b.test", relativeAllowed = false))
        assertFalse(t.safeLink("java\nscript:alert(1)", relativeAllowed = true))
        assertFalse(t.safeLink("data:text/html,x", relativeAllowed = true))
        assertFalse(t.safeLink("//evil.test/a", relativeAllowed = true))
        assertFalse(t.safeLink("/help", relativeAllowed = false))
        assertTrue(t.safeLink("/help", relativeAllowed = true))
        assertFalse(t.safeLink("  ", relativeAllowed = true))
        assertEquals("example.com", t.urlHost("HTTPS://user@Example.com:8080/x"))
        assertNull(t.urlHost("/x"))
        assertNull(t.urlHost("mailto:a@b.test"))
        assertNull(t.shownHost("https://www.example.com", "see example.com"))
        assertEquals("evil.test", t.shownHost("https://evil.test/login", "paypal.com"))
        assertTrue(t.safeImage("https://cdn.test/a.png"))
        assertFalse(t.safeImage("/a.png"))
    }

    @Test
    fun theColoursComeFromTheThemeRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val colors = MonarchAiMarkdownTokens.colors(c)
            assertEquals(c.textDefault, colors.text, theme.id)
            assertEquals(c.textMuted, colors.quoteText, theme.id)
            assertEquals(c.borderDefault, colors.quoteBar, theme.id)
            assertEquals(c.borderSubtle, colors.tableBorder, theme.id)
            assertEquals(c.bgSubtle, colors.tableHeader, theme.id)
            val spans = MonarchAiMarkdownTokens.spanStyles(c, MonarchTypography.Default, TextLinkStyles())
            assertEquals(c.bgMuted, spans.code.background, theme.id)
            assertEquals(c.textMuted, spans.deleted.color, theme.id)
            assertEquals(c.textMuted, spans.host.color, theme.id)
            assertEquals(TextDecoration.Underline, spans.link.style?.textDecoration)
        }
    }
}
