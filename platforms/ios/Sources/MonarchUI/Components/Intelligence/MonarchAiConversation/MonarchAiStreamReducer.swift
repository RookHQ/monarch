/// Pure reducers from stream events to the message model, with the same
/// rules as the web's `applyStreamEvent`.
enum MonarchAiStreamReducer {
    /// How a reply ended, which decides what happens to tool calls still running.
    enum End {
        /// The stream finished.
        case done
        /// The person stopped it, or another turn replaced it.
        case aborted
        /// It failed.
        case error
    }

    /// Closes a reply's parts: streaming text and reasoning become `done`. A
    /// tool call still `running` becomes `awaitingResult` when the model
    /// stopped to call tools, and `cancelled` otherwise.
    static func finishParts(
        _ parts: [MonarchAiPart],
        end: End = .done,
        finishReason: MonarchAiFinishReason? = nil
    ) -> [MonarchAiPart] {
        parts.map { part in
            switch part {
            case .text(var text) where text.state == .streaming:
                text.state = .done
                return .text(text)
            case .reasoning(var reasoning) where reasoning.state == .streaming:
                reasoning.state = .done
                return .reasoning(reasoning)
            case .toolCall(var call) where call.state == .running:
                call.state = end == .done && finishReason == .toolUse ? .awaitingResult : .cancelled
                return .toolCall(call)
            default:
                return part
            }
        }
    }

    /// The tool calls of a message that still wait for a person or for the client.
    static func unresolvedToolCalls(_ message: MonarchAiConversationMessage) -> [MonarchAiToolCallPart] {
        message.toolCalls.filter { $0.state == .pendingApproval || $0.state == .awaitingResult }
    }

    /// Applies one stream event to the message being written.
    static func apply(
        _ event: MonarchAiStreamEvent,
        to message: MonarchAiConversationMessage
    ) -> MonarchAiConversationMessage {
        var message = message
        switch event {
        case .agent(let agent):
            message.agent = agent
        case .textDelta(let text):
            if case .text(var last)? = message.parts.last, last.state == .streaming {
                last.text += text
                message.parts[message.parts.count - 1] = .text(last)
            } else {
                closeLastReasoning(&message.parts)
                message.parts.append(.text(MonarchAiTextPart(text: text, state: .streaming)))
            }
        case .reasoningDelta(let text):
            if case .reasoning(var last)? = message.parts.last, last.state == .streaming {
                last.text += text
                message.parts[message.parts.count - 1] = .reasoning(last)
            } else {
                // Text followed by reasoning: the text is finished.
                closeLastText(&message.parts)
                message.parts.append(.reasoning(MonarchAiReasoningPart(text: text, state: .streaming)))
            }
        case .reasoningEnd(let durationMs, let isRedacted):
            endReasoning(&message.parts, durationMs: durationMs, isRedacted: isRedacted)
        case .part(let incoming):
            // A tool call announced twice (start, then complete input) is merged.
            if case .toolCall(let call) = incoming, message.toolCalls.contains(where: { $0.id == call.id }) {
                message.parts = patchTool(message.parts, id: call.id) { existing in
                    // The second announcement wins, but keeps what only the first one had.
                    var merged = call
                    merged.output = call.output ?? existing.output
                    merged.error = call.error ?? existing.error
                    merged.startedAt = call.startedAt ?? existing.startedAt
                    merged.endedAt = call.endedAt ?? existing.endedAt
                    merged.inputText = call.inputText ?? existing.inputText
                    merged.risk = call.risk ?? existing.risk
                    return merged
                }
            } else {
                closeLastText(&message.parts)
                message.parts.append(incoming)
            }
        case .toolUpdate(let id, let patch):
            message.parts = patchTool(message.parts, id: id) { patch.applied(to: $0) }
        case .toolInputDelta(let id, let delta):
            message.parts = patchTool(message.parts, id: id) { call in
                var call = call
                call.inputText = (call.inputText ?? "") + delta
                return call
            }
        case .memory(let memory):
            let existing = message.parts.firstIndex { part in
                if case .memory(let other) = part { return other.id == memory.id && other.action == memory.action }
                return false
            }
            if let existing {
                message.parts[existing] = .memory(memory)
            } else {
                closeLastText(&message.parts)
                closeLastReasoning(&message.parts)
                message.parts.append(.memory(memory))
            }
        case .finish(let reason):
            message.finishReason = reason
        case .run(let id):
            message.runId = id
        case .usage(let usage, let model):
            message.usage = usage
            if let model, !model.isEmpty { message.model = model }
        case .error(let code, let text, let isRetryable, let retryAfterMs, let isResumable):
            let code = code ?? "unknown"
            message.parts = finishParts(message.parts, end: .error)
            message.parts.append(
                .error(
                    MonarchAiErrorPart(
                        code: code,
                        message: text,
                        isRetryable: isRetryable ?? MonarchAiConversationError.isRetryable(code: code),
                        retryAfterMs: retryAfterMs,
                        isResumable: isResumable
                    )
                )
            )
        case .done:
            message.parts = finishParts(message.parts, end: .done, finishReason: message.finishReason)
        }
        return message
    }

    /// Applies events in order.
    static func apply(
        _ events: [MonarchAiStreamEvent],
        to message: MonarchAiConversationMessage
    ) -> MonarchAiConversationMessage {
        events.reduce(message) { apply($1, to: $0) }
    }

    private static func closeLastText(_ parts: inout [MonarchAiPart]) {
        if case .text(var last)? = parts.last, last.state == .streaming {
            last.state = .done
            parts[parts.count - 1] = .text(last)
        }
    }

    private static func closeLastReasoning(_ parts: inout [MonarchAiPart]) {
        if case .reasoning(var last)? = parts.last, last.state == .streaming {
            last.state = .done
            parts[parts.count - 1] = .reasoning(last)
        }
    }

    private static func endReasoning(_ parts: inout [MonarchAiPart], durationMs: Int?, isRedacted: Bool) {
        func lastReasoning(streamingOnly: Bool) -> Int? {
            parts.lastIndex { part in
                if case .reasoning(let reasoning) = part { return !streamingOnly || reasoning.state == .streaming }
                return false
            }
        }
        let streaming = lastReasoning(streamingOnly: true)
        // A redacted block with nothing streaming is a reasoning part of its own.
        let index = streaming ?? (isRedacted ? nil : lastReasoning(streamingOnly: false))
        if let index, case .reasoning(var reasoning) = parts[index] {
            reasoning.state = .done
            if let durationMs { reasoning.durationMs = durationMs }
            if isRedacted { reasoning.isRedacted = true }
            parts[index] = .reasoning(reasoning)
        } else if isRedacted {
            parts.append(
                .reasoning(MonarchAiReasoningPart(text: "", state: .done, durationMs: durationMs, isRedacted: true))
            )
        }
    }

    /// Changes the tool call with this id, wherever it is in the parts.
    static func patchTool(
        _ parts: [MonarchAiPart],
        id: String,
        _ patch: (MonarchAiToolCallPart) -> MonarchAiToolCallPart
    ) -> [MonarchAiPart] {
        parts.map { part in
            if case .toolCall(let call) = part, call.id == id { return .toolCall(patch(call)) }
            return part
        }
    }
}
