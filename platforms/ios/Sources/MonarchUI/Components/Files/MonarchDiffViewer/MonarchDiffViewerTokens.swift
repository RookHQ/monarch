import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchDiffViewer``.
enum MonarchDiffViewerTokens {
    /// The gap between the header and the lines.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the header's parts.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the number columns, the marker and the text.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding before and after a line.
    static let lineHorizontalPadding: CGFloat = MonarchTokens.Space.space2
    /// Padding above and below a line.
    static let lineVerticalPadding: CGFloat = MonarchTokens.Space.space0_5
    /// The width of the bar that marks the current change.
    static let currentBarWidth: CGFloat = MonarchTokens.Space.space0_5 + MonarchTokens.Space.px
    /// The fewest digits a line number column holds.
    static let minimumNumberDigits = 2
    /// The unchanged lines kept around each change by default.
    static let defaultContext = 3
    /// The SF Symbol of the previous change button.
    static let previousSymbol = "chevron.up"
    /// The SF Symbol of the next change button.
    static let nextSymbol = "chevron.down"
    /// The SF Symbol of the button that shows a folded run.
    static let expandSymbol = "arrow.up.and.down"
    /// The weight of the totals.
    static let statsWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// What separates the parts of spoken text.
    static let spokenSeparator = ": "
    /// What separates the two sides of a spoken row.
    static let sideSeparator = ". "
    /// The border of the lines.
    static var border: MonarchColorRole { \.borderSubtle }
    /// The colour of line text.
    static var text: MonarchColorRole { \.textDefault }
    /// The colour of line numbers.
    static var number: MonarchColorRole { \.textSubtle }
    /// The fill of a folded run and of an empty side.
    static var gapBackground: MonarchColorRole { \.bgSubtle }
    /// The colour of the "no changes" message.
    static var muted: MonarchColorRole { \.textMuted }
    /// The bar that marks the current change.
    static var currentBar: MonarchColorRole { \.accentSolid }

    /// The fill behind a line: success subtle for added, danger subtle for removed.
    static func lineBackground(_ kind: MonarchDiffViewerLineKind) -> MonarchColorRole? {
        switch kind {
        case .same: nil
        case .added: \.successSubtle
        case .removed: \.dangerSubtle
        }
    }

    /// The fill behind a changed word, stronger than its line's.
    static func wordBackground(_ kind: MonarchDiffViewerLineKind) -> MonarchColorRole? {
        switch kind {
        case .same: nil
        case .added: \.successBorder
        case .removed: \.dangerBorder
        }
    }

    /// The colour of a line's marker and of a total.
    static func markerRole(_ kind: MonarchDiffViewerLineKind) -> MonarchColorRole {
        switch kind {
        case .same: \.textSubtle
        case .added: \.successStrong
        case .removed: \.dangerStrong
        }
    }

    /// The visible marker of a line, so a change never depends on colour.
    static func marker(_ kind: MonarchDiffViewerLineKind) -> String {
        switch kind {
        case .same: " "
        case .added: "+"
        case .removed: "\u{2212}"
        }
    }

    /// The visible totals: "+3" and "−1".
    static func statsText(added: Int) -> String { "+\(added)" }

    /// The visible removed total.
    static func statsText(removed: Int) -> String { "\u{2212}\(removed)" }

    /// The layout in use: the app's, else the reader's choice, else side by
    /// side at regular width and unified otherwise.
    static func mode(
        bound: MonarchDiffViewerMode?,
        chosen: MonarchDiffViewerMode?,
        isRegularWidth: Bool
    ) -> MonarchDiffViewerMode {
        bound ?? chosen ?? (isRegularWidth ? .split : .unified)
    }

    /// The folding context: nil never folds, negative values are zero.
    static func context(_ context: Int?) -> Int {
        guard let context else { return Int.max / 4 }
        return max(0, context)
    }

    /// The digits of the widest line number.
    static func numberDigits(_ lines: [MonarchDiffViewerLine]) -> Int {
        let largest = lines.reduce(0) { max($0, $1.oldNumber ?? 0, $1.newNumber ?? 0) }
        return max(minimumNumberDigits, String(largest).count)
    }

    /// A line number padded to the column width, or spaces for no number.
    static func numberText(_ number: Int?, digits: Int) -> String {
        let text = number.map(String.init) ?? ""
        return String(repeating: " ", count: max(0, digits - text.count)) + text
    }

    /// The pieces of a changed line drawn on one side: the old side leaves
    /// out added words, the new side removed words.
    static func visibleSegments(
        _ segments: [MonarchDiffSegment],
        forKind kind: MonarchDiffViewerLineKind
    ) -> [MonarchDiffSegment] {
        let hidden: MonarchDiffViewerLineKind = kind == .removed ? .added : .removed
        return segments.filter { $0.kind != hidden }
    }

    /// What VoiceOver reads for a line: its text after "Added" or "Removed",
    /// and "Blank line" for an empty one.
    static func spokenLine(_ line: MonarchDiffViewerLine, labels: MonarchDiffViewerLabels) -> String {
        let text =
            line.text.allSatisfy(\.isWhitespace) ? (labels.blankLine ?? MonarchDiffViewerStrings.blankLine) : line.text
        switch line.kind {
        case .same: return text
        case .added: return (labels.added ?? MonarchDiffViewerStrings.added) + spokenSeparator + text
        case .removed: return (labels.removed ?? MonarchDiffViewerStrings.removed) + spokenSeparator + text
        }
    }

    /// What VoiceOver reads for a side by side row: an unchanged line once,
    /// otherwise each side after its name.
    static func spokenPair(
        old: MonarchDiffViewerLine?,
        new: MonarchDiffViewerLine?,
        beforeName: String,
        afterName: String,
        labels: MonarchDiffViewerLabels
    ) -> String {
        if let old, let new, old == new { return spokenLine(old, labels: labels) }
        var parts: [String] = []
        if let old { parts.append(beforeName + spokenSeparator + spokenLine(old, labels: labels)) }
        if let new { parts.append(afterName + spokenSeparator + spokenLine(new, labels: labels)) }
        return parts.joined(separator: sideSeparator)
    }

    /// The spoken totals.
    static func statsLabel(added: Int, removed: Int, labels: MonarchDiffViewerLabels) -> String {
        labels.stats?(added, removed) ?? MonarchDiffViewerStrings.stats(added, removed)
    }

    /// The spoken name of the diff.
    static func caption(before: String, after: String, labels: MonarchDiffViewerLabels) -> String {
        labels.caption?(before, after) ?? MonarchDiffViewerStrings.caption(before, after)
    }

    /// The title of the button that shows a folded run.
    static func expandTitle(_ count: Int, labels: MonarchDiffViewerLabels) -> String {
        labels.expand?(count) ?? MonarchDiffViewerStrings.expand(count)
    }

    /// Announced when moving to a change; `index` starts at zero.
    static func positionText(_ index: Int, of total: Int, labels: MonarchDiffViewerLabels) -> String {
        labels.position?(index + 1, total) ?? MonarchDiffViewerStrings.position(index + 1, total)
    }

    /// The change before the current one, wrapping to the last. With no
    /// current change it is the last.
    static func previous(from position: Int?, hunks: Int) -> Int? {
        guard hunks > 0 else { return nil }
        guard let position, position > 0 else { return hunks - 1 }
        return position - 1
    }

    /// The change after the current one, wrapping to the first. With no
    /// current change it is the first.
    static func next(from position: Int?, hunks: Int) -> Int? {
        guard hunks > 0 else { return nil }
        guard let position else { return 0 }
        return (position + 1) % hunks
    }
}
