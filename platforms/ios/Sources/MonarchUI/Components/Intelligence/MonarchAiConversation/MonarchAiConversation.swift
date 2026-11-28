import Foundation
import Observation
import os

/// Headless state for an AI conversation: it sends prompts through a
/// transport you inject, streams the reply into the message model, and
/// handles stop, regenerate, retry, edit and resend (as branches), tool
/// approval, client-side tool results, an offline queue and feedback.
///
/// ```swift
/// @State private var conversation = MonarchAiConversation(transport: MyTransport())
///
/// var body: some View {
///     ForEach(conversation.messages) { message in
///         Text(message.text)
///     }
///     Button("Ask") { conversation.send("What changed since yesterday?") }
///         .disabled(conversation.isBusy)
/// }
/// ```
///
/// It is an `@Observable` class on the main actor, so a view that reads
/// `messages`, `status` or `error` updates when they change. It knows no
/// model vendor and does no networking: the transport decides where requests
/// go. The message model has the same fields as the web's and reads and
/// writes the same JSON.
///
/// Stopping a reply, sending again or releasing the conversation cancels the
/// task that reads the transport's stream, which terminates the stream.
@MainActor
@Observable
public final class MonarchAiConversation {
    // MARK: State

    /// The visible conversation: the active branch, oldest first, the reply
    /// being written, and then any messages that wait in the offline queue.
    public private(set) var messages: [MonarchAiConversationMessage] = []
    /// What the conversation is doing.
    public private(set) var status: MonarchAiConversationStatus = .idle
    /// Why the last turn failed, while `status` is `error`.
    public private(set) var error: MonarchAiConversationError?
    /// The id of the conversation on the service, from the initialiser or the latest `done` event.
    public private(set) var conversationId: String?
    /// The agent from the latest `agent` event.
    public private(set) var agent: MonarchAiAgentRef?
    /// Whether a running reply has sent nothing for `slowAfter`. It clears with the next event.
    public private(set) var isSlow = false
    /// Whether the device is online, as last told with `setOnline(_:)`.
    public private(set) var isOnline = true
    /// The messages written while offline, oldest first, waiting for the connection.
    public private(set) var queued: [MonarchAiQueuedMessage] = []

    /// Whether a reply is on its way: `submitted` or `streaming`.
    public var isBusy: Bool { status == .submitted || status == .streaming }
    /// `false` while a tool call waits for approval: `send` is refused until it is approved or denied.
    public var canSend: Bool { status != .awaitingApproval }

    // MARK: Configuration

    /// Request fields sent on every turn, merged under the fields given to `send`.
    @ObservationIgnored public var body: [String: MonarchAiJSON]
    /// Called when a reply finishes, not when it is stopped or fails.
    @ObservationIgnored public var onFinish: (@MainActor (MonarchAiConversationMessage) -> Void)?
    /// Called when a turn fails.
    @ObservationIgnored public var onError: (@MainActor (MonarchAiConversationError) -> Void)?
    /// Called when a tool call is approved or denied, for transports that do not respond to tools.
    @ObservationIgnored public var onToolResponse: (@MainActor (MonarchAiToolResponse) -> Void)?
    /// Runs client-side tools: called for every call the model leaves in
    /// `awaitingResult`. Return the output, or throw to mark the call failed.
    /// Once every call has a result the reply continues. Without it, call
    /// `submitToolResult(_:_:)` yourself.
    @ObservationIgnored public var onToolCall: (@MainActor (MonarchAiToolCallPart) async throws -> MonarchAiJSON)?
    /// Approves or denies tool calls before anyone is asked. Return `ask` to wait for the person.
    @ObservationIgnored public var toolPolicy: (@MainActor (MonarchAiToolCallPart) -> MonarchAiToolDecision)?
    /// Called when the reply uses, saves or forgets a memory.
    @ObservationIgnored public var onMemoryEvent:
        (@MainActor (MonarchAiMemoryPart, MonarchAiConversationMessage) -> Void)?

    @ObservationIgnored private let transport: any MonarchAiTransport
    @ObservationIgnored private let queuesWhileOffline: Bool
    @ObservationIgnored private let slowAfter: Duration?
    @ObservationIgnored private let connectionLostMessage: String
    @ObservationIgnored private let toolResultFailedMessage: String
    @ObservationIgnored private let generateId: @MainActor () -> String
    @ObservationIgnored private let now: @MainActor () -> Date
    @ObservationIgnored private let sleep: @Sendable (Duration) async throws -> Void

    // MARK: Internal state

    /// One run: a reply streaming into one message. A run that is no longer
    /// `current` never touches the status, the tree or the screen.
    private final class Run {
        let targetId: String
        var draft: MonarchAiConversationMessage?
        var failure: MonarchAiConversationError?
        var isFinished = false
        var lastEvent = ContinuousClock.now
        var task: Task<Void, Never>?

        init(targetId: String, draft: MonarchAiConversationMessage?) {
            self.targetId = targetId
            self.draft = draft
        }
    }

    @ObservationIgnored private var tree: MonarchAiMessageTree
    @ObservationIgnored private var current: Run?
    @ObservationIgnored private var toolTasks: [UUID: Task<Void, Never>] = [:]
    @ObservationIgnored private var slowTask: Task<Void, Never>?
    /// A queued message failed to send: wait for the person, or a reconnect, before the next.
    @ObservationIgnored private var isFlushHeld = false
    @ObservationIgnored private var isFlushing = false
    /// Every task the conversation started, so they can be cancelled when it is released.
    @ObservationIgnored private let tasks = MonarchAiTaskBag()

    // MARK: Life cycle

    /// A conversation.
    ///
    /// - Parameters:
    ///   - transport: Connects the conversation to your backend.
    ///   - initialMessages: Messages to start with, for example a stored conversation. Shown as one branch.
    ///   - conversationId: The id from an earlier session, sent with every request.
    ///   - body: Request fields sent on every turn.
    ///   - queuesWhileOffline: Whether `send` keeps a message while offline and sends it, in order,
    ///     once the connection is back. With `false`, sending offline is tried at once.
    ///   - slowAfter: The time without any event from a running reply before `isSlow` becomes true.
    ///     `nil` turns it off.
    ///   - connectionLostMessage: Replaces the built-in "The connection was lost.".
    ///   - toolResultFailedMessage: Replaces the built-in "The tool result could not be sent.".
    ///   - generateId: Custom message ids, for example for stable test output.
    public convenience init(
        transport: any MonarchAiTransport,
        initialMessages: [MonarchAiConversationMessage] = [],
        conversationId: String? = nil,
        body: [String: MonarchAiJSON] = [:],
        queuesWhileOffline: Bool = true,
        slowAfter: Duration? = .seconds(10),
        connectionLostMessage: String? = nil,
        toolResultFailedMessage: String? = nil,
        generateId: (@MainActor () -> String)? = nil
    ) {
        self.init(
            transport: transport,
            initialMessages: initialMessages,
            conversationId: conversationId,
            body: body,
            queuesWhileOffline: queuesWhileOffline,
            slowAfter: slowAfter,
            connectionLostMessage: connectionLostMessage,
            toolResultFailedMessage: toolResultFailedMessage,
            generateId: generateId,
            now: { Date() },
            sleep: { try await Task.sleep(for: $0) }
        )
    }

    /// The designated initialiser, with the clock and the timer the tests replace.
    init(
        transport: any MonarchAiTransport,
        initialMessages: [MonarchAiConversationMessage],
        conversationId: String?,
        body: [String: MonarchAiJSON],
        queuesWhileOffline: Bool,
        slowAfter: Duration?,
        connectionLostMessage: String?,
        toolResultFailedMessage: String?,
        generateId: (@MainActor () -> String)?,
        now: @escaping @MainActor () -> Date,
        sleep: @escaping @Sendable (Duration) async throws -> Void
    ) {
        self.transport = transport
        self.tree = MonarchAiMessageTree(initialMessages)
        self.conversationId = conversationId
        self.body = body
        self.queuesWhileOffline = queuesWhileOffline
        self.slowAfter = slowAfter
        self.connectionLostMessage = connectionLostMessage ?? MonarchAiConversationStrings.connectionLost
        self.toolResultFailedMessage = toolResultFailedMessage ?? MonarchAiConversationStrings.toolResultFailed
        self.generateId = generateId ?? { UUID().uuidString }
        self.now = now
        self.sleep = sleep
        self.messages = tree.visiblePath
    }

    deinit {
        tasks.cancelAll()
    }

    // MARK: Sending

    /// Sends a user message and asks for a reply.
    ///
    /// Nothing is sent for empty text without files. While a tool call waits
    /// for approval the message is refused. While offline, or behind messages
    /// that still wait, it is kept in `queued` and sent later, in order.
    ///
    /// - Parameters:
    ///   - text: The prompt.
    ///   - files: Attached files, as `file` parts on the user message. Upload them first.
    ///   - body: Extra request fields for this turn, merged over the conversation's `body`.
    /// - Returns: `false` when the message was refused, so the input should keep its text.
    @discardableResult
    public func send(_ text: String, files: [MonarchAiFilePart] = [], body: [String: MonarchAiJSON] = [:]) -> Bool {
        defer { pump() }
        if text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && files.isEmpty { return true }
        guard status != .awaitingApproval else { return false }
        if queuesWhileOffline && (!isOnline || !queued.isEmpty) {
            queued.append(
                MonarchAiQueuedMessage(id: generateId(), text: text, files: files, body: body, createdAt: timestamp())
            )
            publish()
            return true
        }
        isFlushHeld = false
        submitNow(text, files: files, body: body, id: nil)
        return true
    }

    /// Removes a queued message before it is sent.
    public func cancelQueued(_ id: String) {
        guard queued.contains(where: { $0.id == id }) else { return }
        queued.removeAll { $0.id == id }
        publish()
    }

    /// Tells the conversation whether the device is online, for example from
    /// an `NWPathMonitor`. Coming back online sends the queued messages.
    public func setOnline(_ isOnline: Bool) {
        guard self.isOnline != isOnline else { return }
        self.isOnline = isOnline
        if isOnline { isFlushHeld = false }
        pump()
    }

    /// Aborts the reply in progress. What arrived so far stays.
    public func stop() {
        if supersede() { status = .stopped }
        pump()
    }

    /// Asks again for an assistant message, as a new branch.
    ///
    /// - Parameters:
    ///   - messageId: The assistant message. The default is the last one.
    ///   - body: Extra request fields for this turn.
    public func regenerate(_ messageId: String? = nil, body: [String: MonarchAiJSON] = [:]) {
        supersede()
        guard let target = assistantMessage(messageId) else { return }
        ask(under: tree.parentId(of: target.id), trigger: .regenerate, body: body)
    }

    /// Asks again for a failed assistant message in place, without adding a branch.
    ///
    /// - Parameters:
    ///   - messageId: The assistant message. The default is the last one.
    ///   - body: Extra request fields for this turn.
    public func retry(_ messageId: String? = nil, body: [String: MonarchAiJSON] = [:]) {
        supersede()
        guard var target = assistantMessage(messageId) else { return }
        target.finishReason = nil
        target.usage = nil
        target.runId = nil
        target.branch = nil
        target.parts = []
        target.createdAt = timestamp()
        tree.replace(target)
        let ancestors = Set(tree.ancestors(of: target.id))
        let request = MonarchAiTransportRequest(
            messages: tree.visiblePath.filter { ancestors.contains($0.id) },
            trigger: .regenerate,
            conversationId: conversationId,
            body: self.body.merging(body) { _, new in new }
        )
        let transport = transport
        run(into: target.id, status: .submitted) { transport.stream(request) }
    }

    /// Replaces a user message with new text as a new branch and asks again.
    ///
    /// - Parameters:
    ///   - messageId: The user message.
    ///   - text: The new text.
    ///   - files: The attached files. The default keeps the files of the original message.
    ///   - body: Extra request fields for this turn.
    public func editAndResend(
        _ messageId: String,
        text: String,
        files: [MonarchAiFilePart]? = nil,
        body: [String: MonarchAiJSON] = [:]
    ) {
        guard let original = tree.nodes[messageId], original.role == .user else { return }
        supersede()
        let kept: [MonarchAiFilePart] = original.parts.compactMap { part in
            if case .file(let file) = part { return file }
            return nil
        }
        let user = userMessage(text, files: files ?? kept, id: nil)
        tree.addChild(user, under: tree.parentId(of: messageId))
        ask(under: user.id, trigger: .edit, body: body)
    }

    // MARK: Tools

    /// Approves a tool call that waits for approval.
    ///
    /// - Parameters:
    ///   - toolCallId: The id of the tool call.
    ///   - input: The edited arguments, when the person changed them.
    public func approveTool(_ toolCallId: String, input: MonarchAiJSON? = nil) {
        respond(toolCallId, isApproved: true, input: input, reason: nil)
        pump()
    }

    /// Denies a tool call that waits for approval.
    ///
    /// - Parameters:
    ///   - toolCallId: The id of the tool call.
    ///   - reason: Why, recorded as the call's error.
    public func denyTool(_ toolCallId: String, reason: String? = nil) {
        respond(toolCallId, isApproved: false, input: nil, reason: reason)
        pump()
    }

    /// Sends a client-side tool's result. Once every call of the reply has
    /// one, the reply continues with the trigger `toolResult`.
    public func submitToolResult(_ toolCallId: String, _ result: MonarchAiToolResult) {
        defer { pump() }
        let endedAt = timestamp()
        let owner = patchToolCall(toolCallId) { call in
            guard call.state == .awaitingResult || call.state == .running else { return call }
            var call = call
            switch result {
            case .output(let output):
                call.state = .succeeded
                call.output = output
            case .failure(let message, let output):
                call.state = .failed
                call.error = message
                if let output { call.output = output }
            }
            call.endedAt = endedAt
            return call
        }
        guard let owner, current?.targetId != owner.id else { return }
        let states = owner.toolCalls.map(\.state)
        guard !states.contains(.awaitingResult), !states.contains(.pendingApproval) else { return }
        continueWithResults(owner.id)
    }

    // MARK: Runs, branches, feedback

    /// Reconnects to the run behind a reply, when the transport resumes
    /// runs. The run is replayed into the message from its start.
    ///
    /// - Parameter messageId: The assistant message. The default is the last one with a `runId`.
    public func resumeRun(_ messageId: String? = nil) {
        guard transport.resumesRuns else { return }
        supersede()
        let path = tree.visiblePath
        let found = messageId.flatMap { tree.nodes[$0] } ?? path.last { $0.role == .assistant && $0.runId != nil }
        guard var target = found, let runId = target.runId else { return }
        target.parts = []
        target.branch = nil
        tree.replace(target)
        let index = path.firstIndex { $0.id == target.id } ?? 0
        let request = MonarchAiResumeRequest(
            runId: runId,
            conversationId: conversationId,
            messages: Array(path.prefix(index))
        )
        let transport = transport
        run(into: target.id, status: .streaming) { transport.resume(request) }
    }

    /// Shows another branch of a message.
    ///
    /// - Parameters:
    ///   - messageId: A message that has alternatives.
    ///   - index: The alternative to show, counted from 0.
    public func branchTo(_ messageId: String, index: Int) {
        if tree.activate(index: index, ofSiblingsOf: messageId) { publish() }
    }

    /// Sets the feedback shown on a message. Send it to your feedback service yourself.
    public func setFeedback(_ messageId: String, _ feedback: MonarchAiMessageFeedback?) {
        guard var message = tree.nodes[messageId] else { return }
        message.feedback = feedback
        tree.replace(message)
        if let run = current, run.targetId == messageId { run.draft?.feedback = feedback }
        publish()
    }

    /// Replaces the whole conversation, for example to load another one.
    public func reset(_ messages: [MonarchAiConversationMessage] = []) {
        cancelToolTasks()
        let run = current
        current = nil
        run?.task?.cancel()
        clearSlow()
        queued = []
        isFlushing = false
        isFlushHeld = false
        tree = MonarchAiMessageTree(messages)
        status = .idle
        error = nil
        publish()
    }

    /// Waits until no reply is streaming and no client-side tool is running.
    /// For tests: a reply that waits for a person is settled.
    func settled() async {
        while true {
            if let task = current?.task {
                await task.value
            } else if let task = toolTasks.values.first {
                await task.value
            } else {
                return
            }
        }
    }

    // MARK: Publishing

    private func timestamp() -> String {
        MonarchAiTimestamp.string(from: now())
    }

    /// Recomputes `messages` from the tree, the reply being written and the queue.
    private func publish() {
        var path = tree.visiblePath
        if let run = current, let draft = run.draft, let index = path.firstIndex(where: { $0.id == run.targetId }) {
            var shown = draft
            shown.branch = path[index].branch
            path[index] = shown
        }
        path += queued.map(\.message)
        if path != messages { messages = path }
    }

    /// Sends the next queued message when the connection and the status allow.
    private func pump() {
        if status == .idle || status == .stopped {
            isFlushing = false
            isFlushHeld = false
        } else if status == .error && isFlushing {
            isFlushing = false
            isFlushHeld = true
        }
        guard isOnline, !queued.isEmpty else { return }
        guard status == .idle || status == .stopped || status == .error else { return }
        if status == .error && isFlushHeld { return }
        let next = queued.removeFirst()
        isFlushing = true
        submitNow(next.text, files: next.files, body: next.body, id: next.id)
    }

    // MARK: Runs

    private func assistantMessage(_ messageId: String?) -> MonarchAiConversationMessage? {
        let found = messageId.flatMap { tree.nodes[$0] } ?? tree.visiblePath.last { $0.role == .assistant }
        guard let found, found.role == .assistant else { return nil }
        return tree.nodes[found.id]
    }

    private func userMessage(_ text: String, files: [MonarchAiFilePart], id: String?) -> MonarchAiConversationMessage {
        var parts: [MonarchAiPart] = text.isEmpty ? [] : [.text(MonarchAiTextPart(text: text))]
        parts += files.map(MonarchAiPart.file)
        return MonarchAiConversationMessage(id: id ?? generateId(), role: .user, parts: parts, createdAt: timestamp())
    }

    /// Sends now, whatever the network says. `id` keeps a queued message's id.
    private func submitNow(_ text: String, files: [MonarchAiFilePart], body: [String: MonarchAiJSON], id: String?) {
        supersede()
        dropAwaitingResults()
        let user = userMessage(text, files: files, id: id)
        tree.addChild(user, under: tree.lastVisibleId)
        ask(under: user.id, trigger: .send, body: body)
    }

    /// Adds an empty assistant reply under a parent and streams into it.
    private func ask(under parentId: String, trigger: MonarchAiTrigger, body: [String: MonarchAiJSON]) {
        let reply = MonarchAiConversationMessage(id: generateId(), role: .assistant, parts: [], createdAt: timestamp())
        tree.addChild(reply, under: parentId)
        let request = MonarchAiTransportRequest(
            messages: tree.visiblePath.filter { $0.id != reply.id },
            trigger: trigger,
            conversationId: conversationId,
            body: self.body.merging(body) { _, new in new }
        )
        let transport = transport
        run(into: reply.id, status: .submitted) { transport.stream(request) }
    }

    /// Starts a run into an existing assistant message.
    private func run(
        into targetId: String,
        status nextStatus: MonarchAiConversationStatus,
        open: @escaping @MainActor () -> MonarchAiEventStream
    ) {
        let run = Run(targetId: targetId, draft: tree.nodes[targetId])
        current = run
        error = nil
        status = nextStatus
        publish()
        watchSlow(for: run)
        let key = UUID()
        let tasks = tasks
        let task = Task { @MainActor [weak self] in
            defer { tasks.remove(key) }
            do {
                for try await event in open() {
                    guard let self, self.consume(event, in: run) else { break }
                }
            } catch {
                self?.streamFailed(in: run)
            }
            self?.finish(run)
        }
        run.task = task
        tasks.add(task, for: key)
    }

    /// Applies one event to the run. Returns whether to keep reading.
    private func consume(_ event: MonarchAiStreamEvent, in run: Run) -> Bool {
        guard current === run, !Task.isCancelled else { return false }
        noteEvent(in: run)
        var applied = event
        switch event {
        case .agent(let agent):
            self.agent = agent
        case .done(let id):
            if let id, !id.isEmpty { conversationId = id }
        case .error(let code, let message, let isRetryable, let retryAfterMs, let isResumable):
            let known = code ?? "unknown"
            // A dropped connection can be resumed when the service gave the run an id.
            let resumable = isResumable || ((known == "network" || known == "timeout") && canResume(run))
            run.failure = MonarchAiConversationError(
                code: known,
                message: message,
                isRetryable: isRetryable ?? MonarchAiConversationError.isRetryable(code: known),
                retryAfterMs: retryAfterMs,
                isResumable: resumable
            )
            applied = .error(
                code: code,
                message: message,
                isRetryable: isRetryable,
                retryAfterMs: retryAfterMs,
                isResumable: resumable
            )
        default:
            break
        }
        if let draft = run.draft {
            let next = MonarchAiStreamReducer.apply(applied, to: draft)
            run.draft = next
            if let onMemoryEvent {
                if case .memory(let memory) = event {
                    onMemoryEvent(memory, next)
                } else if case .part(.memory(let memory)) = event {
                    onMemoryEvent(memory, next)
                }
            }
        }
        if status == .submitted { status = .streaming }
        publish()
        // Terminal events end the reply even if the transport keeps the stream open.
        if case .done = event {
            run.isFinished = true
            return false
        }
        if case .error = event { return false }
        return true
    }

    private func canResume(_ run: Run) -> Bool {
        transport.resumesRuns && run.draft?.runId != nil
    }

    /// The stream threw: the reply ends with a network error.
    private func streamFailed(in run: Run) {
        guard current === run, !Task.isCancelled else { return }
        let failure = MonarchAiConversationError(
            code: "network",
            message: connectionLostMessage,
            isRetryable: true,
            isResumable: canResume(run)
        )
        run.failure = failure
        if let draft = run.draft {
            run.draft = MonarchAiStreamReducer.apply(
                .error(
                    code: failure.code,
                    message: failure.message,
                    isRetryable: true,
                    isResumable: failure.isResumable
                ),
                to: draft
            )
        }
    }

    /// The stream ended: close the reply and decide what it waits for.
    private func finish(_ run: Run) {
        // Superseded, stopped or reset: whoever ended the run owns the state now.
        guard current === run else { return }
        clearSlow()
        current = nil
        defer { pump() }
        guard var draft = run.draft else {
            publish()
            return
        }
        draft.parts = MonarchAiStreamReducer.finishParts(
            draft.parts,
            end: run.failure == nil ? .done : .error,
            finishReason: draft.finishReason
        )
        tree.replace(draft)
        publish()
        if let failure = run.failure {
            fail(failure)
            return
        }
        settle(run.targetId, finished: run.isFinished)
    }

    /// Ends the current run, if any, keeping what arrived.
    @discardableResult
    private func supersede() -> Bool {
        cancelToolTasks()
        guard let run = current else { return false }
        current = nil
        run.task?.cancel()
        if var draft = run.draft {
            draft.parts = MonarchAiStreamReducer.finishParts(draft.parts, end: .aborted)
            tree.replace(draft)
        }
        clearSlow()
        publish()
        return true
    }

    private func fail(_ failure: MonarchAiConversationError) {
        error = failure
        status = .error
        onError?(failure)
    }

    // MARK: Slow replies

    /// Notes that the run is alive. A reply that was slow is watched again.
    private func noteEvent(in run: Run) {
        run.lastEvent = ContinuousClock.now
        if isSlow || slowTask == nil {
            isSlow = false
            watchSlow(for: run)
        }
    }

    /// Sets `isSlow` once the run has sent nothing for `slowAfter`. One task
    /// watches a run until then, however many events arrive in between.
    private func watchSlow(for run: Run) {
        slowTask?.cancel()
        slowTask = nil
        guard let slowAfter else { return }
        let sleep = sleep
        slowTask = Task { @MainActor [weak self, weak run] in
            var wait = slowAfter
            while true {
                do { try await sleep(wait) } catch { return }
                guard let self, let run, !Task.isCancelled, self.current === run else { return }
                let idle = ContinuousClock.now - run.lastEvent
                if idle >= slowAfter {
                    self.isSlow = true
                    return
                }
                wait = slowAfter - idle
            }
        }
    }

    private func clearSlow() {
        slowTask?.cancel()
        slowTask = nil
        if isSlow { isSlow = false }
    }

    // MARK: Tool calls

    private func cancelToolTasks() {
        for task in toolTasks.values { task.cancel() }
        toolTasks = [:]
    }

    /// Marks calls nobody will answer as cancelled, so the next request's history is consistent.
    private func dropAwaitingResults() {
        for message in tree.visiblePath where message.toolCalls.contains(where: { $0.state == .awaitingResult }) {
            guard var stored = tree.nodes[message.id] else { continue }
            stored.parts = stored.parts.map { part in
                if case .toolCall(var call) = part, call.state == .awaitingResult {
                    call.state = .cancelled
                    return .toolCall(call)
                }
                return part
            }
            tree.replace(stored)
        }
    }

    /// The message holding a tool call: the reply still streaming, or one on the visible path.
    private func toolOwner(_ toolCallId: String) -> MonarchAiConversationMessage? {
        if let draft = current?.draft, draft.toolCalls.contains(where: { $0.id == toolCallId }) { return draft }
        return tree.visiblePath.first { $0.toolCalls.contains { $0.id == toolCallId } }
    }

    /// Changes one tool call wherever it is. Returns the message that holds it.
    private func patchToolCall(
        _ toolCallId: String,
        _ patch: (MonarchAiToolCallPart) -> MonarchAiToolCallPart
    ) -> MonarchAiConversationMessage? {
        if let run = current, let draft = run.draft, draft.toolCalls.contains(where: { $0.id == toolCallId }) {
            var patched = draft
            patched.parts = MonarchAiStreamReducer.patchTool(draft.parts, id: toolCallId, patch)
            run.draft = patched
            publish()
            return patched
        }
        guard let owner = tree.visiblePath.first(where: { $0.toolCalls.contains { $0.id == toolCallId } }),
            var stored = tree.nodes[owner.id]
        else { return nil }
        stored.parts = MonarchAiStreamReducer.patchTool(stored.parts, id: toolCallId, patch)
        tree.replace(stored)
        publish()
        return stored
    }

    /// Continues a reply after tool results, with the trigger `toolResult`.
    private func continueWithResults(_ ownerId: String) {
        let history = tree.visiblePath
        let cut = history.firstIndex { $0.id == ownerId }
        let request = MonarchAiTransportRequest(
            messages: cut.map { Array(history.prefix($0 + 1)) } ?? history,
            trigger: .toolResult,
            conversationId: conversationId,
            body: body
        )
        let transport = transport
        run(into: ownerId, status: .streaming) { transport.stream(request) }
    }

    /// Records a decision on a tool call that waits for approval and tells
    /// the product. Returns the message that holds the call and the response.
    private func decide(
        _ toolCallId: String,
        isApproved: Bool,
        input: MonarchAiJSON?,
        reason: String?
    ) -> (owner: MonarchAiConversationMessage, response: MonarchAiToolResponse)? {
        guard let before = toolOwner(toolCallId),
            let call = before.toolCalls.first(where: { $0.id == toolCallId }),
            call.state == .pendingApproval
        else { return nil }
        let isClientTool = onToolCall != nil && !transport.respondsToTools
        let startedAt = timestamp()
        let patched = patchToolCall(toolCallId) { call in
            var call = call
            if isApproved {
                call.state = isClientTool ? .awaitingResult : .running
                if let input { call.input = input }
                call.startedAt = startedAt
            } else {
                call.state = .denied
                if let reason, !reason.isEmpty { call.error = reason }
            }
            return call
        }
        guard let owner = patched else { return nil }
        let response = MonarchAiToolResponse(
            toolCallId: toolCallId,
            isApproved: isApproved,
            input: input,
            reason: reason,
            messages: tree.visiblePath,
            conversationId: conversationId
        )
        onToolResponse?(response)
        return (owner, response)
    }

    /// Once no call of the reply waits for approval: lets the transport
    /// continue the reply, or settles it.
    private func proceed(after response: MonarchAiToolResponse, owner: MonarchAiConversationMessage) {
        // Still streaming: the run settles the reply when it ends.
        if current?.targetId == owner.id { return }
        if owner.toolCalls.contains(where: { $0.state == .pendingApproval }) { return }
        guard transport.respondsToTools else {
            settle(owner.id, finished: false)
            return
        }
        let transport = transport
        run(into: owner.id, status: .streaming) { transport.respondToTool(response) }
    }

    private func respond(_ toolCallId: String, isApproved: Bool, input: MonarchAiJSON?, reason: String?) {
        guard let decision = decide(toolCallId, isApproved: isApproved, input: input, reason: reason) else { return }
        proceed(after: decision.response, owner: decision.owner)
    }

    /// After a reply ends: wait for approvals, run client tools, or finish.
    private func settle(_ messageId: String, finished: Bool) {
        guard let message = tree.nodes[messageId] else { return }
        if let toolPolicy {
            // Every waiting call is put to the policy before anyone is asked.
            var last: MonarchAiToolResponse?
            for call in message.toolCalls where call.state == .pendingApproval {
                let decision = toolPolicy(call)
                guard decision != .ask else { continue }
                last = decide(call.id, isApproved: decision == .allow, input: nil, reason: nil)?.response ?? last
            }
            if let last, let owner = tree.nodes[messageId] {
                if owner.toolCalls.contains(where: { $0.state == .pendingApproval }) {
                    status = .awaitingApproval
                } else {
                    proceed(after: last, owner: owner)
                }
                return
            }
        }
        let calls = message.toolCalls
        if calls.contains(where: { $0.state == .pendingApproval }) {
            status = .awaitingApproval
            return
        }
        let awaiting = calls.filter { $0.state == .awaitingResult }
        if !awaiting.isEmpty {
            status = .awaitingToolResult
            guard let onToolCall else { return }
            for call in awaiting { runTool(call, with: onToolCall) }
            return
        }
        status = .idle
        if finished { onFinish?(message) }
    }

    private func runTool(
        _ call: MonarchAiToolCallPart,
        with tool: @escaping @MainActor (MonarchAiToolCallPart) async throws -> MonarchAiJSON
    ) {
        let key = UUID()
        let fallback = toolResultFailedMessage
        let tasks = tasks
        let task = Task { @MainActor [weak self] in
            defer { tasks.remove(key) }
            let result: MonarchAiToolResult
            do {
                result = .output(try await tool(call))
            } catch {
                result = .failure(MonarchFailureMessage.message(for: error, fallback: fallback))
            }
            guard let self, !Task.isCancelled, self.toolTasks[key] != nil else { return }
            self.toolTasks[key] = nil
            self.submitToolResult(call.id, result)
        }
        toolTasks[key] = task
        tasks.add(task, for: key)
    }
}

/// The tasks a conversation started and that have not ended. It is
/// `Sendable`, so the conversation can cancel them from its `deinit`, which
/// is not on the main actor.
private final class MonarchAiTaskBag: Sendable {
    private let tasks = OSAllocatedUnfairLock(initialState: [UUID: Task<Void, Never>]())

    func add(_ task: Task<Void, Never>, for key: UUID) {
        tasks.withLock { $0[key] = task }
    }

    func remove(_ key: UUID) {
        tasks.withLock { $0[key] = nil }
    }

    func cancelAll() {
        let all = tasks.withLock { tasks in
            defer { tasks = [:] }
            return Array(tasks.values)
        }
        for task in all { task.cancel() }
    }
}
