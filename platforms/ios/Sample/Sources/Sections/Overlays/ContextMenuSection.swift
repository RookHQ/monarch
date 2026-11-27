import MonarchUI
import SwiftUI

/// Native adoption: `contextMenu`, opened by a long press.
struct ContextMenuSection: CataloguePage {
    static let title = "Context menu (contextMenu)"

    @Environment(\.monarchTheme) private var theme
    @State private var last = "Touch and hold the card"

    var body: some View {
        Text(last).monarchTextStyle(.bodyStrong)

        CatalogueCaption(
            "Touch and hold. The same actions are in the menu button, so they do not depend on the gesture."
        )
        HStack {
            VStack(alignment: .leading, spacing: theme.spacing.space1) {
                Text("Quarterly report.pdf").monarchTextStyle(.bodyStrong)
                Text("2.4 MB").monarchTextStyle(.caption).foregroundStyle(theme.colors.textMuted)
            }
            Spacer()
            Menu {
                actions
            } label: {
                MonarchIcon(systemName: "ellipsis.circle", label: "Actions for Quarterly report.pdf")
            }
            .buttonStyle(.monarch(.ghost))
        }
        .padding(theme.spacing.space4)
        .background(theme.colors.bgSurface, in: RoundedRectangle(cornerRadius: theme.radius.lg))
        .contentShape(.contextMenuPreview, RoundedRectangle(cornerRadius: theme.radius.lg))
        .contextMenu { actions }
    }

    @ViewBuilder private var actions: some View {
        Button {
            last = "Renamed"
        } label: {
            Label("Rename", systemImage: "pencil")
        }
        Button {
            last = "Duplicated"
        } label: {
            Label("Duplicate", systemImage: "plus.square.on.square")
        }
        Divider()
        Button(role: .destructive) {
            last = "Deleted"
        } label: {
            Label("Delete", systemImage: "trash")
        }
    }
}
