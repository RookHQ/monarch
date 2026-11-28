import MonarchUI
import SwiftUI

struct MonarchSortableGridSection: CataloguePage {
    static let title = "Sortable grid"

    @State private var widgets = SortableGridDemoWidget.all
    @State private var lastMove = "None"
    @State private var isLocked = false

    var body: some View {
        CatalogueCaption("Touch and hold a tile to drag it; VoiceOver uses the handle's actions")
        MonarchSortableGrid(
            $widgets,
            itemLabel: \.title,
            isItemDisabled: { $0.isPinned },
            onReorder: { from, to in lastMove = "\(from + 1) → \(to + 1)" },
            content: { widget, state in SortableGridDemoTile(widget: widget, state: state) }
        )
        .disabled(isLocked)
        Text("Last move: \(lastMove)").monarchTextStyle(.caption)
        Toggle("Lock the layout", isOn: $isLocked)
        CatalogueCaption("Three fixed columns")
        MonarchSortableGrid($widgets, itemLabel: \.title, columns: 3) { widget, state in
            SortableGridDemoTile(widget: widget, state: state)
        }
    }
}

private struct SortableGridDemoWidget: Identifiable {
    let id: Int
    let title: String
    let systemImage: String
    var isPinned = false

    static let all = [
        SortableGridDemoWidget(id: 1, title: "Revenue", systemImage: "chart.line.uptrend.xyaxis", isPinned: true),
        SortableGridDemoWidget(id: 2, title: "Signups", systemImage: "person.badge.plus"),
        SortableGridDemoWidget(id: 3, title: "Churn", systemImage: "arrow.uturn.backward"),
        SortableGridDemoWidget(id: 4, title: "Tickets", systemImage: "tray"),
        SortableGridDemoWidget(id: 5, title: "Uptime", systemImage: "bolt.heart"),
    ]
}

private struct SortableGridDemoTile: View {
    let widget: SortableGridDemoWidget
    let state: MonarchSortableGridItemState

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space2) {
            Image(systemName: widget.systemImage)
                .accessibilityHidden(true)
            Text(widget.title).monarchTextStyle(.bodyStrong)
            if widget.isPinned {
                MonarchBadge("Pinned", systemImage: "pin")
            }
        }
        .foregroundStyle(theme.colors.textDefault)
        .padding(theme.spacing.space4)
        .frame(maxWidth: .infinity, minHeight: theme.spacing.space24, alignment: .topLeading)
        .background(
            state.isDropTarget ? theme.colors.accentSubtle : theme.colors.bgSurface,
            in: RoundedRectangle(cornerRadius: theme.radius.lg)
        )
        .overlay(RoundedRectangle(cornerRadius: theme.radius.lg).stroke(theme.colors.borderDefault))
    }
}
