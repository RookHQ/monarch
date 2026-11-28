import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchTableToolbar``.
enum MonarchTableToolbarTokens {
    /// The gap between the filters, the count and the controls.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the two lines of the narrow layout.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space2
    /// The colour of the result count.
    static var countColor: MonarchColorRole { \.textMuted }

    /// The result count text.
    static func resultsText(_ count: Int, labels: MonarchTableToolbarLabels) -> String {
        labels.results?(count) ?? MonarchTableToolbarStrings.results(count)
    }

    /// Whether the bulk selection bar replaces the filters: the app can clear
    /// the selection and something is selected.
    static func isSelecting(selectedCount: Int, canClear: Bool) -> Bool {
        canClear && selectedCount > 0
    }

    /// What is announced when the result count changes, or `nil` when it does
    /// not change or there is no count.
    static func announcement(from old: Int?, to new: Int?, labels: MonarchTableToolbarLabels) -> String? {
        guard let new, old != new else { return nil }
        return resultsText(new, labels: labels)
    }
}
