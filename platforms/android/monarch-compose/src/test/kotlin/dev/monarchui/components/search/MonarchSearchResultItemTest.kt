package dev.monarchui.components.search

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.search.searchresultitem.MonarchSearchResultItem
import dev.monarchui.components.search.searchresultitem.MonarchSearchResultItemHighlight
import dev.monarchui.components.search.searchresultitem.MonarchSearchResultItemHighlight.Segment
import dev.monarchui.components.search.searchresultitem.MonarchSearchResultItemTokens
import dev.monarchui.components.search.searchresultitem.monarchQueryTerms
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal object MonarchSearchResultItemFixtures : ComponentFixtures {
    override val id = "search-result-item"
    override val fixtures = listOf(
        fixture("plain, path and snippet") {
            MonarchSearchResultItem(
                title = "Travel policy",
                path = listOf("Handbook", "Finance"),
                snippet = "Book travel at least two weeks ahead.",
                terms = listOf("travel"),
            )
        },
        fixture("interactive, file and meta") {
            MonarchSearchResultItem(
                title = "Q3 travel budget.xlsx",
                fileName = "Q3 travel budget.xlsx",
                terms = listOf("budget"),
                onClick = {},
                meta = { MonarchBadge(text = "Sheet") },
            )
        },
        fixture("icon tile and mark tags") {
            MonarchSearchResultItem(
                title = "Expense reports",
                snippet = "Submit <mark>receipts</mark> within 30 days.",
                snippetMarkTags = true,
                onClick = {},
                icon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        },
        fixture("disabled") {
            MonarchSearchResultItem(title = "Archived page", path = listOf("Archive"), enabled = false, onClick = {})
        },
        fixture("long text") {
            MonarchSearchResultItem(
                title = "Guidelines for international travel, per diem allowances and reimbursement of expenses",
                path = listOf("Handbook", "Finance and operations", "Travel and expenses"),
                snippet = "Travel booked through the portal is paid by the company. For travel booked elsewhere, " +
                    "keep the receipts and submit them with the expense report within thirty days of returning.",
                terms = listOf("travel", "receipts"),
                onClick = {},
                meta = { Text("2 days ago") },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSearchResultItemTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun termsAreMatchedWithoutRegardToCaseAndLongestFirst() {
        assertEquals(
            listOf(Segment("Travel", true), Segment(" and ", false), Segment("travelling", true)),
            MonarchSearchResultItemHighlight.segments("Travel and travelling", terms = listOf("travel", "travelling")),
        )
        assertEquals(listOf(Segment("a.b", true)), MonarchSearchResultItemHighlight.segments("a.b", listOf("a.b")))
        assertEquals(listOf(Segment("axb", false)), MonarchSearchResultItemHighlight.segments("axb", listOf("a.b")))
        assertEquals(listOf(Segment("Plain", false)), MonarchSearchResultItemHighlight.segments("Plain", listOf(" ")))
        assertEquals(emptyList(), MonarchSearchResultItemHighlight.segments("", listOf("a")))
    }

    @Test
    fun rangesAreMergedClampedAndReplaceTerms() {
        val segments = MonarchSearchResultItemHighlight.segments(
            text = "Quarterly report",
            terms = listOf("report"),
            ranges = listOf(TextRange(0, 3), TextRange(2, 5), TextRange(14, 40), TextRange(7, 7)),
        )
        assertEquals(
            listOf(Segment("Quart", true), Segment("erly repo", false), Segment("rt", true)),
            segments,
        )
    }

    @Test
    fun onlyMarkTagsAreRead() {
        assertEquals(
            listOf(
                Segment("Use ", false),
                Segment("<b>bold</b>", true),
                Segment(" and ", false),
                Segment("MARK", true),
            ),
            MonarchSearchResultItemHighlight.segments(
                "Use <mark><b>bold</b></mark> and <MARK>MARK</Mark>",
                markTags = true,
            ),
        )
    }

    @Test
    fun queryTermsAreLowerCaseWordsOnceAndAtMostEight() {
        assertEquals(listOf("travel", "policy", "2024"), monarchQueryTerms("Travel  policy, TRAVEL 2024!"))
        assertEquals(8, monarchQueryTerms("a b c d e f g h i j").size)
        assertEquals(listOf("café", "über"), monarchQueryTerms("Café — über"))
    }

    @Test
    fun anInteractiveRowIsOneButtonThatReadsEverything() {
        var clicks = 0
        rule.setMonarchContent {
            MonarchSearchResultItem(
                title = "Travel policy",
                path = listOf("Handbook", "Finance"),
                snippet = "Book travel early.",
                onClick = { clicks++ },
                meta = { Text("PDF") },
            )
        }
        val row = rule.onNode(hasText("Travel policy") and isButton)
        row.assert(hasText("Book travel early.") and hasContentDescription("Location: Handbook, Finance"))
        row.assert(hasText("PDF"))
        row.assertTouchTargetAtLeast48dp(rule.density)
        row.performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun aDisabledRowDoesNotRespond() {
        var clicks = 0
        rule.setMonarchContent { MonarchSearchResultItem(title = "Archived", enabled = false, onClick = { clicks++ }) }
        rule.onNode(hasText("Archived") and isButton).assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun aRowWithoutAnActionIsStaticText() {
        rule.setMonarchContent { MonarchSearchResultItem(title = "Travel policy", snippet = "Book early.") }
        val node = rule.onNodeWithText("Travel policy").fetchSemanticsNode()
        assertFalse(node.config.contains(SemanticsActions.OnClick))
        rule.onNodeWithText("Travel policy").assert(hasText("Book early."))
    }

    @Test
    fun matchesAreSemiboldOnTheMatchFillInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val text = MonarchSearchResultItemTokens.highlighted(
                MonarchSearchResultItemHighlight.segments("Travel policy", listOf("policy")),
                enabled = true,
                colors = colors,
            )
            val span = text.spanStyles.single()
            assertEquals(7, span.start)
            assertEquals(FontWeight.SemiBold, span.item.fontWeight)
            assertEquals(colors.warningSubtle, span.item.background)
            assertEquals(colors.textDefault, span.item.color)
            assertNotEquals(colors.bgCanvas, span.item.background, theme.id)
        }
    }

    @Test
    fun theTitleColourSaysWhetherTheRowDoesSomething() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textDefault, MonarchSearchResultItemTokens.titleColor(true, true, colors))
            assertEquals(colors.textDefault, MonarchSearchResultItemTokens.titleColor(false, true, colors))
            assertEquals(colors.textDisabled, MonarchSearchResultItemTokens.titleColor(true, false, colors))
            assertEquals(colors.textDisabled, MonarchSearchResultItemTokens.secondaryColor(false, colors))
        }
    }

    @Test
    fun thePathLabelCanBeReplaced() {
        rule.setMonarchContent {
            MonarchSearchResultItem(title = "Politique", path = listOf("Manuel", "Voyages"), pathLabel = "Emplacement")
        }
        rule.onNode(hasContentDescription("Emplacement: Manuel, Voyages"), useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Manuel / Voyages", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun theMetaFollowsTheTitleInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            Column {
                MonarchSearchResultItem(title = "Travel", onClick = {}, meta = { Text("PDF") })
            }
        }
        for (direction in LayoutDirection.entries) {
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Travel", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("PDF", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun aLongTitleWrapsWholeAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchSearchResultItem(
                title = "Guidelines for international travel and per diem allowances",
                terms = listOf("travel"),
                onClick = {},
            )
        }
        rule.onNodeWithText("Guidelines for international travel and per diem allowances", useUnmergedTree = true)
            .assertTextFullyVisible()
        val node = rule.onNode(isButton).fetchSemanticsNode()
        assertTrue(node.config.getOrNull(SemanticsProperties.Text).orEmpty().isNotEmpty())
    }

    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
}
