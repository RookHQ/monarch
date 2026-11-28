/// Built-in strings of the AI message, from the `MonarchAiMessage` strings table.
enum MonarchAiMessageStrings {
    private static let table = "MonarchAiMessage"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiMessage.\(key)", table: table)
    }

    /// The spoken name of an assistant message.
    static var assistantSaid: String { string("assistantSaid") }
    /// The spoken name of the person's message.
    static var youSaid: String { string("youSaid") }
    /// The spoken name of a system or tool message.
    static var systemSaid: String { string("systemSaid") }
    /// The name in the header when the message has no agent.
    static var assistant: String { string("assistant") }
    /// The line under a message written offline.
    static var queued: String { string("queued") }
    /// The button that cancels a queued message.
    static var cancelQueued: String { string("cancelQueued") }
    /// The button that resumes a dropped reply.
    static var resume: String { string("resume") }
    /// The name of the plan's steps.
    static var plan: String { string("plan") }

    /// The notice under a reply that stopped for a reason the person should know.
    static func finishNotice(_ reason: MonarchAiFinishReason) -> String? {
        switch reason {
        case .length: string("finish.length")
        case .contentFilter: string("finish.contentFilter")
        case .refusal: string("finish.refusal")
        case .stop, .toolUse, .other: nil
        }
    }
}
