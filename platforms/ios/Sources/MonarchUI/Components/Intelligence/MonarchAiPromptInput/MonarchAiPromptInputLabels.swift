import Foundation

/// Replacements for the built-in strings of ``MonarchAiPromptInput``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiPromptInputLabels: Sendable {
    /// The send button. Built in: "Send".
    public var send: String?
    /// The stop button. Built in: "Stop".
    public var stop: String?
    /// The attach button. Built in: "Attach files".
    public var attach: String?
    /// Shown while files are dragged over the prompt. Built in: "Drop files to attach".
    public var dropHint: String?
    /// The visible counter from the count and the limit. Built in: "120 / 4000".
    public var counter: (@Sendable (_ count: Int, _ max: Int) -> String)?
    /// The spoken counter from the count and the limit. Built in: "120 of 4000 characters".
    public var counterLabel: (@Sendable (_ count: Int, _ max: Int) -> String)?
    /// Announced near the limit, from the characters left. Built in: "12 characters left".
    public var counterWarning: (@Sendable (_ remaining: Int) -> String)?
    /// Announced and read over the limit, from the characters over. Built in: "3 characters over the limit".
    public var counterOver: (@Sendable (_ over: Int) -> String)?
    /// The token estimate. Built in: "≈ 120 tokens".
    public var tokens: (@Sendable (_ count: Int) -> String)?
    /// The strings of the attachment tray.
    public var attachments: MonarchAiAttachmentTrayLabels

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - send: The send button.
    ///   - stop: The stop button.
    ///   - attach: The attach button.
    ///   - dropHint: Shown while files are dragged over the prompt.
    ///   - counter: The visible counter.
    ///   - counterLabel: The spoken counter.
    ///   - counterWarning: Announced near the limit.
    ///   - counterOver: Announced and read over the limit.
    ///   - tokens: The token estimate.
    ///   - attachments: The strings of the attachment tray.
    public init(
        send: String? = nil,
        stop: String? = nil,
        attach: String? = nil,
        dropHint: String? = nil,
        counter: (@Sendable (_ count: Int, _ max: Int) -> String)? = nil,
        counterLabel: (@Sendable (_ count: Int, _ max: Int) -> String)? = nil,
        counterWarning: (@Sendable (_ remaining: Int) -> String)? = nil,
        counterOver: (@Sendable (_ over: Int) -> String)? = nil,
        tokens: (@Sendable (_ count: Int) -> String)? = nil,
        attachments: MonarchAiAttachmentTrayLabels = MonarchAiAttachmentTrayLabels()
    ) {
        self.send = send
        self.stop = stop
        self.attach = attach
        self.dropHint = dropHint
        self.counter = counter
        self.counterLabel = counterLabel
        self.counterWarning = counterWarning
        self.counterOver = counterOver
        self.tokens = tokens
        self.attachments = attachments
    }
}
