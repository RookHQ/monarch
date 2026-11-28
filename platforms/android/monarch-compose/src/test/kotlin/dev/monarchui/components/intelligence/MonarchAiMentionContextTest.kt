package dev.monarchui.components.intelligence

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionPickerStatus
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionQuery
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextKind
import dev.monarchui.components.intelligence.aimentioncontext.MonarchAiMentionContext
import dev.monarchui.components.intelligence.aimentioncontext.MonarchAiMentionContextLabels
import dev.monarchui.components.intelligence.aimentioncontext.MonarchAiMentionContextRules
import dev.monarchui.components.intelligence.aimentioncontext.MonarchAiMentionContextTokens
import dev.monarchui.components.intelligence.aimentioncontext.MonarchAiMentionItem
import dev.monarchui.components.intelligence.aimentioncontext.MonarchAiMentionKind
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
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
import kotlin.test.assertTrue

private val Report =
    MonarchAiMentionItem("doc-1", MonarchAiMentionKind.Doc, "Q3 report.pdf", "Finance", tokens = 12_400)
private val Ana = MonarchAiMentionItem("user-1", MonarchAiMentionKind.Person, "Ana Lopez", "Finance lead")
private val Locked = MonarchAiMentionItem("hr", MonarchAiMentionKind.Doc, "Salaries", enabled = false)
private val Query = MonarchMentionQuery.find("Summarize @q3")

internal object MonarchAiMentionContextFixtures : ComponentFixtures {
    override val id = "ai-mention-context"
    override val fixtures = listOf(
        fixture("grouped results") {
            MonarchAiMentionContext(query = Query, items = listOf(Report, Ana), onSelect = {})
        },
        fixture("one kind with a disabled result") {
            MonarchAiMentionContext(query = Query, items = listOf(Report, Locked), onSelect = {})
        },
        fixture("empty") { MonarchAiMentionContext(query = Query, items = emptyList(), onSelect = {}) },
        fixture("failed with retry") {
            MonarchAiMentionContext(
                query = Query,
                items = emptyList(),
                onSelect = {},
                status = MonarchMentionPickerStatus.Failed,
                onRetry = {},
            )
        },
        fixture("long label") {
            MonarchAiMentionContext(
                query = Query,
                items = listOf(
                    MonarchAiMentionItem(
                        "long",
                        MonarchAiMentionKind.Record,
                        "Renewal of the enterprise agreement with the northern region",
                        "Deal in negotiation",
                        tokens = 1,
                    ),
                ),
                onSelect = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiMentionContextTest {
    @get:Rule
    val rule = createComposeRule()

    private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    @Test
    fun eachResultIsAButtonReadWithItsDescriptionAndTokens() {
        val chosen = mutableListOf<MonarchAiMentionItem>()
        rule.setMonarchContent {
            MonarchAiMentionContext(query = Query, items = listOf(Report, Ana), onSelect = { chosen += it })
        }
        rule.onNodeWithContentDescription("Q3 report.pdf, Finance, 12.4K tokens")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(listOf(Report), chosen)
        val chip = chosen.single().contextItem
        assertEquals(MonarchAiContextKind.File, chip.kind)
        assertEquals(12_400, chip.tokens)
    }

    @Test
    fun moreThanOneKindIsGroupedUnderHeadings() {
        rule.setMonarchContent {
            MonarchAiMentionContext(query = Query, items = listOf(Ana, Report), onSelect = {})
        }
        rule.onNodeWithText("Documents").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("People").assertExists()
        rule.onNodeWithContentDescription("Ana Lopez, Finance lead").assertExists()
        rule.onNode(polite).assert(hasContentDescription("Add context"))
        assertEquals("2 results", rule.onNode(polite).stateDescription())
    }

    @Test
    fun aDisabledResultCannotBeChosen() {
        var chosen = 0
        rule.setMonarchContent {
            MonarchAiMentionContext(query = Query, items = listOf(Locked), onSelect = { chosen++ })
        }
        rule.onNodeWithText("Documents").assertDoesNotExist()
        rule.onNodeWithContentDescription("Salaries").assertIsNotEnabled().performClick()
        assertEquals(0, chosen)
    }

    @Test
    fun loadingEmptyAndFailedAreAnnounced() {
        var retries = 0
        rule.setMonarchContent {
            MonarchAiMentionContext(
                query = Query,
                items = emptyList(),
                onSelect = {},
                status = MonarchMentionPickerStatus.Failed,
                onRetry = { retries++ },
            )
        }
        assertEquals(
            "Couldn’t load results.",
            rule.onNode(polite and hasContentDescription("Add context")).stateDescription(),
        )
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun theEmptyTextNamesTheQuery() {
        rule.setMonarchContent {
            MonarchAiMentionContext(query = Query, items = emptyList(), onSelect = {})
        }
        rule.onNodeWithText("Nothing matches “q3”").assertExists()
    }

    @Test
    fun anEmptyQueryAsksToTypeAndSearchingIsSaid() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiMentionContext(query = MonarchMentionQuery.find("@"), items = emptyList(), onSelect = {})
            MonarchAiMentionContext(
                query = MonarchMentionQuery.find("@a"),
                items = emptyList(),
                onSelect = {},
                status = MonarchMentionPickerStatus.Loading,
            )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Type to search").assertExists()
        rule.onNodeWithContentDescription("Searching…").assertExists()
    }

    @Test
    fun nothingIsDrawnWithoutAQueryOrForProseThatFoundNothing() {
        rule.setMonarchContent {
            MonarchAiMentionContext(query = null, items = listOf(Report), onSelect = {})
            MonarchAiMentionContext(query = MonarchMentionQuery.find("@ana lo"), items = emptyList(), onSelect = {})
        }
        rule.onNode(polite).assertDoesNotExist()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiMentionContext(
                query = Query,
                items = listOf(Report, Ana),
                onSelect = {},
                labels = MonarchAiMentionContextLabels(
                    group = { "G ${it.name}" },
                    tokens = { formatted, _ -> "$formatted T" },
                    count = { "$it R" },
                ),
            )
        }
        rule.onNodeWithText("G Doc").assertExists()
        rule.onNodeWithContentDescription("Q3 report.pdf, Finance, 12.4K T").assertExists()
        assertEquals("2 R", rule.onNode(polite).stateDescription())
    }

    @Test
    fun aLongLabelWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiMentionContext(
                query = Query,
                items = listOf(MonarchAiMentionItem("x", MonarchAiMentionKind.App, "Calendar for the whole team")),
                onSelect = {},
            )
        }
        rule.onNodeWithText("Calendar for the whole team", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theRulesGroupInKindOrder() {
        val rules = MonarchAiMentionContextRules
        val app = MonarchAiMentionItem("a", MonarchAiMentionKind.App, "App")
        assertEquals(
            listOf(MonarchAiMentionKind.Doc, MonarchAiMentionKind.App),
            rules.groups(listOf(app, Report)).map { it.first },
        )
        assertFalse(rules.grouped(listOf(Report, Locked)))
        assertTrue(rules.isOpen(Query, MonarchMentionPickerStatus.Ready, 0))
        assertEquals("Ana Lopez, Finance lead", rules.spoken(Ana, null))
        assertEquals(MonarchAiContextKind.Person, Ana.contextItem.kind)
    }

    @Test
    fun theColoursAreRolesInEveryTheme() {
        AllThemes.forEach { tokens ->
            val colors = MonarchColors(tokens.colors)
            val off = MonarchAiMentionContextTokens.rowColors(false, colors)
            assertEquals(colors.textDisabled, off.label)
            assertEquals(colors.textDefault, MonarchAiMentionContextTokens.rowColors(true, colors).label)
            assertEquals(colors.bgSurface, MonarchAiMentionContextTokens.background(colors))
            assertEquals(colors.borderSubtle, MonarchAiMentionContextTokens.border(colors))
            assertEquals(colors.textMuted, MonarchAiMentionContextTokens.noteColor(colors))
        }
    }
}
