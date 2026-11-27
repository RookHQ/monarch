package com.rookhq.monarch.components.intelligence

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiArtifactKind
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiArtifactPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversation
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationError
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationMessage
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationStatus
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiFinishReason
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMemoryAction
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMemoryPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMessageFeedback
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPartState
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiReasoningPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiRole
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiStreamEvent
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiTextPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallPatch
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallState
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolDecision
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolResponse
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiTransport
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiTransportRequest
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiTrigger
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiErrorPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiStreamReducer
import com.rookhq.monarch.components.intelligence.aiconversation.rememberMonarchAiConversation
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.fixture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val StoredJson = """
    [{"id":"u1","role":"user","createdAt":"2025-03-10T12:00:00.000Z",
      "parts":[{"type":"text","text":"What changed?","state":"done"}]},
     {"id":"a1","role":"assistant","createdAt":"2025-03-10T12:00:02.000Z","model":"gemini-2.5-pro",
      "feedback":"up","finishReason":"content-filter",
      "parts":[{"type":"reasoning","text":"Looking","state":"done","durationMs":1200},
               {"type":"tool-call","id":"t1","name":"search","input":{"q":"invoices","limit":10},"state":"succeeded"},
               {"type":"text","text":"Two invoices were paid.","state":"done"},
               {"type":"artifact","id":"x1","kind":"document","title":"Q3 summary","version":3},
               {"type":"error","code":"http-503","message":"Down","retryable":true,"resumable":true}]}]
""".trimIndent()

internal object MonarchAiConversationFixtures : ComponentFixtures {
    override val id = "ai-conversation"
    override val fixtures = listOf(
        fixture("a stored conversation, shown as text") {
            val conversation = rememberMonarchAiConversation(
                transport = ScriptedTransport { flowOf() },
                initialMessages = MonarchAiConversationMessage.listFromJson(StoredJson),
            )
            val messages by conversation.messages.collectAsState()
            Column { messages.forEach { MonarchText(text = "${it.role.name}: ${it.text}") } }
        },
    )
}

/** A transport that answers every request with the flow [reply] builds, and records the requests. */
private class ScriptedTransport(
    override val respondsToTools: Boolean = false,
    private val reply: (MonarchAiTransportRequest) -> Flow<MonarchAiStreamEvent>,
) : MonarchAiTransport {
    val requests = mutableListOf<MonarchAiTransportRequest>()
    val toolResponses = mutableListOf<MonarchAiToolResponse>()

    override fun stream(request: MonarchAiTransportRequest): Flow<MonarchAiStreamEvent> {
        requests += request
        return reply(request)
    }

    override fun respondToTool(response: MonarchAiToolResponse): Flow<MonarchAiStreamEvent> {
        toolResponses += response
        return flowOf(
            MonarchAiStreamEvent.ToolUpdate(
                response.toolCallId,
                MonarchAiToolCallPatch(state = MonarchAiToolCallState.Succeeded),
            ),
            MonarchAiStreamEvent.TextDelta("Done."),
            MonarchAiStreamEvent.Done(),
        )
    }
}

private fun echo(): ScriptedTransport = ScriptedTransport { request ->
    flowOf(
        MonarchAiStreamEvent.TextDelta("You asked: "),
        MonarchAiStreamEvent.TextDelta(request.messages.last().text),
        MonarchAiStreamEvent.Done(conversationId = "c1"),
    )
}

private fun TestScope.conversation(
    transport: MonarchAiTransport,
    scope: CoroutineScope = backgroundScope,
    initial: List<MonarchAiConversationMessage> = emptyList(),
): MonarchAiConversation {
    var next = 0
    return MonarchAiConversation(
        transport = transport,
        scope = scope,
        connectionLostMessage = "Lost",
        toolResultFailedMessage = "Tool failed",
        initialMessages = initial,
        generateId = { "m${next++}" },
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class MonarchAiConversationTest {
    @Test
    fun sendStreamsTheReplyIntoTheMessages() = runTest {
        val transport = echo()
        val conversation = conversation(transport)
        val finished = mutableListOf<MonarchAiConversationMessage>()
        conversation.onFinish = { finished += it }
        assertTrue(conversation.send("Hello"))
        assertEquals(MonarchAiConversationStatus.Submitted, conversation.status.value)
        assertTrue(conversation.busy)
        runCurrent()
        val messages = conversation.messages.value
        assertEquals(listOf(MonarchAiRole.User, MonarchAiRole.Assistant), messages.map { it.role })
        assertEquals("You asked: Hello", messages.last().text)
        assertFalse(messages.last().streaming)
        assertEquals(MonarchAiConversationStatus.Idle, conversation.status.value)
        assertEquals("c1", conversation.conversationId.value)
        assertEquals(1, finished.size)
        assertEquals(MonarchAiTrigger.Send, transport.requests.single().trigger)
        assertEquals(listOf("Hello"), transport.requests.single().messages.map { it.text })
    }

    @Test
    fun blankTextIsNotSent() = runTest {
        val transport = echo()
        val conversation = conversation(transport)
        assertTrue(conversation.send("   "))
        runCurrent()
        assertTrue(transport.requests.isEmpty())
        assertTrue(conversation.messages.value.isEmpty())
    }

    @Test
    fun stopKeepsWhatArrivedAndClosesIt() = runTest {
        val conversation = conversation(
            ScriptedTransport {
                flow {
                    emit(MonarchAiStreamEvent.TextDelta("Partial"))
                    awaitCancellation()
                }
            },
        )
        conversation.send("Hi")
        runCurrent()
        assertTrue(conversation.messages.value.last().streaming)
        conversation.stop()
        runCurrent()
        assertEquals(MonarchAiConversationStatus.Stopped, conversation.status.value)
        assertEquals("Partial", conversation.messages.value.last().text)
        assertFalse(conversation.messages.value.last().streaming)
    }

    @Test
    fun anErrorEventFailsTheTurnAndRetryRewritesTheSameMessage() = runTest {
        var attempt = 0
        val conversation = conversation(
            ScriptedTransport {
                attempt++
                if (attempt == 1) {
                    flowOf(MonarchAiStreamEvent.Error(message = "Down", code = "http-503"))
                } else {
                    flowOf(MonarchAiStreamEvent.TextDelta("Back"), MonarchAiStreamEvent.Done())
                }
            },
        )
        val errors = mutableListOf<MonarchAiConversationError>()
        conversation.onError = { errors += it }
        conversation.send("Hi")
        runCurrent()
        assertEquals(MonarchAiConversationStatus.Error, conversation.status.value)
        val error = conversation.error.value
        assertEquals("http-503", error?.code)
        assertTrue(error?.retryable == true)
        assertEquals(1, errors.size)
        val failed = conversation.messages.value.last()
        assertTrue(failed.parts.last() is MonarchAiErrorPart)
        conversation.retry()
        runCurrent()
        val retried = conversation.messages.value.last()
        assertEquals(failed.id, retried.id)
        assertEquals("Back", retried.text)
        assertNull(retried.branch)
        assertNull(conversation.error.value)
    }

    @Test
    fun aFlowThatThrowsEndsWithARetryableNetworkError() = runTest {
        val conversation = conversation(
            ScriptedTransport {
                flow {
                    emit(MonarchAiStreamEvent.TextDelta("Half"))
                    throw IOException("reset")
                }
            },
        )
        conversation.send("Hi")
        runCurrent()
        val error = conversation.error.value
        assertEquals("network", error?.code)
        assertEquals("Lost", error?.message)
        assertTrue(error?.retryable == true)
        assertEquals("Half", conversation.messages.value.last().text)
    }

    @Test
    fun regenerateAddsABranchAndBranchToSwitchesBack() = runTest {
        var answer = 0
        val conversation = conversation(
            ScriptedTransport {
                flowOf(MonarchAiStreamEvent.TextDelta("Answer ${++answer}"), MonarchAiStreamEvent.Done())
            },
        )
        conversation.send("Hi")
        runCurrent()
        conversation.regenerate()
        runCurrent()
        val reply = conversation.messages.value.last()
        assertEquals("Answer 2", reply.text)
        assertEquals(1, reply.branch?.index)
        assertEquals(2, reply.branch?.count)
        conversation.branchTo(reply.id, 0)
        assertEquals("Answer 1", conversation.messages.value.last().text)
    }

    @Test
    fun editAndResendBranchesTheUserMessage() = runTest {
        val transport = echo()
        val conversation = conversation(transport)
        conversation.send("First")
        runCurrent()
        val user = conversation.messages.value.first()
        conversation.editAndResend(user.id, "Second")
        runCurrent()
        val messages = conversation.messages.value
        assertEquals("Second", messages.first().text)
        assertEquals(2, messages.first().branch?.count)
        assertEquals("You asked: Second", messages.last().text)
        assertEquals(MonarchAiTrigger.Edit, transport.requests.last().trigger)
    }

    @Test
    fun aToolCallWaitsForApprovalAndRefusesNewMessages() = runTest {
        val call = MonarchAiToolCallPart("t1", "delete_file", state = MonarchAiToolCallState.PendingApproval)
        val transport = ScriptedTransport { flowOf(MonarchAiStreamEvent.Part(call), MonarchAiStreamEvent.Done()) }
        val conversation = conversation(transport)
        val responses = mutableListOf<MonarchAiToolResponse>()
        conversation.onToolResponse = { responses += it }
        conversation.send("Delete it")
        runCurrent()
        assertEquals(MonarchAiConversationStatus.AwaitingApproval, conversation.status.value)
        assertFalse(conversation.canSend)
        assertFalse(conversation.send("Another"))
        conversation.denyTool("t1", reason = "Not now")
        runCurrent()
        val denied = conversation.messages.value.last().toolCalls.single()
        assertEquals(MonarchAiToolCallState.Denied, denied.state)
        assertEquals("Not now", denied.error)
        assertEquals(MonarchAiConversationStatus.Idle, conversation.status.value)
        assertFalse(responses.single().approved)
    }

    @Test
    fun anApprovedCallContinuesThroughATransportThatRespondsToTools() = runTest {
        val call = MonarchAiToolCallPart("t1", "send_email", state = MonarchAiToolCallState.PendingApproval)
        val transport = ScriptedTransport(respondsToTools = true) {
            flowOf(MonarchAiStreamEvent.Part(call), MonarchAiStreamEvent.Done())
        }
        val conversation = conversation(transport)
        conversation.send("Email Ada")
        runCurrent()
        conversation.approveTool("t1", input = MonarchAiJSON.Object(mapOf("to" to MonarchAiJSON.Text("ada"))))
        runCurrent()
        assertTrue(transport.toolResponses.single().approved)
        val reply = conversation.messages.value.last()
        assertEquals("Done.", reply.text)
        assertEquals(MonarchAiJSON.Text("ada"), reply.toolCalls.single().input["to"])
        assertEquals(MonarchAiToolCallState.Succeeded, reply.toolCalls.single().state)
        assertEquals(MonarchAiConversationStatus.Idle, conversation.status.value)
    }

    @Test
    fun aToolPolicyDecidesWithoutAsking() = runTest {
        val call = MonarchAiToolCallPart("t1", "read_file", state = MonarchAiToolCallState.PendingApproval)
        val conversation = conversation(
            ScriptedTransport { flowOf(MonarchAiStreamEvent.Part(call), MonarchAiStreamEvent.Done()) },
        )
        conversation.toolPolicy = { MonarchAiToolDecision.Allow }
        conversation.send("Read it")
        runCurrent()
        assertEquals(MonarchAiToolCallState.Running, conversation.messages.value.last().toolCalls.single().state)
        assertEquals(MonarchAiConversationStatus.Idle, conversation.status.value)
    }

    @Test
    fun clientSideToolsRunAndTheReplyContinues() = runTest {
        val call = MonarchAiToolCallPart("t1", "local_time", state = MonarchAiToolCallState.Running)
        val transport = ScriptedTransport { request ->
            if (request.trigger == MonarchAiTrigger.ToolResult) {
                flowOf(MonarchAiStreamEvent.TextDelta("It is noon."), MonarchAiStreamEvent.Done())
            } else {
                flowOf(
                    MonarchAiStreamEvent.Part(call),
                    MonarchAiStreamEvent.Finish(MonarchAiFinishReason.ToolUse),
                    MonarchAiStreamEvent.Done(),
                )
            }
        }
        val conversation = conversation(transport)
        conversation.onToolCall = { MonarchAiJSON.Text("12:00") }
        conversation.send("What time is it?")
        runCurrent()
        val reply = conversation.messages.value.last()
        assertEquals(MonarchAiToolCallState.Succeeded, reply.toolCalls.single().state)
        assertEquals(MonarchAiJSON.Text("12:00"), reply.toolCalls.single().output)
        assertEquals("It is noon.", reply.text)
        assertEquals(MonarchAiTrigger.ToolResult, transport.requests.last().trigger)
    }

    @Test
    fun aClientToolThatThrowsFailsTheCall() = runTest {
        val call = MonarchAiToolCallPart("t1", "local_time", state = MonarchAiToolCallState.AwaitingResult)
        val transport = ScriptedTransport { request ->
            if (request.trigger == MonarchAiTrigger.ToolResult) {
                flowOf(MonarchAiStreamEvent.Done())
            } else {
                flowOf(MonarchAiStreamEvent.Part(call), MonarchAiStreamEvent.Done())
            }
        }
        val conversation = conversation(transport)
        conversation.onToolCall = { throw IOException() }
        conversation.send("Time?")
        runCurrent()
        val failed = conversation.messages.value.last().toolCalls.single()
        assertEquals(MonarchAiToolCallState.Failed, failed.state)
        assertEquals("Tool failed", failed.error)
    }

    @Test
    fun messagesWrittenOfflineWaitAndAreSentOnReconnect() = runTest {
        val transport = echo()
        val conversation = conversation(transport)
        conversation.setOnline(false)
        conversation.send("Queued one")
        runCurrent()
        assertTrue(transport.requests.isEmpty())
        assertEquals(1, conversation.queued.value.size)
        assertTrue(conversation.messages.value.single().queued)
        conversation.setOnline(true)
        runCurrent()
        assertTrue(conversation.queued.value.isEmpty())
        assertEquals("You asked: Queued one", conversation.messages.value.last().text)
        assertFalse(conversation.messages.value.first().queued)
    }

    @Test
    fun aQueuedMessageCanBeCancelled() = runTest {
        val conversation = conversation(echo())
        conversation.setOnline(false)
        conversation.send("Never mind")
        conversation.cancelQueued(conversation.queued.value.single().id)
        assertTrue(conversation.queued.value.isEmpty())
        assertTrue(conversation.messages.value.isEmpty())
    }

    @Test
    fun aReplyThatSendsNothingBecomesSlow() = runTest {
        val conversation = conversation(ScriptedTransport { flow { awaitCancellation() } })
        conversation.send("Hi")
        advanceTimeBy(MonarchAiConversation.DEFAULT_SLOW_AFTER_MILLIS - 1)
        assertFalse(conversation.slow.value)
        advanceTimeBy(2)
        assertTrue(conversation.slow.value)
        conversation.stop()
        assertFalse(conversation.slow.value)
    }

    @Test
    fun feedbackAndResetChangeTheMessages() = runTest {
        val conversation = conversation(echo())
        conversation.send("Hi")
        runCurrent()
        val reply = conversation.messages.value.last()
        conversation.setFeedback(reply.id, MonarchAiMessageFeedback.Down)
        assertEquals(MonarchAiMessageFeedback.Down, conversation.messages.value.last().feedback)
        conversation.reset()
        assertTrue(conversation.messages.value.isEmpty())
        assertEquals(MonarchAiConversationStatus.Idle, conversation.status.value)
    }

    @Test
    fun aMemoryEventIsReportedWithTheReply() = runTest {
        val memory = MonarchAiMemoryPart("m1", MonarchAiMemoryAction.Write, "Prefers metric units")
        val conversation = conversation(
            ScriptedTransport { flowOf(MonarchAiStreamEvent.Memory(memory), MonarchAiStreamEvent.Done()) },
        )
        val seen = mutableListOf<MonarchAiMemoryPart>()
        conversation.onMemoryEvent = { part, _ -> seen += part }
        conversation.send("Use metric")
        runCurrent()
        assertEquals(listOf(memory), seen)
        assertEquals(listOf(memory), conversation.messages.value.last().memories)
    }

    @Test
    fun theReducerFollowsTheWebRules() {
        val start =
            MonarchAiConversationMessage("a", MonarchAiRole.Assistant, emptyList(), "2025-03-10T12:00:00.000Z")
        val call = MonarchAiToolCallPart("t1", "search", state = MonarchAiToolCallState.Running, startedAt = "s")
        val reply = MonarchAiStreamReducer.apply(
            listOf(
                MonarchAiStreamEvent.ReasoningDelta("Think"),
                MonarchAiStreamEvent.TextDelta("Hi"),
                MonarchAiStreamEvent.Part(call),
                MonarchAiStreamEvent.Part(call.copy(input = MonarchAiJSON.Text("q"), startedAt = null)),
                MonarchAiStreamEvent.ToolInputDelta("t1", "{\"a\""),
                MonarchAiStreamEvent.ToolUpdate("t1", MonarchAiToolCallPatch(output = MonarchAiJSON.Bool(true))),
                MonarchAiStreamEvent.ReasoningEnd(durationMs = 5, redacted = true),
                MonarchAiStreamEvent.Done(),
            ),
            start,
        )
        val reasoning = reply.parts[0] as MonarchAiReasoningPart
        assertEquals(MonarchAiPartState.Done, reasoning.state)
        assertEquals((reply.parts[1] as MonarchAiTextPart).state, MonarchAiPartState.Done)
        val merged = reply.toolCalls.single()
        assertEquals("s", merged.startedAt)
        assertEquals(MonarchAiJSON.Text("q"), merged.input)
        assertEquals("{\"a\"", merged.inputText)
        assertEquals(MonarchAiJSON.Bool(true), merged.output)
        // Done without a tool-use finish cancels a running call.
        assertEquals(MonarchAiToolCallState.Cancelled, merged.state)
        // A redacted end with nothing streaming adds its own part.
        val redacted = reply.parts.last() as MonarchAiReasoningPart
        assertTrue(redacted.redacted)
        assertEquals(5L, redacted.durationMs)
    }

    @Test
    fun storedConversationsReadAndWriteTheWebsJson() {
        val messages = MonarchAiConversationMessage.listFromJson(StoredJson)
        val reply = messages.last()
        assertEquals(MonarchAiFinishReason.ContentFilter, reply.finishReason)
        assertEquals(MonarchAiMessageFeedback.Up, reply.feedback)
        assertEquals(1200L, (reply.parts[0] as MonarchAiReasoningPart).durationMs)
        assertEquals(MonarchAiJSON.Number(10.0), reply.toolCalls.single().input["limit"])
        assertEquals(MonarchAiArtifactPart("x1", MonarchAiArtifactKind.Document, "Q3 summary", 3), reply.parts[3])
        assertTrue((reply.parts[4] as MonarchAiErrorPart).resumable)
        assertEquals(1_741_608_002_000L, reply.createdAtMillis)
        val again = MonarchAiConversationMessage.listFromJson(MonarchAiConversationMessage.listToJson(messages))
        assertEquals(messages, again)
        val text = messages.first().toJson().toJsonString()
        assertEquals(
            """{"id":"u1","role":"user","parts":[{"type":"text","text":"What changed?","state":"done"}],""" +
                """"createdAt":"2025-03-10T12:00:00.000Z"}""",
            text,
        )
        assertEquals("2025-03-10T12:00:00.000Z", MonarchAiConversationMessage.timestamp(1_741_608_000_000L))
    }

    @Test
    fun unknownPartsAndBadJsonAreRejected() {
        val unknown = """[{"id":"x","role":"user","createdAt":"t","parts":[{"type":"video"}]}]"""
        assertFailsWith<IllegalArgumentException> { MonarchAiConversationMessage.listFromJson(unknown) }
        assertFailsWith<IllegalArgumentException> { MonarchAiJSON.parse("{\"a\":") }
        assertEquals(
            MonarchAiJSON.Object(mapOf("s" to MonarchAiJSON.Text("a\"b\né"), "n" to MonarchAiJSON.Null)),
            MonarchAiJSON.parse("""{"s":"a\"b\né","n":null}"""),
        )
        assertEquals("[1.5,-2,true]", MonarchAiJSON.parse("[1.5, -2, true]").toJsonString())
    }

    @Test
    fun onlyKnownTransientCodesAreRetryable() {
        assertTrue(MonarchAiConversationError.isRetryable("network"))
        assertTrue(MonarchAiConversationError.isRetryable("http-503"))
        assertFalse(MonarchAiConversationError.isRetryable("http-404"))
        assertFalse(MonarchAiConversationError.isRetryable("http-5000"))
        assertFalse(MonarchAiConversationError.isRetryable("invalid_request"))
        assertFalse(MonarchAiConversationError.isRetryable(null))
    }
}
