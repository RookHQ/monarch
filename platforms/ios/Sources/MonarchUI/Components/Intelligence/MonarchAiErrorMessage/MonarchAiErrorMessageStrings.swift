/// Built-in strings of the AI failure card, from the `MonarchAiErrorMessage` strings table.
enum MonarchAiErrorMessageStrings {
    private static let table = "MonarchAiErrorMessage"

    private static func key(_ kind: MonarchAiErrorKind) -> String {
        switch kind {
        case .rateLimit: "rateLimit"
        case .contextOverflow: "contextOverflow"
        case .safetyBlock: "safetyBlock"
        case .network: "network"
        case .toolFailure: "toolFailure"
        case .unknown: "unknown"
        }
    }

    /// The title of a kind.
    static func title(_ kind: MonarchAiErrorKind) -> String {
        MonarchStrings.string("monarch.aiErrorMessage.title.\(key(kind))", table: table)
    }

    /// The description of a kind.
    static func message(_ kind: MonarchAiErrorKind) -> String {
        MonarchStrings.string("monarch.aiErrorMessage.message.\(key(kind))", table: table)
    }

    /// The title of an action.
    static func action(_ action: MonarchAiErrorMessageTokens.Action) -> String {
        switch action {
        case .retry: MonarchStrings.string("monarch.aiErrorMessage.retry", table: table)
        case .editPrompt: MonarchStrings.string("monarch.aiErrorMessage.editPrompt", table: table)
        case .switchModel: MonarchStrings.string("monarch.aiErrorMessage.switchModel", table: table)
        case .newConversation: MonarchStrings.string("monarch.aiErrorMessage.newConversation", table: table)
        case .summarize: MonarchStrings.string("monarch.aiErrorMessage.summarize", table: table)
        }
    }

    /// The title of the retry action while it counts down.
    static func retryIn(_ seconds: Int) -> String {
        MonarchStrings.string("monarch.aiErrorMessage.retryIn", table: table, seconds)
    }

    /// Announced when the countdown ends.
    static var retryReady: String { MonarchStrings.string("monarch.aiErrorMessage.retryReady", table: table) }
    /// The title of the technical details disclosure.
    static var details: String { MonarchStrings.string("monarch.aiErrorMessage.details", table: table) }
}
