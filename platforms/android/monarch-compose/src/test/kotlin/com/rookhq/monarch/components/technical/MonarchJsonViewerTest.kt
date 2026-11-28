package com.rookhq.monarch.components.technical

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.rookhq.monarch.components.technical.jsonviewer.MonarchJsonParseException
import com.rookhq.monarch.components.technical.jsonviewer.MonarchJsonValue
import com.rookhq.monarch.components.technical.jsonviewer.MonarchJsonViewer
import com.rookhq.monarch.components.technical.jsonviewer.MonarchJsonViewerLabels
import com.rookhq.monarch.components.technical.jsonviewer.MonarchJsonViewerModel
import com.rookhq.monarch.components.technical.jsonviewer.MonarchJsonViewerOpenState
import com.rookhq.monarch.components.technical.jsonviewer.MonarchJsonViewerPaths
import com.rookhq.monarch.components.technical.jsonviewer.MonarchJsonViewerSegment
import com.rookhq.monarch.components.technical.jsonviewer.MonarchJsonViewerTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private val payload = MonarchJsonValue.parse(
    """{"id": "evt_1", "amount": 4200, "price": 1.50, "paid": true, "refund": null,
       "lines": [{"sku": "A1"}, {"sku": "B7"}], "customer name": "Ada"}""",
)
private val small = MonarchJsonValue.parse(
    """{"id": "evt_1", "lines": [{"sku": "A1"}, {"sku": "B7"}], "customer name": "Ada"}""",
)
private val chevron: @androidx.compose.runtime.Composable () -> Unit = {
    MonarchIcon(imageVector = TestIcon, contentDescription = null)
}

internal object MonarchJsonViewerFixtures : ComponentFixtures {
    override val id = "json-viewer"
    override val fixtures = listOf(
        fixture("object, depth 1") {
            MonarchJsonViewer(value = MonarchJsonValue.parse("""{"id": "evt_1", "n": 2}"""), expandIcon = chevron)
        },
        fixture("array with show more, no search, no footer") {
            MonarchJsonViewer(
                value = MonarchJsonValue.from(listOf(1, 2, 3)),
                expandIcon = chevron,
                maxChildren = 1,
                searchable = false,
                copyable = false,
            )
        },
        fixture("primitive root, closed") {
            MonarchJsonViewer(
                value = MonarchJsonValue.String("a long string value"),
                expandIcon = chevron,
                expandDepth = 0,
                maxStringLength = 6,
                searchable = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchJsonViewerTest {
    @get:Rule
    val rule = createComposeRule()

    private fun clipboardText(): String? {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return context.getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.text?.toString()
    }

    @Test
    fun parsingKeepsKeyOrderNumberTextAndTheLastValueOfARepeatedKey() {
        val value = MonarchJsonValue.parse(""" {"b": 1.50, "a": [true, null], "b": "x\n\u00e9"} """)
        assertEquals(
            MonarchJsonValue.Object(
                listOf(
                    "b" to MonarchJsonValue.String("x\né"),
                    "a" to MonarchJsonValue.Array(listOf(MonarchJsonValue.Bool(true), MonarchJsonValue.Null)),
                ),
            ),
            value,
        )
        assertEquals(MonarchJsonValue.Number("12345678901234567890"), MonarchJsonValue.parse("12345678901234567890"))
        assertEquals(MonarchJsonValue.Number("-1.5e+3"), MonarchJsonValue.parse("-1.5e+3"))
    }

    @Test
    fun invalidTextAndDeepNestingAreRejectedWithTheirPosition() {
        assertEquals(6, assertFailsWith<MonarchJsonParseException> { MonarchJsonValue.parse("""{"a": }""") }.offset)
        assertFailsWith<MonarchJsonParseException> { MonarchJsonValue.parse("[1,]") }
        assertFailsWith<MonarchJsonParseException> { MonarchJsonValue.parse("01x") }
        assertFailsWith<MonarchJsonParseException> { MonarchJsonValue.parse("\"\\u-123\"") }
        assertFailsWith<MonarchJsonParseException> { MonarchJsonValue.parse("[".repeat(101) + "]".repeat(101)) }
        MonarchJsonValue.parse("[".repeat(100) + "]".repeat(100))
    }

    @Test
    fun kotlinValuesBecomeJson() {
        assertEquals(
            MonarchJsonValue.Object(
                listOf(
                    "n" to MonarchJsonValue.Number("2"),
                    "list" to MonarchJsonValue.Array(listOf(MonarchJsonValue.Bool(false), MonarchJsonValue.Null)),
                ),
            ),
            MonarchJsonValue.from(mapOf("n" to 2, "list" to listOf(false, null))),
        )
    }

    @Test
    fun pathsQuoteKeysThatAreNotIdentifiers() {
        val segments = listOf(
            MonarchJsonViewerSegment.Key("users"),
            MonarchJsonViewerSegment.Index(0),
            MonarchJsonViewerSegment.Key("first name"),
        )
        assertEquals("$.users[0][\"first name\"]", MonarchJsonViewerPaths.path(segments, "$"))
        assertEquals("users[0]", MonarchJsonViewerPaths.path(segments.take(2), ""))
        assertEquals("\"a\\\"b\\n\\u0001\"", MonarchJsonViewerPaths.quoted("a\"b\n\u0001"))
    }

    @Test
    fun copiedValuesAreJsonTextExceptStrings() {
        assertEquals("Ada", MonarchJsonViewerPaths.copyText(MonarchJsonValue.String("Ada")))
        assertEquals(
            "{\n  \"a\": [\n    1,\n    \"x\"\n  ],\n  \"b\": {}\n}",
            MonarchJsonViewerPaths.copyText(MonarchJsonValue.parse("""{"a": [1, "x"], "b": {}}""")),
        )
    }

    @Test
    fun searchFindsKeysAndValuesOpensTheirContainersAndStops() {
        val search = MonarchJsonViewerModel.search(payload, "b7", maxMatches = 500)
        assertEquals(setOf("$.lines[1].sku"), search.matched)
        assertEquals(setOf("$", "$.lines", "$.lines[1]"), search.open)
        assertEquals(2, search.reveal["$.lines"])
        val capped = MonarchJsonViewerModel.search(payload, "sku", maxMatches = 1)
        assertEquals(1, capped.count)
        assertTrue(capped.capped)
        assertEquals(listOf(0..1, 4..5), MonarchJsonViewerModel.matchRanges("AbxxaB", "ab"))
    }

    @Test
    fun onlyOpenContainersAreFlattenedAndLargeOnesShowMore() {
        val list = MonarchJsonValue.from((1..5).toList())
        val state = MonarchJsonViewerOpenState(emptyMap(), emptyMap(), MonarchJsonViewerModel.search(list, "", 1), 1, 2)
        val rows = MonarchJsonViewerModel.rows(list, state)
        assertEquals(listOf("$", "$[0]", "$[1]", "$#more"), rows.map { it.id })
        assertEquals(3, rows.last().remaining)
        val closed = state.copy(baseDepth = 0)
        assertEquals(1, MonarchJsonViewerModel.rows(list, closed).size)
        val reopened = closed.copy(overrides = mapOf("$" to true), limits = mapOf("$" to 5))
        assertEquals(6, MonarchJsonViewerModel.rows(list, reopened).size)
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { tokens ->
            val colors = MonarchColors(tokens.colors)
            val t = MonarchJsonViewerTokens
            assertEquals(colors.bgMuted, t.row(true, colors))
            assertEquals(colors.bgSurface, t.row(false, colors))
            assertEquals(colors.warningSubtle, t.match(colors))
            assertEquals(colors.textLink, t.more(colors))
            assertEquals(colors.infoStrong, t.key(MonarchJsonViewerSegment.Key("a"), colors))
            assertEquals(colors.textMuted, t.key(MonarchJsonViewerSegment.Index(0), colors))
            assertEquals(colors.successStrong, t.value(MonarchJsonValue.String("a"), colors))
            assertEquals(colors.accentStrong, t.value(MonarchJsonValue.Number("1"), colors))
            assertEquals(colors.warningStrong, t.value(MonarchJsonValue.Null, colors))
            assertEquals(colors.borderSubtle, t.border(colors))
        }
    }

    @Test
    fun rowsAreButtonsWithSummariesStatesAndSelection() {
        rule.setMonarchContent {
            MonarchJsonViewer(
                value = small,
                expandIcon = chevron,
                modifier = Modifier.height(460.dp),
                searchable = false,
            )
        }
        rule.onNodeWithContentDescription("JSON").assertExists()
        val root = rule.onNodeWithContentDescription("{ } 3 keys")
        root.assertIsSelected()
        assertEquals("Expanded", root.stateDescription())
        rule.onNodeWithContentDescription("id: \"evt_1\"").assertIsNotSelected()
            .assertTouchTargetAtLeast48dp(rule.density)
        val lines = rule.onNodeWithContentDescription("lines: [ ] 2 items")
        assertEquals("Collapsed", lines.stateDescription())
        lines.performClick()
        assertEquals("Expanded", lines.stateDescription())
        lines.assertIsSelected()
        rule.onNodeWithContentDescription("0: { } 1 key").assertExists()
        rule.onNodeWithContentDescription("Selected, $.lines").assertExists()
    }

    @Test
    fun copyPathAndCopyValueCopyTheSelection() {
        val copied = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchJsonViewer(
                value = small,
                expandIcon = chevron,
                modifier = Modifier.height(460.dp),
                searchable = false,
                onCopy = { copied += it },
            )
        }
        rule.onNodeWithContentDescription("customer name: \"Ada\"").performClick()
        rule.onNodeWithContentDescription("Copy path").performClick()
        rule.waitForIdle()
        assertEquals("$[\"customer name\"]", clipboardText())
        rule.onNodeWithContentDescription("Copy value").performClick()
        rule.waitForIdle()
        assertEquals("Ada", clipboardText())
        assertEquals(listOf("$[\"customer name\"]", "Ada"), copied)
    }

    @Test
    fun searchOpensMatchesCountsThemAndCollapseAllResets() {
        val selected = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchJsonViewer(
                value = small,
                expandIcon = chevron,
                modifier = Modifier.height(460.dp),
                copyable = false,
                onSelect = { path, _ -> selected += path },
            )
        }
        rule.onNodeWithContentDescription("Search keys and values").performTextInput("b7")
        rule.onNodeWithText("1 match")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithContentDescription("sku: \"B7\"").assertExists()
        rule.onNodeWithText("Collapse all").performClick()
        rule.onAllNodesWithContentDescription("sku: \"B7\"").assertCountEquals(0)
        rule.onAllNodesWithContentDescription("lines: [ ] 2 items").assertCountEquals(1)
        assertEquals(listOf("$"), selected)
    }

    @Test
    fun showMoreRevealsTheNextBatch() {
        rule.setMonarchContent {
            MonarchJsonViewer(
                value = MonarchJsonValue.from(listOf("a", "b", "c")),
                expandIcon = chevron,
                maxChildren = 2,
                searchable = false,
            )
        }
        rule.onNodeWithText("Show 1 more (1 hidden)").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("2: \"c\"").assertIsSelected()
    }

    @Test
    fun labelsReplaceBuiltInStrings() {
        rule.setMonarchContent {
            MonarchJsonViewer(
                value = MonarchJsonValue.parse("""{"s": "abcdefgh"}"""),
                expandIcon = chevron,
                maxStringLength = 3,
                searchable = false,
                copyable = false,
                labels = MonarchJsonViewerLabels(label = "Antwort", keys = { "$it Schlüssel" }),
            )
        }
        rule.onNodeWithContentDescription("Antwort").assertExists()
        rule.onNodeWithContentDescription("{ } 1 Schlüssel").assertExists()
        rule.onNodeWithContentDescription("s: \"abc… (8 characters)\"").assertExists()
    }

    @Test
    fun textIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchJsonViewer(
                value = MonarchJsonValue.parse("""{"description": "A long value that wraps"}"""),
                expandIcon = chevron,
                searchable = false,
                copyable = false,
            )
        }
        rule.onNodeWithText("description: \"A long value that wraps\"", useUnmergedTree = true)
            .assertTextFullyVisible()
    }
}
