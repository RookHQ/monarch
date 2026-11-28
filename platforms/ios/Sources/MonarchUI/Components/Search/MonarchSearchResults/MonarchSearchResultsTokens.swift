import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchSearchResults``.
enum MonarchSearchResultsTokens {
    /// Space between the summary and the results.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// Space between a group heading's parts.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space2
    /// Leading and trailing inset of a result row; the row pads itself vertically.
    static let rowInset: CGFloat = MonarchTokens.Space.space2
    /// Horizontal padding of the count pill next to a heading.
    static let countPadding: CGFloat = MonarchTokens.Space.space1_5

    /// What the results area shows.
    enum Status: Equatable {
        /// The search failed.
        case error
        /// No results yet and a search is running: skeleton rows.
        case loading
        /// The search finished with no results.
        case empty
        /// Results, possibly while a new search runs.
        case ready
    }

    /// Groups with at least one result, in order.
    static func visibleGroups(_ groups: [MonarchSearchResultGroup]) -> [MonarchSearchResultGroup] {
        groups.filter { !$0.results.isEmpty }
    }

    /// A group's total: the given total, never less than the results shown.
    static func total(of group: MonarchSearchResultGroup) -> Int {
        max(group.total ?? group.results.count, group.results.count)
    }

    /// The total across groups.
    static func total(of groups: [MonarchSearchResultGroup]) -> Int {
        visibleGroups(groups).reduce(0) { $0 + total(of: $1) }
    }

    /// What to show, as on the web: an error wins, then results, then the
    /// skeleton while loading, then the empty state.
    static func status(isFailed: Bool, resultCount: Int, isLoading: Bool) -> Status {
        if isFailed { return .error }
        if resultCount > 0 { return .ready }
        return isLoading ? .loading : .empty
    }

    /// The summary line, also announced: "Searching…" while loading, the
    /// count with results, the empty title when empty, nothing on error.
    static func summary(
        status: Status,
        isLoading: Bool,
        total: Int,
        query: String,
        labels: MonarchSearchResultsLabels
    ) -> String {
        switch status {
        case .error: return ""
        case .loading: return loading(labels: labels)
        case .empty: return emptyTitle(query: query, labels: labels)
        case .ready:
            if isLoading { return loading(labels: labels) }
            if let summary = labels.summary { return summary(total, query) }
            let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
            return trimmed.isEmpty
                ? MonarchSearchResultsStrings.count(total)
                : MonarchSearchResultsStrings.countForQuery(total, query: trimmed)
        }
    }

    /// "Searching…", with the override applied.
    static func loading(labels: MonarchSearchResultsLabels) -> String {
        labels.loading ?? MonarchSearchResultsStrings.loading
    }

    /// The empty state's title, with the override applied.
    static func emptyTitle(query: String, labels: MonarchSearchResultsLabels) -> String {
        if let emptyTitle = labels.emptyTitle { return emptyTitle(query) }
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty
            ? MonarchSearchResultsStrings.emptyTitle
            : MonarchSearchResultsStrings.emptyTitle(query: trimmed)
    }

    /// The empty state's message, with the override applied.
    static func emptyMessage(labels: MonarchSearchResultsLabels) -> String {
        labels.emptyMessage ?? MonarchSearchResultsStrings.emptyMessage
    }

    /// The error state's title, with the override applied.
    static func errorTitle(labels: MonarchSearchResultsLabels) -> String {
        labels.errorTitle ?? MonarchSearchResultsStrings.errorTitle
    }

    /// Whether a group ends with a Show all button: the app handles it and
    /// the group has more matches than it shows.
    static func showsShowAll(_ group: MonarchSearchResultGroup, hasHandler: Bool) -> Bool {
        hasHandler && total(of: group) > group.results.count
    }

    /// The Show all button's title.
    static func showAllTitle(_ group: MonarchSearchResultGroup, labels: MonarchSearchResultsLabels) -> String {
        let total = total(of: group)
        if let showAll = labels.showAll { return showAll(group.title, total) }
        if let title = group.title { return MonarchSearchResultsStrings.showAll(group: title, total: total) }
        return MonarchSearchResultsStrings.showAll(total: total)
    }

    /// The visible count next to a heading.
    static func countText(_ group: MonarchSearchResultGroup) -> String {
        total(of: group).formatted()
    }

    /// What VoiceOver reads for a heading: "Documents, 24 results".
    static func headerAccessibilityLabel(
        _ group: MonarchSearchResultGroup,
        labels: MonarchSearchResultsLabels
    ) -> String {
        let total = total(of: group)
        let count = labels.groupCount?(total) ?? MonarchSearchResultsStrings.count(total)
        return [group.title, count].compactMap { $0 }.joined(separator: ", ")
    }

    /// Placeholder text for a skeleton row, hidden from VoiceOver and redacted.
    static let skeletonTitle = "Placeholder result title"
    /// Placeholder snippet for a skeleton row.
    static let skeletonSnippet = "A placeholder line of text that stands in for a result's snippet."
}
