package dev.monarchui.components.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextChips
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextChipsLabels
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextChipsLogic
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextChipsTokens
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextItem
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextKind
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextWeight
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val SampleItems = listOf(
    MonarchAiContextItem("contract", MonarchAiContextKind.File, "contract.pdf", tokens = 1200),
    MonarchAiContextItem("selection", MonarchAiContextKind.Selection, "Selected rows", tokens = 300),
    MonarchAiContextItem("page", MonarchAiContextKind.Page, "Q3 planning", tokens = 2100, removable = false),
)

internal object MonarchAiContextChipsFixtures : ComponentFixtures {
    override val id = "ai-context-chips"
    override val fixtures = listOf(
        fixture("read only with total") { MonarchAiContextChips(items = SampleItems) },
        fixture("removable with icons") {
            MonarchAiContextChips(
                items = SampleItems,
                onRemove = {},
                kindIcon = { MonarchIcon(TestIcon, contentDescription = null, size = MonarchIconSize.Sm) },
                removeIcon = { MonarchIcon(TestIcon, contentDescription = null, size = MonarchIconSize.Sm) },
            )
        },
        fixture("disabled") { MonarchAiContextChips(items = SampleItems, enabled = false, onRemove = {}) },
        fixture("against a budget, no counts") {
            MonarchAiContextChips(
                items = listOf(
                    MonarchAiContextItem("person", MonarchAiContextKind.Person, "Ana Silva"),
                    MonarchAiContextItem("app", MonarchAiContextKind.App, "Calendar", tokens = 90_000),
                ),
                budget = 128_000,
            )
        },
        fixture("long name") {
            MonarchAiContextChips(
                items = listOf(
                    MonarchAiContextItem(
                        "long",
                        MonarchAiContextKind.Record,
                        "Customer escalation for the northern region renewal, quarter three",
                        tokens = 1,
                    ),
                ),
                onRemove = {},
            )
        },
        fixture("empty with text") { MonarchAiContextChips(items = emptyList(), emptyText = "No context added") },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiContextChipsTest {
    @get:Rule
    val rule = createComposeRule()

    @Composable
    private fun Removable(labels: MonarchAiContextChipsLabels = MonarchAiContextChipsLabels()) {
        val items = remember { mutableStateListOf(*SampleItems.toTypedArray()) }
        MonarchAiContextChips(
            items = items,
            onRemove = { id -> items.removeAll { it.id == id } },
            labels = labels,
        )
    }

    @Test
    fun eachChipIsReadAsKindNameAndCount() {
        rule.setMonarchContent { MonarchAiContextChips(items = SampleItems) }
        rule.onNodeWithContentDescription("File: contract.pdf, 1.2K tokens").assertExists().assertHasNoClickAction()
        rule.onNodeWithContentDescription("Context").assertExists()
    }

    @Test
    fun aRemovableChipIsAButtonThatRemovesAndAnnounces() {
        rule.setMonarchContent { Removable() }
        val chip = rule.onNodeWithContentDescription("File: contract.pdf, 1.2K tokens")
        chip.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Button))
        val action = chip.fetchSemanticsNode().config[SemanticsActions.OnClick]
        assertEquals("Remove contract.pdf from context", action.label)
        chip.assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onAllNodesWithContentDescription("File: contract.pdf, 1.2K tokens").assertCountEquals(0)
        rule.onNodeWithContentDescription("contract.pdf removed from context")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun anItemThatIsNotRemovableHasNoAction() {
        rule.setMonarchContent { Removable() }
        rule.onNodeWithContentDescription("Page: Q3 planning, 2.1K tokens").assertHasNoClickAction()
    }

    @Test
    fun disabledChipsDoNotRemove() {
        var removed = 0
        rule.setMonarchContent { MonarchAiContextChips(items = SampleItems, enabled = false, onRemove = { removed++ }) }
        rule.onNodeWithContentDescription("File: contract.pdf, 1.2K tokens").assertIsNotEnabled().performClick()
        assertEquals(0, removed)
    }

    @Test
    fun theTotalIsShownWithCounts() {
        rule.setMonarchContent { MonarchAiContextChips(items = SampleItems) }
        rule.onNodeWithText("≈ 3.6K tokens in context", useUnmergedTree = true).assertExists()
    }

    @Test
    fun emptyItemsShowTheEmptyTextOnly() {
        rule.setMonarchContent { MonarchAiContextChips(items = emptyList(), emptyText = "No context") }
        rule.onNodeWithText("No context").assertExists()
        rule.onAllNodesWithContentDescription("Context").assertCountEquals(0)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            Removable(
                labels = MonarchAiContextChipsLabels(
                    label = "Contexto",
                    kind = { "Archivo" },
                    tokens = { formatted, _ -> "$formatted fichas" },
                    total = { formatted, _ -> "Total $formatted" },
                    remove = { "Quitar $it" },
                ),
            )
        }
        val chip = rule.onNodeWithContentDescription("Archivo: contract.pdf, 1.2K fichas")
        assertEquals("Quitar contract.pdf", chip.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
        rule.onNodeWithText("Total 3.6K", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Contexto").assertExists()
    }

    @Test
    fun theFirstChipIsAtTheStartRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { MonarchAiContextChips(items = SampleItems) }
        // The chips wrap onto lines in a narrow window: the first sits on the first line, at the start (right) edge.
        val first = rule.onNodeWithContentDescription("File: contract.pdf, 1.2K tokens").getUnclippedBoundsInRoot()
        val second = rule.onNodeWithContentDescription("Selection: Selected rows, 300 tokens")
            .getUnclippedBoundsInRoot()
        assertTrue(first.bottom <= second.top || first.right > second.right, "$first is not before $second")
    }

    @Test
    fun theTotalIsNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchAiContextChips(items = SampleItems.take(1)) }
        rule.onNodeWithText("≈ 1.2K tokens in context", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun weightsComeFromTheShareOfTheReference() {
        assertEquals(MonarchAiContextWeight.Heavy, MonarchAiContextWeight.of(50, 100))
        assertEquals(MonarchAiContextWeight.Medium, MonarchAiContextWeight.of(20, 100))
        assertEquals(MonarchAiContextWeight.Light, MonarchAiContextWeight.of(19, 100))
        assertEquals(MonarchAiContextWeight.Light, MonarchAiContextWeight.of(10, 0))
        val item = MonarchAiContextItem("a", MonarchAiContextKind.File, "a", tokens = 30)
        assertEquals(MonarchAiContextWeight.Heavy, MonarchAiContextChipsLogic.weight(item, total = 40, budget = null))
        assertEquals(MonarchAiContextWeight.Light, MonarchAiContextChipsLogic.weight(item, total = 40, budget = 1000))
        val unknown = MonarchAiContextItem("b", MonarchAiContextKind.App, "b")
        assertNull(MonarchAiContextChipsLogic.weight(unknown, total = 1, budget = null))
        assertEquals(3, MonarchAiContextChipsTokens.filledBars(MonarchAiContextWeight.Heavy))
    }

    @Test
    fun countsAreCompactAndTotalsSkipUnknownAndNegativeCounts() {
        val en = Locale.ENGLISH
        assertEquals("950", MonarchAiContextChipsLogic.formatTokens(950, en))
        assertEquals("1.2K", MonarchAiContextChipsLogic.formatTokens(1200, en))
        assertEquals("18K", MonarchAiContextChipsLogic.formatTokens(18_000, en))
        assertEquals("1.1M", MonarchAiContextChipsLogic.formatTokens(1_100_000, en))
        assertEquals("0", MonarchAiContextChipsLogic.formatTokens(-5, en))
        val items = listOf(
            MonarchAiContextItem("a", MonarchAiContextKind.File, "a", tokens = 10),
            MonarchAiContextItem("b", MonarchAiContextKind.File, "b", tokens = -4),
            MonarchAiContextItem("c", MonarchAiContextKind.File, "c"),
        )
        assertEquals(10, MonarchAiContextChipsLogic.total(items))
        assertTrue(MonarchAiContextChipsLogic.showsTotal(items, null))
        assertFalse(MonarchAiContextChipsLogic.showsTotal(emptyList(), true))
        assertFalse(MonarchAiContextChipsLogic.showsTotal(items.drop(2), null))
        assertEquals(
            "File: a, 1 token, Notes",
            MonarchAiContextChipsLogic.spokenLabel("File", "a", "1 token", "Notes"),
        )
    }

    @Test
    fun theColoursComeFromTheAiRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val enabled = MonarchAiContextChipsTokens.chipColors(true, c)
            assertEquals(c.aiSubtle, enabled.container, theme.id)
            assertEquals(c.aiBorder, enabled.border, theme.id)
            val disabled = MonarchAiContextChipsTokens.chipColors(false, c)
            assertEquals(c.bgMuted, disabled.container, theme.id)
            assertEquals(c.textDisabled, disabled.content, theme.id)
            val medium = MonarchAiContextWeight.Medium
            assertEquals(c.aiStrong, MonarchAiContextChipsTokens.barColor(1, medium, c), theme.id)
            assertEquals(c.borderDefault, MonarchAiContextChipsTokens.barColor(2, medium, c), theme.id)
            val heavy = MonarchAiContextChipsTokens.tokensColor(MonarchAiContextWeight.Heavy, true, c)
            assertEquals(c.textDefault, heavy, theme.id)
            assertEquals(c.textMuted, MonarchAiContextChipsTokens.tokensColor(medium, true, c), theme.id)
        }
    }
}
