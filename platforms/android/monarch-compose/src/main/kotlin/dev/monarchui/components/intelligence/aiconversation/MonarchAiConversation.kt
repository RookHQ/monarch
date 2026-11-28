package dev.monarchui.components.intelligence.aiconversation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Headless state for an AI conversation: it sends prompts through a
 * transport you inject, streams the reply into the message model, and
 * handles stop, regenerate, retry, edit and resend (as branches), tool
 * approval, client-side tool results, an offline queue and feedback.
 *
 * ```kotlin
 * val conversation = rememberMonarchAiConversation(MyTransport())
 * val messages by conversation.messages.collectAsState()
 * messages.forEach { MonarchText(text = it.text) }
 * MonarchButton(text = "Ask", onClick = { conversation.send("What changed since yesterday?") })
 * ```
 *
 * It draws nothing: the state is exposed as `StateFlow`s ([messages],
 * [status], [error] and the others) for any UI to collect. It knows no model
 * vendor and does no networking: the transport decides where requests go.
 * The message model has the same fields as the web's and reads and writes
 * the same JSON.
 *
 * Every reply is collected in [scope]. Use a scope on the main thread, for
 * example `viewModelScope` or `rememberCoroutineScope()`, and call the
 * functions from the main thread. Stopping a reply, sending again or ending
 * the scope cancels the collection of the transport's flow.
 *
 * @param transport connects the conversation to your backend.
 * @param scope the scope that collects replies and runs client-side tools.
 * @param connectionLostMessage the message of the error raised when the transport's flow throws. The
 * built-in text is the string resource `monarch_aiconversation_connection_lost`;
 * `rememberMonarchAiConversation` passes it for you.
 * @param toolResultFailedMessage the error of a tool call whose client-side tool threw without a message.
 * The built-in text is `monarch_aiconversation_tool_result_failed`.
 * @param initialMessages messages to start with, for example a stored conversation. Shown as one branch.
 * @param conversationId the id from an earlier session, sent with every request. A `Done` event replaces it.
 * @param body request fields sent on every turn, merged under the fields given to [send].
 * @param queuesWhileOffline whether [send] keeps a message while offline and sends it, in order, once
 * [setOnline] is called with `true`. With `false`, sending offline is tried at once.
 * @param slowAfterMillis the time without any event from a running reply before [slow] becomes `true`.
 * `null` turns it off.
 * @param generateId custom message ids, for example for stable test output. The default is a random UUID.
 */
@Suppress("TooManyFunctions")
public class MonarchAiConversation(
    private val transport: MonarchAiTransport,
    private val scope: CoroutineScope,
    private val connectionLostMessage: String,
    private val toolResultFailedMessage: String,
    initialMessages: List<MonarchAiConversationMessage> = emptyList(),
    conversationId: String? = null,
    body: Map<String, MonarchAiJSON> = emptyMap(),
    private val queuesWhileOffline: Boolean = true,
    private val slowAfterMillis: Long? = DEFAULT_SLOW_AFTER_MILLIS,
    generateId: (() -> String)? = null,
) {
    private val generateId: () -> String = generateId ?: { UUID.randomUUID().toString() }
    private var tree = MonarchAiMessageTree(initialMessages)

    private val messageState = MutableStateFlow(tree.visiblePath)
    private val statusState = MutableStateFlow(MonarchAiConversationStatus.Idle)
    private val errorState = MutableStateFlow<MonarchAiConversationError?>(null)
    private val conversationIdState = MutableStateFlow(conversationId)
    private val agentState = MutableStateFlow<MonarchAiAgentRef?>(null)
    private val slowState = MutableStateFlow(false)
    private val onlineState = MutableStateFlow(true)
    private val queuedState = MutableStateFlow<List<MonarchAiQueuedMessage>>(emptyList())

    /**
     * The visible conversation: the active branch, oldest first, the reply
     * being written, and then any messages that wait in the offline queue.
     */
    public val messages: StateFlow<List<MonarchAiConversationMessage>> = messageState.asStateFlow()

    /** What the conversation is doing. */
    public val status: StateFlow<MonarchAiConversationStatus> = statusState.asStateFlow()

    /** Why the last turn failed, while [status] is `Error`. */
    public val error: StateFlow<MonarchAiConversationError?> = errorState.asStateFlow()

    /** The id of the conversation on the service, from the constructor or the latest `Done` event. */
    public val conversationId: StateFlow<String?> = conversationIdState.asStateFlow()

    /** The agent from the latest `Agent` event. */
    public val agent: StateFlow<MonarchAiAgentRef?> = agentState.asStateFlow()

    /** Whether a running reply has sent nothing for `slowAfterMillis`. It clears with the next event. */
    public val slow: StateFlow<Boolean> = slowState.asStateFlow()

    /** Whether the device is online, as last told with [setOnline]. */
    public val online: StateFlow<Boolean> = onlineState.asStateFlow()

    /** The messages written while offline, oldest first, waiting for the connection. */
    public val queued: StateFlow<List<MonarchAiQueuedMessage>> = queuedState.asStateFlow()

    /** Whether a reply is on its way: the status is `Submitted` or `Streaming`. Reads the current status. */
    public val busy: Boolean
        get() = statusState.value == MonarchAiConversationStatus.Submitted ||
            statusState.value == MonarchAiConversationStatus.Streaming

    /** `false` while a tool call waits for approval: [send] is refused until it is approved or denied. */
    public val canSend: Boolean
        get() = statusState.value != MonarchAiConversationStatus.AwaitingApproval

    /** Request fields sent on every turn, merged under the fields given to [send]. */
    public var body: Map<String, MonarchAiJSON> = body

    /** Called when a reply finishes, not when it is stopped or fails. */
    public var onFinish: ((MonarchAiConversationMessage) -> Unit)? = null

    /** Called when a turn fails. */
    public var onError: ((MonarchAiConversationError) -> Unit)? = null

    /** Called when a tool call is approved or denied, for transports that do not respond to tools. */
    public var onToolResponse: ((MonarchAiToolResponse) -> Unit)? = null

    /**
     * Runs client-side tools: called for every call the model leaves in
     * `AwaitingResult`. Return the output, or throw to mark the call failed.
     * Once every call has a result the reply continues. Without it, call
     * [submitToolResult] yourself.
     */
    public var onToolCall: (suspend (MonarchAiToolCallPart) -> MonarchAiJSON)? = null

    /** Approves or denies tool calls before anyone is asked. Return `Ask` to wait for the person. */
    public var toolPolicy: ((MonarchAiToolCallPart) -> MonarchAiToolDecision)? = null

    /** Called when the reply uses, saves or forgets a memory, with the reply as it is then. */
    public var onMemoryEvent: ((MonarchAiMemoryPart, MonarchAiConversationMessage) -> Unit)? = null

    /** One run: a reply streaming into one message. A run that is no longer current never touches the state. */
    private class Run(val targetId: String, var draft: MonarchAiConversationMessage?) {
        var failure: MonarchAiConversationError? = null
        var finished = false
        var job: Job? = null
    }

    private var current: Run? = null
    private val toolJobs = mutableMapOf<Int, Job>()
    private var nextToolKey = 0
    private var slowJob: Job? = null

    /** A queued message failed to send: wait for the person, or a reconnect, before the next. */
    private var flushHeld = false
    private var flushing = false

    // Sending

    /**
     * Sends a user message and asks for a reply.
     *
     * Nothing is sent for blank text without files. While a tool call waits
     * for approval the message is refused. While offline, or behind messages
     * that still wait, it is kept in [queued] and sent later, in order.
     *
     * @param text the prompt.
     * @param files attached files, as file parts on the user message. Upload them first.
     * @param body extra request fields for this turn, merged over the conversation's [body].
     * @return `false` when the message was refused, so the input should keep its text.
     */
    public fun send(
        text: String,
        files: List<MonarchAiFilePart> = emptyList(),
        body: Map<String, MonarchAiJSON> = emptyMap(),
    ): Boolean {
        val accepted = when {
            text.isBlank() && files.isEmpty() -> true
            statusState.value == MonarchAiConversationStatus.AwaitingApproval -> false
            queuesWhileOffline && (!onlineState.value || queuedState.value.isNotEmpty()) -> {
                queuedState.value += MonarchAiQueuedMessage(generateId(), text, timestamp(), files, body)
                publish()
                true
            }
            else -> {
                flushHeld = false
                submitNow(text, files, body, id = null)
                true
            }
        }
        pump()
        return accepted
    }

    /** Removes a queued message before it is sent. */
    public fun cancelQueued(id: String) {
        if (queuedState.value.none { it.id == id }) return
        queuedState.value = queuedState.value.filterNot { it.id == id }
        publish()
    }

    /**
     * Tells the conversation whether the device is online, for example from a
     * `ConnectivityManager` callback. Coming back online sends the queued messages.
     */
    public fun setOnline(online: Boolean) {
        if (onlineState.value == online) return
        onlineState.value = online
        if (online) flushHeld = false
        pump()
    }

    /** Ends the reply in progress. What arrived so far stays. */
    public fun stop() {
        if (supersede()) statusState.value = MonarchAiConversationStatus.Stopped
        pump()
    }

    /**
     * Asks again for an assistant message, as a new branch.
     *
     * @param messageId the assistant message. `null` is the last one.
     * @param body extra request fields for this turn.
     */
    public fun regenerate(messageId: String? = null, body: Map<String, MonarchAiJSON> = emptyMap()) {
        supersede()
        val target = assistantMessage(messageId) ?: return
        ask(under = tree.parentId(target.id), trigger = MonarchAiTrigger.Regenerate, body = body)
    }

    /**
     * Asks again for a failed assistant message in place, without adding a branch.
     *
     * @param messageId the assistant message. `null` is the last one.
     * @param body extra request fields for this turn.
     */
    public fun retry(messageId: String? = null, body: Map<String, MonarchAiJSON> = emptyMap()) {
        supersede()
        val found = assistantMessage(messageId) ?: return
        val target = found.copy(
            finishReason = null,
            usage = null,
            runId = null,
            branch = null,
            parts = emptyList(),
            createdAt = timestamp(),
        )
        tree.replace(target)
        val ancestors = tree.ancestors(target.id).toSet()
        val request = MonarchAiTransportRequest(
            messages = tree.visiblePath.filter { it.id in ancestors },
            trigger = MonarchAiTrigger.Regenerate,
            conversationId = conversationIdState.value,
            body = this.body + body,
        )
        run(target.id, MonarchAiConversationStatus.Submitted) { transport.stream(request) }
    }

    /**
     * Replaces a user message with new text as a new branch and asks again.
     *
     * @param messageId the user message.
     * @param text the new text.
     * @param files the attached files. `null` keeps the files of the original message.
     * @param body extra request fields for this turn.
     */
    public fun editAndResend(
        messageId: String,
        text: String,
        files: List<MonarchAiFilePart>? = null,
        body: Map<String, MonarchAiJSON> = emptyMap(),
    ) {
        val original = tree[messageId]?.takeIf { it.role == MonarchAiRole.User } ?: return
        supersede()
        val kept = original.parts.filterIsInstance<MonarchAiFilePart>()
        val user = userMessage(text, files ?: kept, id = null)
        tree.addChild(user, tree.parentId(messageId))
        ask(under = user.id, trigger = MonarchAiTrigger.Edit, body = body)
    }

    // Tools

    /**
     * Approves a tool call that waits for approval.
     *
     * @param toolCallId the id of the tool call.
     * @param input the edited arguments, when the person changed them.
     */
    public fun approveTool(toolCallId: String, input: MonarchAiJSON? = null) {
        respond(toolCallId, approved = true, input = input, reason = null)
        pump()
    }

    /**
     * Denies a tool call that waits for approval.
     *
     * @param toolCallId the id of the tool call.
     * @param reason why, recorded as the call's error.
     */
    public fun denyTool(toolCallId: String, reason: String? = null) {
        respond(toolCallId, approved = false, input = null, reason = reason)
        pump()
    }

    /**
     * Sends a client-side tool's result. Once every call of the reply has
     * one, the reply continues with the trigger `ToolResult`.
     */
    public fun submitToolResult(toolCallId: String, result: MonarchAiToolResult) {
        val endedAt = timestamp()
        val owner = patchToolCall(toolCallId) { call ->
            if (call.state != MonarchAiToolCallState.AwaitingResult && call.state != MonarchAiToolCallState.Running) {
                call
            } else {
                when (result) {
                    is MonarchAiToolResult.Output ->
                        call.copy(state = MonarchAiToolCallState.Succeeded, output = result.output, endedAt = endedAt)
                    is MonarchAiToolResult.Failure -> call.copy(
                        state = MonarchAiToolCallState.Failed,
                        error = result.message,
                        output = result.output ?: call.output,
                        endedAt = endedAt,
                    )
                }
            }
        }
        val resolved = owner != null && MonarchAiStreamReducer.unresolvedToolCalls(owner).isEmpty()
        if (owner != null && current?.targetId != owner.id && resolved) continueWithResults(owner.id)
        pump()
    }

    // Runs, branches, feedback

    /**
     * Reconnects to the run behind a reply, when the transport resumes runs.
     * The run is replayed into the message from its start.
     *
     * @param messageId the assistant message. `null` is the last one with a `runId`.
     */
    public fun resumeRun(messageId: String? = null) {
        if (!transport.resumesRuns) return
        supersede()
        val path = tree.visiblePath
        val found = messageId?.let { tree[it] }
            ?: path.lastOrNull { it.role == MonarchAiRole.Assistant && it.runId != null }
        val runId = found?.runId ?: return
        val target = found.copy(parts = emptyList(), branch = null)
        tree.replace(target)
        val index = path.indexOfFirst { it.id == target.id }.coerceAtLeast(0)
        val request = MonarchAiResumeRequest(
            runId = runId,
            conversationId = conversationIdState.value,
            messages = path.take(index),
        )
        run(target.id, MonarchAiConversationStatus.Streaming) { transport.resume(request) }
    }

    /**
     * Shows another branch of a message.
     *
     * @param messageId a message that has alternatives.
     * @param index the alternative to show, counted from 0.
     */
    public fun branchTo(messageId: String, index: Int) {
        if (tree.activate(index, siblingsOf = messageId)) publish()
    }

    /** Sets the feedback shown on a message. Send it to your feedback service yourself. */
    public fun setFeedback(messageId: String, feedback: MonarchAiMessageFeedback?) {
        val message = tree[messageId] ?: return
        tree.replace(message.copy(feedback = feedback))
        current?.takeIf { it.targetId == messageId }?.let { run -> run.draft = run.draft?.copy(feedback = feedback) }
        publish()
    }

    /** Replaces the whole conversation, for example to load another one. */
    public fun reset(messages: List<MonarchAiConversationMessage> = emptyList()) {
        cancelToolJobs()
        val run = current
        current = null
        run?.job?.cancel()
        clearSlow()
        queuedState.value = emptyList()
        flushing = false
        flushHeld = false
        tree = MonarchAiMessageTree(messages)
        statusState.value = MonarchAiConversationStatus.Idle
        errorState.value = null
        publish()
    }

    // Publishing

    private fun timestamp(): String = MonarchAiTimestamp.text(System.currentTimeMillis())

    /** Recomputes [messages] from the tree, the reply being written and the queue. */
    private fun publish() {
        val path = tree.visiblePath.toMutableList()
        val run = current
        val draft = run?.draft
        if (run != null && draft != null) {
            val index = path.indexOfFirst { it.id == run.targetId }
            if (index >= 0) path[index] = draft.copy(branch = path[index].branch)
        }
        val shown = path + queuedState.value.map { it.message }
        if (shown != messageState.value) messageState.value = shown
    }

    /** Sends the next queued message when the connection and the status allow. */
    private fun pump() {
        val status = statusState.value
        val settled = status == MonarchAiConversationStatus.Idle || status == MonarchAiConversationStatus.Stopped
        if (settled) {
            flushing = false
            flushHeld = false
        } else if (status == MonarchAiConversationStatus.Error && flushing) {
            flushing = false
            flushHeld = true
        }
        val canFlush = settled || (status == MonarchAiConversationStatus.Error && !flushHeld)
        if (!onlineState.value || queuedState.value.isEmpty() || !canFlush) return
        val next = queuedState.value.first()
        queuedState.value = queuedState.value.drop(1)
        flushing = true
        submitNow(next.text, next.files, next.body, next.id)
    }

    // Runs

    private fun assistantMessage(messageId: String?): MonarchAiConversationMessage? {
        val found = messageId?.let { tree[it] } ?: tree.visiblePath.lastOrNull { it.role == MonarchAiRole.Assistant }
        return found?.takeIf { it.role == MonarchAiRole.Assistant }?.let { tree[it.id] }
    }

    private fun userMessage(text: String, files: List<MonarchAiFilePart>, id: String?): MonarchAiConversationMessage =
        MonarchAiConversationMessage(
            id = id ?: generateId(),
            role = MonarchAiRole.User,
            parts = (if (text.isEmpty()) emptyList() else listOf(MonarchAiTextPart(text))) + files,
            createdAt = timestamp(),
        )

    /** Sends now, whatever the network says. [id] keeps a queued message's id. */
    private fun submitNow(text: String, files: List<MonarchAiFilePart>, body: Map<String, MonarchAiJSON>, id: String?) {
        supersede()
        dropAwaitingResults()
        val user = userMessage(text, files, id)
        tree.addChild(user, tree.lastVisibleId)
        ask(under = user.id, trigger = MonarchAiTrigger.Send, body = body)
    }

    /** Adds an empty assistant reply under a parent and streams into it. */
    private fun ask(under: String, trigger: MonarchAiTrigger, body: Map<String, MonarchAiJSON>) {
        val reply = MonarchAiConversationMessage(generateId(), MonarchAiRole.Assistant, emptyList(), timestamp())
        tree.addChild(reply, under)
        val request = MonarchAiTransportRequest(
            messages = tree.visiblePath.filter { it.id != reply.id },
            trigger = trigger,
            conversationId = conversationIdState.value,
            body = this.body + body,
        )
        run(reply.id, MonarchAiConversationStatus.Submitted) { transport.stream(request) }
    }

    /** Starts a run into an existing assistant message. */
    private fun run(targetId: String, nextStatus: MonarchAiConversationStatus, open: () -> Flow<MonarchAiStreamEvent>) {
        val run = Run(targetId, tree[targetId])
        current = run
        errorState.value = null
        statusState.value = nextStatus
        publish()
        watchSlow(run)
        run.job = scope.launch {
            open()
                .takeWhile { event -> consume(event, run) }
                .catch { cause -> if (cause !is CancellationException) streamFailed(run) }
                .collect()
            finish(run)
        }
    }

    /** Applies one event to the run. Returns whether to keep reading. */
    private fun consume(event: MonarchAiStreamEvent, run: Run): Boolean {
        if (current !== run) return false
        noteEvent(run)
        var applied = event
        when (event) {
            is MonarchAiStreamEvent.Agent -> agentState.value = event.agent
            is MonarchAiStreamEvent.Done -> event.conversationId?.takeIf { it.isNotEmpty() }?.let {
                conversationIdState.value = it
            }
            is MonarchAiStreamEvent.Error -> applied = recordFailure(event, run)
            else -> Unit
        }
        run.draft?.let { draft ->
            val next = MonarchAiStreamReducer.apply(applied, draft)
            run.draft = next
            val memory = (event as? MonarchAiStreamEvent.Memory)?.memory
                ?: ((event as? MonarchAiStreamEvent.Part)?.part as? MonarchAiMemoryPart)
            if (memory != null) onMemoryEvent?.invoke(memory, next)
        }
        if (statusState.value == MonarchAiConversationStatus.Submitted) {
            statusState.value = MonarchAiConversationStatus.Streaming
        }
        publish()
        // Terminal events end the reply even if the transport keeps the flow open.
        if (event is MonarchAiStreamEvent.Done) run.finished = true
        return event !is MonarchAiStreamEvent.Done && event !is MonarchAiStreamEvent.Error
    }

    /** Records an error event as the run's failure, and returns the event as it is applied to the reply. */
    private fun recordFailure(event: MonarchAiStreamEvent.Error, run: Run): MonarchAiStreamEvent.Error {
        val known = event.code ?: "unknown"
        // A dropped connection can be resumed when the service gave the run an id.
        val resumable = event.resumable || ((known == "network" || known == "timeout") && canResume(run))
        run.failure = MonarchAiConversationError(
            code = known,
            message = event.message,
            retryable = event.retryable ?: MonarchAiConversationError.isRetryable(known),
            retryAfterMs = event.retryAfterMs,
            resumable = resumable,
        )
        return event.copy(resumable = resumable)
    }

    private fun canResume(run: Run): Boolean = transport.resumesRuns && run.draft?.runId != null

    /** The flow threw: the reply ends with a network error. */
    private fun streamFailed(run: Run) {
        if (current !== run) return
        val failure = MonarchAiConversationError(
            code = "network",
            message = connectionLostMessage,
            retryable = true,
            resumable = canResume(run),
        )
        run.failure = failure
        run.draft = run.draft?.let { draft ->
            MonarchAiStreamReducer.apply(
                MonarchAiStreamEvent.Error(failure.message, failure.code, true, resumable = failure.resumable),
                draft,
            )
        }
    }

    /** The flow ended: close the reply and decide what it waits for. */
    private fun finish(run: Run) {
        // Superseded, stopped or reset: whoever ended the run owns the state now.
        if (current !== run) return
        clearSlow()
        current = null
        val draft = run.draft
        if (draft != null) {
            val end = if (run.failure == null) MonarchAiReplyEnd.Done else MonarchAiReplyEnd.Error
            tree.replace(draft.copy(parts = MonarchAiStreamReducer.finishParts(draft.parts, end, draft.finishReason)))
        }
        publish()
        val failure = run.failure
        if (failure != null) {
            fail(failure)
        } else if (draft != null) {
            settle(run.targetId, run.finished)
        }
        pump()
    }

    /** Ends the current run, if any, keeping what arrived. */
    private fun supersede(): Boolean {
        cancelToolJobs()
        val run = current ?: return false
        current = null
        run.job?.cancel()
        run.draft?.let { draft ->
            tree.replace(draft.copy(parts = MonarchAiStreamReducer.finishParts(draft.parts, MonarchAiReplyEnd.Aborted)))
        }
        clearSlow()
        publish()
        return true
    }

    private fun fail(failure: MonarchAiConversationError) {
        errorState.value = failure
        statusState.value = MonarchAiConversationStatus.Error
        onError?.invoke(failure)
    }

    // Slow replies

    /** Notes that the run is alive and watches it again. */
    private fun noteEvent(run: Run) {
        slowState.value = false
        watchSlow(run)
    }

    /** Sets [slow] once the run has sent nothing for `slowAfterMillis`. */
    private fun watchSlow(run: Run) {
        slowJob?.cancel()
        slowJob = null
        val wait = slowAfterMillis ?: return
        slowJob = scope.launch {
            delay(wait)
            if (current === run) slowState.value = true
        }
    }

    private fun clearSlow() {
        slowJob?.cancel()
        slowJob = null
        slowState.value = false
    }

    // Tool calls

    private fun cancelToolJobs() {
        toolJobs.values.forEach { it.cancel() }
        toolJobs.clear()
    }

    /** Marks calls nobody will answer as cancelled, so the next request's history is consistent. */
    private fun dropAwaitingResults() {
        tree.visiblePath
            .filter { message -> message.toolCalls.any { it.state == MonarchAiToolCallState.AwaitingResult } }
            .forEach { message ->
                val stored = tree[message.id] ?: return@forEach
                tree.replace(
                    stored.copy(
                        parts = stored.parts.map {
                            if (it is MonarchAiToolCallPart && it.state == MonarchAiToolCallState.AwaitingResult) {
                                it.copy(state = MonarchAiToolCallState.Cancelled)
                            } else {
                                it
                            }
                        },
                    ),
                )
            }
    }

    /** The message holding a tool call: the reply still streaming, or one on the visible path. */
    private fun toolOwner(toolCallId: String): MonarchAiConversationMessage? {
        current?.draft?.takeIf { draft -> draft.toolCalls.any { it.id == toolCallId } }?.let { return it }
        return tree.visiblePath.firstOrNull { message -> message.toolCalls.any { it.id == toolCallId } }
    }

    /** Changes one tool call wherever it is. Returns the message that holds it. */
    private fun patchToolCall(
        toolCallId: String,
        patch: (MonarchAiToolCallPart) -> MonarchAiToolCallPart,
    ): MonarchAiConversationMessage? {
        val run = current
        val draft = run?.draft
        if (run != null && draft != null && draft.toolCalls.any { it.id == toolCallId }) {
            val patched = draft.copy(parts = MonarchAiStreamReducer.patchTool(draft.parts, toolCallId, patch))
            run.draft = patched
            publish()
            return patched
        }
        val owner = tree.visiblePath.firstOrNull { message -> message.toolCalls.any { it.id == toolCallId } }
        val stored = owner?.let { tree[it.id] } ?: return null
        val patched = stored.copy(parts = MonarchAiStreamReducer.patchTool(stored.parts, toolCallId, patch))
        tree.replace(patched)
        publish()
        return patched
    }

    /** Continues a reply after tool results, with the trigger `ToolResult`. */
    private fun continueWithResults(ownerId: String) {
        val history = tree.visiblePath
        val cut = history.indexOfFirst { it.id == ownerId }
        val request = MonarchAiTransportRequest(
            messages = if (cut >= 0) history.take(cut + 1) else history,
            trigger = MonarchAiTrigger.ToolResult,
            conversationId = conversationIdState.value,
            body = body,
        )
        run(ownerId, MonarchAiConversationStatus.Streaming) { transport.stream(request) }
    }

    /**
     * Records a decision on a tool call that waits for approval and tells
     * the app. Returns the message that holds the call and the response.
     */
    private fun decide(
        toolCallId: String,
        approved: Boolean,
        input: MonarchAiJSON?,
        reason: String?,
    ): Pair<MonarchAiConversationMessage, MonarchAiToolResponse>? {
        val call = toolOwner(toolCallId)?.toolCalls?.firstOrNull { it.id == toolCallId }
        if (call?.state != MonarchAiToolCallState.PendingApproval) return null
        val clientTool = onToolCall != null && !transport.respondsToTools
        val startedAt = timestamp()
        val owner = patchToolCall(toolCallId) {
            if (approved) {
                it.copy(
                    state = if (clientTool) MonarchAiToolCallState.AwaitingResult else MonarchAiToolCallState.Running,
                    input = input ?: it.input,
                    startedAt = startedAt,
                )
            } else {
                it.copy(
                    state = MonarchAiToolCallState.Denied,
                    error = reason?.takeIf { r -> r.isNotEmpty() } ?: it.error,
                )
            }
        } ?: return null
        val response = MonarchAiToolResponse(
            toolCallId = toolCallId,
            approved = approved,
            input = input,
            reason = reason,
            messages = tree.visiblePath,
            conversationId = conversationIdState.value,
        )
        onToolResponse?.invoke(response)
        return owner to response
    }

    /** Once no call of the reply waits for approval: lets the transport continue the reply, or settles it. */
    private fun proceed(response: MonarchAiToolResponse, owner: MonarchAiConversationMessage) {
        // Still streaming: the run settles the reply when it ends.
        if (current?.targetId == owner.id) return
        if (owner.toolCalls.any { it.state == MonarchAiToolCallState.PendingApproval }) return
        if (!transport.respondsToTools) {
            settle(owner.id, finished = false)
            return
        }
        run(owner.id, MonarchAiConversationStatus.Streaming) { transport.respondToTool(response) }
    }

    private fun respond(toolCallId: String, approved: Boolean, input: MonarchAiJSON?, reason: String?) {
        val (owner, response) = decide(toolCallId, approved, input, reason) ?: return
        proceed(response, owner)
    }

    /** After a reply ends: wait for approvals, run client tools, or finish. */
    private fun settle(messageId: String, finished: Boolean) {
        val message = tree[messageId] ?: return
        if (applyPolicy(message)) return
        val calls = message.toolCalls
        val awaiting = calls.filter { it.state == MonarchAiToolCallState.AwaitingResult }
        when {
            calls.any { it.state == MonarchAiToolCallState.PendingApproval } ->
                statusState.value = MonarchAiConversationStatus.AwaitingApproval
            awaiting.isNotEmpty() -> {
                statusState.value = MonarchAiConversationStatus.AwaitingToolResult
                onToolCall?.let { tool -> awaiting.forEach { runTool(it, tool) } }
            }
            else -> {
                statusState.value = MonarchAiConversationStatus.Idle
                if (finished) onFinish?.invoke(message)
            }
        }
    }

    /**
     * Puts every waiting call to the tool policy before anyone is asked.
     * Returns whether the policy decided something, which then settles the reply.
     */
    private fun applyPolicy(message: MonarchAiConversationMessage): Boolean {
        val policy = toolPolicy ?: return false
        var last: MonarchAiToolResponse? = null
        message.toolCalls.filter { it.state == MonarchAiToolCallState.PendingApproval }.forEach { call ->
            val decision = policy(call)
            if (decision != MonarchAiToolDecision.Ask) {
                last = decide(call.id, decision == MonarchAiToolDecision.Allow, null, null)?.second ?: last
            }
        }
        val response = last ?: return false
        val owner = tree[message.id]
        if (owner?.toolCalls?.any { it.state == MonarchAiToolCallState.PendingApproval } == true) {
            statusState.value = MonarchAiConversationStatus.AwaitingApproval
        } else if (owner != null) {
            proceed(response, owner)
        }
        return true
    }

    private fun runTool(call: MonarchAiToolCallPart, tool: suspend (MonarchAiToolCallPart) -> MonarchAiJSON) {
        val key = nextToolKey++
        toolJobs[key] = scope.launch {
            val outcome = runCatching { tool(call) }
            (outcome.exceptionOrNull() as? CancellationException)?.let { throw it }
            if (toolJobs.remove(key) == null) return@launch
            val result = outcome.fold(
                onSuccess = { MonarchAiToolResult.Output(it) },
                onFailure = {
                    MonarchAiToolResult.Failure(it.message?.takeIf { m -> m.isNotEmpty() } ?: toolResultFailedMessage)
                },
            )
            submitToolResult(call.id, result)
        }
    }

    /** Defaults of the conversation. */
    public companion object {
        /** The default time without any event before a running reply counts as slow: 10 seconds. */
        public const val DEFAULT_SLOW_AFTER_MILLIS: Long = 10_000L
    }
}
