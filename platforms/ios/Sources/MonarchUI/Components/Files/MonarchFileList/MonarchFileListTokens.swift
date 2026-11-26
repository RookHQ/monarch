import MonarchTokens
import SwiftUI

/// Component tokens and selection logic for ``MonarchFileList``.
enum MonarchFileListTokens {
    /// The gap between files.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the toolbar, the bar and the files.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The narrowest card in the grid before Dynamic Type.
    static let minimumCardWidth: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space16
    /// The SF Symbol of the empty state.
    static let emptySymbol = "doc"

    /// The ids select all acts on: the files that are not disabled.
    static func selectableIDs(_ items: [MonarchFileListItem]) -> [String] {
        items.filter { !$0.isDisabled }.map(\.id)
    }

    /// How many of the files are selected. Ids of files not in the list are not counted.
    static func selectedCount(_ items: [MonarchFileListItem], selection: Set<String>) -> Int {
        items.filter { selection.contains($0.id) }.count
    }

    /// The selection with one file selected or not.
    static func setting(_ id: String, isOn: Bool, in selection: Set<String>) -> Set<String> {
        var next = selection
        if isOn { next.insert(id) } else { next.remove(id) }
        return next
    }

    /// The selection after Escape or Clear: empty.
    static func cleared(_ selection: Set<String>) -> Set<String> { [] }
}
