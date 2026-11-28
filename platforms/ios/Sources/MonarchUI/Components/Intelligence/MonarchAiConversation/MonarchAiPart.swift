/// One piece of a message. A message is an ordered list of parts, so text,
/// reasoning, tool calls, sources and artifacts appear in the order the model
/// produced them.
///
/// It encodes to, and decodes from, the web's JSON: the fields of the part
/// with a `type` field that names the case.
public enum MonarchAiPart: Sendable, Hashable, Codable {
    /// Text.
    case text(MonarchAiTextPart)
    /// Reasoning.
    case reasoning(MonarchAiReasoningPart)
    /// A tool call.
    case toolCall(MonarchAiToolCallPart)
    /// A citation of a source.
    case citation(MonarchAiCitationPart)
    /// A source.
    case source(MonarchAiSourcePart)
    /// An attached file.
    case file(MonarchAiFilePart)
    /// An artifact.
    case artifact(MonarchAiArtifactPart)
    /// A proposed edit.
    case editProposal(MonarchAiEditProposalPart)
    /// A plan.
    case plan(MonarchAiPlanPart)
    /// Generative UI.
    case ui(MonarchAiUiPart)
    /// A memory that was used, saved or forgotten.
    case memory(MonarchAiMemoryPart)
    /// A failure of the turn.
    case error(MonarchAiErrorPart)

    /// The name of the part in the web model: `text`, `tool-call`, `edit-proposal` and so on.
    public var type: String {
        switch self {
        case .text: "text"
        case .reasoning: "reasoning"
        case .toolCall: "tool-call"
        case .citation: "citation"
        case .source: "source"
        case .file: "file"
        case .artifact: "artifact"
        case .editProposal: "edit-proposal"
        case .plan: "plan"
        case .ui: "ui"
        case .memory: "memory"
        case .error: "error"
        }
    }

    private enum CodingKeys: String, CodingKey {
        case type
    }

    /// Decodes a part from the web's JSON.
    public init(from decoder: any Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let type = try container.decode(String.self, forKey: .type)
        switch type {
        case "text": self = .text(try MonarchAiTextPart(from: decoder))
        case "reasoning": self = .reasoning(try MonarchAiReasoningPart(from: decoder))
        case "tool-call": self = .toolCall(try MonarchAiToolCallPart(from: decoder))
        case "citation": self = .citation(try MonarchAiCitationPart(from: decoder))
        case "source": self = .source(try MonarchAiSourcePart(from: decoder))
        case "file": self = .file(try MonarchAiFilePart(from: decoder))
        case "artifact": self = .artifact(try MonarchAiArtifactPart(from: decoder))
        case "edit-proposal": self = .editProposal(try MonarchAiEditProposalPart(from: decoder))
        case "plan": self = .plan(try MonarchAiPlanPart(from: decoder))
        case "ui": self = .ui(try MonarchAiUiPart(from: decoder))
        case "memory": self = .memory(try MonarchAiMemoryPart(from: decoder))
        case "error": self = .error(try MonarchAiErrorPart(from: decoder))
        default:
            throw DecodingError.dataCorruptedError(
                forKey: .type,
                in: container,
                debugDescription: "Unknown part type \(type)"
            )
        }
    }

    /// Encodes the part as the web's JSON.
    public func encode(to encoder: any Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(type, forKey: .type)
        switch self {
        case .text(let part): try part.encode(to: encoder)
        case .reasoning(let part): try part.encode(to: encoder)
        case .toolCall(let part): try part.encode(to: encoder)
        case .citation(let part): try part.encode(to: encoder)
        case .source(let part): try part.encode(to: encoder)
        case .file(let part): try part.encode(to: encoder)
        case .artifact(let part): try part.encode(to: encoder)
        case .editProposal(let part): try part.encode(to: encoder)
        case .plan(let part): try part.encode(to: encoder)
        case .ui(let part): try part.encode(to: encoder)
        case .memory(let part): try part.encode(to: encoder)
        case .error(let part): try part.encode(to: encoder)
        }
    }
}
