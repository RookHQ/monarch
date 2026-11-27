/// Replacements for the built-in strings of ``MonarchAiThread``.
/// A property that is `nil` keeps the built-in English string.
@_spi(Experimental) public struct MonarchAiThreadLabels: Sendable {
    /// The name of the list. Built in: "Conversation".
    public var label: String?
    /// The jump button. Built in: "Jump to latest".
    public var jumpToLatest: String?
    /// The count on the jump button. Receives the count. Built in: "2 new messages".
    public var newMessages: (@Sendable (Int) -> String)?
    /// The button that loads older messages. Built in: "Load earlier messages".
    public var loadEarlier: String?
    /// The load button while older messages load. Built in: "Loading earlier messages…".
    public var loadingEarlier: String?
    /// The thinking indicator and its announcement. Built in: "Thinking…".
    public var thinking: String?
    /// Announced when a reply finishes. Built in: "Response complete".
    public var responseComplete: String?
    /// Announced when the person stops a reply. Built in: "Response stopped".
    public var responseStopped: String?
    /// Announced for a conversation error no message shows. Receives the message. Built in: "Error: <message>".
    public var error: (@Sendable (String) -> String)?
    /// Spoken in place of a code block. Built in: "Code block".
    public var codeBlock: String?
    /// Spoken in place of a table. Built in: "Table".
    public var table: String?
    /// The day label for today. Built in: "Today".
    public var today: String?
    /// The day label for yesterday. Built in: "Yesterday".
    public var yesterday: String?
    /// The notice while offline with nothing queued. Built in: "You are offline. Messages you send will wait…".
    public var offline: String?
    /// The notice while offline with messages queued. Receives the count.
    public var offlineQueued: (@Sendable (Int) -> String)?
    /// The notice while queued messages are sent. Receives the count. Built in: "Back online. Sending 2 queued messages…".
    public var sendingQueued: (@Sendable (Int) -> String)?
    /// The notice while a running reply is slow. Built in: "This is taking longer than usual. Still working…".
    public var slow: String?
    /// The VoiceOver action that stops a running reply. Built in: "Stop response".
    public var stop: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        jumpToLatest: String? = nil,
        newMessages: (@Sendable (Int) -> String)? = nil,
        loadEarlier: String? = nil,
        loadingEarlier: String? = nil,
        thinking: String? = nil,
        responseComplete: String? = nil,
        responseStopped: String? = nil,
        error: (@Sendable (String) -> String)? = nil,
        codeBlock: String? = nil,
        table: String? = nil,
        today: String? = nil,
        yesterday: String? = nil,
        offline: String? = nil,
        offlineQueued: (@Sendable (Int) -> String)? = nil,
        sendingQueued: (@Sendable (Int) -> String)? = nil,
        slow: String? = nil,
        stop: String? = nil
    ) {
        self.label = label
        self.jumpToLatest = jumpToLatest
        self.newMessages = newMessages
        self.loadEarlier = loadEarlier
        self.loadingEarlier = loadingEarlier
        self.thinking = thinking
        self.responseComplete = responseComplete
        self.responseStopped = responseStopped
        self.error = error
        self.codeBlock = codeBlock
        self.table = table
        self.today = today
        self.yesterday = yesterday
        self.offline = offline
        self.offlineQueued = offlineQueued
        self.sendingQueued = sendingQueued
        self.slow = slow
        self.stop = stop
    }
}
