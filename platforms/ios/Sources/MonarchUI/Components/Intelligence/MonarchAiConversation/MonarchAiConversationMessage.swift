import Foundation

/// One turn of a conversation: the vendor-neutral message model every Monarch
/// AI component renders. It has the same fields as the web's `AiMessage` and
/// reads and writes the same JSON, so a stored conversation can move between
/// platforms.
///
/// The web type is `AiMessage`. On iOS that name belongs to the message view,
/// so the model is `MonarchAiConversationMessage`.
public struct MonarchAiConversationMessage: Identifiable, Sendable, Hashable, Codable {
    /// The id of the message.
    public var id: String
    /// Who wrote it.
    public var role: MonarchAiRole
    /// The parts, in the order the model produced them.
    public var parts: [MonarchAiPart]
    /// The agent that answered.
    public var agent: MonarchAiAgentRef?
    /// The ISO 8601 time the message was created.
    public var createdAt: String
    /// The model that produced it, for example `gemini-2.5-pro`.
    public var model: String?
    /// The token counts and cost of the turn.
    public var usage: MonarchAiUsage?
    /// The feedback a person gave.
    public var feedback: MonarchAiMessageFeedback?
    /// Set when the message has alternatives: which one is shown.
    public var branch: MonarchAiBranchPosition?
    /// Why the model stopped. `length` means the answer was cut short.
    public var finishReason: MonarchAiFinishReason?
    /// The service's id for the run that wrote this reply, used to resume it.
    public var runId: String?
    /// Whether this is a user message written while offline that has not been sent yet.
    public var isQueued: Bool

    /// A message.
    ///
    /// - Parameters:
    ///   - id: The id of the message.
    ///   - role: Who wrote it.
    ///   - parts: The parts, in order.
    ///   - agent: The agent that answered.
    ///   - createdAt: The ISO 8601 time the message was created.
    ///   - model: The model that produced it.
    ///   - usage: The token counts and cost of the turn.
    ///   - feedback: The feedback a person gave.
    ///   - branch: Which alternative is shown, when the message has alternatives.
    ///   - finishReason: Why the model stopped.
    ///   - runId: The service's id for the run that wrote this reply.
    ///   - isQueued: Whether the message was written offline and has not been sent yet.
    public init(
        id: String,
        role: MonarchAiRole,
        parts: [MonarchAiPart],
        agent: MonarchAiAgentRef? = nil,
        createdAt: String,
        model: String? = nil,
        usage: MonarchAiUsage? = nil,
        feedback: MonarchAiMessageFeedback? = nil,
        branch: MonarchAiBranchPosition? = nil,
        finishReason: MonarchAiFinishReason? = nil,
        runId: String? = nil,
        isQueued: Bool = false
    ) {
        self.id = id
        self.role = role
        self.parts = parts
        self.agent = agent
        self.createdAt = createdAt
        self.model = model
        self.usage = usage
        self.feedback = feedback
        self.branch = branch
        self.finishReason = finishReason
        self.runId = runId
        self.isQueued = isQueued
    }

    /// The plain text of the message: its text parts joined. Reasoning and tool parts are left out.
    public var text: String {
        parts.reduce(into: "") { text, part in
            if case .text(let part) = part { text += part.text }
        }
    }

    /// Whether any text or reasoning part of the message is still streaming.
    public var isStreaming: Bool {
        parts.contains { part in
            switch part {
            case .text(let part): part.state == .streaming
            case .reasoning(let part): part.state == .streaming
            default: false
            }
        }
    }

    /// The tool calls of the message, in order.
    public var toolCalls: [MonarchAiToolCallPart] {
        parts.compactMap { part in
            if case .toolCall(let call) = part { return call }
            return nil
        }
    }

    /// The memories the reply used, saved and forgot, in order.
    public var memories: [MonarchAiMemoryPart] {
        parts.compactMap { part in
            if case .memory(let memory) = part { return memory }
            return nil
        }
    }

    /// `createdAt` as a date, or `nil` if it is not an ISO 8601 time.
    public var createdDate: Date? {
        MonarchAiTimestamp.date(from: createdAt)
    }

    /// The ISO 8601 text of a date, as `createdAt` and the tool call times hold it.
    public static func timestamp(_ date: Date) -> String {
        MonarchAiTimestamp.string(from: date)
    }

    private enum CodingKeys: String, CodingKey {
        case id
        case role
        case parts
        case agent
        case createdAt
        case model
        case usage
        case feedback
        case branch
        case finishReason
        case runId
        case isQueued = "queued"
    }

    /// Decodes the web's shape, where `queued` is left out unless it is true.
    public init(from decoder: any Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        role = try container.decode(MonarchAiRole.self, forKey: .role)
        parts = try container.decode([MonarchAiPart].self, forKey: .parts)
        agent = try container.decodeIfPresent(MonarchAiAgentRef.self, forKey: .agent)
        createdAt = try container.decode(String.self, forKey: .createdAt)
        model = try container.decodeIfPresent(String.self, forKey: .model)
        usage = try container.decodeIfPresent(MonarchAiUsage.self, forKey: .usage)
        feedback = try container.decodeIfPresent(MonarchAiMessageFeedback.self, forKey: .feedback)
        branch = try container.decodeIfPresent(MonarchAiBranchPosition.self, forKey: .branch)
        finishReason = try container.decodeIfPresent(MonarchAiFinishReason.self, forKey: .finishReason)
        runId = try container.decodeIfPresent(String.self, forKey: .runId)
        isQueued = try container.decodeIfPresent(Bool.self, forKey: .isQueued) ?? false
    }

    /// Encodes the web's shape.
    public func encode(to encoder: any Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(role, forKey: .role)
        try container.encode(parts, forKey: .parts)
        try container.encodeIfPresent(agent, forKey: .agent)
        try container.encode(createdAt, forKey: .createdAt)
        try container.encodeIfPresent(model, forKey: .model)
        try container.encodeIfPresent(usage, forKey: .usage)
        try container.encodeIfPresent(feedback, forKey: .feedback)
        try container.encodeIfPresent(branch, forKey: .branch)
        try container.encodeIfPresent(finishReason, forKey: .finishReason)
        try container.encodeIfPresent(runId, forKey: .runId)
        if isQueued { try container.encode(true, forKey: .isQueued) }
    }
}
