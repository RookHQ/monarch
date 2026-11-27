import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiToolCall``.
enum MonarchAiToolCallTokens {
    /// Padding inside the card.
    static let padding: CGFloat = MonarchTokens.Space.space3
    /// The gap between the parts of the card and of the details.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the icon, the title and the state.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space2
    /// The width of the card's border.
    static let borderWidth: CGFloat = MonarchTokens.Space.px
    /// The default tool symbol.
    static let toolSymbol = "wrench.and.screwdriver"
    /// The symbol before the error line.
    static let errorSymbol = "exclamationmark.octagon"
    /// The card fill.
    static var fill: MonarchColorRole { \.bgSubtle }
    /// The card border.
    static var border: MonarchColorRole { \.borderSubtle }
    /// The tool icon.
    static var toolIcon: MonarchColorRole { \.aiTool }
    /// The error text and icon.
    static var errorText: MonarchColorRole { \.dangerStrong }
    /// Secondary text: the duration, the tool line, the empty and truncation notes.
    static var secondaryText: MonarchColorRole { \.textMuted }

    // MARK: State

    /// The tone and symbol of a state, so it is never told by colour alone.
    static func stateLook(_ state: MonarchAiToolCallState) -> (tone: MonarchTone, symbol: String) {
        switch state {
        case .pendingApproval: (.warning, "exclamationmark.triangle")
        case .awaitingResult: (.neutral, "clock")
        case .running: (.info, "arrow.triangle.2.circlepath")
        case .succeeded: (.success, "checkmark.circle")
        case .failed: (.danger, "xmark.octagon")
        case .denied: (.neutral, "xmark.circle")
        case .cancelled: (.neutral, "minus.circle")
        }
    }

    /// The name of a state.
    static func stateName(_ state: MonarchAiToolCallState, labels: MonarchAiToolCallLabels) -> String {
        labels.state?(state) ?? MonarchAiToolCallStrings.state(state)
    }

    // MARK: Names

    /// A tool name as words: `search_documents`, `searchDocuments` and
    /// `search-documents` all become "Search documents".
    static func humanizedName(_ name: String) -> String {
        var spaced = ""
        var previous: Character?
        for character in name {
            if let previous, character.isUppercase, previous.isLowercase || previous.isNumber {
                spaced.append(" ")
            }
            spaced.append("_-.:/".contains(character) ? " " : character)
            previous = character
        }
        let words = spaced.split(separator: " ").joined(separator: " ").lowercased()
        guard let first = words.first else { return name }
        return first.uppercased() + words.dropFirst()
    }

    /// The display name of a tool: its entry in `toolLabels`, else the humanized name.
    static func toolLabel(_ name: String, toolLabels: [String: String]) -> String {
        toolLabels[name] ?? humanizedName(name)
    }

    /// The summary row's title: `summary`, then `describeTool`, then the tool's label.
    static func title(summary: String?, described: String?, label: String) -> String {
        [summary, described].compactMap { $0 }.first { !$0.isEmpty } ?? label
    }

    // MARK: Values

    /// A value as display text. Objects and arrays are pretty JSON; a string
    /// that holds JSON is re-indented; any other string is shown as it is.
    static func formatted(_ value: MonarchAiJSON) -> (text: String, isJSON: Bool) {
        switch value {
        case .string(let text):
            let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
            if let first = trimmed.first, first == "{" || first == "[",
                let data = trimmed.data(using: .utf8),
                (try? JSONDecoder().decode(MonarchAiJSON.self, from: data)) != nil,
                let pretty = reindented(trimmed)
            {
                // Re-indented from the text itself, so the keys keep the order they were written in.
                return (pretty, true)
            }
            return (text, false)
        default:
            if let pretty = prettyJSON(value) { return (pretty, true) }
            return ("", false)
        }
    }

    /// Valid JSON text re-indented with two spaces, keeping key order and writing `"key": value`.
    static func reindented(_ json: String) -> String? {
        var output = ""
        var depth = 0
        var inString = false
        var escaped = false
        var characters = Array(json)
        func newline() { output += "\n" + String(repeating: "  ", count: depth) }
        var index = 0
        while index < characters.count {
            let character = characters[index]
            if inString {
                output.append(character)
                if escaped {
                    escaped = false
                } else if character == "\\" {
                    escaped = true
                } else if character == "\"" {
                    inString = false
                }
            } else if character == "\"" {
                inString = true
                output.append(character)
            } else if character == "{" || character == "[" {
                var next = index + 1
                while next < characters.count, characters[next].isWhitespace { next += 1 }
                let close: Character = character == "{" ? "}" : "]"
                output.append(character)
                if next < characters.count, characters[next] == close {
                    output.append(close)
                    index = next
                } else {
                    depth += 1
                    newline()
                }
            } else if character == "}" || character == "]" {
                depth -= 1
                newline()
                output.append(character)
            } else if character == "," {
                output.append(character)
                newline()
            } else if character == ":" {
                output += ": "
            } else if !character.isWhitespace {
                output.append(character)
            }
            index += 1
        }
        characters.removeAll()
        return depth == 0 && !inString ? output : nil
    }

    private static func prettyJSON(_ value: MonarchAiJSON) -> String? {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys, .withoutEscapingSlashes]
        guard let data = try? encoder.encode(value), let text = String(data: data, encoding: .utf8) else { return nil }
        // `"key": value`, not the encoder's `"key" : value`, as the web writes it.
        return reindented(text) ?? text
    }

    /// Display text cut to `max` characters, at a line end when one is near
    /// the cut. `max` of 0 or less keeps everything.
    static func truncated(_ text: String, max: Int) -> (text: String, isTruncated: Bool) {
        guard max > 0, text.count > max else { return (text, false) }
        let head = text.prefix(max)
        if let newline = head.lastIndex(of: "\n") {
            let length = head.distance(from: head.startIndex, to: newline)
            if Double(length) > Double(max) * 0.8 {
                return (String(head[..<newline]), true)
            }
        }
        return (String(head), true)
    }

    /// The input to show: the raw streamed text while the parsed input is
    /// still empty, else the parsed input, else nothing.
    static func inputValue(_ part: MonarchAiToolCallPart) -> MonarchAiJSON? {
        let isEmptyInput: Bool
        switch part.input {
        case .null: isEmptyInput = true
        case .object(let members): isEmptyInput = members.isEmpty
        default: isEmptyInput = false
        }
        if let text = part.inputText, !text.isEmpty, isEmptyInput { return .string(text) }
        if case .null = part.input { return nil }
        return part.input
    }

    // MARK: Duration

    /// The seconds between `startedAt` and `endedAt`, or `nil` while either is
    /// missing or unreadable, or the end is before the start.
    static func duration(_ part: MonarchAiToolCallPart) -> TimeInterval? {
        guard let start = date(part.startedAt), let end = date(part.endedAt) else { return nil }
        let seconds = end.timeIntervalSince(start)
        return seconds >= 0 ? seconds : nil
    }

    /// An ISO 8601 time, with or without fractions of a second.
    static func date(_ text: String?) -> Date? {
        guard let text, !text.isEmpty else { return nil }
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = formatter.date(from: text) { return date }
        formatter.formatOptions = [.withInternetDateTime]
        return formatter.date(from: text)
    }

    /// How long the call took: "0.4s" under a second (at least 0.1s), "12s"
    /// under a minute, then "2m 05s".
    static func durationText(_ seconds: TimeInterval, locale: Locale, labels: MonarchAiToolCallLabels) -> String {
        if let format = labels.formatDuration { return format(seconds) }
        if seconds < 1 {
            let value = max(seconds, 0.1)
            return MonarchAiToolCallStrings.seconds(
                value.formatted(.number.precision(.fractionLength(1)).locale(locale))
            )
        }
        let whole = Int(seconds.rounded())
        if whole < 60 {
            return MonarchAiToolCallStrings.seconds(whole.formatted(.number.locale(locale)))
        }
        return MonarchAiToolCallStrings.minutesSeconds(
            (whole / 60).formatted(.number.locale(locale)),
            (whole % 60).formatted(.number.precision(.integerLength(2)).locale(locale))
        )
    }

    // MARK: Accessibility

    /// What VoiceOver reads for the summary row: the title, the state and the duration.
    static func rowLabel(title: String, state: String, duration: String?) -> String {
        [title, state, duration].compactMap { $0 }.joined(separator: ", ")
    }

    /// The announcement for a state change.
    static func announcement(label: String, state: String, labels: MonarchAiToolCallLabels) -> String {
        labels.stateChanged?(label, state) ?? MonarchAiToolCallStrings.stateChanged(label, state)
    }

    /// The note under a truncated value.
    static func truncatedNote(shown: Int, total: Int, locale: Locale, labels: MonarchAiToolCallLabels) -> String {
        labels.truncated?(shown, total)
            ?? MonarchAiToolCallStrings.truncated(
                shown.formatted(.number.locale(locale)),
                total.formatted(.number.locale(locale))
            )
    }
}
