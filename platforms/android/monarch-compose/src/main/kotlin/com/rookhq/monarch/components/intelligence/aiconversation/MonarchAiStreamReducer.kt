package com.rookhq.monarch.components.intelligence.aiconversation

/** How a reply ended, which decides what happens to tool calls still running. */
internal enum class MonarchAiReplyEnd {
    /** The stream finished. */
    Done,

    /** The person stopped it, or another turn replaced it. */
    Aborted,

    /** It failed. */
    Error,
}

/** Pure reducers from stream events to the message model, with the same rules as the web's `applyStreamEvent`. */
internal object MonarchAiStreamReducer {
    /**
     * Closes a reply's parts: streaming text and reasoning become done. A tool
     * call still running becomes `AwaitingResult` when the model stopped to
     * call tools, and `Cancelled` otherwise.
     */
    fun finishParts(
        parts: List<MonarchAiPart>,
        end: MonarchAiReplyEnd = MonarchAiReplyEnd.Done,
        finishReason: MonarchAiFinishReason? = null,
    ): List<MonarchAiPart> = parts.map { part ->
        when {
            part is MonarchAiTextPart && part.state == MonarchAiPartState.Streaming ->
                part.copy(state = MonarchAiPartState.Done)
            part is MonarchAiReasoningPart && part.state == MonarchAiPartState.Streaming ->
                part.copy(state = MonarchAiPartState.Done)
            part is MonarchAiToolCallPart && part.state == MonarchAiToolCallState.Running -> part.copy(
                state = if (end == MonarchAiReplyEnd.Done && finishReason == MonarchAiFinishReason.ToolUse) {
                    MonarchAiToolCallState.AwaitingResult
                } else {
                    MonarchAiToolCallState.Cancelled
                },
            )
            else -> part
        }
    }

    /** The tool calls of a message that still wait for a person or for the client. */
    fun unresolvedToolCalls(message: MonarchAiConversationMessage): List<MonarchAiToolCallPart> =
        message.toolCalls.filter {
            it.state == MonarchAiToolCallState.PendingApproval || it.state == MonarchAiToolCallState.AwaitingResult
        }

    /** Applies events in order. */
    fun apply(events: List<MonarchAiStreamEvent>, message: MonarchAiConversationMessage): MonarchAiConversationMessage =
        events.fold(message) { current, event -> apply(event, current) }

    /** Applies one stream event to the message being written. */
    fun apply(event: MonarchAiStreamEvent, message: MonarchAiConversationMessage): MonarchAiConversationMessage =
        when (event) {
            is MonarchAiStreamEvent.Agent -> message.copy(agent = event.agent)
            is MonarchAiStreamEvent.Finish -> message.copy(finishReason = event.reason)
            is MonarchAiStreamEvent.Run -> message.copy(runId = event.id)
            is MonarchAiStreamEvent.Usage ->
                message.copy(usage = event.usage, model = event.model?.takeIf { it.isNotEmpty() } ?: message.model)
            is MonarchAiStreamEvent.Error -> {
                val code = event.code ?: "unknown"
                val failure = MonarchAiErrorPart(
                    code = code,
                    message = event.message,
                    retryable = event.retryable ?: MonarchAiConversationError.isRetryable(code),
                    retryAfterMs = event.retryAfterMs,
                    resumable = event.resumable,
                )
                message.copy(parts = finishParts(message.parts, MonarchAiReplyEnd.Error) + failure)
            }
            is MonarchAiStreamEvent.Done ->
                message.copy(parts = finishParts(message.parts, MonarchAiReplyEnd.Done, message.finishReason))
            else -> message.copy(parts = applyToParts(event, message))
        }

    /** Applies an event that changes the parts. */
    private fun applyToParts(event: MonarchAiStreamEvent, message: MonarchAiConversationMessage): List<MonarchAiPart> =
        when (event) {
            is MonarchAiStreamEvent.TextDelta -> appendText(message.parts, event.text)
            is MonarchAiStreamEvent.ReasoningDelta -> appendReasoning(message.parts, event.text)
            is MonarchAiStreamEvent.ReasoningEnd -> endReasoning(message.parts, event.durationMs, event.redacted)
            is MonarchAiStreamEvent.Part -> addPart(message, event.part)
            is MonarchAiStreamEvent.ToolUpdate -> patchTool(message.parts, event.id) { event.patch.applyTo(it) }
            is MonarchAiStreamEvent.ToolInputDelta ->
                patchTool(message.parts, event.id) { it.copy(inputText = (it.inputText ?: "") + event.delta) }
            is MonarchAiStreamEvent.Memory -> addMemory(message.parts, event.memory)
            else -> message.parts
        }

    private fun appendText(parts: List<MonarchAiPart>, text: String): List<MonarchAiPart> {
        val last = parts.lastOrNull()
        return if (last is MonarchAiTextPart && last.state == MonarchAiPartState.Streaming) {
            parts.dropLast(1) + last.copy(text = last.text + text)
        } else {
            closeLastReasoning(parts) + MonarchAiTextPart(text, MonarchAiPartState.Streaming)
        }
    }

    private fun appendReasoning(parts: List<MonarchAiPart>, text: String): List<MonarchAiPart> {
        val last = parts.lastOrNull()
        return if (last is MonarchAiReasoningPart && last.state == MonarchAiPartState.Streaming) {
            parts.dropLast(1) + last.copy(text = last.text + text)
        } else {
            // Text followed by reasoning: the text is finished.
            closeLastText(parts) + MonarchAiReasoningPart(text, MonarchAiPartState.Streaming)
        }
    }

    private fun addPart(message: MonarchAiConversationMessage, incoming: MonarchAiPart): List<MonarchAiPart> {
        // A tool call announced twice (start, then complete input) is merged.
        if (incoming is MonarchAiToolCallPart && message.toolCalls.any { it.id == incoming.id }) {
            return patchTool(message.parts, incoming.id) { existing ->
                // The second announcement wins, but keeps what only the first one had.
                incoming.copy(
                    output = incoming.output ?: existing.output,
                    error = incoming.error ?: existing.error,
                    startedAt = incoming.startedAt ?: existing.startedAt,
                    endedAt = incoming.endedAt ?: existing.endedAt,
                    inputText = incoming.inputText ?: existing.inputText,
                    risk = incoming.risk ?: existing.risk,
                )
            }
        }
        return closeLastText(message.parts) + incoming
    }

    private fun addMemory(parts: List<MonarchAiPart>, memory: MonarchAiMemoryPart): List<MonarchAiPart> {
        val existing = parts.indexOfFirst {
            it is MonarchAiMemoryPart && it.id == memory.id && it.action == memory.action
        }
        return if (existing >= 0) {
            parts.toMutableList().apply { this[existing] = memory }
        } else {
            closeLastReasoning(closeLastText(parts)) + memory
        }
    }

    private fun closeLastText(parts: List<MonarchAiPart>): List<MonarchAiPart> {
        val last = parts.lastOrNull()
        return if (last is MonarchAiTextPart && last.state == MonarchAiPartState.Streaming) {
            parts.dropLast(1) + last.copy(state = MonarchAiPartState.Done)
        } else {
            parts
        }
    }

    private fun closeLastReasoning(parts: List<MonarchAiPart>): List<MonarchAiPart> {
        val last = parts.lastOrNull()
        return if (last is MonarchAiReasoningPart && last.state == MonarchAiPartState.Streaming) {
            parts.dropLast(1) + last.copy(state = MonarchAiPartState.Done)
        } else {
            parts
        }
    }

    private fun endReasoning(parts: List<MonarchAiPart>, durationMs: Long?, redacted: Boolean): List<MonarchAiPart> {
        val streaming = parts.indexOfLast { it is MonarchAiReasoningPart && it.state == MonarchAiPartState.Streaming }
        // A redacted block with nothing streaming is a reasoning part of its own.
        val index = when {
            streaming >= 0 -> streaming
            redacted -> -1
            else -> parts.indexOfLast { it is MonarchAiReasoningPart }
        }
        if (index < 0) {
            return if (redacted) {
                parts + MonarchAiReasoningPart("", MonarchAiPartState.Done, durationMs, redacted = true)
            } else {
                parts
            }
        }
        val reasoning = parts[index] as MonarchAiReasoningPart
        return parts.toMutableList().apply {
            this[index] = reasoning.copy(
                state = MonarchAiPartState.Done,
                durationMs = durationMs ?: reasoning.durationMs,
                redacted = reasoning.redacted || redacted,
            )
        }
    }

    /** Changes the tool call with this id, wherever it is in the parts. */
    fun patchTool(
        parts: List<MonarchAiPart>,
        id: String,
        patch: (MonarchAiToolCallPart) -> MonarchAiToolCallPart,
    ): List<MonarchAiPart> = parts.map { if (it is MonarchAiToolCallPart && it.id == id) patch(it) else it }
}
