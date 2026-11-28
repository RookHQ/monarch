import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiSummaryCard``.
enum MonarchAiSummaryCardTokens {
    /// The gap between the parts of the card.
    static let gap: CGFloat = MonarchTokens.Space.space3
    /// The gap between the provenance label and the title.
    static let titleGap: CGFloat = MonarchTokens.Space.space1
    /// The gap between the title and Refresh, and between footer items.
    static let headerGap: CGFloat = MonarchTokens.Space.space2
    /// How many placeholder lines show while the first summary is written.
    static let placeholderLines = 3
    /// The SF Symbol of Refresh.
    static let refreshSymbol = "arrow.clockwise"
    /// The colour role of the title.
    static var titleRole: MonarchColorRole { \.textDefault }
    /// The colour role of secondary text.
    static var mutedRole: MonarchColorRole { \.textMuted }

    /// What the body of the card shows.
    enum Body: Equatable {
        case summary
        case placeholder
        case nothing
        case empty
    }

    /// Refresh is busy while a refresh runs or a summary is being written.
    static func isBusy(status: MonarchAiSummaryCardStatus, isRefreshing: Bool) -> Bool {
        isRefreshing || status == .generating
    }

    /// The summary when there is one, a placeholder while busy, nothing after an error, else the empty text.
    static func body(summary: String, isBusy: Bool, status: MonarchAiSummaryCardStatus) -> Body {
        if !summary.allSatisfy(\.isWhitespace) { return .summary }
        if isBusy { return .placeholder }
        if status == .error { return .nothing }
        return .empty
    }

    /// What to announce when busy changes: `generating` when it starts,
    /// `updated` when it ends (nothing after an error), `nil` when nothing
    /// changed, so nothing is announced when the card first appears.
    static func announcement(
        wasBusy: Bool,
        isBusy: Bool,
        status: MonarchAiSummaryCardStatus,
        labels: MonarchAiSummaryCardLabels
    ) -> String? {
        if isBusy, !wasBusy { return labels.generating }
        if !isBusy, wasBusy, status != .error { return labels.updated }
        return nil
    }

    /// The error banner's title and message: the detail under the title, or the title alone as the message.
    static func errorBanner(detail: String?, labels: MonarchAiSummaryCardLabels) -> (title: String?, message: String) {
        if let detail, !detail.allSatisfy(\.isWhitespace) { return (labels.error, detail) }
        return (nil, labels.error)
    }

    /// The visible age, such as "Generated 3 hours ago", relative to `now` in the locale.
    static func generatedLine(
        _ date: Date,
        now: Date,
        locale: Locale,
        labels: MonarchAiSummaryCardLabels
    ) -> String {
        labels.generated(MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true))
    }

    /// The spoken age: the full date and time, which does not go stale.
    static func generatedSpoken(
        _ date: Date,
        locale: Locale,
        timeZone: TimeZone,
        labels: MonarchAiSummaryCardLabels
    ) -> String {
        let style = Date.FormatStyle(date: .long, time: .omitted, locale: locale, timeZone: timeZone).monarchTime()
        return labels.generated(date.formatted(style))
    }

    /// The text behind the redacted placeholder lines: the generating label
    /// repeated, so the bars take the length of real text in the language.
    static func placeholderText(_ generating: String) -> String {
        Array(repeating: generating, count: placeholderLines * placeholderLines).joined(separator: " ")
    }
}
