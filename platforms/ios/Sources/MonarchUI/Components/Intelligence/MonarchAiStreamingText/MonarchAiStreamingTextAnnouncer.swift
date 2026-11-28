import Foundation

/// Decides what VoiceOver hears while an answer streams, as the web's polite
/// live region does: whole sentences and finished blocks, never single
/// tokens, at most once per interval, then "Response complete". A value
/// type with the time passed in, so every decision is testable.
struct MonarchAiStreamingTextAnnouncer: Equatable, Sendable {
    /// What to do after an update.
    enum Outcome: Equatable, Sendable {
        /// Nothing new to say.
        case silent
        /// Post this announcement now.
        case announce(String)
        /// Something is ready but the interval has not passed: update again at this time.
        case wait(until: Date)
    }

    /// The shortest interval between announcements.
    static let minimumInterval: Duration = .seconds(1)

    /// How much of the spoken text has been announced, in UTF-16 units.
    private(set) var announced = 0
    /// The spoken text up to `announced`, to notice a different answer.
    private(set) var prefix = ""
    /// When the last announcement was posted.
    private(set) var lastAnnouncement: Date?
    /// Whether this text has streamed since the last completion.
    private(set) var wasStreaming = false
    /// The text finished while the reply went on: "Response complete" is still owed.
    private(set) var owesCompletion = false

    /// The interval actually used: never below one second.
    static func interval(_ requested: Duration) -> TimeInterval {
        let seconds = max(requested, minimumInterval).components
        return TimeInterval(seconds.seconds) + TimeInterval(seconds.attoseconds) / 1e18
    }

    /// Takes the current state of the text and returns what to announce.
    ///
    /// - Parameters:
    ///   - spoken: The spoken text of every block shown, finished blocks first.
    ///   - done: The UTF-16 length of the finished blocks' part of `spoken`.
    ///   - isStreaming: Whether text is still arriving.
    ///   - announcesCompletion: Whether "Response complete" may be said when streaming ends.
    ///   - complete: The completion announcement.
    ///   - interval: The minimum time between announcements, in seconds.
    ///   - now: The current time.
    /// - Returns: Whether to stay silent, announce now, or update again later.
    mutating func update(
        spoken: String,
        done: Int,
        isStreaming: Bool,
        announcesCompletion: Bool,
        complete: String,
        interval: TimeInterval,
        now: Date
    ) -> Outcome {
        if isStreaming {
            wasStreaming = true
            owesCompletion = false
        }
        // The reply finished after this text did: only "Response complete" is left to say.
        if !isStreaming && !wasStreaming && owesCompletion {
            guard announcesCompletion else { return .silent }
            owesCompletion = false
            return .announce(complete)
        }
        // Text that was never streamed here (history) is not announced.
        if !isStreaming && !wasStreaming { return .silent }
        // A different answer (regenerate, reset): start over.
        if !spoken.hasPrefix(prefix) { announced = 0 }

        let length = spoken.utf16.count
        var boundary = length
        if isStreaming {
            boundary = min(done, length)
            let start = String.Index(utf16Offset: boundary, in: spoken)
            // A local pattern: `Regex` is not `Sendable`, so it cannot be a static of this value type.
            let sentenceEnd = /[.!?…。！？][”’"')\]]*(?=\s)/
            for match in spoken[start...].matches(of: sentenceEnd) {
                boundary = match.range.upperBound.utf16Offset(in: spoken)
            }
        }
        let finished = !isStreaming && wasStreaming
        let chunk =
            boundary > announced
            ? Self.slice(spoken, from: announced, to: boundary).trimmingCharacters(in: .whitespacesAndNewlines)
            : ""
        if chunk.isEmpty && !finished { return .silent }
        if let lastAnnouncement {
            let next = lastAnnouncement.addingTimeInterval(interval)
            if now < next { return .wait(until: next) }
        }
        announced = max(announced, boundary)
        prefix = Self.slice(spoken, from: 0, to: announced)
        lastAnnouncement = now
        if finished {
            wasStreaming = false
            owesCompletion = !announcesCompletion
        }
        let message =
            finished && announcesCompletion
            ? [chunk, complete].filter { !$0.isEmpty }.joined(separator: "\n\n")
            : chunk
        return message.isEmpty ? .silent : .announce(message)
    }

    private static func slice(_ text: String, from start: Int, to end: Int) -> String {
        let lower = String.Index(utf16Offset: min(start, text.utf16.count), in: text)
        let upper = String.Index(utf16Offset: min(end, text.utf16.count), in: text)
        return lower <= upper ? String(text[lower..<upper]) : ""
    }
}
