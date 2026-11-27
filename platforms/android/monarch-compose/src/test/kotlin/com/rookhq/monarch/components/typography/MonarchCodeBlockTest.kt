package com.rookhq.monarch.components.typography

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.typography.codeblock.MonarchCodeBlock
import com.rookhq.monarch.components.typography.codeblock.MonarchCodeBlockDiffKind
import com.rookhq.monarch.components.typography.codeblock.MonarchCodeBlockHighlighter
import com.rookhq.monarch.components.typography.codeblock.MonarchCodeBlockLabels
import com.rookhq.monarch.components.typography.codeblock.MonarchCodeBlockLines
import com.rookhq.monarch.components.typography.codeblock.MonarchCodeBlockToken
import com.rookhq.monarch.components.typography.codeblock.MonarchCodeBlockTokenType
import com.rookhq.monarch.components.typography.codeblock.MonarchCodeBlockTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchCodeBlockFixtures : ComponentFixtures {
    override val id = "code-block"
    override val fixtures = listOf(
        fixture("highlighted with a title") {
            MonarchCodeBlock(
                code = "const retries = 3; // keep it small\nexport default \"client\";",
                language = "ts",
                title = "client.ts",
            )
        },
        fixture("line numbers, wrapped") {
            MonarchCodeBlock(
                code = "export const client = createClient({ retries: 3, timeout: 2500, " +
                    "baseURL: \"https://api.example.com\" });",
                language = "ts",
                showLineNumbers = true,
                wrapped = true,
            )
        },
        fixture("unwrapped long line") {
            MonarchCodeBlock(
                code = "curl https://api.example.com/v1/items -H \"Authorization: Bearer \$TOKEN\" -H \"Accept: json\"",
                language = "bash",
            )
        },
        fixture("diff") {
            MonarchCodeBlock(code = "@@ -1,2 +1,2 @@\n-const port = 3000;\n+const port = 8080;", language = "diff")
        },
        fixture("no header, unknown language") {
            MonarchCodeBlock(code = "key = value", language = "toml", showWrapToggle = false, copyable = false)
        },
        fixture("no header at all") {
            MonarchCodeBlock(code = "", showWrapToggle = false, copyable = false)
        },
        fixture("long title") {
            MonarchCodeBlock(code = "{ \"a\": 1 }", language = "json", title = "settings/notifications/defaults.json")
        },
        fixture("header actions") {
            MonarchCodeBlock(
                code = "npm test",
                language = "bash",
                headerActions = { MonarchButton(text = "Run", onClick = {}) },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchCodeBlockTest {
    @get:Rule
    val rule = createComposeRule()

    private fun clipboardText(): String? {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        return clipboard.primaryClip?.getItemAt(0)?.text?.toString()
    }

    private fun types(code: String, language: String): List<Pair<MonarchCodeBlockTokenType, String>> =
        MonarchCodeBlockHighlighter.tokenize(code, language).map { it.type to it.text }

    @Test
    fun theHighlighterFindsKeywordsStringsNumbersAndComments() {
        val tokens = types("const port = 8080; // main\nlet s = 'x';", "ts")
        assertTrue(MonarchCodeBlockTokenType.Keyword to "const" in tokens)
        assertTrue(MonarchCodeBlockTokenType.Number to "8080" in tokens)
        assertTrue(MonarchCodeBlockTokenType.Comment to "// main" in tokens)
        assertTrue(MonarchCodeBlockTokenType.Str to "'x'" in tokens)
        assertTrue(MonarchCodeBlockTokenType.Keyword to "let" in tokens)
        // Adjacent runs of one type merge, and nothing is lost.
        assertEquals("const port = 8080; // main\nlet s = 'x';", tokens.joinToString("") { it.second })
    }

    @Test
    fun theHighlighterKnowsTheLanguageRules() {
        val json = types("{ \"name\": \"monarch\", \"ok\": true }", "json")
        assertTrue(MonarchCodeBlockTokenType.Property to "\"name\"" in json)
        assertTrue(MonarchCodeBlockTokenType.Str to "\"monarch\"" in json)
        assertTrue(MonarchCodeBlockTokenType.Keyword to "true" in json)
        // In shell, # starts a comment only after white space.
        val bash = types("echo a#b # note", "sh")
        assertTrue(MonarchCodeBlockTokenType.Comment to "# note" in bash)
        assertTrue(bash.none { it.second.contains("#b") && it.first == MonarchCodeBlockTokenType.Comment })
        assertTrue(MonarchCodeBlockTokenType.Keyword to "SELECT" in types("SELECT id FROM t", "postgres"))
        val css = types("a { color: #fff; }", "scss")
        assertTrue(MonarchCodeBlockTokenType.Property to "color" in css)
        assertTrue(MonarchCodeBlockTokenType.Number to "#fff" in css)
        val html = types("<a href=\"/x\">Go</a><!-- c -->", "html")
        assertTrue(MonarchCodeBlockTokenType.Tag to "<a" in html)
        assertTrue(MonarchCodeBlockTokenType.Attr to "href" in html)
        assertTrue(MonarchCodeBlockTokenType.Str to "\"/x\"" in html)
        assertTrue(MonarchCodeBlockTokenType.Comment to "<!-- c -->" in html)
        // A Rust lifetime is not a string.
        assertTrue(types("fn f<'a>(x: &'a str)", "rust").none { it.first == MonarchCodeBlockTokenType.Str })
        assertEquals(listOf(MonarchCodeBlockTokenType.Plain to "a = 1"), types("a = 1", "toml"))
    }

    @Test
    fun linesFollowTheEdgeRules() {
        fun texts(code: String) =
            MonarchCodeBlockLines.lines(code, null, diff = false, highlighted = true).map { it.text }
        assertEquals(listOf("a", "b"), texts("a\r\nb\n"))
        assertEquals(listOf(""), texts(""))
        assertEquals(listOf("    x"), texts("\tx"))
        assertEquals(listOf("a", "", "b"), texts("a\n\nb"))
        assertTrue(MonarchCodeBlockLines.isDiff(null, "patch"))
        assertTrue(!MonarchCodeBlockLines.isDiff(false, "diff"))
        assertTrue(MonarchCodeBlockLines.isDiff(true, "ts"))
    }

    @Test
    fun diffLinesGetTheirKinds() {
        val kinds = MonarchCodeBlockLines.diffKinds(
            listOf("--- a/x", "+++ b/x", "@@ -1 +1 @@", "-old", "+new", " same", "--- not meta now"),
        )
        assertEquals(
            listOf(
                MonarchCodeBlockDiffKind.Meta,
                MonarchCodeBlockDiffKind.Meta,
                MonarchCodeBlockDiffKind.Hunk,
                MonarchCodeBlockDiffKind.Remove,
                MonarchCodeBlockDiffKind.Add,
                MonarchCodeBlockDiffKind.Context,
                MonarchCodeBlockDiffKind.Remove,
            ),
            kinds,
        )
        val lines = MonarchCodeBlockLines.lines("-old\n+new\n same", "diff", diff = true, highlighted = true)
        assertEquals(listOf("old", "new", "same"), lines.map { it.text })
    }

    @Test
    fun theCodeIsNamedAfterTheTitleTheLanguageOrCode() {
        rule.setMonarchContent {
            androidx.compose.foundation.layout.Column {
                MonarchCodeBlock(code = "a", title = "client.ts", language = "ts")
                MonarchCodeBlock(code = "b", language = "py")
                MonarchCodeBlock(code = "c")
                MonarchCodeBlock(code = "d", language = "toml")
            }
        }
        rule.onNodeWithContentDescription("client.ts").assertExists()
        rule.onNodeWithContentDescription("Python code").assertExists()
        rule.onNodeWithContentDescription("Code").assertExists()
        rule.onNodeWithContentDescription("toml code").assertExists()
        rule.onNodeWithContentDescription("Copy client.ts").assertExists()
        rule.onNodeWithText("Python").assertExists()
    }

    @Test
    fun eachLineIsReadAndDiffLinesSayWhatChanged() {
        rule.setMonarchContent {
            MonarchCodeBlock(
                code = "@@ -1 +1 @@\n-const a = 1;\n+const a = 2;\n   \n same",
                language = "diff",
                showLineNumbers = true,
            )
        }
        rule.onNodeWithText("@@ -1 +1 @@").assertExists()
        rule.onNodeWithText("Removed: const a = 1;").assertExists()
        rule.onNodeWithText("Added: const a = 2;").assertExists()
        rule.onNodeWithText("same").assertExists()
        // Line numbers and markers are not read.
        rule.onNodeWithText("  1", substring = true).assertDoesNotExist()
        rule.onNodeWithText("+").assertDoesNotExist()
    }

    @Test
    fun theWrapToggleKeepsItsOwnState() {
        rule.setMonarchContent { MonarchCodeBlock(code = "a") }
        val toggle = rule.onNodeWithText("Wrap lines")
        toggle.assertIsNotSelected().performClick()
        toggle.assertIsSelected()
    }

    @Test
    fun aHeldWrapStateOnlyChangesThroughTheCallback() {
        val changes = mutableListOf<Boolean>()
        var held by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchCodeBlock(code = "b", wrapped = held, onWrappedChange = { changes += it })
        }
        rule.onNodeWithText("Wrap lines").performClick()
        assertEquals(listOf(true), changes)
        rule.onNodeWithText("Wrap lines").assertIsNotSelected()
        held = true
        rule.onNodeWithText("Wrap lines").assertIsSelected()
    }

    @Test
    fun longLinesScrollUnlessWrapped() {
        val long = "const message = \"a line that is much wider than the block it is shown in on a phone\";"
        var wrapped by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchCodeBlock(code = long, modifier = Modifier.width(200.dp).testTag("block"), wrapped = wrapped)
        }
        fun bounds(text: String) = rule.onNodeWithText(text).getUnclippedBoundsInRoot()
        val block = rule.onNodeWithTag("block").getUnclippedBoundsInRoot()
        val blockWidth = block.right - block.left
        val unwrapped = bounds(long)
        assertTrue(unwrapped.right - unwrapped.left > blockWidth, "One line, wider than the block")
        wrapped = true
        rule.waitForIdle()
        val folded = bounds(long)
        assertTrue(folded.right - folded.left <= blockWidth, "Wrapped inside the block")
        assertTrue(folded.bottom - folded.top > unwrapped.bottom - unwrapped.top, "On several lines")
    }

    @Test
    fun copyingCopiesTheCodeAsGiven() {
        val copied = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchCodeBlock(code = "a\tb\n", title = "tabs.txt", onCopy = { copied += it })
        }
        rule.onNodeWithContentDescription("Copy tabs.txt").performClick()
        rule.waitForIdle()
        assertEquals("a\tb\n", clipboardText())
        assertEquals(listOf("a\tb\n"), copied)
    }

    @Test
    fun withoutTitleToggleAndCopyThereIsNoHeader() {
        rule.setMonarchContent {
            MonarchCodeBlock(code = "x", showWrapToggle = false, copyable = false)
        }
        rule.onNodeWithText("Wrap lines").assertDoesNotExist()
        rule.onNodeWithContentDescription("Copy Code").assertDoesNotExist()
        rule.onNodeWithText("x").assertExists()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchCodeBlock(
                code = "+a\n-b",
                language = "diff",
                labels = MonarchCodeBlockLabels(
                    copy = "Copier",
                    wrap = "Retour à la ligne",
                    added = "Ajouté",
                    removed = "Supprimé",
                    codeName = { title, language -> "Exemple ${title ?: language}" },
                ),
            )
        }
        rule.onNodeWithText("Retour à la ligne").assertExists()
        rule.onNodeWithText("Ajouté: a").assertExists()
        rule.onNodeWithText("Supprimé: b").assertExists()
        rule.onNodeWithContentDescription("Exemple diff").assertExists()
        rule.onNodeWithContentDescription("Copier Exemple diff").assertExists()
    }

    @Test
    fun theHeaderControlsHave48dpTargets() {
        rule.setMonarchContent { MonarchCodeBlock(code = "a", title = "a.ts") }
        rule.onNodeWithText("Wrap lines").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithContentDescription("Copy a.ts").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theHeaderMirrorsButTheCodeReadsLeftToRight() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchCodeBlock(
                code = "abc",
                modifier = Modifier.testTag("block"),
                title = "a.ts",
                showLineNumbers = true,
                copyable = false,
            )
        }
        val gutters = mutableListOf<Float>()
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("a.ts").assertIsBefore(rule.onNodeWithText("Wrap lines"), direction)
            val block = rule.onNodeWithTag("block").getUnclippedBoundsInRoot()
            val code = rule.onNodeWithText("abc").getUnclippedBoundsInRoot()
            // The code fills the line after the number gutter, which stays on the left.
            gutters += (code.left - block.left).value
        }
        assertTrue(gutters[0] > 0f, "The line number comes first")
        assertEquals(gutters[0], gutters[1], 0.5f, "The code is not mirrored")
    }

    @Test
    fun aLongTitleWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchCodeBlock(code = "a", title = "settings/notifications/defaults.json")
        }
        rule.onNodeWithText("settings/notifications/defaults.json").assertTextFullyVisible()
        rule.onNodeWithText("Wrap lines").assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val t = MonarchCodeBlockTokens
            assertEquals(colors.infoStrong, t.tokenColor(MonarchCodeBlockTokenType.Keyword, colors), theme.id)
            assertEquals(colors.successStrong, t.tokenColor(MonarchCodeBlockTokenType.Str, colors), theme.id)
            assertEquals(colors.warningStrong, t.tokenColor(MonarchCodeBlockTokenType.Number, colors), theme.id)
            assertEquals(colors.dangerStrong, t.tokenColor(MonarchCodeBlockTokenType.Tag, colors), theme.id)
            assertEquals(colors.aiStrong, t.tokenColor(MonarchCodeBlockTokenType.Attr, colors), theme.id)
            assertEquals(colors.aiStrong, t.tokenColor(MonarchCodeBlockTokenType.Property, colors), theme.id)
            assertEquals(FontStyle.Italic, t.tokenStyle(MonarchCodeBlockTokenType.Comment, colors)?.fontStyle)
            assertEquals(colors.textMuted, t.tokenStyle(MonarchCodeBlockTokenType.Comment, colors)?.color)
            assertNull(t.tokenStyle(MonarchCodeBlockTokenType.Plain, colors))
            assertEquals(colors.aiDiffAdd, t.lineBackground(MonarchCodeBlockDiffKind.Add, colors), theme.id)
            assertEquals(colors.aiDiffRemove, t.lineBackground(MonarchCodeBlockDiffKind.Remove, colors), theme.id)
            assertEquals(Color.Transparent, t.lineBackground(null, colors), theme.id)
            assertEquals(colors.aiDiffAddStrong, t.markerColor(MonarchCodeBlockDiffKind.Add, colors), theme.id)
            assertEquals(colors.aiDiffRemoveStrong, t.markerColor(MonarchCodeBlockDiffKind.Remove, colors), theme.id)
            assertEquals(colors.infoStrong, t.lineColor(MonarchCodeBlockDiffKind.Hunk, colors), theme.id)
            assertEquals(colors.textMuted, t.lineColor(MonarchCodeBlockDiffKind.Meta, colors), theme.id)
            assertEquals(colors.bgSubtle, t.container(colors), theme.id)
            assertEquals(colors.bgMuted, t.header(colors), theme.id)
        }
        assertEquals("+", MonarchCodeBlockLines.marker(MonarchCodeBlockDiffKind.Add))
        assertEquals("−", MonarchCodeBlockLines.marker(MonarchCodeBlockDiffKind.Remove))
        assertEquals(
            listOf(listOf(MonarchCodeBlockToken(MonarchCodeBlockTokenType.Plain, "a")), emptyList()),
            MonarchCodeBlockHighlighter.toLines(listOf(MonarchCodeBlockToken(MonarchCodeBlockTokenType.Plain, "a\n"))),
        )
    }

    @Test
    fun headerActionsSitInTheHeader() {
        var runs = 0
        rule.setMonarchContent {
            MonarchCodeBlock(
                code = "npm test",
                language = "bash",
                showWrapToggle = false,
                copyable = false,
                headerActions = { MonarchButton(text = "Run", onClick = { runs++ }) },
            )
        }
        rule.onNodeWithText("Run").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, runs)
    }
}
