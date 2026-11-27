import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchLogViewer``.
enum MonarchLogViewerTokens {
    /// The gap between the toolbar, the log and the footer.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between toolbar controls.
    static let toolbarSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding around each line.
    static let lineHorizontalPadding: CGFloat = MonarchTokens.Space.space2
    /// Padding above and below each line.
    static let lineVerticalPadding: CGFloat = MonarchTokens.Space.space0_5
    /// Padding inside the scrolling area.
    static let viewportPadding: CGFloat = MonarchTokens.Space.space1
    /// The smallest height of the scrolling area: three times the largest spacing step, about twelve lines.
    static let minimumViewportHeight: CGFloat = MonarchTokens.Space.space24 * 3
    /// The corner radius of the scrolling area.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.md
    /// The width of its border.
    static let borderWidth: CGFloat = MonarchShapes.borderWidth
    /// How far a drag toward older lines must move before following pauses, in points.
    static let pauseDragDistance: CGFloat = MonarchTokens.Space.space2
    /// The gap between the search icon and the text.
    static let searchIconSpacing: CGFloat = MonarchTokens.Space.space2
    /// The type role of the lines.
    static let lineTextStyle: MonarchTextStyle = .code
    /// The background of the scrolling area.
    static var viewportBackground: MonarchColorRole { \.bgSubtle }
    /// The border of the scrolling area.
    static var viewportBorder: MonarchColorRole { \.borderSubtle }
    /// The colour of the time, line number and plain level text.
    static var mutedRole: MonarchColorRole { \.textMuted }
    /// The colour of the source.
    static var sourceRole: MonarchColorRole { \.accentStrong }
    /// The background of a search match.
    static var matchBackground: MonarchColorRole { \.warningSubtle }
    /// The SF Symbol of the search field.
    static let searchSymbol = "magnifyingglass"
    /// The SF Symbol of the clear button.
    static let clearSymbol = "xmark.circle.fill"
    /// The SF Symbol of the jump button.
    static let jumpSymbol = "arrow.down"
    /// The SF Symbol of the empty log.
    static let emptySymbol = "text.alignleft"
    /// The id of the invisible view after the last line, scrolled to while following.
    static let bottomID = "monarch.logViewer.bottom"
    /// The separator between the parts VoiceOver reads for a line.
    static let separator = ", "
    /// The gap between columns of a line, in characters.
    static let columnGap = "  "

    /// A line with its 1-based position among all lines.
    struct Entry: Equatable, Identifiable {
        /// The line.
        let line: MonarchLogLine
        /// Its position in the full list, from 1.
        let number: Int

        var id: String { line.id }
    }

    /// The search text as it is matched: trimmed. Matching ignores case.
    static func needle(_ query: String) -> String {
        query.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// Whether the message or the source contains the needle, ignoring case.
    static func matches(_ line: MonarchLogLine, needle: String) -> Bool {
        guard !needle.isEmpty else { return true }
        if line.message.range(of: needle, options: .caseInsensitive) != nil { return true }
        return line.source?.range(of: needle, options: .caseInsensitive) != nil
    }

    /// The lines that are shown, in order, with their positions in the full list.
    static func visible(
        _ lines: [MonarchLogLine],
        levels: Set<MonarchLogLevel>,
        query: String,
        mode: MonarchLogViewerSearchMode
    ) -> [Entry] {
        let needle = needle(query)
        var result: [Entry] = []
        result.reserveCapacity(lines.count)
        for (index, line) in lines.enumerated() where levels.contains(line.level) {
            if mode == .filter, !matches(line, needle: needle) { continue }
            result.append(Entry(line: line, number: index + 1))
        }
        return result
    }

    /// How many lines each level has, for the counts on the filters.
    static func counts(_ lines: [MonarchLogLine]) -> [MonarchLogLevel: Int] {
        var counts = Dictionary(uniqueKeysWithValues: MonarchLogLevel.allCases.map { ($0, 0) })
        for line in lines { counts[line.level, default: 0] += 1 }
        return counts
    }

    /// The ranges of every match of the needle in the text, ignoring case.
    static func matchRanges(of needle: String, in text: String) -> [Range<String.Index>] {
        guard !needle.isEmpty else { return [] }
        var ranges: [Range<String.Index>] = []
        var start = text.startIndex
        while start < text.endIndex,
            let found = text.range(of: needle, options: .caseInsensitive, range: start..<text.endIndex)
        {
            ranges.append(found)
            start = found.upperBound
        }
        return ranges
    }

    /// The time column: 24-hour `HH:mm:ss.SSS` in the time zone.
    static func timeText(_ date: Date, timeZone: TimeZone) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let parts = calendar.dateComponents([.hour, .minute, .second, .nanosecond], from: date)
        let milliseconds = min((parts.nanosecond ?? 0) / 1_000_000, 999)
        return
            "\(pad(parts.hour ?? 0, 2)):\(pad(parts.minute ?? 0, 2)):\(pad(parts.second ?? 0, 2)).\(pad(milliseconds, 3))"
    }

    private static func pad(_ value: Int, _ width: Int) -> String {
        let text = String(value)
        return String(repeating: "0", count: max(0, width - text.count)) + text
    }

    /// The colour of a level's name: muted for debug, the tone's strong colour otherwise.
    static func levelRole(_ level: MonarchLogLevel) -> MonarchColorRole {
        switch level {
        case .debug: \.textMuted
        case .info: \.infoStrong
        case .warn: \.warningStrong
        case .error: \.dangerStrong
        }
    }

    /// The background of a line: tinted for warnings and errors, none otherwise.
    static func lineBackground(_ level: MonarchLogLevel) -> MonarchColorRole? {
        switch level {
        case .debug, .info: nil
        case .warn: \.warningSubtle
        case .error: \.dangerSubtle
        }
    }

    /// What VoiceOver reads for a line: level, time, source and message.
    static func accessibilityLabel(_ line: MonarchLogLine, levelName: String, time: String?) -> String {
        var parts = [levelName]
        if let time { parts.append(time) }
        if let source = line.source, !source.isEmpty { parts.append(source) }
        parts.append(line.message)
        return parts.joined(separator: separator)
    }

    /// The level set after a filter is turned on or off.
    static func toggled(_ levels: Set<MonarchLogLevel>, _ level: MonarchLogLevel, isOn: Bool) -> Set<MonarchLogLevel> {
        var next = levels
        if isOn { next.insert(level) } else { next.remove(level) }
        return next
    }

    /// The lines that arrived since following paused.
    static func unseen(total: Int, pausedAt: Int?) -> Int {
        guard let pausedAt else { return 0 }
        return max(0, total - pausedAt)
    }

    /// The jump button's title: the new lines when some arrived, Jump to latest otherwise.
    static func jumpTitle(unseen: Int, labels: MonarchLogViewerLabels) -> String {
        unseen > 0 ? labels.newLines(unseen) : labels.jumpToLatest
    }

    /// What is announced when following changes.
    static func followAnnouncement(isFollowing: Bool, labels: MonarchLogViewerLabels) -> String {
        isFollowing ? labels.followResumed : labels.followPaused
    }

    /// Whether a drag moves toward older lines: the finger moves down by more than the pause distance.
    static func isDragTowardOlder(_ translation: CGSize) -> Bool {
        translation.height > pauseDragDistance && abs(translation.height) > abs(translation.width)
    }

    /// What the scrolling area shows.
    enum Content: Equatable {
        case loading, empty, noMatches, lines
    }

    /// What the scrolling area shows for the state.
    static func content(isLoading: Bool, total: Int, visible: Int) -> Content {
        if total == 0 { return isLoading ? .loading : .empty }
        return visible == 0 ? .noMatches : .lines
    }

    /// The text of one line: number, time, level, source and message, in
    /// columns separated by spaces, with the matches marked.
    static func lineText(
        _ entry: Entry,
        needle: String,
        showsTimestamps: Bool,
        showsLineNumbers: Bool,
        numberWidth: Int,
        timeZone: TimeZone,
        levelName: String,
        colors: MonarchColors
    ) -> AttributedString {
        let line = entry.line
        var text = AttributedString()
        if showsLineNumbers {
            let number = String(entry.number)
            var part = AttributedString(String(repeating: " ", count: max(0, numberWidth - number.count)) + number)
            part.foregroundColor = colors[role: mutedRole]
            text += part + AttributedString(columnGap)
        }
        if showsTimestamps, let time = line.time {
            var part = AttributedString(timeText(time, timeZone: timeZone))
            part.foregroundColor = colors[role: mutedRole]
            text += part + AttributedString(columnGap)
        }
        var level = AttributedString(levelName)
        level.foregroundColor = colors[role: levelRole(line.level)]
        level.inlinePresentationIntent = .stronglyEmphasized
        text += level + AttributedString(columnGap)
        if let source = line.source, !source.isEmpty {
            var part = marked(source, needle: needle, colors: colors)
            part.foregroundColor = colors[role: sourceRole]
            text += part + AttributedString(columnGap)
        }
        text += marked(line.message, needle: needle, colors: colors)
        return text
    }

    /// The text with every match tinted and underlined, so a match does not rely on colour alone.
    private static func marked(_ string: String, needle: String, colors: MonarchColors) -> AttributedString {
        var text = AttributedString(string)
        for range in matchRanges(of: needle, in: string) {
            let lower = string.distance(from: string.startIndex, to: range.lowerBound)
            let length = string.distance(from: range.lowerBound, to: range.upperBound)
            let start = text.characters.index(text.startIndex, offsetBy: lower)
            let end = text.characters.index(start, offsetBy: length)
            text[start..<end].backgroundColor = colors[role: matchBackground]
            text[start..<end].underlineStyle = Text.LineStyle.single
        }
        return text
    }
}
