package dev.monarchui.components.intelligence

import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.intelligence.aiconversation.MonarchAiMemoryAction
import dev.monarchui.components.intelligence.aiconversation.MonarchAiMemoryPart
import dev.monarchui.components.intelligence.aimemorychip.MonarchAiMemoryChip
import dev.monarchui.components.intelligence.aimemorychip.MonarchAiMemoryChipLabels
import dev.monarchui.components.intelligence.aimemorychip.MonarchAiMemoryChipTokens
import dev.monarchui.components.intelligence.aimemorychip.MonarchAiMemoryChipVariant
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
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun memory(action: MonarchAiMemoryAction = MonarchAiMemoryAction.Write, scope: String? = "Project Atlas") =
    MonarchAiMemoryPart(id = "m1", action = action, text = "Prefers short summaries", scope = scope)

internal object MonarchAiMemoryChipFixtures : ComponentFixtures {
    override val id = "ai-memory-chip"
    override val fixtures = MonarchAiMemoryAction.entries.map { action ->
        fixture("chip, ${action.name}") {
            MonarchAiMemoryChip(memory = memory(action), onEdit = { _, _ -> }, onForget = {})
        }
    } + listOf(
        fixture("chip, read only, with icon") {
            MonarchAiMemoryChip(memory = memory(), icon = { Icon(TestIcon, contentDescription = null) })
        },
        fixture("row, editable") {
            MonarchAiMemoryChip(
                memory = memory(),
                variant = MonarchAiMemoryChipVariant.Row,
                onEdit = { _, _ -> },
                onForget = {},
            )
        },
        fixture("row, read only, with scope") {
            MonarchAiMemoryChip(memory = memory(), variant = MonarchAiMemoryChipVariant.Row, showScope = true)
        },
        fixture("row, forgotten") {
            MonarchAiMemoryChip(
                memory = memory(MonarchAiMemoryAction.Forget),
                variant = MonarchAiMemoryChipVariant.Row,
                onForget = {},
            )
        },
        fixture("long text") {
            MonarchAiMemoryChip(
                memory = MonarchAiMemoryPart(
                    id = "m2",
                    action = MonarchAiMemoryAction.Read,
                    text = "Writes reports for the board every quarter and wants them in plain English",
                ),
                onForget = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiMemoryChipTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun thePrefixFollowsTheAction() {
        rule.setMonarchContent {
            MonarchAiMemoryChip(memory = memory(MonarchAiMemoryAction.Read, scope = null))
        }
        rule.onNodeWithText("Used memory: Prefers short summaries").assertExists().assertHasNoClickAction()
    }

    @Test
    fun theChipIsOneButtonThatOpensTheMenuAndOffersActions() {
        var forgotten = ""
        rule.setMonarchContent {
            MonarchAiMemoryChip(memory = memory(), onEdit = { _, _ -> }, onForget = { forgotten = it })
        }
        val chip = rule.onNode(hasText("Remembered: Prefers short summaries") and hasClickAction())
        chip.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .assert(
                SemanticsMatcher("offers Edit and Forget") { node ->
                    node.config.getOrNull(SemanticsActions.CustomActions)?.map { it.label } == listOf("Edit", "Forget")
                },
            )
        // The scope is shown on the chip by default and read with it.
        chip.assert(hasText("Project Atlas"))
        chip.performClick()
        rule.onNodeWithText("Forget").performClick()
        assertEquals("m1", forgotten)
    }

    @Test
    fun editingSavesTrimmedChangedTextOnly() {
        var saved: Pair<String, String>? = null
        rule.setMonarchContent { MonarchAiMemoryChip(memory = memory(), onEdit = { id, text -> saved = id to text }) }
        rule.onNode(hasText("Remembered: Prefers short summaries") and hasClickAction()).performClick()
        rule.onNodeWithText("Edit").performClick()
        rule.onNodeWithText("Edit memory").assertExists()
        rule.onNodeWithText("Save").assertIsNotEnabled()
        rule.onNode(hasSetTextAction()).performTextReplacement("   ")
        rule.onNodeWithText("Save").assertIsNotEnabled()
        rule.onNode(hasSetTextAction()).performTextReplacement("  Prefers bullet points ")
        rule.onNodeWithText("Save").assertIsEnabled().performClick()
        assertEquals("m1" to "Prefers bullet points", saved)
        rule.onNodeWithText("Edit memory").assertDoesNotExist()
    }

    @Test
    fun cancelClosesTheDialogWithoutSaving() {
        var saves = 0
        rule.setMonarchContent { MonarchAiMemoryChip(memory = memory(), onEdit = { _, _ -> saves++ }) }
        val chip = rule.onNode(hasText("Remembered: Prefers short summaries") and hasClickAction())
        val edit = chip.fetchSemanticsNode().config[SemanticsActions.CustomActions].first()
        rule.runOnIdle { edit.action() }
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Edit memory").assertDoesNotExist()
        assertEquals(0, saves)
    }

    @Test
    fun aForgottenMemoryIsStruckThroughAndOffersNoCommands() {
        rule.setMonarchContent {
            MonarchAiMemoryChip(memory = memory(MonarchAiMemoryAction.Forget), onEdit = { _, _ -> }, onForget = {})
        }
        rule.onNodeWithText("Forgotten: Prefers short summaries", substring = true)
            .assertHasNoClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
        val struck = SemanticsMatcher("struck through") { node ->
            node.config.getOrNull(SemanticsProperties.Text).orEmpty().any { text ->
                text.spanStyles.any { it.item.textDecoration == TextDecoration.LineThrough }
            }
        }
        rule.onNode(struck, useUnmergedTree = true).assertExists()
    }

    @Test
    fun theRowHasNoPrefixAndNoScopeByDefault() {
        rule.setMonarchContent {
            MonarchAiMemoryChip(memory = memory(), variant = MonarchAiMemoryChipVariant.Row, onForget = {})
        }
        rule.onNodeWithText("Prefers short summaries")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Remembered:", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Project Atlas").assertDoesNotExist()
    }

    @Test
    fun theIconIsBeforeTheTextInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiMemoryChip(
                memory = memory(scope = null),
                variant = MonarchAiMemoryChipVariant.Row,
                icon = { Icon(TestIcon, contentDescription = "AI") },
            )
        }
        rule.onNode(hasContentDescriptionAi(), useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Prefers short summaries", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiMemoryChip(
                memory = memory(scope = null),
                labels = MonarchAiMemoryChipLabels(prefix = { "Mémoire :" }, forget = "Oublier"),
                onForget = {},
            )
        }
        val chip = rule.onNode(hasText("Mémoire : Prefers short summaries") and hasClickAction())
        chip.performClick()
        rule.onNodeWithText("Oublier").assertExists()
    }

    @Test
    fun longTextWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiMemoryChip(memory = memory(scope = null), variant = MonarchAiMemoryChipVariant.Row)
        }
        rule.onNodeWithText("Prefers short summaries", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theLogicDecidesActionsScopeAndEdits() {
        assertTrue(MonarchAiMemoryChipTokens.actionable(memory(), canEdit = true, canForget = false))
        assertFalse(MonarchAiMemoryChipTokens.actionable(memory(), canEdit = false, canForget = false))
        val forgotten = memory(MonarchAiMemoryAction.Forget)
        assertFalse(MonarchAiMemoryChipTokens.actionable(forgotten, canEdit = true, canForget = true))
        assertTrue(MonarchAiMemoryChipTokens.showsScope(memory(), MonarchAiMemoryChipVariant.Chip, null))
        assertFalse(MonarchAiMemoryChipTokens.showsScope(memory(), MonarchAiMemoryChipVariant.Row, null))
        assertFalse(MonarchAiMemoryChipTokens.showsScope(memory(scope = " "), MonarchAiMemoryChipVariant.Chip, true))
        assertNull(MonarchAiMemoryChipTokens.edited("A", " A "))
        assertNull(MonarchAiMemoryChipTokens.edited("A", "  "))
        assertEquals("B", MonarchAiMemoryChipTokens.edited("A", " B "))
    }

    @Test
    fun theColoursFollowTheVariantAndActionInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val chip = MonarchAiMemoryChipTokens.colors(MonarchAiMemoryChipVariant.Chip, MonarchAiMemoryAction.Write, c)
            assertEquals(c.aiSubtle, chip.container, theme.id)
            assertEquals(c.aiBorder, chip.border, theme.id)
            val row = MonarchAiMemoryChipTokens.colors(MonarchAiMemoryChipVariant.Row, MonarchAiMemoryAction.Read, c)
            assertEquals(c.bgSurface, row.container, theme.id)
            val gone =
                MonarchAiMemoryChipTokens.colors(MonarchAiMemoryChipVariant.Chip, MonarchAiMemoryAction.Forget, c)
            assertEquals(c.bgMuted, gone.container, theme.id)
            assertEquals(c.textMuted, gone.content, theme.id)
        }
    }

    private fun hasContentDescriptionAi() =
        SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("AI"))
}
