import MonarchUI
import SwiftUI

/// Native adoption: a Menu of Toggle items that shows and hides table columns.
struct ColumnMenuSection: CataloguePage {
    static let title = "Column menu (Menu with Toggle items)"

    @Environment(\.monarchTheme) private var theme
    @State private var visible: Set<String> = ["name", "email", "plan"]

    private let columns = [("name", "Name"), ("email", "Email"), ("plan", "Plan"), ("created", "Created")]

    var body: some View {
        Menu {
            Section("Visible columns") {
                ForEach(columns, id: \.0) { column in
                    Toggle(column.1, isOn: binding(for: column.0))
                        .disabled(visible.count == 1 && visible.contains(column.0))
                        .menuActionDismissBehavior(.disabled)
                }
            }
            Divider()
            Button("Show all columns") { visible = Set(columns.map(\.0)) }
                .disabled(visible.count == columns.count)
            Button("Reset columns") { visible = ["name", "email", "plan"] }
        } label: {
            Label("Columns", systemImage: "tablecells")
        }
        .menuStyle(.button)
        .buttonStyle(.monarch(.secondary, size: .sm))
        .accessibilityValue("\(visible.count) of \(columns.count) shown")
        CatalogueCaption("Shown: \(columns.filter { visible.contains($0.0) }.map(\.1).joined(separator: ", "))")
        HStack(spacing: theme.spacing.space4) {
            ForEach(columns.filter { visible.contains($0.0) }, id: \.0) { column in
                Text(column.1).monarchTextStyle(.label)
            }
        }
    }

    private func binding(for id: String) -> Binding<Bool> {
        Binding(
            get: { visible.contains(id) },
            set: { isOn in
                if isOn { visible.insert(id) } else { visible.remove(id) }
            }
        )
    }
}
