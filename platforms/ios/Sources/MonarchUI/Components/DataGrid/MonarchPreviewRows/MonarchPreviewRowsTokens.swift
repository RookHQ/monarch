import MonarchTokens
import SwiftUI

/// One filter tab of ``MonarchPreviewRows``: the filter, its title and its count.
struct MonarchPreviewRowsOption: Equatable {
    let filter: MonarchPreviewRowsFilter
    let title: String
    let count: Int
    let countTone: MonarchTone
}

/// Component tokens and pure logic of ``MonarchPreviewRows``.
enum MonarchPreviewRowsTokens {
    /// The gap between the filter tabs, the rows and the load more control.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the rows.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the parts of a row.
    static let partSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between a value's label and the value, and between an icon and its text.
    static let pairSpacing: CGFloat = MonarchTokens.Space.space2
    /// The number of columns a row's cells are set in.
    static let gridColumns = 3
    /// The gap between the lines of the issues.
    static let issueSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding inside a row.
    static let padding: CGFloat = MonarchTokens.Space.space3
    /// The weight of a row's title.
    static let titleWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The visible mark of a missing value.
    static let emptyMark = "\u{2014}"
    /// What separates the parts of spoken text.
    static let spokenSeparator = ". "
    /// What separates a field from its message.
    static let fieldSeparator = ": "
    /// The fill of a row.
    static var background: MonarchColorRole { \.bgSurface }
    /// The border of a row.
    static var border: MonarchColorRole { \.borderSubtle }
    /// The colour of a row's title and values.
    static var text: MonarchColorRole { \.textDefault }
    /// The colour of value labels, the empty mark and the note.
    static var muted: MonarchColorRole { \.textMuted }
    /// The order of the status tabs after All: problems first.
    static let filterOrder: [MonarchPreviewRowStatus] = [.error, .warning, .duplicate, .ready]

    /// The tone of a status: ready is success, warning warning, error danger and duplicate info.
    static func tone(_ status: MonarchPreviewRowStatus) -> MonarchTone {
        switch status {
        case .ready: .success
        case .warning: .warning
        case .error: .danger
        case .duplicate: .info
        }
    }

    /// The SF Symbol of a status, so the status never depends on colour.
    static func symbol(_ status: MonarchPreviewRowStatus) -> String {
        switch status {
        case .ready: "checkmark.circle"
        case .warning: "exclamationmark.triangle"
        case .error: "xmark.octagon"
        case .duplicate: "doc.on.doc"
        }
    }

    /// The colour of an issue line: error issues in danger, warnings in warning.
    static func issueRole(isError: Bool) -> MonarchColorRole {
        isError ? \.dangerStrong : \.warningStrong
    }

    /// The name of a status.
    static func statusName(_ status: MonarchPreviewRowStatus, labels: MonarchPreviewRowsLabels) -> String {
        switch status {
        case .ready: labels.ready ?? MonarchPreviewRowsStrings.ready
        case .warning: labels.warning ?? MonarchPreviewRowsStrings.warning
        case .error: labels.error ?? MonarchPreviewRowsStrings.error
        case .duplicate: labels.duplicate ?? MonarchPreviewRowsStrings.duplicate
        }
    }

    /// The title of a row: "Row 4".
    static func rowTitle(_ row: Int, labels: MonarchPreviewRowsLabels) -> String {
        labels.row?(row) ?? MonarchPreviewRowsStrings.row(row)
    }

    /// The number of rows of each status. Every status has an entry.
    static func counts(_ rows: [MonarchPreviewRow]) -> [MonarchPreviewRowStatus: Int] {
        var counts = Dictionary(uniqueKeysWithValues: MonarchPreviewRowStatus.allCases.map { ($0, 0) })
        for row in rows { counts[row.resolvedStatus, default: 0] += 1 }
        return counts
    }

    /// The rows a filter shows, in their order.
    static func filtered(_ rows: [MonarchPreviewRow], by filter: MonarchPreviewRowsFilter) -> [MonarchPreviewRow] {
        switch filter {
        case .all: rows
        case .status(let status): rows.filter { $0.resolvedStatus == status }
        }
    }

    /// Whether the filter tabs are shown: when filtering is on and some row
    /// is not ready, or a filter other than All is chosen.
    static func showsFilter(
        isFilterable: Bool,
        counts: [MonarchPreviewRowStatus: Int],
        filter: MonarchPreviewRowsFilter
    ) -> Bool {
        guard isFilterable else { return false }
        let problems = (counts[.error] ?? 0) + (counts[.warning] ?? 0) + (counts[.duplicate] ?? 0)
        return problems > 0 || filter != .all
    }

    /// The filter tabs: All, then each status that has rows (or is chosen), problems first.
    static func options(
        rows: [MonarchPreviewRow],
        filter: MonarchPreviewRowsFilter,
        labels: MonarchPreviewRowsLabels
    ) -> [MonarchPreviewRowsOption] {
        let counts = counts(rows)
        var options = [
            MonarchPreviewRowsOption(
                filter: .all,
                title: labels.all ?? MonarchPreviewRowsStrings.all,
                count: rows.count,
                countTone: .neutral
            )
        ]
        for status in filterOrder where (counts[status] ?? 0) > 0 || filter == .status(status) {
            options.append(
                MonarchPreviewRowsOption(
                    filter: .status(status),
                    title: statusName(status, labels: labels),
                    count: counts[status] ?? 0,
                    countTone: countTone(status)
                )
            )
        }
        return options
    }

    /// The tone of a tab's count: danger for errors, warning for warnings, neutral otherwise.
    static func countTone(_ status: MonarchPreviewRowStatus) -> MonarchTone {
        switch status {
        case .error: .danger
        case .warning: .warning
        case .ready, .duplicate: .neutral
        }
    }

    /// How many of `total` rows are shown after `pages` pages. A page size of
    /// zero or less shows every row.
    static func visibleCount(total: Int, pageSize: Int, pages: Int) -> Int {
        guard pageSize > 0 else { return total }
        return min(total, pageSize * max(1, pages))
    }

    /// A value as it is drawn, or `nil` when it is missing or empty.
    static func value(_ value: String?) -> String? {
        guard let value, !value.isEmpty else { return nil }
        return value
    }

    /// The visible text of an issue: "Email: Not a valid address".
    static func issueText(_ issue: MonarchPreviewRowIssue) -> String {
        guard let field = issue.field, !field.isEmpty else { return issue.message }
        return field + fieldSeparator + issue.message
    }

    /// What VoiceOver reads as the value of a row, after its title: the
    /// status, each column with its value, each issue after its kind, and the note.
    static func spokenValue(
        _ row: MonarchPreviewRow,
        columns: [MonarchPreviewRowsColumn],
        labels: MonarchPreviewRowsLabels
    ) -> String {
        var parts = [statusName(row.resolvedStatus, labels: labels)]
        for column in columns {
            let shown = value(row.values[column.id]) ?? (labels.emptyValue ?? MonarchPreviewRowsStrings.emptyValue)
            parts.append(column.label + fieldSeparator + shown)
        }
        let errorName = statusName(.error, labels: labels)
        let warningName = statusName(.warning, labels: labels)
        parts += row.errors.map { errorName + fieldSeparator + issueText($0) }
        parts += row.warnings.map { warningName + fieldSeparator + issueText($0) }
        if let note = row.note, !note.isEmpty { parts.append(note) }
        return parts.joined(separator: spokenSeparator)
    }
}
