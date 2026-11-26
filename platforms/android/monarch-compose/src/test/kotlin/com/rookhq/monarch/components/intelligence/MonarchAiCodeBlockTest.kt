package com.rookhq.monarch.components.intelligence

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.intelligence.aicodeblock.MonarchAiCodeBlock
import com.rookhq.monarch.components.intelligence.aicodeblock.MonarchAiCodeBlockLabels
import com.rookhq.monarch.components.intelligence.aicodeblock.MonarchAiCodeBlockTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

private const val Code = "const port = 8080;\nlisten(port);\n"
private const val Diff = "-const port = 3000;\n+const port = 8080;"

internal object MonarchAiCodeBlockFixtures : ComponentFixtures {
    override val id = "ai-code-block"
    override val fixtures = listOf(
        fixture("apply and insert") {
            MonarchAiCodeBlock(code = Code, language = "ts", title = "server.ts", onInsert = {}, onApply = {})
        },
        fixture("incomplete diff") {
            MonarchAiCodeBlock(code = Diff, language = "diff", incomplete = true, onInsert = {}, onApply = {})
        },
        fixture("app actions, line numbers, no copy or wrap") {
            MonarchAiCodeBlock(
                code = Code,
                showLineNumbers = true,
                copyable = false,
                showWrapToggle = false,
                actions = { MonarchButton(text = "Explain", onClick = {}, size = MonarchButtonSize.Sm) },
            )
        },
        fixture("no actions, unhighlighted") { MonarchAiCodeBlock(code = Code, highlighted = false) },
        fixture("empty code") { MonarchAiCodeBlock(code = "", onApply = {}) },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiCodeBlockTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchAiCodeBlockTokens

    @Test
    fun applyAndInsertHandOverTheCodeAsGiven() {
        val applied = mutableListOf<String>()
        val inserted = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiCodeBlock(
                code = Code,
                language = "ts",
                title = "server.ts",
                onInsert = { inserted += it },
                onApply = { applied += it },
            )
        }
        rule.onNodeWithContentDescription("Apply server.ts").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("Insert server.ts").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf(Code), applied)
        assertEquals(listOf(Code), inserted)
    }

    @Test
    fun incompleteCodeDisablesTheActionsAndSaysWhy() {
        var applied = 0
        rule.setMonarchContent {
            MonarchAiCodeBlock(code = Code, language = "ts", incomplete = true, onApply = { applied++ })
        }
        rule.onNodeWithContentDescription("Apply TypeScript code").assertIsNotEnabled().performClick()
        rule.onNodeWithText("Available when the code is complete").assertExists()
        assertEquals(0, applied)
    }

    @Test
    fun withoutActionsItIsAPlainCodeBlock() {
        rule.setMonarchContent { MonarchAiCodeBlock(code = Code, incomplete = true) }
        rule.onNodeWithText("Apply").assertDoesNotExist()
        rule.onNodeWithText("Available when the code is complete").assertDoesNotExist()
        rule.onNodeWithText("Wrap lines").assertExists()
    }

    @Test
    fun theAppsActionsComeBeforeApplyRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiCodeBlock(
                code = Code,
                onApply = {},
                actions = {
                    MonarchButton(
                        text = "Explain",
                        onClick = {},
                        modifier = Modifier.testTag("explain"),
                        size = MonarchButtonSize.Sm,
                    )
                },
            )
        }
        rule.onNodeWithTag("explain").assertIsEnabled()
            .assertIsBefore(rule.onNodeWithContentDescription("Apply Code"), LayoutDirection.Rtl)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiCodeBlock(
                code = Code,
                title = "app.kt",
                incomplete = true,
                onApply = {},
                labels = MonarchAiCodeBlockLabels(
                    apply = "Aplicar",
                    actionLabel = { action, name -> "$action en $name" },
                    incompleteHint = "Espera",
                ),
            )
        }
        rule.onNodeWithContentDescription("Aplicar en app.kt").assertExists()
        rule.onNodeWithText("Espera").assertExists()
    }

    @Test
    fun theHintWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiCodeBlock(code = "x", incomplete = true, onApply = {})
        }
        rule.onNodeWithText("Available when the code is complete").assertTextFullyVisible()
    }

    @Test
    fun theRowAndTheHintFollowTheCallbacks() {
        assertEquals(false, tokens.hasActions(onApply = false, onInsert = false, actions = false))
        assertEquals(true, tokens.hasActions(onApply = false, onInsert = false, actions = true))
        assertEquals(false, tokens.actionsEnabled(incomplete = true))
        assertEquals(false, tokens.showsHint(incomplete = true, onApply = false, onInsert = false))
        assertEquals(true, tokens.showsHint(incomplete = true, onApply = false, onInsert = true))
        assertEquals(false, tokens.showsHint(incomplete = false, onApply = true, onInsert = true))
    }

    @Test
    fun theHintColourComesFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.textMuted, tokens.hintColor(c), theme.id)
        }
    }
}
