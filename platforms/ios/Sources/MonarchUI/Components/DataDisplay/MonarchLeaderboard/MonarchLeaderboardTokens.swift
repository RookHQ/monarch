import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchLeaderboard``.
enum MonarchLeaderboardTokens {
    /// The gap between rows.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the rank, the avatar, the text and the figures.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of a row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The padding inside a row, so the current row's tint has room.
    static let rowPadding: CGFloat = MonarchTokens.Space.space2
    /// The width of the rank column, before Dynamic Type scaling.
    static let rankWidth: CGFloat = MonarchTokens.Space.space6
    /// The colour of the rank number.
    static var rankText: MonarchColorRole { \.textMuted }
    /// The fill of the rank marker of the top three.
    static var topFill: MonarchColorRole { \.bgMuted }
    /// The rank number on that fill.
    static var topText: MonarchColorRole { \.textDefault }
    /// The tint of the viewer's own row.
    static var currentFill: MonarchColorRole { \.accentSubtle }
    /// The border of the viewer's own row.
    static var currentBorder: MonarchColorRole { \.accentBorder }
    /// The colour of names and values.
    static var primaryText: MonarchColorRole { \.textDefault }
    /// The colour of the secondary line, the "(you)" tag and the more line.
    static var secondaryText: MonarchColorRole { \.textMuted }
    /// The number of top ranks that get a filled marker with `highlightsTop`.
    static let highlightedRanks = 3
    /// Placeholder text drawn redacted while loading; never shown or read.
    static let placeholderName = "Placeholder name"

    /// An entry with its rank.
    struct Ranked: Identifiable, Equatable {
        /// The entry.
        let entry: MonarchLeaderboardEntry
        /// Its rank, from 1. Equal values share a rank.
        let rank: Int
        /// The entry's id.
        var id: String { entry.id }
    }

    /// Orders and ranks entries as the web's `rankEntries`: equal values
    /// share a rank and the next rank skips (1, 1, 3). The sort is stable.
    static func rank(_ entries: [MonarchLeaderboardEntry], sort: MonarchBarListSort) -> [Ranked] {
        let ordered: [MonarchLeaderboardEntry]
        switch sort {
        case .none:
            ordered = entries
        case .descending, .ascending:
            ordered = entries.enumerated()
                .sorted { lhs, rhs in
                    if lhs.element.value == rhs.element.value { return lhs.offset < rhs.offset }
                    return sort == .descending
                        ? lhs.element.value > rhs.element.value : lhs.element.value < rhs.element.value
                }
                .map(\.element)
        }
        var result: [Ranked] = []
        for (index, entry) in ordered.enumerated() {
            let previous = result.last
            let rank = previous.map { $0.entry.value == entry.value ? $0.rank : index + 1 } ?? 1
            result.append(Ranked(entry: entry, rank: rank))
        }
        return result
    }

    /// The rows shown: the first `limit` when a positive limit is given.
    static func shown(_ ranked: [Ranked], limit: Int?) -> [Ranked] {
        guard let limit, limit > 0 else { return ranked }
        return Array(ranked.prefix(limit))
    }

    /// How many entries are not shown: from `total` when the entries are already a top N.
    static func hidden(shown: Int, entries: Int, total: Int?) -> Int {
        max(0, (total ?? entries) - shown)
    }

    /// The length of a row's bar as a share of the largest magnitude shown, from 0 to 1.
    static func share(_ value: Double, among shown: [Ranked]) -> Double {
        let maximum = shown.map { abs($0.entry.value) }.max() ?? 0
        guard maximum > 0, value.isFinite else { return 0 }
        return min(1, abs(value) / maximum)
    }

    /// The value as shown: the entry's own text, or the value in the board's format.
    static func valueText(
        _ entry: MonarchLeaderboardEntry,
        format: MonarchMetricFormat,
        isCompact: Bool,
        locale: Locale
    ) -> String {
        entry.display ?? MonarchMetricTokens.format(entry.value, format: format, isCompact: isCompact, locale: locale)
    }

    /// Whether a rank gets the filled marker.
    static func isHighlighted(rank: Int, highlightsTop: Bool) -> Bool {
        highlightsTop && rank <= highlightedRanks
    }

    /// The fill and text roles of a rank marker: filled for the top three
    /// with `highlightsTop`, plain muted text otherwise.
    static func rankMarker(rank: Int, highlightsTop: Bool) -> (fill: MonarchColorRole?, text: MonarchColorRole) {
        isHighlighted(rank: rank, highlightsTop: highlightsTop) ? (topFill, topText) : (nil, rankText)
    }

    /// The spoken change of an entry, as ``MonarchDeltaIndicator`` reads it, or `nil`.
    static func changeLabel(
        _ entry: MonarchLeaderboardEntry,
        comparison: String?,
        goodDirection: MonarchDeltaIndicatorGoodDirection,
        locale: Locale
    ) -> String? {
        guard let change = entry.change else { return nil }
        return MonarchDeltaIndicatorTokens.resolve(
            value: change,
            comparison: comparison,
            goodDirection: goodDirection,
            flatThreshold: 0,
            words: MonarchDeltaIndicatorTokens.Words(),
            format: { MonarchDeltaIndicatorTokens.percent($0, locale: locale) }
        )
        .label
    }

    /// What VoiceOver reads for a row, in reading order:
    /// "Rank 1: Ana Lima, (you), 3 breached, Tier 2, 42 open tickets, up 12%, better".
    static func rowLabel(
        _ ranked: Ranked,
        valueText: String,
        valueLabel: String?,
        changeLabel: String?,
        labels: MonarchLeaderboardLabels
    ) -> String {
        let entry = ranked.entry
        let rank = labels.rank?(ranked.rank) ?? MonarchLeaderboardStrings.rank(ranked.rank)
        var parts = ["\(rank) \(entry.name)"]
        if entry.isCurrent { parts.append(labels.you ?? MonarchLeaderboardStrings.you) }
        if let badge = entry.badge, !badge.isEmpty { parts.append(badge) }
        if let secondary = entry.secondary, !secondary.isEmpty { parts.append(secondary) }
        if let valueLabel, !valueLabel.isEmpty {
            parts.append("\(valueText) \(valueLabel)")
        } else {
            parts.append(valueText)
        }
        if let changeLabel { parts.append(changeLabel) }
        return parts.joined(separator: ", ")
    }

    /// The text for entries not shown.
    static func moreText(_ hidden: Int, labels: MonarchLeaderboardLabels) -> String {
        labels.more?(hidden) ?? MonarchLeaderboardStrings.more(hidden)
    }

    /// What the board shows: loading wins, then a failure, then empty.
    enum Phase: Equatable {
        /// Placeholder rows.
        case loading
        /// The failure message.
        case failed
        /// "Nothing to rank yet".
        case empty
        /// The ranked rows.
        case ready
    }

    /// The phase from the inputs.
    static func phase(isLoading: Bool, errorMessage: String?, shownCount: Int) -> Phase {
        if isLoading { return .loading }
        if let errorMessage, !errorMessage.isEmpty { return .failed }
        return shownCount == 0 ? .empty : .ready
    }
}
