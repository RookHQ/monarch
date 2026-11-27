import SwiftUI

/// The actions of one row as menu buttons, shared by the actions menu, the
/// context menu and the swipe actions of ``MonarchDataTable``,
/// ``MonarchDataGrid`` and ``MonarchTimesheetGrid``.
struct MonarchRowActionButtons: View {
    let actions: [MonarchDataTableRowAction]

    var body: some View {
        ForEach(actions) { action in
            Button(role: action.isDestructive ? .destructive : nil) {
                action.action()
            } label: {
                if let systemImage = action.systemImage {
                    Label(action.title, systemImage: systemImage)
                } else {
                    Text(action.title)
                }
            }
            .disabled(action.isDisabled)
        }
    }
}

/// The icon-only menu button that holds a row's actions.
struct MonarchRowActionsMenu: View {
    let actions: [MonarchDataTableRowAction]
    let label: String

    /// The SF Symbol of the menu button.
    static let symbol = "ellipsis"

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Menu {
            MonarchRowActionButtons(actions: actions)
        } label: {
            MonarchIcon(systemName: Self.symbol, label: label)
                .foregroundStyle(theme.colors.textMuted)
                .monarchMinimumHitTarget()
        }
        .menuStyle(.button)
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}
