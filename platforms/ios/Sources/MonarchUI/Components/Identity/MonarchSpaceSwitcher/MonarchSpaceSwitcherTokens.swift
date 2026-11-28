import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchSpaceSwitcher``.
enum MonarchSpaceSwitcherTokens {
    /// The gap between the icon, the text and the chevron of the button, and inside a row.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between a row's name and its description.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The SF Symbol of "All spaces", and of a Space without its own.
    static let allSymbol = "square.3.layers.3d"
    /// The SF Symbol at the end of the button.
    static let chevronSymbol = "chevron.up.chevron.down"
    /// The SF Symbol beside the chosen row.
    static let checkSymbol = "checkmark"
    /// The SF Symbol of the create action.
    static let createSymbol = "plus"
    /// The SF Symbol of the manage link.
    static let manageSymbol = "gearshape"

    /// The Space that is chosen. An id that is not in the list (deleted, access removed) is none.
    static func current(_ selection: String?, in spaces: [MonarchSpaceOption]) -> MonarchSpaceOption? {
        guard let selection else { return nil }
        return spaces.first { $0.id == selection }
    }

    /// The name on the button: the Space, "All spaces", or the placeholder when all spaces is not offered.
    static func triggerName(
        _ current: MonarchSpaceOption?,
        allowsAll: Bool,
        labels: MonarchSpaceSwitcherLabels
    ) -> String {
        if let current { return current.name }
        return allowsAll
            ? labels.all ?? MonarchSpaceSwitcherStrings.all
            : labels.placeholder ?? MonarchSpaceSwitcherStrings.placeholder
    }

    /// The SF Symbol on the button.
    static func triggerSymbol(_ current: MonarchSpaceOption?) -> String {
        current?.systemImage ?? allSymbol
    }

    /// Whether the switcher is shown: not when there are no Spaces, nothing loads and it hides when empty.
    static func isVisible(count: Int, isLoading: Bool, hidesWhenEmpty: Bool) -> Bool {
        count > 0 || isLoading || !hidesWhenEmpty
    }

    /// Whether choosing `id` changes the selection: "All spaces" (`nil`) only when it is offered.
    static func accepts(_ id: String?, allowsAll: Bool) -> Bool {
        id != nil || allowsAll
    }

    /// The Spaces whose name or description contains the query, ignoring case and diacritics.
    static func filter(_ spaces: [MonarchSpaceOption], query: String) -> [MonarchSpaceOption] {
        let query = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return spaces }
        return spaces.filter { space in
            [space.name, space.description].contains { text in
                text?.range(of: query, options: [.caseInsensitive, .diacriticInsensitive]) != nil
            }
        }
    }

    /// Whether "All spaces" is listed: when it is offered and no search is typed.
    static func showsAll(allowsAll: Bool, query: String) -> Bool {
        allowsAll && query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    /// The spoken name of a row: the name, then the description.
    static func rowLabel(title: String, description: String?) -> String {
        [title, description]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: ", ")
    }
}
