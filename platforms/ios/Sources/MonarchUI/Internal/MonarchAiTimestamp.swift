import Foundation

/// ISO 8601 times as the web writes them: UTC with milliseconds. Shared by
/// the AI conversation, message and tool-call models.
enum MonarchAiTimestamp {
    /// The text of a date, for example `2025-03-10T12:00:00.000Z`.
    static func string(from date: Date) -> String {
        Date.ISO8601FormatStyle(includingFractionalSeconds: true).format(date)
    }

    /// The date of an ISO 8601 text, with or without fractional seconds.
    static func date(from text: String) -> Date? {
        (try? Date.ISO8601FormatStyle(includingFractionalSeconds: true).parse(text))
            ?? (try? Date.ISO8601FormatStyle().parse(text))
    }
}
