import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and selection logic for ``MonarchAppAccessPicker`` and ``MonarchAppAccessSummary``.
enum MonarchAppAccessPickerTokens {
    /// The gap between the label, the toolbar, the rows and the help text.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between rows.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding before and after a row's content.
    static let rowHorizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding above and below a row's content.
    static let rowVerticalPadding: CGFloat = MonarchTokens.Space.space1
    /// The gap between the logo and the text.
    static let logoSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the name and its description.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the name and the badge, and between summary tags.
    static let badgeSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The corner radius of a row.
    static let radius: CGFloat = MonarchTokens.Radius.md
    /// The number of placeholder rows while loading.
    static let loadingRowCount = 3

    /// What the picker shows.
    enum Content: Equatable {
        /// The placeholder rows.
        case loading
        /// The empty message.
        case empty
        /// The rows.
        case rows
    }

    /// What the picker shows while loading or for a number of apps.
    static func content(isLoading: Bool, count: Int) -> Content {
        if isLoading { return .loading }
        return count == 0 ? .empty : .rows
    }

    /// The selection with one app turned on or off.
    static func setting(_ id: String, isOn: Bool, in selection: Set<String>) -> Set<String> {
        var next = selection
        if isOn { next.insert(id) } else { next.remove(id) }
        return next
    }

    /// The ids of the apps that can be changed here.
    static func changeableIDs(_ apps: [MonarchAppAccessOption]) -> [String] {
        apps.filter { !$0.isDisabled }.map(\.id)
    }

    /// Whether every app that can be changed is selected. False when none can be.
    static func isAllSelected(_ apps: [MonarchAppAccessOption], selection: Set<String>) -> Bool {
        let changeable = changeableIDs(apps)
        return !changeable.isEmpty && changeable.allSatisfy(selection.contains)
    }

    /// The selection after the bulk button: every changeable app cleared when
    /// all are selected, otherwise all selected. Apps that cannot be changed
    /// keep their state.
    static func toggledAll(_ apps: [MonarchAppAccessOption], selection: Set<String>) -> Set<String> {
        let changeable = Set(changeableIDs(apps))
        return isAllSelected(apps, selection: selection)
            ? selection.subtracting(changeable) : selection.union(changeable)
    }

    /// The number of listed apps that are selected.
    static func selectedCount(_ apps: [MonarchAppAccessOption], selection: Set<String>) -> Int {
        apps.filter { selection.contains($0.id) }.count
    }

    /// The fill of a row: one neutral step darker when selected.
    static func rowFill(isSelected: Bool) -> MonarchColorRole {
        isSelected ? \.bgMuted : \.bgSubtle
    }

    /// The border of a row: the accent border when selected, none otherwise.
    static func rowBorder(isSelected: Bool) -> MonarchColorRole? {
        isSelected ? \.accentBorder : nil
    }

    /// The colour of an app's description.
    static func descriptionColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// The names a summary shows and the ones it hides, sorted: ids without
    /// a matching app show the id; at least one name is shown.
    static func summary(
        _ apps: [MonarchAppAccessOption],
        granted: Set<String>,
        maxVisible: Int
    ) -> (shown: [String], hidden: [String]) {
        let names =
            granted
            .map { id in apps.first { $0.id == id }?.name ?? id }
            .sorted { $0.localizedStandardCompare($1) == .orderedAscending }
        let count = max(1, maxVisible)
        return (Array(names.prefix(count)), Array(names.dropFirst(count)))
    }

    /// What VoiceOver reads for a summary: every name, or "No app access".
    static func summaryLabel(shown: [String], hidden: [String], labels: MonarchAppAccessPickerLabels) -> String {
        guard !shown.isEmpty else { return labels.none ?? MonarchAppAccessPickerStrings.none }
        let visible = shown.joined(separator: ", ")
        guard !hidden.isEmpty else { return visible }
        let names = hidden.joined(separator: ", ")
        let more = labels.more?(hidden.count, names) ?? MonarchAppAccessPickerStrings.more(hidden.count, names: names)
        return "\(visible), \(more)"
    }

    /// The selection count text.
    static func countText(selected: Int, total: Int, labels: MonarchAppAccessPickerLabels) -> String {
        labels.count?(selected, total) ?? MonarchAppAccessPickerStrings.count(selected: selected, total: total)
    }
}
