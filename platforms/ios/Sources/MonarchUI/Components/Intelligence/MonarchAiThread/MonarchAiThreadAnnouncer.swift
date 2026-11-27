import Foundation

/// Decides what VoiceOver hears for a whole conversation, as the web
/// thread's polite live region does: "Thinking…" once when a reply is
/// submitted, the reply in whole sentences at most once per interval while
/// it streams, then the rest with "Response complete" or "Response stopped".
/// Replies that were already finished when first seen (history, a branch
/// switch) are not read. A value type with the time passed in, so every
/// decision is testable.
struct MonarchAiThreadAnnouncer: Equatable, Sendable {
    /// What to do after an update.
    enum Outcome: Equatable, Sendable {
        /// Nothing to say.
        case silent
        /// Post this announcement now.
        case announce(String)
        /// Something is queued but the interval has not passed: update again at this time.
        case wait(until: Date)
    }

    /// The newest message as the announcer needs it.
    struct Snapshot: Equatable, Sendable {
        /// The id of the newest message.
        let id: String
        /// Whether it is the assistant's.
        let isAssistant: Bool
        /// Its spoken text: Markdown as speech, code blocks and tables named.
        let spoken: String
        /// Whether any of its text is still streaming.
        let isStreaming: Bool
    }

    /// The fixed words of the announcer.
    struct Words: Equatable, Sendable {
        /// Said when a reply is submitted.
        let thinking: String
        /// Said when a reply finishes.
        let complete: String
        /// Said when a reply is stopped.
        let stopped: String
    }

    /// The message being followed.
    private(set) var messageID: String?
    /// How much of its spoken text is queued or said, in UTF-16 units.
    private(set) var spokenLength = 0
    /// Whether the followed message arrived while the thread was shown.
    private(set) var isLive = false
    /// Text waiting for the interval to pass.
    private(set) var queue = ""
    /// When the last announcement was posted.
    private(set) var lastAnnouncement: Date?
    /// The status of the previous update, to say "Thinking…" once per submission.
    private(set) var lastStatus: MonarchAiConversationStatus?

    /// The interval in seconds.
    static func interval(_ duration: Duration) -> TimeInterval {
        let parts = max(duration, .zero).components
        return TimeInterval(parts.seconds) + TimeInterval(parts.attoseconds) / 1e18
    }

    /// Takes the newest message and the status and returns what to announce.
    ///
    /// - Parameters:
    ///   - message: The newest message, or `nil` for an empty conversation.
    ///   - status: The conversation's status.
    ///   - words: The fixed words.
    ///   - interval: The least time between polite announcements, in seconds.
    ///   - now: The current time.
    /// - Returns: Whether to stay silent, announce now, or update again later.
    mutating func update(
        message: Snapshot?,
        status: MonarchAiConversationStatus,
        words: Words,
        interval: TimeInterval,
        now: Date
    ) -> Outcome {
        let previousStatus = lastStatus
        lastStatus = status
        // A new submission: say "Thinking…" once and drop what an earlier reply left queued.
        if status == .submitted && previousStatus != .submitted {
            queue = ""
            lastAnnouncement = now
            return .announce(words.thinking)
        }
        guard let message, message.isAssistant else { return flushIfDue(interval: interval, now: now) }
        let isRunning = status == .submitted || status == .streaming
        if message.id != messageID {
            let isLive = message.isStreaming || isRunning
            messageID = message.id
            spokenLength = isLive ? 0 : message.spoken.utf16.count
            self.isLive = isLive
        }
        guard isLive else { return flushIfDue(interval: interval, now: now) }
        if message.isStreaming {
            let end = Self.lastBoundary(message.spoken, from: spokenLength)
            if end > spokenLength {
                queue += " " + Self.slice(message.spoken, from: spokenLength, to: end)
                spokenLength = end
            }
            return flushIfDue(interval: interval, now: now)
        }
        // Not finished yet: a reply waiting for its first words, or a tool
        // call waiting for approval or a result. The end belongs to the reply.
        if isRunning || status == .awaitingApproval || status == .awaitingToolResult {
            return flushIfDue(interval: interval, now: now)
        }
        isLive = false
        queue += " " + Self.slice(message.spoken, from: spokenLength, to: message.spoken.utf16.count)
        spokenLength = message.spoken.utf16.count
        if status != .error {
            queue += " " + (status == .stopped ? words.stopped : words.complete)
        }
        return flush(now: now)
    }

    private mutating func flushIfDue(interval: TimeInterval, now: Date) -> Outcome {
        guard !Self.normalized(queue).isEmpty else { return .silent }
        if let lastAnnouncement {
            let next = lastAnnouncement.addingTimeInterval(interval)
            if now < next { return .wait(until: next) }
        }
        return flush(now: now)
    }

    private mutating func flush(now: Date) -> Outcome {
        let text = Self.normalized(queue)
        queue = ""
        guard !text.isEmpty else { return .silent }
        lastAnnouncement = now
        return .announce(text)
    }

    /// The text with runs of white space made one space and the ends trimmed.
    static func normalized(_ text: String) -> String {
        text.split(whereSeparator: \.isWhitespace).joined(separator: " ")
    }

    /// The UTF-16 end of the last sentence or line that ends after `from`,
    /// or `from` when none does. A sentence ends with `.`, `!`, `?`, `…` or
    /// their full-width forms, followed (after closing quotes or brackets)
    /// by white space.
    static func lastBoundary(_ text: String, from: Int) -> Int {
        let length = text.utf16.count
        guard from < length else { return from }
        let start = String.Index(utf16Offset: from, in: text)
        // A local pattern: `Regex` is not `Sendable`, so it cannot be a static of this value type.
        let boundary = /[.!?…。！？](?=["'”’)\]]*\s)|\n/
        var end = from
        for match in text[start...].matches(of: boundary) {
            end = match.range.upperBound.utf16Offset(in: text)
        }
        return end
    }

    private static func slice(_ text: String, from start: Int, to end: Int) -> String {
        let length = text.utf16.count
        let lower = String.Index(utf16Offset: min(start, length), in: text)
        let upper = String.Index(utf16Offset: min(end, length), in: text)
        return lower <= upper ? String(text[lower..<upper]) : ""
    }
}
