import MonarchUI
import SwiftUI

/// Recipe: a segmented Picker that switches between views of the same data.
struct ViewSwitcherSection: CataloguePage {
    static let title = "View switcher (segmented Picker)"

    enum ViewMode: String, CaseIterable, Identifiable {
        case table, board, calendar, gallery

        var id: Self { self }

        var title: String {
            switch self {
            case .table: "Table"
            case .board: "Board"
            case .calendar: "Calendar"
            case .gallery: "Gallery"
            }
        }

        var systemImage: String {
            switch self {
            case .table: "tablecells"
            case .board: "rectangle.split.3x1"
            case .calendar: "calendar"
            case .gallery: "square.grid.2x2"
            }
        }
    }

    @State private var mode = ViewMode.table

    var body: some View {
        CatalogueCaption("Text labels")
        Picker("View", selection: $mode) {
            ForEach(ViewMode.allCases) { mode in
                Text(mode.title).tag(mode)
            }
        }
        .pickerStyle(.segmented)
        CatalogueCaption("Icons, named for VoiceOver")
        Picker("View", selection: $mode) {
            ForEach(ViewMode.allCases) { mode in
                Image(systemName: mode.systemImage)
                    .accessibilityLabel(mode.title)
                    .tag(mode)
            }
        }
        .pickerStyle(.segmented)
        Label("Showing the \(mode.title.lowercased()) view", systemImage: mode.systemImage)
            .monarchTextStyle(.body)
    }
}
