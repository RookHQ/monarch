@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiassistantpanel.MonarchAiAssistantPanel
import com.rookhq.monarch.components.intelligence.aiassistantpanel.MonarchAiAssistantPanelLabels
import com.rookhq.monarch.components.intelligence.aiassistantpanel.MonarchAiAssistantPanelPlacement
import com.rookhq.monarch.components.intelligence.aiassistantpanel.MonarchAiAssistantPanelTokens
import com.rookhq.monarch.components.intelligence.aicontextchips.MonarchAiContextItem
import com.rookhq.monarch.components.intelligence.aicontextchips.MonarchAiContextKind
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversation
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationMessage
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationStatus
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiRole
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiStreamEvent
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiTextPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiTransport
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiTransportRequest
import com.rookhq.monarch.components.intelligence.aiconversation.rememberMonarchAiConversation
import com.rookhq.monarch.components.intelligence.aistarterprompts.MonarchAiStarterPrompt
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val AiAssistantPanelInline = MonarchAiAssistantPanelPlacement.Inline
private val AiAssistantPanelSheet = MonarchAiAssistantPanelPlacement.Sheet

private val AiAssistantPanelMessages = listOf(
    MonarchAiConversationMessage(
        id = "u1",
        role = MonarchAiRole.User,
        parts = listOf(MonarchAiTextPart("Summarize this page")),
        createdAt = "2026-11-05T09:05:00.000Z",
    ),
    MonarchAiConversationMessage(
        id = "a1",
        role = MonarchAiRole.Assistant,
        parts = listOf(MonarchAiTextPart("The plan has three goals.")),
        createdAt = "2026-11-05T09:05:02.000Z",
    ),
)

private val AiAssistantPanelStarters = listOf(
    MonarchAiStarterPrompt("sum", "Summarize this page", "Summarize this page."),
    MonarchAiStarterPrompt("week", "What changed?", "What changed this week?"),
)

private val AiAssistantPanelAgent = MonarchAiAgentRef(name = "Dossier", role = "Workspace assistant")

/** A transport that answers "You asked: <prompt>", standing in for a backend. */
private class AiAssistantPanelEchoTransport : MonarchAiTransport {
    override fun stream(request: MonarchAiTransportRequest): Flow<MonarchAiStreamEvent> = flowOf(
        MonarchAiStreamEvent.TextDelta("You asked: ${request.messages.lastOrNull()?.text.orEmpty()}"),
        MonarchAiStreamEvent.Done(),
    )
}

internal object MonarchAiAssistantPanelFixtures : ComponentFixtures {
    override val id = "ai-assistant-panel"
    override val fixtures = listOf(
        fixture("a conversation with an agent, text buttons") {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = AiAssistantPanelMessages,
                open = true,
                onOpenChange = {},
                onSend = {},
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
                agent = AiAssistantPanelAgent,
                onMinimizedChange = {},
            )
        },
        fixture("empty with starters, icon buttons") {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = emptyList(),
                open = true,
                onOpenChange = {},
                onSend = {},
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
                starters = AiAssistantPanelStarters,
                closeIcon = { Icon(TestIcon, contentDescription = null) },
                minimizeIcon = { Icon(TestIcon, contentDescription = null) },
                onMinimizedChange = {},
            )
        },
        fixture("context chips and a running reply") {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = AiAssistantPanelMessages,
                open = true,
                onOpenChange = {},
                onSend = {},
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
                status = MonarchAiConversationStatus.Streaming,
                context = listOf(MonarchAiContextItem("page", MonarchAiContextKind.Page, "Quarterly plan")),
                onStop = {},
                closeIcon = { Icon(TestIcon, contentDescription = null) },
            )
        },
        fixture("minimized") {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = AiAssistantPanelMessages,
                open = true,
                onOpenChange = {},
                onSend = {},
                placement = AiAssistantPanelInline,
                minimized = true,
                onMinimizedChange = {},
            )
        },
        fixture("offline with a long title") {
            MonarchAiAssistantPanel(
                title = "Assistant for the quarterly planning workspace",
                messages = AiAssistantPanelMessages,
                open = true,
                onOpenChange = {},
                onSend = {},
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
                offline = true,
                queuedCount = 1,
                closeIcon = { Icon(TestIcon, contentDescription = null) },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiAssistantPanelTest {
    @get:Rule
    val rule = createComposeRule()

    private val input = SemanticsMatcher.expectValue(
        SemanticsProperties.ContentDescription,
        listOf("Message the assistant"),
    )

    @Test
    fun theTitleIsAHeadingThatNamesThePaneWithTheAgentUnderIt() {
        rule.setMonarchContent {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = AiAssistantPanelMessages,
                open = true,
                onOpenChange = {},
                onSend = {},
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
                agent = AiAssistantPanelAgent,
            )
        }
        rule.onNodeWithText("Workspace help").assert(isHeading())
        rule.onNodeWithText("Dossier · Workspace assistant").assertExists()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Workspace help")).assertExists()
        rule.onNodeWithText("The plan has three goals.").assertExists()
    }

    @Test
    fun theCloseButtonClosesThePanel() {
        val changes = mutableListOf<Boolean>()
        rule.setMonarchContent {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = AiAssistantPanelMessages,
                open = true,
                onOpenChange = { changes += it },
                onSend = {},
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
                closeIcon = { Icon(TestIcon, contentDescription = null) },
            )
        }
        rule.onNodeWithContentDescription("Close assistant").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf(false), changes)
    }

    @Test
    fun withoutAnIconTheCloseButtonShowsItsLabel() {
        var open by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = AiAssistantPanelMessages,
                open = open,
                onOpenChange = { open = it },
                onSend = {},
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
                closeIcon = null,
                minimizeIcon = null,
            )
        }
        rule.onNode(hasText("Close assistant") and hasClickAction()).assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitUntil { !open }
        rule.onNodeWithText("Workspace help").assertDoesNotExist()
    }

    @Test
    fun minimizeCollapsesAnInlinePaneToItsHeader() {
        var minimized by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = AiAssistantPanelMessages,
                open = true,
                onOpenChange = {},
                onSend = {},
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
                closeIcon = null,
                minimizeIcon = null,
                minimized = minimized,
                onMinimizedChange = { minimized = it },
            )
        }
        rule.onNode(input).assertExists()
        rule.onNode(hasText("Minimize assistant") and hasClickAction()).assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitUntil { minimized }
        rule.onNode(input).assertDoesNotExist()
        rule.onNodeWithText("Workspace help").assertExists()
        rule.onNode(hasText("Expand assistant") and hasClickAction()).performClick()
        rule.waitUntil { !minimized }
        rule.onNode(input).assertExists()
    }

    @Test
    fun withoutAHandlerThereIsNoMinimizeButton() {
        rule.setMonarchContent {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = AiAssistantPanelMessages,
                open = true,
                onOpenChange = {},
                onSend = {},
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
            )
        }
        rule.onNodeWithText("Minimize assistant").assertDoesNotExist()
    }

    @Test
    fun aSheetShowsTheConversationAndNeverMinimizes() {
        rule.setMonarchContent {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = AiAssistantPanelMessages,
                open = true,
                onOpenChange = {},
                onSend = {},
                placement = AiAssistantPanelSheet,
                minimized = true,
                onMinimizedChange = {},
            )
        }
        rule.onNodeWithText("Workspace help").assert(isHeading())
        rule.onNodeWithText("The plan has three goals.").assertExists()
        rule.onNodeWithText("Minimize assistant").assertDoesNotExist()
        rule.onNodeWithText("Expand assistant").assertDoesNotExist()
        rule.onNode(input).assertExists()
    }

    @Test
    fun onAPhoneAutoIsASheet() {
        rule.setMonarchContent {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = AiAssistantPanelMessages,
                open = true,
                onOpenChange = {},
                onSend = {},
                onMinimizedChange = {},
            )
        }
        rule.onNodeWithText("The plan has three goals.").assertExists()
        rule.onNodeWithText("Minimize assistant").assertDoesNotExist()
    }

    @Test
    fun aClosedPanelShowsNothing() {
        rule.setMonarchContent {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = AiAssistantPanelMessages,
                open = false,
                onOpenChange = {},
                onSend = {},
                placement = AiAssistantPanelInline,
            )
        }
        rule.onNodeWithText("Workspace help").assertDoesNotExist()
    }

    @Test
    fun theDraftIsKeptWhileThePanelIsClosed() {
        val prompt = TextFieldState()
        var open by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = AiAssistantPanelMessages,
                open = open,
                onOpenChange = { open = it },
                onSend = {},
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
                prompt = prompt,
            )
        }
        rule.onNode(input).performTextInput("Half a question")
        open = false
        rule.waitUntil { rule.onAllNodes(input).fetchSemanticsNodes().isEmpty() }
        open = true
        rule.waitUntil { rule.onAllNodes(input).fetchSemanticsNodes().isNotEmpty() }
        assertEquals("Half a question", prompt.text.toString())
    }

    @Test
    fun sendPassesTheTypedText() {
        val sent = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = AiAssistantPanelMessages,
                open = true,
                onOpenChange = {},
                onSend = { sent += it },
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
            )
        }
        rule.onNode(input).performTextInput("What is due?")
        rule.onNodeWithText("Send").assertIsEnabled().performClick()
        rule.waitUntil { sent.isNotEmpty() }
        assertEquals(listOf("What is due?"), sent)
    }

    @Test
    fun anEmptyConversationWelcomesAndAStarterIsSent() {
        val sent = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = emptyList(),
                open = true,
                onOpenChange = {},
                onSend = { sent += it },
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
                starters = AiAssistantPanelStarters,
            )
        }
        rule.onNodeWithText("How can I help?").assert(isHeading())
        rule.onNode(hasText("What changed?") and hasClickAction()).performScrollTo()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitUntil { sent.isNotEmpty() }
        assertEquals(listOf("What changed this week?"), sent)
    }

    @Test
    fun startersAreDisabledWhileAReplyRuns() {
        rule.setMonarchContent {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = emptyList(),
                open = true,
                onOpenChange = {},
                onSend = {},
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
                status = MonarchAiConversationStatus.AwaitingApproval,
                starters = AiAssistantPanelStarters,
            )
        }
        rule.onNode(hasText("What changed?") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun aCustomEmptyStateReplacesTheWelcome() {
        rule.setMonarchContent {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = emptyList(),
                open = true,
                onOpenChange = {},
                onSend = {},
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
                starters = AiAssistantPanelStarters,
                empty = { Text("Ask about this invoice") },
            )
        }
        rule.onNodeWithText("Ask about this invoice").assertExists()
        rule.onNodeWithText("How can I help?").assertDoesNotExist()
    }

    @Test
    fun whileAReplyRunsStopIsOfferedAndCallsBack() {
        var stops = 0
        rule.setMonarchContent {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = AiAssistantPanelMessages,
                open = true,
                onOpenChange = {},
                onSend = {},
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
                status = MonarchAiConversationStatus.Streaming,
                onStop = { stops++ },
            )
        }
        rule.onNode(hasText("Stop") and hasClickAction()).performClick()
        rule.waitUntil { stops == 1 }
    }

    @Test
    fun contextChipsAreShownAndRemovable() {
        val removed = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = AiAssistantPanelMessages,
                open = true,
                onOpenChange = {},
                onSend = {},
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
                context = listOf(MonarchAiContextItem("page", MonarchAiContextKind.Page, "Quarterly plan")),
                onRemoveContext = { removed += it },
            )
        }
        rule.onNode(hasContentDescription("Quarterly plan", substring = true) and hasClickAction())
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitUntil { removed.isNotEmpty() }
        assertEquals(listOf("page"), removed)
    }

    @Test
    fun theConversationOverloadSendsAndShowsTheReply() {
        rule.setMonarchContent {
            val conversation = rememberMonarchAiConversation(AiAssistantPanelEchoTransport())
            MonarchAiAssistantPanel(
                conversation = conversation,
                title = "Workspace help",
                open = true,
                onOpenChange = {},
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
                starters = AiAssistantPanelStarters,
            )
        }
        rule.onNode(hasText("Summarize this page") and hasClickAction()).performClick()
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodes(hasText("You asked: Summarize this page.", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun theConversationOverloadSendsThePanelsBodyWithEachMessage() {
        val bodies = mutableListOf<Map<String, MonarchAiJSON>>()
        val transport = object : MonarchAiTransport {
            override fun stream(request: MonarchAiTransportRequest): Flow<MonarchAiStreamEvent> {
                bodies += request.body
                return flowOf(MonarchAiStreamEvent.TextDelta("Done"), MonarchAiStreamEvent.Done())
            }
        }
        var selection by mutableStateOf("Northwind")
        lateinit var conversation: MonarchAiConversation
        rule.setMonarchContent {
            conversation = rememberMonarchAiConversation(transport)
            MonarchAiAssistantPanel(
                conversation = conversation,
                title = "Workspace help",
                open = true,
                onOpenChange = {},
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
                starters = AiAssistantPanelStarters,
                body = mapOf("selection" to MonarchAiJSON.Text(selection)),
            )
        }
        // The app's own request fields on the conversation.
        rule.runOnIdle { conversation.body = mapOf("app" to MonarchAiJSON.Text("crm")) }
        rule.onNode(hasText("Summarize this page") and hasClickAction()).performClick()
        rule.waitUntil(timeoutMillis = 5_000) { bodies.size == 1 }
        assertEquals(
            mapOf("app" to MonarchAiJSON.Text("crm"), "selection" to MonarchAiJSON.Text("Northwind")),
            bodies.single(),
        )
        // The next send reads the body as it is then, without the app touching the conversation.
        selection = "Harbor"
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextInput("And this one?")
        rule.onNode(hasText("Send") and hasClickAction()).performClick()
        rule.waitUntil(timeoutMillis = 5_000) { bodies.size == 2 }
        assertEquals(MonarchAiJSON.Text("Harbor"), bodies.last()["selection"])
    }

    @Test
    fun theContextChipsTakeTheirOwnRemoveMark() {
        rule.setMonarchContent {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = AiAssistantPanelMessages,
                open = true,
                onOpenChange = {},
                onSend = {},
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
                context = listOf(MonarchAiContextItem("page", MonarchAiContextKind.Page, "Quarterly plan")),
                onRemoveContext = {},
                closeIcon = { Text("close-mark") },
                removeContextIcon = { Text("remove-mark") },
            )
        }
        rule.onAllNodesWithText("close-mark", useUnmergedTree = true).assertCountEquals(1)
        rule.onAllNodesWithText("remove-mark", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = emptyList(),
                open = true,
                onOpenChange = {},
                onSend = {},
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
                closeIcon = null,
                minimizeIcon = null,
                onMinimizedChange = {},
                labels = MonarchAiAssistantPanelLabels(
                    close = "Fermer",
                    minimize = "Réduire",
                    welcome = "Comment puis-je aider ?",
                    inputLabel = "Écrire à l’assistant",
                    assistant = "Assistant IA",
                ),
            )
        }
        rule.onNodeWithText("Fermer").assertExists()
        rule.onNodeWithText("Réduire").assertExists()
        rule.onNodeWithText("Comment puis-je aider ?").assertExists()
        val renamed = SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription,
            listOf("Écrire à l’assistant"),
        )
        rule.onNode(renamed).assertExists()
    }

    @Test
    fun theCloseButtonIsAtTheStartInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiAssistantPanel(
                title = "Workspace help",
                messages = AiAssistantPanelMessages,
                open = true,
                onOpenChange = {},
                onSend = {},
                modifier = Modifier.height(440.dp),
                placement = AiAssistantPanelInline,
                closeIcon = { Icon(TestIcon, contentDescription = null) },
            )
        }
        rule.onNodeWithText("Workspace help")
            .assertIsBefore(rule.onNodeWithContentDescription("Close assistant"), LayoutDirection.Rtl)
    }

    @Test
    fun aLongTitleWrapsAtTheLargestFontSize() {
        val title = "Assistant for the quarterly planning workspace"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiAssistantPanel(
                title = title,
                messages = AiAssistantPanelMessages,
                open = true,
                onOpenChange = {},
                onSend = {},
                placement = AiAssistantPanelInline,
                minimized = true,
                onMinimizedChange = {},
                closeIcon = { Icon(TestIcon, contentDescription = null) },
                minimizeIcon = { Icon(TestIcon, contentDescription = null) },
            )
        }
        rule.onNodeWithText(title).assertTextFullyVisible()
    }

    @Test
    fun theRules() {
        val t = MonarchAiAssistantPanelTokens
        assertTrue(t.asSheet(MonarchAiAssistantPanelPlacement.Auto, 400.dp))
        assertFalse(t.asSheet(MonarchAiAssistantPanelPlacement.Auto, 800.dp))
        assertTrue(t.asSheet(MonarchAiAssistantPanelPlacement.Sheet, 800.dp))
        assertFalse(t.asSheet(MonarchAiAssistantPanelPlacement.Inline, 400.dp))
        assertTrue(t.busy(MonarchAiConversationStatus.Submitted))
        assertTrue(t.busy(MonarchAiConversationStatus.Streaming))
        assertFalse(t.busy(MonarchAiConversationStatus.Error))
        assertTrue(t.startersEnabled(MonarchAiConversationStatus.Idle))
        assertFalse(t.startersEnabled(MonarchAiConversationStatus.AwaitingApproval))
        assertFalse(t.startersEnabled(MonarchAiConversationStatus.Streaming))
        assertTrue(t.showsMinimize(sheet = false, hasHandler = true))
        assertFalse(t.showsMinimize(sheet = true, hasHandler = true))
        assertFalse(t.showsMinimize(sheet = false, hasHandler = false))
        assertEquals("Dossier · Support", t.agentLine("Dossier", "Support"))
        assertEquals("Dossier", t.agentLine("Dossier", " "))
        assertNull(t.agentLine(null, null))
    }

    @Test
    fun theColoursComeFromTheThemeRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.bgSurface, MonarchAiAssistantPanelTokens.containerColor(c), theme.id)
            assertEquals(c.textDefault, MonarchAiAssistantPanelTokens.titleColor(c), theme.id)
            assertEquals(c.textMuted, MonarchAiAssistantPanelTokens.mutedColor(c), theme.id)
        }
    }
}
