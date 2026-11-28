package com.rookhq.monarch.components.intelligence

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.rookhq.monarch.components.intelligence.aislashcommands.MonarchAiSlashCommand
import com.rookhq.monarch.components.intelligence.aislashcommands.MonarchAiSlashCommands
import com.rookhq.monarch.components.intelligence.aislashcommands.MonarchAiSlashCommandsLabels
import com.rookhq.monarch.components.intelligence.aislashcommands.MonarchAiSlashCommandsTokens
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

private val Summarize = MonarchAiSlashCommand(
    "summarize",
    "/summarize",
    description = "Shorten the selected text",
    keywords = listOf("tl;dr"),
)
private val Translate = MonarchAiSlashCommand("translate", "translate", description = "Into another language")
private val Resume = MonarchAiSlashCommand("resume", "resume", description = "Continue where the reply stopped")
private val Chart = MonarchAiSlashCommand("chart", "chart", description = "Needs a table", enabled = false)
private val Commands = listOf(Summarize, Translate, Resume, Chart)

private fun typed(text: String) = TextFieldValue(text, selection = TextRange(text.length))

internal object MonarchAiSlashCommandsFixtures : ComponentFixtures {
    override val id = "ai-slash-commands"
    override val fixtures = listOf(
        fixture("every command") {
            MonarchAiSlashCommands(value = typed("/"), onValueChange = {}, commands = Commands, onSelect = { null })
        },
        fixture("filtered") {
            MonarchAiSlashCommands(
                value = typed("Please /su"),
                onValueChange = {},
                commands = Commands,
                onSelect = { null },
            )
        },
        fixture("nothing matches") {
            MonarchAiSlashCommands(value = typed("/zzz"), onValueChange = {}, commands = Commands, onSelect = { null })
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiSlashCommandsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun namesThatStartWithTheTextComeFirstThenNamesThenDescriptions() {
        val t = MonarchAiSlashCommandsTokens
        assertEquals(Commands, t.filter(Commands, " "))
        assertEquals(listOf(Summarize, Resume), t.filter(Commands, "su"))
        assertEquals(listOf(Translate), t.filter(Commands, "LANG"))
        assertEquals(listOf(Summarize), t.filter(Commands, "tl;dr"))
        assertEquals(emptyList(), t.filter(Commands, "zzz"))
        assertEquals("summarize", t.bare("//summarize"))
        assertEquals("/summarize", t.item(Summarize, '/').label)
        assertEquals("!resume", t.item(Resume, '!').label)
    }

    @Test
    fun nothingIsShownUntilACommandIsTyped() {
        rule.setMonarchContent {
            MonarchAiSlashCommands(value = typed("Hello"), onValueChange = {}, commands = Commands, onSelect = { null })
        }
        rule.onNodeWithContentDescription("Commands").assertDoesNotExist()
    }

    @Test
    fun aSlashInsideAWordIsNotACommand() {
        rule.setMonarchContent {
            MonarchAiSlashCommands(
                value = typed("and/or"),
                onValueChange = {},
                commands = Commands,
                onSelect = { null },
            )
        }
        rule.onNodeWithContentDescription("Commands").assertDoesNotExist()
    }

    @Test
    fun pickingACommandReplacesTheTypedCommandWithTheReturnedText() {
        var value by mutableStateOf(typed("Please /sum"))
        val picked = mutableListOf<String>()
        rule.setMonarchContent {
            Column {
                MonarchAiSlashCommands(
                    value = value,
                    onValueChange = { value = it },
                    commands = Commands,
                    onSelect = { command ->
                        picked += command.id
                        "/${command.name.trimStart('/')} "
                    },
                )
                OutlinedTextField(value = value, onValueChange = { value = it })
            }
        }
        rule.onNode(hasText("/summarize") and hasClickAction())
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitForIdle()
        assertEquals(listOf("summarize"), picked)
        assertEquals("Please /summarize ", value.text)
        assertEquals("Please /summarize ".length, value.selection.start)
    }

    @Test
    fun aCommandThatRunsAtOnceRemovesTheTypedText() {
        var value by mutableStateOf(typed("/res"))
        rule.setMonarchContent {
            MonarchAiSlashCommands(
                value = value,
                onValueChange = { value = it },
                commands = Commands,
                onSelect = { null },
            )
        }
        rule.onNode(hasText("/resume") and hasClickAction()).performClick()
        rule.waitForIdle()
        assertEquals("", value.text)
    }

    @Test
    fun aDisabledCommandIsListedButCannotBePicked() {
        var changes = 0
        rule.setMonarchContent {
            MonarchAiSlashCommands(
                value = typed("/ch"),
                onValueChange = { changes++ },
                commands = Commands,
                onSelect = { "x" },
            )
        }
        rule.onNode(hasText("/chart") and hasClickAction()).assertIsNotEnabled()
        assertEquals(0, changes)
    }

    @Test
    fun theCountAndTheEmptyListAreAnnouncedPolitely() {
        var value by mutableStateOf(typed("/"))
        rule.setMonarchContent {
            MonarchAiSlashCommands(value = value, onValueChange = {}, commands = Commands, onSelect = { null })
        }
        val list = rule.onNodeWithContentDescription("Commands")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        assertEquals("4 commands", list.stateDescription())
        value = typed("/zzz")
        rule.waitForIdle()
        assertEquals("No commands match “/zzz”", rule.onNodeWithContentDescription("Commands").stateDescription())
        rule.onNodeWithText("No commands match “/zzz”").assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiSlashCommands(
                value = typed("/tr"),
                onValueChange = {},
                commands = Commands,
                onSelect = { null },
                labels = MonarchAiSlashCommandsLabels(label = "Befehle", count = { "$it Befehle" }),
            )
        }
        assertEquals("1 Befehle", rule.onNodeWithContentDescription("Befehle").stateDescription())
    }

    @Test
    fun rowsFitAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiSlashCommands(value = typed("/tr"), onValueChange = {}, commands = Commands, onSelect = { null })
        }
        rule.onNodeWithText("Into another language", useUnmergedTree = true).assertTextFullyVisible()
    }
}
