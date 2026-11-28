package dev.monarchui.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.components.intelligence.aistructuredanswer.MonarchAiStructuredAnswer
import dev.monarchui.components.intelligence.aistructuredanswer.MonarchAiStructuredAnswerLabels
import dev.monarchui.components.intelligence.aistructuredanswer.MonarchAiStructuredAnswerLayout
import dev.monarchui.components.intelligence.aistructuredanswer.MonarchAiStructuredAnswerTokens
import dev.monarchui.components.intelligence.aistructuredanswer.MonarchAiStructuredField
import dev.monarchui.components.intelligence.aistructuredanswer.MonarchAiStructuredFieldType
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
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
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val BriefSchema = listOf(
    MonarchAiStructuredField("candidate", "Candidate"),
    MonarchAiStructuredField(
        "stage",
        "Stage",
        MonarchAiStructuredFieldType.Badge,
        tone = { if (it == "Rejected") MonarchTone.Danger else MonarchTone.Info },
    ),
    MonarchAiStructuredField("score", "Overall score", MonarchAiStructuredFieldType.Number),
    MonarchAiStructuredField("strengths", "Strengths", MonarchAiStructuredFieldType.List),
    MonarchAiStructuredField("summary", "Summary", MonarchAiStructuredFieldType.Markdown),
)

private val Brief = mapOf(
    "candidate" to MonarchAiJSON.Text("Alex Morgan"),
    "stage" to MonarchAiJSON.Text("Final interview"),
    "score" to MonarchAiJSON.Number(1234.5),
    "strengths" to MonarchAiJSON.Array(
        listOf(MonarchAiJSON.Text("Clear design reasoning"), MonarchAiJSON.Null, MonarchAiJSON.Text("Writing")),
    ),
    "summary" to MonarchAiJSON.Text("Confirm the start date."),
)

internal object MonarchAiStructuredAnswerFixtures : ComponentFixtures {
    override val id = "ai-structured-answer"
    override val fixtures = listOf(
        fixture("list layout, every type") {
            MonarchAiStructuredAnswer(schema = BriefSchema.take(3), data = Brief, title = "Interview brief")
        },
        fixture("list and markdown") { MonarchAiStructuredAnswer(schema = BriefSchema.drop(3), data = Brief) },
        fixture("grid layout") {
            MonarchAiStructuredAnswer(
                schema = BriefSchema.take(3),
                data = Brief,
                layout = MonarchAiStructuredAnswerLayout.Grid,
            )
        },
        fixture("streaming with fields still to come") {
            MonarchAiStructuredAnswer(
                schema = BriefSchema.take(3),
                data = Brief.filterKeys { it == "candidate" },
                streaming = true,
            )
        },
        fixture("complete with an empty field and actions") {
            MonarchAiStructuredAnswer(
                schema = BriefSchema.take(2),
                data = Brief.filterKeys { it == "candidate" },
            ) { MonarchButton(text = "Copy", onClick = {}) }
        },
        fixture("long values") {
            MonarchAiStructuredAnswer(
                schema = listOf(MonarchAiStructuredField("candidate", "Candidate's full legal name on the passport")),
                data = mapOf("candidate" to MonarchAiJSON.Text("Alexandra Morgan-Fitzwilliam of the Highlands")),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiStructuredAnswerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun headingAndEveryFieldTypeAreReadWithTheirLabels() {
        rule.setMonarchContent {
            MonarchAiStructuredAnswer(schema = BriefSchema, data = Brief, title = "Interview brief", locale = Locale.US)
        }
        rule.onNodeWithText("AI answer").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Interview brief").assertExists()
        rule.onNodeWithText("Alex Morgan").assertExists()
        rule.onNodeWithText("Final interview", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("1,234.5").assertExists()
        rule.onNodeWithText("Clear design reasoning", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Writing", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("•", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("Confirm the start date.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun missingFieldsArePendingWhileStreamingThenNotProvidedAndCompletionIsAnnounced() {
        var streaming by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchAiStructuredAnswer(
                schema = BriefSchema.take(2),
                data = Brief.filterKeys { it == "candidate" },
                streaming = streaming,
            )
        }
        rule.onNodeWithText("Still generating").assert(hasText("Stage"))
        assertEquals("Still generating", rule.onNodeWithText("AI answer").stateDescription())
        rule.onNode(hasContentDescription("Response complete.")).assertDoesNotExist()
        streaming = false
        rule.waitForIdle()
        rule.onNodeWithText("Still generating").assertDoesNotExist()
        // The empty value of MonarchDescriptionList is read as text since C-043.
        rule.onNodeWithText("Not provided").assertExists()
        assertNull(rule.onNodeWithText("AI answer").stateDescription())
        val live = rule.onNode(hasContentDescription("Response complete."))
        live.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchAiStructuredAnswer(
                schema = BriefSchema.take(2),
                data = emptyMap(),
                streaming = true,
                labels = MonarchAiStructuredAnswerLabels(aiAnswer = "Réponse IA", pending = "En cours"),
            )
        }
        rule.onNodeWithText("Réponse IA").assertExists()
        rule.onAllNodesWithText("En cours").assertCountEquals(2)
    }

    @Test
    fun actionsAreSeparateTargets() {
        var copied = 0
        rule.setMonarchContent {
            MonarchAiStructuredAnswer(schema = BriefSchema.take(1), data = Brief) {
                MonarchButton(text = "Copy", onClick = { copied++ })
            }
        }
        rule.onNodeWithText("Copy").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, copied)
    }

    @Test
    fun labelSitsBeforeItsValueRightToLeftAndTextWrapsAtLargeFonts() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiStructuredAnswer(schema = BriefSchema.take(1), data = Brief)
        }
        rule.onNodeWithText("Candidate", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Alex Morgan", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun valuesStayFullyVisibleAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiStructuredAnswer(schema = BriefSchema.take(1), data = Brief, title = "Interview brief")
        }
        rule.onNodeWithText("Interview brief", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Alex Morgan", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun valuesAreReadAsTheSchemaSays() {
        val tokens = MonarchAiStructuredAnswerTokens
        assertTrue(tokens.isMissing(null))
        assertTrue(tokens.isMissing(MonarchAiJSON.Null))
        assertTrue(tokens.isMissing(MonarchAiJSON.Text("")))
        assertTrue(tokens.isMissing(MonarchAiJSON.Array(emptyList())))
        assertFalse(tokens.isMissing(MonarchAiJSON.Number(0.0)))
        assertEquals(listOf("Clear design reasoning", "Writing"), tokens.asList(Brief.getValue("strengths")))
        assertEquals(listOf("Solo"), tokens.asList(MonarchAiJSON.Text("Solo")))
        assertEquals("true", tokens.asText(MonarchAiJSON.Bool(true)))
        assertEquals("{\"a\":1}", tokens.asText(MonarchAiJSON.Object(mapOf("a" to MonarchAiJSON.Number(1.0)))))
        assertEquals("1.234,5", tokens.numberText(MonarchAiJSON.Number(1234.5), null, Locale.GERMANY))
        assertEquals("1,200", tokens.numberText(MonarchAiJSON.Text("1,200"), null, Locale.US))
        assertEquals("42%", tokens.numberText(MonarchAiJSON.Number(0.42), { "${(it * 100).toInt()}%" }, Locale.US))
        assertEquals("about four", tokens.numberText(MonarchAiJSON.Text("about four"), null, Locale.US))
        assertTrue(tokens.isWide(BriefSchema[3]))
        assertTrue(tokens.isWide(BriefSchema[4]))
        assertFalse(tokens.isWide(BriefSchema[0]))
        assertFalse(tokens.isWide(MonarchAiStructuredField("x", "X", MonarchAiStructuredFieldType.List, wide = false)))
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.aiStrong, tokens.eyebrowColor(colors))
            assertEquals(colors.aiSubtle, tokens.placeholderColor(colors))
            assertEquals(colors.bgSurface, tokens.containerColor(colors))
            assertEquals(colors.borderDefault, tokens.borderColor(colors))
        }
    }
}
