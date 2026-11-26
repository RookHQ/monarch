import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchWorkspaceSwitcher``.
enum MonarchWorkspaceSwitcherTokens {
    /// The gap between the avatar, the text and the chevron of the button, and inside a row.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between a row's name and its handle.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The most workspaces shown in a menu. A longer list, or one still loading, opens a searchable sheet.
    static let menuLimit = 5
    /// The SF Symbol of the avatar when no workspace is chosen.
    static let placeholderSymbol = "building.2"
    /// The SF Symbol at the end of the button.
    static let chevronSymbol = "chevron.up.chevron.down"
    /// The SF Symbol beside the chosen row.
    static let checkSymbol = "checkmark"
    /// The SF Symbol of the create action.
    static let createSymbol = "plus"
    /// The SF Symbol of the manage link.
    static let manageSymbol = "gearshape"

    /// How the list is presented.
    enum Presentation: Equatable {
        /// A menu from the button, for a short list.
        case menu
        /// A sheet with a searchable list.
        case sheet
    }

    /// A menu for up to ``menuLimit`` loaded workspaces, otherwise a searchable sheet.
    static func presentation(count: Int, isLoading: Bool) -> Presentation {
        !isLoading && count <= menuLimit ? .menu : .sheet
    }

    /// The workspace that is chosen. An id that is not in the list is none.
    static func current(_ selection: String?, in workspaces: [MonarchWorkspaceOption]) -> MonarchWorkspaceOption? {
        guard let selection else { return nil }
        return workspaces.first { $0.id == selection }
    }

    /// The name on the button: the workspace or the placeholder.
    static func triggerName(_ current: MonarchWorkspaceOption?, labels: MonarchWorkspaceSwitcherLabels) -> String {
        current?.name ?? labels.placeholder ?? MonarchWorkspaceSwitcherStrings.placeholder
    }

    /// The handle as shown, with its `@`, or `nil` when there is none.
    static func handleText(_ workspace: MonarchWorkspaceOption?) -> String? {
        guard let handle = workspace?.handle?.trimmingCharacters(in: .whitespacesAndNewlines), !handle.isEmpty
        else { return nil }
        return handle.hasPrefix("@") ? handle : "@\(handle)"
    }

    /// Whether choosing `id` changes the selection: choosing the current workspace again does nothing.
    static func accepts(_ id: String, current: String?) -> Bool {
        id != current
    }

    /// The workspaces whose name, handle or keywords contain the query, ignoring case and diacritics.
    static func filter(_ workspaces: [MonarchWorkspaceOption], query: String) -> [MonarchWorkspaceOption] {
        let query = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return workspaces }
        return workspaces.filter { workspace in
            [workspace.name, workspace.handle, workspace.keywords].contains { text in
                text?.range(of: query, options: [.caseInsensitive, .diacriticInsensitive]) != nil
            }
        }
    }

    /// The spoken name of a row or the button's value: the name, then the handle.
    static func spokenName(_ workspace: MonarchWorkspaceOption) -> String {
        [workspace.name, handleText(workspace)].compactMap { $0 }.joined(separator: ", ")
    }
}
