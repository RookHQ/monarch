package dev.monarchui.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallState
import dev.monarchui.components.intelligence.aitoolcall.MonarchAiToolCall
import dev.monarchui.components.intelligence.aitoolcall.MonarchAiToolCallFormat
import dev.monarchui.components.intelligence.aitoolcall.MonarchAiToolCallLabels
import dev.monarchui.components.intelligence.aitoolcall.MonarchAiToolCallTokens
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Query = MonarchAiJSON.Object(mapOf("query" to MonarchAiJSON.Text("Q3 plan")))

private fun call(
    state: MonarchAiToolCallState = MonarchAiToolCallState.Succeeded,
    name: String = "search_documents",
    input: MonarchAiJSON = Query,
    output: MonarchAiJSON? = MonarchAiJSON.Object(mapOf("count" to MonarchAiJSON.Number(3.0))),
    error: String? = null,
    startedAt: String? = "2026-03-02T09:00:00Z",
    endedAt: String? = "2026-03-02T09:00:12Z",
    inputText: String? = null,
) = MonarchAiToolCallPart(
    id = "call-1",
    name = name,
    input = input,
    state = state,
    output = output,
    error = error,
    startedAt = startedAt,
    endedAt = endedAt,
    inputText = inputText,
)

internal object MonarchAiToolCallFixtures : ComponentFixtures {
    override val id = "ai-tool-call"
    override val fixtures = listOf(
        fixture("succeeded with icons, collapsed") {
            MonarchAiToolCall(
                part = call(),
                icon = { MonarchIcon(TestIcon, contentDescription = null) },
                toggleIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                stateIcon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        },
        fixture("pending, awaiting and running") {
            MonarchAiToolCall(part = call(MonarchAiToolCallState.PendingApproval, output = null, endedAt = null))
            MonarchAiToolCall(part = call(MonarchAiToolCallState.AwaitingResult, output = null, endedAt = null))
            MonarchAiToolCall(part = call(MonarchAiToolCallState.Running, output = null, endedAt = null))
        },
        fixture("denied and cancelled") {
            MonarchAiToolCall(part = call(MonarchAiToolCallState.Denied, output = null))
            MonarchAiToolCall(part = call(MonarchAiToolCallState.Cancelled, output = null))
        },
        fixture("failed with an error") {
            MonarchAiToolCall(
                part = call(MonarchAiToolCallState.Failed, output = null, error = "The mail server refused it."),
            )
        },
        fixture("expanded with a truncated output") {
            MonarchAiToolCall(
                part = call(input = MonarchAiJSON.Null, output = MonarchAiJSON.Text("line one\nline two")),
                expanded = true,
                maxValueLength = 8,
            )
        },
        fixture("expanded, no details yet") {
            MonarchAiToolCall(part = call(input = MonarchAiJSON.Null, output = null), expanded = true)
        },
        fixture("long summary") {
            MonarchAiToolCall(
                part = call(),
                summary = "Searched every shared drive for the quarterly planning documents of the sales team",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiToolCallTest {
    @get:Rule
    val rule = createComposeRule()

    private fun header(title: String) = rule.onNode(hasText(title, substring = true) and hasClickAction())

    @Test
    fun theHeaderIsOneButtonWithTheTitleStateAndDuration() {
        rule.setMonarchContent { MonarchAiToolCall(part = call()) }
        header("Search documents")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(hasText("Done"))
            .assert(hasText("12s"))
            .assertTouchTargetAtLeast48dp(rule.density)
        assertEquals("Collapsed", header("Search documents").stateDescription())
    }

    @Test
    fun activatingTheHeaderShowsTheToolInputAndOutput() {
        rule.setMonarchContent { MonarchAiToolCall(part = call()) }
        rule.onNodeWithText("Tool: search_documents").assertDoesNotExist()
        header("Search documents").performClick()
        rule.waitForIdle()
        assertEquals("Expanded", header("Search documents").stateDescription())
        rule.onNodeWithText("Tool: search_documents").assertExists()
        rule.onNodeWithText("Input", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Output", useUnmergedTree = true).assertExists()
        header("Search documents").performSemanticsAction(SemanticsActions.Collapse)
        rule.waitForIdle()
        rule.onNodeWithText("Tool: search_documents").assertDoesNotExist()
    }

    @Test
    fun aControlledCardReportsTheNewState() {
        val changes = mutableListOf<Boolean>()
        rule.setMonarchContent {
            MonarchAiToolCall(part = call(), expanded = false, onExpandedChange = { changes += it })
        }
        header("Search documents").performClick()
        rule.waitForIdle()
        assertEquals(listOf(true), changes)
        rule.onNodeWithText("Tool: search_documents").assertDoesNotExist()
    }

    @Test
    fun aCallWithoutDetailsSaysSo() {
        rule.setMonarchContent {
            MonarchAiToolCall(part = call(input = MonarchAiJSON.Null, output = null), expanded = true)
        }
        rule.onNodeWithText("No details yet.").assertExists()
    }

    @Test
    fun aFailedCallShowsItsErrorWithoutOpening() {
        rule.setMonarchContent {
            MonarchAiToolCall(part = call(MonarchAiToolCallState.Failed, output = null, error = "Timed out"))
        }
        rule.onNodeWithContentDescription("Error: Timed out").assertExists()
        header("Search documents").assert(hasText("Failed"))
    }

    @Test
    fun aLongValueIsCutWithShowAll() {
        rule.setMonarchContent {
            MonarchAiToolCall(
                part = call(input = MonarchAiJSON.Null, output = MonarchAiJSON.Text("a".repeat(30))),
                expanded = true,
                maxValueLength = 10,
            )
        }
        rule.onNodeWithText("Showing 10 of 30 characters").assertExists()
        rule.onNodeWithText("Show all").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Showing 10 of 30 characters").assertDoesNotExist()
    }

    @Test
    fun aNewStateIsAnnouncedButNotTheFirst() {
        var state by mutableStateOf(MonarchAiToolCallState.Running)
        rule.setMonarchContent { MonarchAiToolCall(part = call(state, output = null)) }
        rule.onNodeWithContentDescription("Search documents: Running").assertDoesNotExist()
        state = MonarchAiToolCallState.Succeeded
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Search documents: Done")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun announcementsCanBeTurnedOff() {
        var state by mutableStateOf(MonarchAiToolCallState.Running)
        rule.setMonarchContent { MonarchAiToolCall(part = call(state, output = null), announcesChanges = false) }
        state = MonarchAiToolCallState.Failed
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Search documents: Failed").assertDoesNotExist()
    }

    @Test
    fun summaryAndDescribeToolAndToolLabelsNameTheCall() {
        rule.setMonarchContent {
            MonarchAiToolCall(part = call(name = "send_email"), toolLabels = mapOf("send_email" to "Send an email"))
            MonarchAiToolCall(
                part = call(name = "find"),
                describeTool = { "Searched for ${it.input["query"]?.stringValue}" },
            )
            MonarchAiToolCall(part = call(name = "x"), summary = "Custom summary", describeTool = { "Ignored" })
        }
        header("Send an email").assertExists()
        header("Searched for Q3 plan").assertExists()
        header("Custom summary").assertExists()
        header("Ignored").assertDoesNotExist()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiToolCall(
                part = call(input = MonarchAiJSON.Null, output = null),
                expanded = true,
                labels = MonarchAiToolCallLabels(
                    state = { "Hecho" },
                    formatDuration = { "${it / 1000} seg" },
                    tool = "Herramienta",
                    empty = "Sin detalles.",
                    expanded = "Abierto",
                ),
            )
        }
        header("Search documents").assert(hasText("Hecho")).assert(hasText("12 seg"))
        assertEquals("Abierto", header("Search documents").stateDescription())
        rule.onNodeWithText("Herramienta: search_documents").assertExists()
        rule.onNodeWithText("Sin detalles.").assertExists()
    }

    @Test
    fun theStateComesAfterTheTitleRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiToolCall(part = call(), icon = { MonarchIcon(TestIcon, contentDescription = "tool") })
        }
        rule.onNodeWithContentDescription("tool", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Search documents", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun aLongSummaryWrapsAtTheLargestFontSize() {
        val text = "Searched every shared drive for the quarterly planning documents"
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchAiToolCall(part = call(), summary = text) }
        rule.onNodeWithText(text, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun namesAreHumanized() {
        val t = MonarchAiToolCallTokens
        assertEquals("Search documents", t.humanize("search_documents"))
        assertEquals("Search documents", t.humanize("searchDocuments"))
        assertEquals("Search documents", t.humanize("search-documents"))
        assertEquals("Crm contacts list", t.humanize("crm.contacts/list"))
        assertEquals("___", t.humanize("___"))
        assertEquals("Mine", t.label("x", mapOf("x" to "Mine")))
    }

    @Test
    fun durationsComeFromTheTimes() {
        val t = MonarchAiToolCallTokens
        assertEquals(12_000L, t.duration(call()))
        assertNull(t.duration(call(endedAt = null)))
        assertNull(t.duration(call(endedAt = "2026-03-02T08:00:00Z")))
        assertNull(t.duration(call(endedAt = "soon")))
        assertEquals(0.1, t.durationParts(20).first)
        assertEquals(0.4, t.durationParts(400).first)
        assertEquals(Triple<Double?, Long, Long>(null, 0, 12), t.durationParts(12_000))
        assertEquals(Triple<Double?, Long, Long>(null, 2, 5), t.durationParts(125_000))
    }

    @Test
    fun valuesArePrettyPrinted() {
        val t = MonarchAiToolCallFormat
        val value = MonarchAiJSON.Object(
            mapOf("b" to MonarchAiJSON.Number(1.0), "a" to MonarchAiJSON.Array(listOf(MonarchAiJSON.Bool(true)))),
        )
        assertEquals("{\n  \"b\": 1,\n  \"a\": [\n    true\n  ]\n}", t.format(value).text)
        assertTrue(t.format(value).json)
        assertEquals("{\n  \"x\": 1\n}", t.format(MonarchAiJSON.Text(" {\"x\":1}")).text)
        assertFalse(t.format(MonarchAiJSON.Text("{not json")).json)
        assertEquals("plain", t.format(MonarchAiJSON.Text("plain")).text)
        assertEquals("{}", t.format(MonarchAiJSON.Object(emptyMap())).text)
    }

    @Test
    fun streamingTextStandsInForAnEmptyInput() {
        val t = MonarchAiToolCallTokens
        assertEquals(
            MonarchAiJSON.Text("{\"q\":"),
            t.input(call(input = MonarchAiJSON.Object(emptyMap()), inputText = "{\"q\":")),
        )
        assertEquals(Query, t.input(call(inputText = "ignored")))
        assertNull(t.input(call(input = MonarchAiJSON.Null)))
    }

    @Test
    fun textIsCutAtALineEndWhenOneIsNear() {
        val t = MonarchAiToolCallFormat
        assertFalse(t.truncate("short", 10).truncated)
        assertFalse(t.truncate("a".repeat(50), 0).truncated)
        assertEquals("aaaaaaaaa", t.truncate("aaaaaaaaa\nbbbbbbbb", 10).text)
        assertEquals("aa\nbbbbbbb", t.truncate("aa\nbbbbbbbbbbb", 10).text)
    }

    @Test
    fun theColoursAndTonesComeFromTheTheme() {
        val t = MonarchAiToolCallTokens
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.bgSubtle, t.cardColors(c).container, theme.id)
            assertEquals(c.borderSubtle, t.cardColors(c).border, theme.id)
        }
        assertEquals(MonarchTone.Warning, t.tone(MonarchAiToolCallState.PendingApproval))
        assertEquals(MonarchTone.Info, t.tone(MonarchAiToolCallState.Running))
        assertEquals(MonarchTone.Success, t.tone(MonarchAiToolCallState.Succeeded))
        assertEquals(MonarchTone.Danger, t.tone(MonarchAiToolCallState.Failed))
        assertEquals(MonarchTone.Neutral, t.tone(MonarchAiToolCallState.Denied))
    }
}
