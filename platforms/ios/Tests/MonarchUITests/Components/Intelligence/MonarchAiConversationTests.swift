import Foundation
import MonarchTokens
import SwiftUI
import Testing
import os

@testable import MonarchUI

/// A transport that plays scripted replies and records what it was asked.
final class MonarchAiScriptedTransport: MonarchAiTransport {
    enum Reply: Sendable {
        /// Yields the events, then finishes.
        case events([MonarchAiStreamEvent])
        /// Yields the events, then throws.
        case failing([MonarchAiStreamEvent])
        /// Yields the events and stays open until `yield` or `finish` is called.
        case open([MonarchAiStreamEvent])
    }

    struct Failure: Error {}

    struct State: Sendable {
        var replies: [Reply] = []
        var requests: [MonarchAiTransportRequest] = []
        var toolResponses: [MonarchAiToolResponse] = []
        var resumes: [MonarchAiResumeRequest] = []
        var open: [MonarchAiEventStream.Continuation] = []
        var terminated = 0
    }

    let state: OSAllocatedUnfairLock<State>
    let respondsToTools: Bool
    let resumesRuns: Bool

    init(_ replies: [Reply] = [], respondsToTools: Bool = false, resumesRuns: Bool = false) {
        state = OSAllocatedUnfairLock(initialState: State(replies: replies))
        self.respondsToTools = respondsToTools
        self.resumesRuns = resumesRuns
    }

    var requests: [MonarchAiTransportRequest] { state.withLock { $0.requests } }
    var toolResponses: [MonarchAiToolResponse] { state.withLock { $0.toolResponses } }
    var resumes: [MonarchAiResumeRequest] { state.withLock { $0.resumes } }
    var terminated: Int { state.withLock { $0.terminated } }

    /// Sends more events into the oldest stream that is still open.
    func yield(_ events: MonarchAiStreamEvent...) {
        let continuation = state.withLock { $0.open.first }
        for event in events { continuation?.yield(event) }
    }

    /// Ends the oldest stream that is still open.
    func finish() {
        let continuation = state.withLock { $0.open.isEmpty ? nil : $0.open.removeFirst() }
        continuation?.finish()
    }

    private func next() -> MonarchAiEventStream {
        let reply = state.withLock { $0.replies.isEmpty ? Reply.events([.done()]) : $0.replies.removeFirst() }
        let state = state
        return MonarchAiEventStream { continuation in
            continuation.onTermination = { _ in state.withLock { $0.terminated += 1 } }
            switch reply {
            case .events(let events):
                for event in events { continuation.yield(event) }
                continuation.finish()
            case .failing(let events):
                for event in events { continuation.yield(event) }
                continuation.finish(throwing: Failure())
            case .open(let events):
                for event in events { continuation.yield(event) }
                state.withLock { $0.open.append(continuation) }
            }
        }
    }

    func stream(_ request: MonarchAiTransportRequest) -> MonarchAiEventStream {
        state.withLock { $0.requests.append(request) }
        return next()
    }

    func respondToTool(_ response: MonarchAiToolResponse) -> MonarchAiEventStream {
        state.withLock { $0.toolResponses.append(response) }
        return next()
    }

    func resume(_ request: MonarchAiResumeRequest) -> MonarchAiEventStream {
        state.withLock { $0.resumes.append(request) }
        return next()
    }
}

/// Shows a conversation as plain text, for the fixtures.
private struct MonarchAiConversationFixtureView: View {
    let conversation: MonarchAiConversation

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space2) {
            Text("Status: \(conversation.status.rawValue)")
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
            ForEach(conversation.messages) { message in
                VStack(alignment: .leading) {
                    Text(message.role.rawValue).monarchTextStyle(.caption)
                    Text(message.text.isEmpty ? message.parts.map(\.type).joined(separator: ", ") : message.text)
                        .monarchTextStyle(.body)
                }
                .foregroundStyle(message.isQueued ? theme.colors.textMuted : theme.colors.textDefault)
            }
            if let error = conversation.error {
                Text(error.message).monarchTextStyle(.body).foregroundStyle(theme.colors.dangerStrong)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

enum MonarchAiConversationFixtures: ComponentFixtures {
    static let id = "ai-conversation"

    @MainActor
    static func stored() -> [MonarchAiConversationMessage] {
        [
            MonarchAiConversationMessage(
                id: "u1",
                role: .user,
                parts: [.text(MonarchAiTextPart(text: "What changed since yesterday?"))],
                createdAt: "2025-03-10T12:00:00.000Z"
            ),
            MonarchAiConversationMessage(
                id: "a1",
                role: .assistant,
                parts: [
                    .reasoning(MonarchAiReasoningPart(text: "Compare the two reports.", durationMs: 1200)),
                    .text(MonarchAiTextPart(text: "Three invoices were paid and one is now overdue.")),
                    .source(MonarchAiSourcePart(id: "s1", title: "Invoices", kind: .record)),
                ],
                createdAt: "2025-03-10T12:00:02.000Z",
                model: "gemini-2.5-pro"
            ),
        ]
    }

    static let fixtures = [
        Fixture("empty") {
            MonarchAiConversationFixtureView(
                conversation: MonarchAiConversation(transport: MonarchAiScriptedTransport())
            )
        },
        Fixture("stored conversation") {
            MonarchAiConversationFixtureView(
                conversation: MonarchAiConversation(transport: MonarchAiScriptedTransport(), initialMessages: stored())
            )
        },
        Fixture("queued while offline") {
            let conversation = MonarchAiConversation(
                transport: MonarchAiScriptedTransport(),
                initialMessages: stored()
            )
            conversation.setOnline(false)
            conversation.send("And the week before? This message is long enough to wrap onto another line.")
            return MonarchAiConversationFixtureView(conversation: conversation)
        },
        Fixture("waiting for the first event") {
            let conversation = MonarchAiConversation(
                transport: MonarchAiScriptedTransport([.open([])]),
                initialMessages: stored(),
                slowAfter: nil
            )
            conversation.send("Summarise it.")
            return MonarchAiConversationFixtureView(conversation: conversation)
        },
    ]
}

@Suite("MonarchAiConversation")
@MainActor
struct MonarchAiConversationTests {
    private static let noon = Date(timeIntervalSince1970: 1_741_608_000)
    private static let noonText = "2025-03-10T12:00:00.000Z"

    /// A conversation with ids m1, m2, … and a fixed clock.
    private func make(
        _ transport: MonarchAiScriptedTransport,
        initialMessages: [MonarchAiConversationMessage] = [],
        conversationId: String? = nil,
        body: [String: MonarchAiJSON] = [:],
        queuesWhileOffline: Bool = true,
        slowAfter: Duration? = nil,
        connectionLostMessage: String? = nil
    ) -> MonarchAiConversation {
        var counter = 0
        return MonarchAiConversation(
            transport: transport,
            initialMessages: initialMessages,
            conversationId: conversationId,
            body: body,
            queuesWhileOffline: queuesWhileOffline,
            slowAfter: slowAfter,
            connectionLostMessage: connectionLostMessage,
            toolResultFailedMessage: nil,
            generateId: {
                counter += 1
                return "m\(counter)"
            },
            now: { Self.noon },
            sleep: { _ in }
        )
    }

    /// Waits, for at most a few seconds, until the condition holds.
    private func eventually(_ condition: () -> Bool) async -> Bool {
        for _ in 0..<3000 {
            if condition() { return true }
            try? await Task.sleep(for: .milliseconds(1))
        }
        return condition()
    }

    private func toolCall(
        _ id: String = "t1",
        state: MonarchAiToolCallState,
        input: MonarchAiJSON = ["query": "invoices"]
    ) -> MonarchAiStreamEvent {
        .part(.toolCall(MonarchAiToolCallPart(id: id, name: "search", input: input, state: state)))
    }

    // MARK: Sending and streaming

    @Test("A prompt is sent through the transport and the reply streams into the message model")
    func sendAndStream() async {
        let transport = MonarchAiScriptedTransport([
            .events([
                .agent(MonarchAiAgentRef(name: "Rook Intelligence", role: "Writing assistant")),
                .textDelta("Three invoices "),
                .textDelta("were paid."),
                .usage(MonarchAiUsage(inputTokens: 12, outputTokens: 5), model: "gemini-2.5-pro"),
                .finish(.stop),
                .done(conversationId: "c1"),
            ])
        ])
        let conversation = make(transport, body: ["workspace": "acme", "mode": "ask"])
        var finished: [MonarchAiConversationMessage] = []
        conversation.onFinish = { finished.append($0) }

        #expect(conversation.send("What changed?", body: ["mode": "edit"]))
        #expect(conversation.status == .submitted)
        #expect(conversation.isBusy)
        #expect(conversation.messages.map(\.role) == [.user, .assistant])
        #expect(conversation.messages[1].parts.isEmpty)

        await conversation.settled()

        #expect(conversation.status == .idle)
        #expect(!conversation.isBusy)
        #expect(conversation.error == nil)
        #expect(conversation.conversationId == "c1")
        #expect(conversation.agent?.name == "Rook Intelligence")
        let reply = conversation.messages[1]
        #expect(reply.id == "m2")
        #expect(reply.text == "Three invoices were paid.")
        #expect(reply.parts == [.text(MonarchAiTextPart(text: "Three invoices were paid.", state: .done))])
        #expect(!reply.isStreaming)
        #expect(reply.model == "gemini-2.5-pro")
        #expect(reply.usage?.outputTokens == 5)
        #expect(reply.finishReason == .stop)
        #expect(reply.agent?.role == "Writing assistant")
        #expect(reply.createdAt == Self.noonText)
        #expect(finished == [reply])

        let request = transport.requests[0]
        #expect(transport.requests.count == 1)
        #expect(request.trigger == .send)
        #expect(request.conversationId == nil)
        #expect(request.messages.map(\.id) == ["m1"])
        #expect(request.messages[0].text == "What changed?")
        // The fields of the turn win over the fields of the conversation.
        #expect(request.body == ["workspace": "acme", "mode": "edit"])
    }

    @Test("Empty text without files sends nothing, and files become file parts")
    func emptyAndFiles() async {
        let transport = MonarchAiScriptedTransport()
        let conversation = make(transport, conversationId: "c0")
        #expect(conversation.send("   \n"))
        #expect(conversation.messages.isEmpty)
        #expect(transport.requests.isEmpty)

        let file = MonarchAiFilePart(name: "report.pdf", mediaType: "application/pdf", size: 1024, fileId: "f1")
        conversation.send("", files: [file])
        await conversation.settled()
        #expect(conversation.messages[0].parts == [.file(file)])
        #expect(transport.requests[0].conversationId == "c0")
    }

    @Test("The status becomes streaming with the first event and partial text is visible")
    func streamingIsVisible() async {
        let transport = MonarchAiScriptedTransport([.open([.textDelta("Thinking")])])
        let conversation = make(transport)
        conversation.send("Hello")
        #expect(await eventually { conversation.status == .streaming })
        #expect(conversation.messages[1].text == "Thinking")
        #expect(conversation.messages[1].isStreaming)

        transport.yield(.textDelta(" aloud"), .done())
        await conversation.settled()
        #expect(conversation.messages[1].text == "Thinking aloud")
        #expect(conversation.status == .idle)
        // The done event ends the reply even though the transport kept the stream open.
        #expect(await eventually { transport.terminated == 1 })
    }

    @Test("A stream that ends without a done event leaves the conversation idle and does not call onFinish")
    func endsWithoutDone() async {
        let conversation = make(MonarchAiScriptedTransport([.events([.textDelta("Partial")])]))
        var finished = 0
        conversation.onFinish = { _ in finished += 1 }
        conversation.send("Hello")
        await conversation.settled()
        #expect(conversation.status == .idle)
        #expect(conversation.messages[1].parts == [.text(MonarchAiTextPart(text: "Partial", state: .done))])
        #expect(finished == 0)
    }

    // MARK: Stop and supersede

    @Test("Stop keeps what arrived, closes the parts and terminates the stream")
    func stop() async {
        let transport = MonarchAiScriptedTransport([
            .open([.textDelta("Half an "), toolCall(state: .running)])
        ])
        let conversation = make(transport)
        conversation.send("Hello")
        #expect(await eventually { conversation.messages.last?.parts.count == 2 })

        conversation.stop()
        #expect(conversation.status == .stopped)
        #expect(!conversation.isBusy)
        let reply = conversation.messages[1]
        #expect(reply.parts.first == .text(MonarchAiTextPart(text: "Half an ", state: .done)))
        #expect(reply.toolCalls.first?.state == .cancelled)
        #expect(await eventually { transport.terminated == 1 })

        // Events that arrive after the stop do not reach the conversation.
        transport.yield(.textDelta("answer"))
        try? await Task.sleep(for: .milliseconds(20))
        #expect(conversation.messages[1].text == "Half an ")
        #expect(conversation.status == .stopped)

        // Stop with nothing running changes nothing.
        conversation.stop()
        #expect(conversation.status == .stopped)
    }

    @Test("Sending again while a reply streams ends that reply and starts the next")
    func sendSupersedes() async {
        let transport = MonarchAiScriptedTransport([
            .open([.textDelta("First")]),
            .events([.textDelta("Second"), .done()]),
        ])
        let conversation = make(transport)
        conversation.send("One")
        #expect(await eventually { conversation.messages.last?.text == "First" })
        conversation.send("Two")
        await conversation.settled()

        #expect(conversation.messages.map(\.text) == ["One", "First", "Two", "Second"])
        #expect(conversation.messages[1].isStreaming == false)
        #expect(conversation.status == .idle)
        #expect(transport.requests[1].messages.map(\.text) == ["One", "First", "Two"])
    }

    // MARK: Errors and retry

    @Test("An error event fails the turn, and retry asks again in place")
    func errorAndRetry() async {
        let transport = MonarchAiScriptedTransport([
            .events([
                .textDelta("Half"),
                .error(code: "rate-limited", message: "Too many requests.", retryAfterMs: 12_000),
                .textDelta("never applied"),
            ]),
            .events([.textDelta("Whole answer"), .done()]),
        ])
        let conversation = make(transport)
        var reported: [MonarchAiConversationError] = []
        conversation.onError = { reported.append($0) }
        conversation.send("Hello")
        await conversation.settled()

        let expected = MonarchAiConversationError(
            code: "rate-limited",
            message: "Too many requests.",
            isRetryable: true,
            retryAfterMs: 12_000
        )
        #expect(conversation.status == .error)
        #expect(conversation.error == expected)
        #expect(reported == [expected])
        #expect(
            conversation.messages[1].parts == [
                .text(MonarchAiTextPart(text: "Half", state: .done)),
                .error(
                    MonarchAiErrorPart(
                        code: "rate-limited",
                        message: "Too many requests.",
                        isRetryable: true,
                        retryAfterMs: 12_000
                    )
                ),
            ]
        )

        conversation.retry()
        #expect(conversation.status == .submitted)
        #expect(conversation.error == nil)
        await conversation.settled()

        // The same message is written again: no branch, the same id.
        #expect(conversation.messages.map(\.id) == ["m1", "m2"])
        #expect(conversation.messages[1].branch == nil)
        #expect(conversation.messages[1].text == "Whole answer")
        #expect(conversation.status == .idle)
        #expect(transport.requests[1].trigger == .regenerate)
        #expect(transport.requests[1].messages.map(\.id) == ["m1"])
    }

    @Test("An error without a code is unknown and not retryable")
    func unknownError() async {
        let conversation = make(MonarchAiScriptedTransport([.events([.error(message: "No.")])]))
        conversation.send("Hello")
        await conversation.settled()
        #expect(conversation.error == MonarchAiConversationError(code: "unknown", message: "No.", isRetryable: false))
    }

    @Test("A stream that throws ends the reply with a retryable network error and the built-in message")
    func streamThrows() async {
        let conversation = make(MonarchAiScriptedTransport([.failing([.textDelta("Half")])]))
        conversation.send("Hello")
        await conversation.settled()
        #expect(conversation.status == .error)
        #expect(
            conversation.error
                == MonarchAiConversationError(code: "network", message: "The connection was lost.", isRetryable: true)
        )
        #expect(conversation.messages[1].parts.count == 2)
        #expect(conversation.messages[1].parts.last?.type == "error")

        let custom = make(
            MonarchAiScriptedTransport([.failing([])]),
            connectionLostMessage: "Verbindung verloren."
        )
        custom.send("Hallo")
        await custom.settled()
        #expect(custom.error?.message == "Verbindung verloren.")
    }

    @Test("Codes that may be tried again unchanged are retryable; unknown ones are not")
    func retryableCodes() {
        for code in [
            "network", "timeout", "rate-limited", "rate-limit", "server", "overloaded", "http-500", "http-503",
        ] {
            #expect(MonarchAiConversationError.isRetryable(code: code), "\(code)")
        }
        for code in ["auth", "bad-request", "unknown", "http-404", "http-429", "http-5000", "http-5xx", ""] {
            #expect(!MonarchAiConversationError.isRetryable(code: code), "\(code)")
        }
        #expect(!MonarchAiConversationError.isRetryable(code: nil))
    }

    // MARK: Branches

    @Test("Regenerate adds a branch, and branchTo switches between the versions")
    func regenerate() async {
        let transport = MonarchAiScriptedTransport([
            .events([.textDelta("First answer"), .done()]),
            .events([.textDelta("Second answer"), .done()]),
        ])
        let conversation = make(transport)
        conversation.send("Hello")
        await conversation.settled()
        #expect(conversation.messages[1].branch == nil)

        conversation.regenerate(body: ["temperature": 1])
        await conversation.settled()

        #expect(conversation.messages.count == 2)
        #expect(conversation.messages[1].text == "Second answer")
        #expect(conversation.messages[1].branch == MonarchAiBranchPosition(index: 1, count: 2))
        #expect(transport.requests[1].trigger == .regenerate)
        #expect(transport.requests[1].messages.map(\.id) == ["m1"])
        #expect(transport.requests[1].body == ["temperature": 1])

        conversation.branchTo(conversation.messages[1].id, index: 0)
        #expect(conversation.messages[1].text == "First answer")
        #expect(conversation.messages[1].branch == MonarchAiBranchPosition(index: 0, count: 2))

        // An index outside the alternatives is ignored.
        conversation.branchTo(conversation.messages[1].id, index: 5)
        conversation.branchTo("missing", index: 0)
        #expect(conversation.messages[1].text == "First answer")

        // Regenerating a user message does nothing.
        conversation.regenerate("m1")
        #expect(transport.requests.count == 2)
    }

    @Test("Edit and resend replaces a user message as a new branch and keeps its files")
    func editAndResend() async {
        let transport = MonarchAiScriptedTransport([
            .events([.textDelta("About cats"), .done()]),
            .events([.textDelta("About dogs"), .done()]),
        ])
        let conversation = make(transport)
        let file = MonarchAiFilePart(name: "pets.csv", mediaType: "text/csv")
        conversation.send("Tell me about cats", files: [file])
        await conversation.settled()

        conversation.editAndResend("m1", text: "Tell me about dogs")
        await conversation.settled()

        #expect(conversation.messages.map(\.text) == ["Tell me about dogs", "About dogs"])
        #expect(conversation.messages[0].branch == MonarchAiBranchPosition(index: 1, count: 2))
        #expect(conversation.messages[0].parts.last == .file(file))
        #expect(transport.requests[1].trigger == .edit)
        #expect(transport.requests[1].messages.map(\.text) == ["Tell me about dogs"])

        conversation.branchTo(conversation.messages[0].id, index: 0)
        #expect(conversation.messages.map(\.text) == ["Tell me about cats", "About cats"])

        // Only a user message can be edited.
        conversation.editAndResend(conversation.messages[1].id, text: "No")
        #expect(transport.requests.count == 2)
    }

    // MARK: Tool approval

    @Test("A tool call that needs approval stops the conversation until it is approved")
    func approval() async {
        let transport = MonarchAiScriptedTransport([
            .events([.textDelta("Let me search."), toolCall(state: .pendingApproval), .done()])
        ])
        let conversation = make(transport)
        var responses: [MonarchAiToolResponse] = []
        conversation.onToolResponse = { responses.append($0) }
        var finished = 0
        conversation.onFinish = { _ in finished += 1 }
        conversation.send("Find the invoices")
        await conversation.settled()

        #expect(conversation.status == .awaitingApproval)
        #expect(!conversation.canSend)
        #expect(!conversation.send("Another question"))
        #expect(conversation.messages.count == 2)
        #expect(finished == 0)

        conversation.approveTool("t1", input: ["query": "paid invoices"])
        let call = conversation.messages[1].toolCalls[0]
        #expect(call.state == .running)
        #expect(call.input == ["query": "paid invoices"])
        #expect(call.startedAt == Self.noonText)
        #expect(conversation.status == .idle)
        #expect(conversation.canSend)
        #expect(responses.count == 1)
        #expect(responses[0].toolCallId == "t1")
        #expect(responses[0].isApproved)
        #expect(responses[0].input == ["query": "paid invoices"])

        // Approving again, or an unknown call, does nothing.
        conversation.approveTool("t1")
        conversation.denyTool("missing")
        #expect(responses.count == 1)
    }

    @Test("Denying a tool call records the reason")
    func deny() async {
        let conversation = make(
            MonarchAiScriptedTransport([.events([toolCall(state: .pendingApproval), .done()])])
        )
        conversation.send("Delete everything")
        await conversation.settled()
        conversation.denyTool("t1", reason: "Too risky")
        let call = conversation.messages[1].toolCalls[0]
        #expect(call.state == .denied)
        #expect(call.error == "Too risky")
        #expect(call.startedAt == nil)
        #expect(conversation.status == .idle)
    }

    @Test("With two calls waiting, the conversation waits until both are answered")
    func twoApprovals() async {
        let conversation = make(
            MonarchAiScriptedTransport([
                .events([toolCall("t1", state: .pendingApproval), toolCall("t2", state: .pendingApproval), .done()])
            ])
        )
        conversation.send("Do both")
        await conversation.settled()
        conversation.approveTool("t1")
        #expect(conversation.status == .awaitingApproval)
        conversation.denyTool("t2")
        #expect(conversation.status == .idle)
    }

    @Test("A transport that responds to tools continues the same reply after approval")
    func approvalContinues() async {
        let transport = MonarchAiScriptedTransport(
            [
                .events([toolCall(state: .pendingApproval), .done()]),
                .events([
                    .toolUpdate(id: "t1", patch: MonarchAiToolCallPatch(state: .succeeded, output: ["count": 3])),
                    .textDelta("Found three."),
                    .done(),
                ]),
            ],
            respondsToTools: true
        )
        let conversation = make(transport, conversationId: "c9")
        conversation.send("Find the invoices")
        await conversation.settled()
        conversation.approveTool("t1")
        #expect(conversation.status == .streaming)
        await conversation.settled()

        #expect(conversation.status == .idle)
        #expect(conversation.messages.count == 2)
        #expect(conversation.messages[1].toolCalls[0].state == .succeeded)
        #expect(conversation.messages[1].toolCalls[0].output == ["count": 3])
        #expect(conversation.messages[1].text == "Found three.")
        #expect(transport.toolResponses.count == 1)
        #expect(transport.toolResponses[0].isApproved)
        #expect(transport.toolResponses[0].conversationId == "c9")
        #expect(transport.toolResponses[0].messages.map(\.id) == ["m1", "m2"])
        #expect(transport.requests.count == 1)
    }

    @Test("A tool policy approves or denies without asking")
    func policy() async {
        let conversation = make(
            MonarchAiScriptedTransport([
                .events([
                    toolCall("safe", state: .pendingApproval),
                    toolCall("unsafe", state: .pendingApproval),
                    toolCall("unsure", state: .pendingApproval),
                    .done(),
                ])
            ])
        )
        conversation.toolPolicy = { call in
            switch call.id {
            case "safe": .allow
            case "unsafe": .deny
            default: .ask
            }
        }
        conversation.send("Do three things")
        await conversation.settled()
        #expect(conversation.messages[1].toolCalls.map(\.state) == [.running, .denied, .pendingApproval])
        #expect(conversation.status == .awaitingApproval)
    }

    // MARK: Client-side tools

    @Test("A tool the client runs gets its result from onToolCall, and the reply continues")
    func clientTool() async {
        let transport = MonarchAiScriptedTransport([
            .events([toolCall(state: .running), .finish(.toolUse), .done()]),
            .events([.textDelta("It is 21 degrees."), .finish(.stop), .done()]),
        ])
        let conversation = make(transport, body: ["workspace": "acme"])
        var called: [String] = []
        conversation.onToolCall = { call in
            called.append(call.name)
            return ["temperature": 21]
        }
        var finished = 0
        conversation.onFinish = { _ in finished += 1 }
        conversation.send("How warm is it?")
        await conversation.settled()

        #expect(called == ["search"])
        #expect(conversation.status == .idle)
        let reply = conversation.messages[1]
        #expect(reply.toolCalls[0].state == .succeeded)
        #expect(reply.toolCalls[0].output == ["temperature": 21])
        #expect(reply.toolCalls[0].endedAt == Self.noonText)
        #expect(reply.text == "It is 21 degrees.")
        #expect(reply.finishReason == .stop)
        #expect(finished == 1)
        #expect(transport.requests.count == 2)
        #expect(transport.requests[1].trigger == .toolResult)
        #expect(transport.requests[1].messages.map(\.id) == ["m1", "m2"])
        #expect(transport.requests[1].messages[1].toolCalls[0].state == .succeeded)
        #expect(transport.requests[1].body == ["workspace": "acme"])
    }

    @Test("A client-side tool that throws marks the call failed, and the reply still continues")
    func clientToolThrows() async {
        struct Offline: LocalizedError {
            var errorDescription: String? { "The weather service is offline." }
        }
        struct Silent: Error {}
        let transport = MonarchAiScriptedTransport([
            .events([toolCall("t1", state: .running), toolCall("t2", state: .running), .finish(.toolUse), .done()])
        ])
        let conversation = make(transport)
        conversation.onToolCall = { call in
            if call.id == "t1" { throw Offline() }
            throw Silent()
        }
        conversation.send("How warm is it?")
        await conversation.settled()
        let calls = conversation.messages[1].toolCalls
        #expect(calls.map(\.state) == [.failed, .failed])
        #expect(calls[0].error == "The weather service is offline.")
        #expect(calls[1].error == "The tool result could not be sent.")
        #expect(transport.requests.count == 2)
    }

    @Test("Without onToolCall the conversation waits for submitToolResult")
    func manualToolResult() async {
        let transport = MonarchAiScriptedTransport([
            .events([toolCall("t1", state: .running), toolCall("t2", state: .running), .finish(.toolUse), .done()]),
            .events([.textDelta("Done."), .done()]),
        ])
        let conversation = make(transport)
        conversation.send("Do two things")
        await conversation.settled()
        #expect(conversation.status == .awaitingToolResult)
        #expect(conversation.messages[1].toolCalls.map(\.state) == [.awaitingResult, .awaitingResult])

        conversation.submitToolResult("t1", .output("first"))
        #expect(conversation.status == .awaitingToolResult)
        #expect(transport.requests.count == 1)

        conversation.submitToolResult("t2", .failure("No access", output: ["partial": true]))
        await conversation.settled()
        let calls = conversation.messages[1].toolCalls
        #expect(calls[0].state == .succeeded)
        #expect(calls[0].output == "first")
        #expect(calls[1].state == .failed)
        #expect(calls[1].error == "No access")
        #expect(calls[1].output == ["partial": true])
        #expect(conversation.messages[1].text == "Done.")
        #expect(conversation.status == .idle)

        // A result for a call that is already resolved changes nothing.
        conversation.submitToolResult("t1", .failure("late"))
        #expect(conversation.messages[1].toolCalls[0].state == .succeeded)
    }

    @Test("Sending a new message cancels tool calls nobody answered")
    func newMessageDropsAwaitingResults() async {
        let transport = MonarchAiScriptedTransport([
            .events([toolCall(state: .running), .finish(.toolUse), .done()]),
            .events([.textDelta("Never mind."), .done()]),
        ])
        let conversation = make(transport)
        conversation.send("Do it")
        await conversation.settled()
        #expect(conversation.status == .awaitingToolResult)
        conversation.send("Forget it")
        await conversation.settled()
        #expect(conversation.messages[1].toolCalls[0].state == .cancelled)
        #expect(transport.requests[1].messages[1].toolCalls[0].state == .cancelled)
    }

    // MARK: Offline queue

    @Test("Messages written offline wait in order and are sent one after another when the connection is back")
    func offlineQueue() async {
        let transport = MonarchAiScriptedTransport([
            .events([.textDelta("A1"), .done()]),
            .events([.textDelta("A2"), .done()]),
        ])
        let conversation = make(transport)
        conversation.setOnline(false)
        #expect(!conversation.isOnline)
        #expect(conversation.send("Q1", body: ["n": 1]))
        #expect(conversation.send("Q2"))
        #expect(conversation.send("Q3"))

        #expect(conversation.queued.map(\.text) == ["Q1", "Q2", "Q3"])
        #expect(conversation.messages.map(\.text) == ["Q1", "Q2", "Q3"])
        #expect(conversation.messages.allSatisfy { $0.isQueued })
        #expect(conversation.status == .idle)
        #expect(transport.requests.isEmpty)

        let dropped = conversation.queued[2].id
        conversation.cancelQueued(dropped)
        conversation.cancelQueued("missing")
        #expect(conversation.queued.map(\.text) == ["Q1", "Q2"])

        let firstId = conversation.queued[0].id
        conversation.setOnline(true)
        await conversation.settled()
        #expect(await eventually { conversation.queued.isEmpty && conversation.status == .idle })
        await conversation.settled()

        #expect(conversation.messages.map(\.text) == ["Q1", "A1", "Q2", "A2"])
        #expect(conversation.messages.allSatisfy { !$0.isQueued })
        // A queued message keeps its id when it is sent.
        #expect(conversation.messages[0].id == firstId)
        #expect(transport.requests.count == 2)
        #expect(transport.requests[0].body == ["n": 1])
        #expect(transport.requests[1].messages.map(\.text) == ["Q1", "A1", "Q2"])
    }

    @Test("A queued message that fails holds the rest until the connection comes back again")
    func queueHeldOnError() async {
        let transport = MonarchAiScriptedTransport([
            .events([.error(code: "network", message: "Offline again.")]),
            .events([.textDelta("A2"), .done()]),
        ])
        let conversation = make(transport)
        conversation.setOnline(false)
        conversation.send("Q1")
        conversation.send("Q2")
        conversation.setOnline(true)
        await conversation.settled()

        #expect(conversation.status == .error)
        #expect(conversation.queued.map(\.text) == ["Q2"])
        #expect(transport.requests.count == 1)

        conversation.setOnline(false)
        conversation.setOnline(true)
        await conversation.settled()
        #expect(conversation.queued.isEmpty)
        #expect(conversation.messages.last?.text == "A2")
        #expect(conversation.status == .idle)
    }

    @Test("Without the queue, sending offline is tried at once")
    func noQueue() async {
        let transport = MonarchAiScriptedTransport([.events([.textDelta("A"), .done()])])
        let conversation = make(transport, queuesWhileOffline: false)
        conversation.setOnline(false)
        conversation.send("Q")
        #expect(conversation.queued.isEmpty)
        await conversation.settled()
        #expect(transport.requests.count == 1)
    }

    // MARK: Slow replies, resume, feedback, reset

    @Test("A reply that sends nothing for slowAfter is slow until the next event")
    func slow() async {
        let transport = MonarchAiScriptedTransport([.open([])])
        let conversation = make(transport, slowAfter: .zero)
        #expect(!conversation.isSlow)
        conversation.send("Hello")
        #expect(await eventually { conversation.isSlow })
        #expect(conversation.status == .submitted)

        transport.yield(.done())
        await conversation.settled()
        #expect(!conversation.isSlow)

        let quick = make(MonarchAiScriptedTransport([.open([])]), slowAfter: nil)
        quick.send("Hello")
        try? await Task.sleep(for: .milliseconds(20))
        #expect(!quick.isSlow)
        quick.stop()
    }

    @Test("A dropped connection can be resumed when the run has an id and the transport resumes runs")
    func resume() async {
        let transport = MonarchAiScriptedTransport(
            [
                .failing([.run(id: "run_7"), .textDelta("Half")]),
                .events([.run(id: "run_7"), .textDelta("Half and the rest"), .done()]),
            ],
            resumesRuns: true
        )
        let conversation = make(transport, conversationId: "c1")
        conversation.send("Hello")
        await conversation.settled()

        #expect(conversation.error?.code == "network")
        #expect(conversation.error?.isResumable == true)
        #expect(conversation.messages[1].runId == "run_7")
        if case .error(let part)? = conversation.messages[1].parts.last {
            #expect(part.isResumable)
        } else {
            Issue.record("The reply should end with an error part")
        }

        conversation.resumeRun()
        #expect(conversation.status == .streaming)
        await conversation.settled()
        #expect(conversation.status == .idle)
        #expect(conversation.messages.map(\.id) == ["m1", "m2"])
        #expect(conversation.messages[1].text == "Half and the rest")
        #expect(transport.resumes.count == 1)
        #expect(transport.resumes[0].runId == "run_7")
        #expect(transport.resumes[0].conversationId == "c1")
        #expect(transport.resumes[0].messages.map(\.id) == ["m1"])
    }

    @Test("Without a transport that resumes runs, a dropped connection is not resumable")
    func notResumable() async {
        let transport = MonarchAiScriptedTransport([.failing([.run(id: "run_7")])])
        let conversation = make(transport)
        conversation.send("Hello")
        await conversation.settled()
        #expect(conversation.error?.isResumable == false)
        conversation.resumeRun()
        #expect(conversation.status == .error)
        #expect(transport.resumes.isEmpty)
    }

    @Test("Memory events reach the callback with the reply, and feedback is stored on the message")
    func memoryAndFeedback() async {
        let saved = MonarchAiMemoryPart(id: "mem1", action: .write, text: "Prefers metric units", scope: "user")
        let used = MonarchAiMemoryPart(id: "mem2", action: .read, text: "Works at Acme")
        let conversation = make(
            MonarchAiScriptedTransport([.events([.memory(saved), .part(.memory(used)), .textDelta("Noted."), .done()])])
        )
        var seen: [MonarchAiMemoryPart] = []
        conversation.onMemoryEvent = { memory, message in
            seen.append(memory)
            #expect(message.id == "m2")
        }
        conversation.send("Use metric units")
        await conversation.settled()
        #expect(seen == [saved, used])
        #expect(conversation.messages[1].memories == [saved, used])

        conversation.setFeedback("m2", .up)
        #expect(conversation.messages[1].feedback == .up)
        conversation.setFeedback("m2", nil)
        #expect(conversation.messages[1].feedback == nil)
        conversation.setFeedback("missing", .down)
    }

    @Test("Reset replaces the conversation and ends what was running")
    func reset() async {
        let transport = MonarchAiScriptedTransport([.open([.textDelta("Half")])])
        let conversation = make(transport, initialMessages: MonarchAiConversationFixtures.stored())
        #expect(conversation.messages.map(\.id) == ["u1", "a1"])
        conversation.send("More")
        #expect(await eventually { conversation.status == .streaming })
        conversation.setOnline(false)
        conversation.send("Queued")

        conversation.reset()
        #expect(conversation.messages.isEmpty)
        #expect(conversation.queued.isEmpty)
        #expect(conversation.status == .idle)
        #expect(conversation.error == nil)
        #expect(await eventually { transport.terminated == 1 })

        conversation.reset(MonarchAiConversationFixtures.stored())
        #expect(conversation.messages.map(\.id) == ["u1", "a1"])
    }

    @Test("Releasing the conversation cancels the stream it was reading")
    func releaseCancelsStream() async {
        let transport = MonarchAiScriptedTransport([.open([.textDelta("Half")])])
        do {
            let conversation = make(transport)
            conversation.send("Hello")
            #expect(await eventually { conversation.status == .streaming })
        }
        #expect(await eventually { transport.terminated == 1 })
    }

    // MARK: Reducer

    private func draft(_ parts: [MonarchAiPart] = []) -> MonarchAiConversationMessage {
        MonarchAiConversationMessage(id: "a", role: .assistant, parts: parts, createdAt: Self.noonText)
    }

    @Test("Text and reasoning deltas extend the last part and close the other kind")
    func reducerDeltas() {
        let message = MonarchAiStreamReducer.apply(
            [
                .reasoningDelta("Think "),
                .reasoningDelta("first."),
                .textDelta("Then "),
                .textDelta("answer."),
                .reasoningDelta("Think again."),
                .reasoningEnd(durationMs: 800),
            ],
            to: draft()
        )
        #expect(
            message.parts == [
                .reasoning(MonarchAiReasoningPart(text: "Think first.", state: .done)),
                .text(MonarchAiTextPart(text: "Then answer.", state: .done)),
                .reasoning(MonarchAiReasoningPart(text: "Think again.", state: .done, durationMs: 800)),
            ]
        )
    }

    @Test("A redacted reasoning block with nothing streaming is a part of its own")
    func reducerRedacted() {
        let own = MonarchAiStreamReducer.apply(
            [.textDelta("Answer"), .reasoningEnd(durationMs: 300, isRedacted: true)],
            to: draft()
        )
        #expect(own.parts.count == 2)
        #expect(
            own.parts[1]
                == .reasoning(MonarchAiReasoningPart(text: "", state: .done, durationMs: 300, isRedacted: true))
        )

        let streaming = MonarchAiStreamReducer.apply(
            [.reasoningDelta("Hidden"), .reasoningEnd(isRedacted: true)],
            to: draft()
        )
        #expect(streaming.parts == [.reasoning(MonarchAiReasoningPart(text: "Hidden", state: .done, isRedacted: true))])

        // Without a streaming block, a plain end sets the duration of the last reasoning.
        let late = MonarchAiStreamReducer.apply(
            [.reasoningDelta("Think"), .textDelta("Answer"), .reasoningEnd(durationMs: 500)],
            to: draft()
        )
        #expect(late.parts[0] == .reasoning(MonarchAiReasoningPart(text: "Think", state: .done, durationMs: 500)))
    }

    @Test("Tool calls are merged, patched and receive their streamed arguments")
    func reducerTools() {
        let message = MonarchAiStreamReducer.apply(
            [
                .textDelta("Searching"),
                .part(.toolCall(MonarchAiToolCallPart(id: "t1", name: "search", state: .running, risk: .low))),
                .toolInputDelta(id: "t1", delta: "{\"query\":"),
                .toolInputDelta(id: "t1", delta: "\"invoices\"}"),
                .part(
                    .toolCall(
                        MonarchAiToolCallPart(id: "t1", name: "search", input: ["query": "invoices"], state: .running)
                    )
                ),
                .toolUpdate(id: "t1", patch: MonarchAiToolCallPatch(state: .succeeded, output: ["count": 3])),
                .toolUpdate(id: "other", patch: MonarchAiToolCallPatch(state: .failed)),
            ],
            to: draft()
        )
        #expect(message.parts.count == 2)
        #expect(message.parts[0] == .text(MonarchAiTextPart(text: "Searching", state: .done)))
        let call = message.toolCalls[0]
        #expect(call.state == .succeeded)
        #expect(call.input == ["query": "invoices"])
        #expect(call.inputText == "{\"query\":\"invoices\"}")
        #expect(call.output == ["count": 3])
        // The second announcement did not say the risk, so the first one's stays.
        #expect(call.risk == .low)
    }

    @Test("The same memory and action again updates the part instead of adding one")
    func reducerMemory() {
        let message = MonarchAiStreamReducer.apply(
            [
                .memory(MonarchAiMemoryPart(id: "m", action: .write, text: "Likes tea")),
                .memory(MonarchAiMemoryPart(id: "m", action: .write, text: "Likes green tea")),
                .memory(MonarchAiMemoryPart(id: "m", action: .read, text: "Likes green tea")),
            ],
            to: draft()
        )
        #expect(message.memories.map(\.text) == ["Likes green tea", "Likes green tea"])
        #expect(message.memories.map(\.action) == [.write, .read])
    }

    @Test("Closing a reply ends streaming parts and decides what happens to running tool calls")
    func reducerFinish() {
        let parts: [MonarchAiPart] = [
            .text(MonarchAiTextPart(text: "Half", state: .streaming)),
            .toolCall(MonarchAiToolCallPart(id: "t1", name: "search", state: .running)),
            .toolCall(MonarchAiToolCallPart(id: "t2", name: "search", state: .pendingApproval)),
        ]
        let waits = MonarchAiStreamReducer.finishParts(parts, end: .done, finishReason: .toolUse)
        #expect(waits[0] == .text(MonarchAiTextPart(text: "Half", state: .done)))
        #expect(MonarchAiStreamReducer.apply(.finish(.toolUse), to: draft(waits)).finishReason == .toolUse)
        for (end, reason, expected) in [
            (MonarchAiStreamReducer.End.done, MonarchAiFinishReason.toolUse, MonarchAiToolCallState.awaitingResult),
            (.done, .stop, .cancelled),
            (.aborted, .toolUse, .cancelled),
            (.error, .toolUse, .cancelled),
        ] {
            let closed = draft(MonarchAiStreamReducer.finishParts(parts, end: end, finishReason: reason))
            #expect(closed.toolCalls.map(\.state) == [expected, .pendingApproval])
        }
        let done = MonarchAiStreamReducer.apply([.finish(.toolUse), .done()], to: draft(parts))
        #expect(done.toolCalls[0].state == .awaitingResult)
        #expect(MonarchAiStreamReducer.unresolvedToolCalls(done).map(\.id) == ["t1", "t2"])

        let failed = MonarchAiStreamReducer.apply(.error(code: "http-503", message: "Down."), to: draft(parts))
        #expect(failed.toolCalls[0].state == .cancelled)
        #expect(
            failed.parts.last == .error(MonarchAiErrorPart(code: "http-503", message: "Down.", isRetryable: true))
        )
    }

    // MARK: Tree

    @Test("The visible path follows the active child, and only messages with alternatives carry a branch")
    func tree() {
        func message(_ id: String, _ role: MonarchAiRole) -> MonarchAiConversationMessage {
            MonarchAiConversationMessage(id: id, role: role, parts: [], createdAt: Self.noonText)
        }
        var tree = MonarchAiMessageTree([message("u1", .user), message("a1", .assistant)])
        #expect(tree.visibleIds == ["u1", "a1"])
        #expect(tree.lastVisibleId == "a1")
        #expect(tree.visiblePath.allSatisfy { $0.branch == nil })
        #expect(tree.parentId(of: "u1") == MonarchAiMessageTree.root)
        #expect(tree.ancestors(of: "a1") == ["u1"])

        tree.addChild(message("a2", .assistant), under: "u1")
        tree.addChild(message("u2", .user), under: "a2")
        #expect(tree.visibleIds == ["u1", "a2", "u2"])
        #expect(tree.visiblePath.map(\.branch) == [nil, MonarchAiBranchPosition(index: 1, count: 2), nil])
        #expect(tree.ancestors(of: "u2") == ["a2", "u1"])

        let shown = tree.activate(index: 0, ofSiblingsOf: "a2")
        #expect(shown)
        #expect(tree.visibleIds == ["u1", "a1"])
        let outcomes = [
            tree.activate(index: 2, ofSiblingsOf: "a2"),
            tree.activate(index: -1, ofSiblingsOf: "a2"),
            tree.activate(index: 0, ofSiblingsOf: "missing"),
        ]
        #expect(outcomes == [false, false, false])
        #expect(tree.visibleIds == ["u1", "a1"])

        var changed = message("a1", .assistant)
        changed.model = "m"
        tree.replace(changed)
        tree.replace(message("missing", .assistant))
        #expect(tree.visiblePath[1].model == "m")
        #expect(tree.nodes["missing"] == nil)
        #expect(MonarchAiMessageTree().visiblePath.isEmpty)
        #expect(MonarchAiMessageTree().lastVisibleId == MonarchAiMessageTree.root)
    }

    // MARK: The message model and its JSON

    @Test("A message written by the web decodes field for field")
    func decodesWebJSON() throws {
        let json = """
            {
              "id": "a1", "role": "assistant", "createdAt": "2025-03-10T12:00:00.000Z",
              "agent": {"id": "dossier-assistant", "name": "Rook Intelligence", "codename": "RI"},
              "model": "gemini-2.5-pro",
              "usage": {"inputTokens": 120, "outputTokens": 40, "costMicros": 310, "cacheReadTokens": 100,
                        "reasoningTokens": 12},
              "feedback": "up", "branch": {"index": 1, "count": 3}, "finishReason": "tool-use", "runId": "run_7",
              "parts": [
                {"type": "reasoning", "text": "", "state": "done", "durationMs": 900, "redacted": true},
                {"type": "text", "text": "Here is the plan.", "state": "streaming"},
                {"type": "tool-call", "id": "t1", "name": "search_documents", "input": {"query": "invoices", "limit": 5},
                 "state": "pending-approval", "risk": "destructive", "inputText": "{}",
                 "startedAt": "2025-03-10T12:00:01.000Z"},
                {"type": "citation", "id": "c1", "sourceId": "s1", "quote": "Paid on 3 March"},
                {"type": "source", "id": "s1", "title": "Invoices", "url": "https://example.com/i", "kind": "record",
                 "product": "dossier"},
                {"type": "file", "name": "report.pdf", "mediaType": "application/pdf", "size": 2048, "fileId": "f1"},
                {"type": "artifact", "id": "ar1", "kind": "html", "title": "Dashboard", "version": 2},
                {"type": "edit-proposal", "id": "e1", "target": "doc_9", "diff": [{"op": "insert", "at": 4}],
                 "state": "applied"},
                {"type": "plan", "steps": [{"id": "p1", "label": "Read", "state": "done"},
                                           {"id": "p2", "label": "Write", "state": "active"}]},
                {"type": "ui", "component": "WeatherCard", "props": {"city": "Oslo", "celsius": -2.5},
                 "id": "ui1", "state": "done", "toolCallId": "t1"},
                {"type": "memory", "id": "mem1", "action": "forget", "text": "", "scope": "project"},
                {"type": "error", "code": "network", "message": "The connection was lost.", "retryable": true,
                 "retryAfterMs": 5000, "resumable": true}
              ]
            }
            """
        let message = try JSONDecoder().decode(MonarchAiConversationMessage.self, from: Data(json.utf8))
        #expect(message.id == "a1")
        #expect(message.role == .assistant)
        #expect(message.agent == MonarchAiAgentRef(id: "dossier-assistant", name: "Rook Intelligence", codename: "RI"))
        #expect(message.model == "gemini-2.5-pro")
        #expect(
            message.usage
                == MonarchAiUsage(
                    inputTokens: 120,
                    outputTokens: 40,
                    costMicros: 310,
                    cacheReadTokens: 100,
                    reasoningTokens: 12
                )
        )
        #expect(message.feedback == .up)
        #expect(message.branch == MonarchAiBranchPosition(index: 1, count: 3))
        #expect(message.finishReason == .toolUse)
        #expect(message.runId == "run_7")
        #expect(!message.isQueued)
        #expect(message.createdDate == Self.noon)
        #expect(message.text == "Here is the plan.")
        #expect(message.isStreaming)
        #expect(
            message.parts.map(\.type) == [
                "reasoning", "text", "tool-call", "citation", "source", "file", "artifact", "edit-proposal", "plan",
                "ui", "memory", "error",
            ]
        )
        #expect(
            message.parts[0]
                == .reasoning(MonarchAiReasoningPart(text: "", state: .done, durationMs: 900, isRedacted: true))
        )
        #expect(
            message.parts[2]
                == .toolCall(
                    MonarchAiToolCallPart(
                        id: "t1",
                        name: "search_documents",
                        input: ["query": "invoices", "limit": 5],
                        state: .pendingApproval,
                        startedAt: "2025-03-10T12:00:01.000Z",
                        inputText: "{}",
                        risk: .destructive
                    )
                )
        )
        #expect(
            message.parts[3] == .citation(MonarchAiCitationPart(id: "c1", sourceId: "s1", quote: "Paid on 3 March"))
        )
        #expect(
            message.parts[4]
                == .source(
                    MonarchAiSourcePart(
                        id: "s1",
                        title: "Invoices",
                        url: "https://example.com/i",
                        kind: .record,
                        product: "dossier"
                    )
                )
        )
        #expect(
            message.parts[6] == .artifact(MonarchAiArtifactPart(id: "ar1", kind: .html, title: "Dashboard", version: 2))
        )
        #expect(
            message.parts[7]
                == .editProposal(
                    MonarchAiEditProposalPart(
                        id: "e1",
                        target: "doc_9",
                        diff: [["op": "insert", "at": 4]],
                        state: .applied
                    )
                )
        )
        #expect(
            message.parts[8]
                == .plan(
                    MonarchAiPlanPart(steps: [
                        MonarchAiPlanStep(id: "p1", label: "Read", state: .done),
                        MonarchAiPlanStep(id: "p2", label: "Write", state: .active),
                    ])
                )
        )
        #expect(
            message.parts[9]
                == .ui(
                    MonarchAiUiPart(
                        component: "WeatherCard",
                        props: ["city": "Oslo", "celsius": -2.5],
                        id: "ui1",
                        state: .done,
                        toolCallId: "t1"
                    )
                )
        )
        #expect(
            message.parts[10] == .memory(MonarchAiMemoryPart(id: "mem1", action: .forget, text: "", scope: "project"))
        )
        #expect(
            message.parts[11]
                == .error(
                    MonarchAiErrorPart(
                        code: "network",
                        message: "The connection was lost.",
                        isRetryable: true,
                        retryAfterMs: 5000,
                        isResumable: true
                    )
                )
        )

        // What is written back is read back unchanged.
        let again = try JSONDecoder().decode(
            MonarchAiConversationMessage.self,
            from: JSONEncoder().encode(message)
        )
        #expect(again == message)
    }

    @Test("A message is written with the web's field names, and optional fields are left out")
    func encodesWebJSON() throws {
        let message = MonarchAiConversationMessage(
            id: "u1",
            role: .user,
            parts: [
                .text(MonarchAiTextPart(text: "Hello")),
                .toolCall(MonarchAiToolCallPart(id: "t1", name: "search", state: .awaitingResult)),
                .error(MonarchAiErrorPart(code: "auth", message: "Sign in.", isRetryable: false)),
            ],
            createdAt: Self.noonText
        )
        let value = try JSONDecoder().decode(MonarchAiJSON.self, from: JSONEncoder().encode(message))
        guard case .object(let object) = value, case .array(let parts)? = object["parts"] else {
            Issue.record("A message encodes as an object with parts")
            return
        }
        #expect(Set(object.keys) == ["id", "role", "parts", "createdAt"])
        #expect(object["role"] == "user")
        #expect(parts[0] == ["type": "text", "text": "Hello", "state": "done"])
        #expect(
            parts[1] == ["type": "tool-call", "id": "t1", "name": "search", "input": nil, "state": "awaiting-result"]
        )
        #expect(parts[2] == ["type": "error", "code": "auth", "message": "Sign in.", "retryable": false])

        var queued = message
        queued.isQueued = true
        let queuedValue = try JSONDecoder().decode(MonarchAiJSON.self, from: JSONEncoder().encode(queued))
        #expect(queuedValue["queued"] == true)
    }

    @Test("An unknown part type is an error, not a silent loss")
    func unknownPart() {
        let json = #"{"type": "hologram", "text": "?"}"#
        #expect(throws: DecodingError.self) {
            try JSONDecoder().decode(MonarchAiPart.self, from: Data(json.utf8))
        }
    }

    @Test("The enumerations have the web's raw values")
    func rawValues() {
        #expect(MonarchAiRole.allCases.map(\.rawValue) == ["user", "assistant", "system", "tool"])
        #expect(
            MonarchAiToolCallState.allCases.map(\.rawValue) == [
                "pending-approval", "awaiting-result", "running", "succeeded", "failed", "denied", "cancelled",
            ]
        )
        #expect(MonarchAiToolCallRisk.allCases.map(\.rawValue) == ["low", "medium", "high", "destructive"])
        #expect(
            MonarchAiFinishReason.allCases.map(\.rawValue)
                == ["stop", "length", "content-filter", "refusal", "tool-use", "other"]
        )
        #expect(MonarchAiSourceKind.allCases.map(\.rawValue) == ["web", "doc", "record", "file"])
        #expect(
            MonarchAiArtifactKind.allCases.map(\.rawValue) == ["document", "code", "table", "chart", "image", "html"]
        )
        #expect(
            MonarchAiEditProposalState.allCases.map(\.rawValue) == ["proposed", "applied", "rejected", "reverted"]
        )
        #expect(MonarchAiPlanStepState.allCases.map(\.rawValue) == ["todo", "active", "done", "skipped", "failed"])
        #expect(MonarchAiMemoryAction.allCases.map(\.rawValue) == ["read", "write", "forget"])
        #expect(MonarchAiMessageFeedback.allCases.map(\.rawValue) == ["up", "down"])
        #expect(MonarchAiPartState.allCases.map(\.rawValue) == ["streaming", "done"])
        #expect(
            MonarchAiTrigger.allCases.map(\.rawValue) == ["send", "regenerate", "edit", "tool-result", "resume"]
        )
        #expect(
            MonarchAiConversationStatus.allCases.map(\.rawValue) == [
                "idle", "submitted", "streaming", "awaiting-approval", "awaiting-tool-result", "error", "stopped",
            ]
        )
    }

    @Test("JSON values are written with literals and read by key")
    func json() throws {
        let value: MonarchAiJSON = [
            "name": "Ada", "age": 36, "ratio": 0.5, "tags": ["a", "b"], "active": true, "none": nil,
        ]
        #expect(value["name"]?.stringValue == "Ada")
        #expect(value["age"] == .number(36))
        #expect(value["tags"] == .array([.string("a"), .string("b")]))
        #expect(value["active"] == .bool(true))
        #expect(value["none"] == .null)
        #expect(value["missing"] == nil)
        #expect(MonarchAiJSON.string("x")["key"] == nil)
        #expect(MonarchAiJSON.number(1).stringValue == nil)
        let decoded = try JSONDecoder().decode(MonarchAiJSON.self, from: JSONEncoder().encode(value))
        #expect(decoded == value)
    }

    @Test("Times are ISO 8601 in UTC with milliseconds, as the web writes them")
    func timestamps() {
        #expect(MonarchAiConversationMessage.timestamp(Self.noon) == Self.noonText)
        #expect(MonarchAiTimestamp.date(from: "2025-03-10T12:00:00Z") == Self.noon)
        #expect(MonarchAiTimestamp.date(from: "2025-03-10T13:00:00.000+01:00") == Self.noon)
        #expect(MonarchAiTimestamp.date(from: "yesterday") == nil)
    }

    @Test("A queued message is shown as the user message it will become")
    func queuedMessage() {
        let file = MonarchAiFilePart(name: "a.txt", mediaType: "text/plain")
        let queued = MonarchAiQueuedMessage(id: "q1", text: "Hello", files: [file], createdAt: Self.noonText)
        #expect(
            queued.message
                == MonarchAiConversationMessage(
                    id: "q1",
                    role: .user,
                    parts: [.text(MonarchAiTextPart(text: "Hello")), .file(file)],
                    createdAt: Self.noonText,
                    isQueued: true
                )
        )
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAiConversationStrings.connectionLost == "The connection was lost.")
        #expect(MonarchAiConversationStrings.toolResultFailed == "The tool result could not be sent.")
    }

    @Test("A transport that only streams does not respond to tools or resume runs")
    func transportDefaults() async throws {
        struct Minimal: MonarchAiTransport {
            func stream(_ request: MonarchAiTransportRequest) -> MonarchAiEventStream {
                MonarchAiEventStream { $0.finish() }
            }
        }
        let transport = Minimal()
        #expect(!transport.respondsToTools)
        #expect(!transport.resumesRuns)
        var events = 0
        for try await _ in transport.respondToTool(MonarchAiToolResponse(toolCallId: "t", isApproved: true)) {
            events += 1
        }
        for try await _ in transport.resume(MonarchAiResumeRequest(runId: "r")) { events += 1 }
        #expect(events == 0)
    }
}
