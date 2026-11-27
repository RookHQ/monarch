import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiPermissionScope``.
enum MonarchAiPermissionScopeTokens {
    /// The gap between the list and the manage button.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the icon, the names and the access.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the name block and the control when they stack.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding above and below a row.
    static let rowPadding: CGFloat = MonarchTokens.Space.space2
    /// The gap between the app name and its resources.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the access symbol and its text.
    static let accessSpacing: CGFloat = MonarchTokens.Space.space1_5

    /// The symbol of a level, so the level is never colour alone.
    static func symbol(_ level: MonarchAiAccessLevel) -> String {
        switch level {
        case .none: "lock"
        case .read: "eye"
        case .write: "pencil"
        }
    }

    /// The colour of a level's text in the read-only list. No access is
    /// muted; read and write use the default text.
    static func accessTone(_ level: MonarchAiAccessLevel) -> MonarchTone {
        switch level {
        case .none: .neutral
        case .read: .ai
        case .write: .warning
        }
    }

    /// The colour role of the access text.
    static func accessRole(_ level: MonarchAiAccessLevel) -> MonarchColorRole {
        level == .none ? \.textMuted : \.textDefault
    }

    /// The scopes with the row at `index` set to `level`. Out of range
    /// indexes leave the list as it is.
    static func updating(
        _ scopes: [MonarchAiPermissionScopeItem],
        at index: Int,
        to level: MonarchAiAccessLevel
    ) -> [MonarchAiPermissionScopeItem] {
        guard scopes.indices.contains(index) else { return scopes }
        var copy = scopes
        copy[index].access = level
        return copy
    }

    /// What VoiceOver reads for a read-only row: the app with its
    /// resources as the label and the access as the value.
    static func spoken(
        _ item: MonarchAiPermissionScopeItem,
        labels: MonarchAiPermissionScopeLabels
    ) -> (label: String, value: String) {
        let label = [item.app, item.resources].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
        return (label, labels.access(item.access))
    }
}
