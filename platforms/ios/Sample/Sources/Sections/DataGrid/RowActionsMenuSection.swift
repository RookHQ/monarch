import MonarchUI
import SwiftUI

/// Native adoption: a "⋯" Menu on each row and swipe actions, destructive items last.
struct RowActionsMenuSection: CataloguePage {
    static let title = "Row actions (Menu and swipeActions)"

    @State private var companies = ["Acme Ltd", "Globex", "Initech"]
    @State private var archived: [String] = []
    @State private var pendingDelete: String?

    var body: some View {
        CatalogueCaption("Press ⋯, swipe a row from its trailing edge, or press and hold")
        List {
            ForEach(companies, id: \.self) { company in
                HStack {
                    Text(company).monarchTextStyle(.body)
                    Spacer()
                    Menu {
                        actions(for: company)
                    } label: {
                        Label("Actions for \(company)", systemImage: "ellipsis")
                            .labelStyle(.iconOnly)
                    }
                    .menuStyle(.button)
                    .buttonStyle(.monarch(.ghost, size: .sm))
                }
                .swipeActions(edge: .trailing) {
                    Button("Delete", systemImage: "trash", role: .destructive) { pendingDelete = company }
                    Button("Archive", systemImage: "archivebox") { archive(company) }
                }
                .contextMenu { actions(for: company) }
            }
        }
        .listStyle(.plain)
        .frame(minHeight: 220)
        .confirmationDialog(
            "Delete \(pendingDelete ?? "")?",
            isPresented: Binding(get: { pendingDelete != nil }, set: { if !$0 { pendingDelete = nil } }),
            titleVisibility: .visible,
            actions: {
                Button("Delete", role: .destructive) {
                    companies.removeAll { $0 == pendingDelete }
                    pendingDelete = nil
                }
                Button("Cancel", role: .cancel) { pendingDelete = nil }
            },
            message: { Text("Its contacts and notes are deleted with it.") }
        )
        if !archived.isEmpty {
            CatalogueCaption("Archived: \(archived.joined(separator: ", "))")
        }
    }

    @ViewBuilder private func actions(for company: String) -> some View {
        Button("Edit", systemImage: "pencil") {}
        Button("Archive", systemImage: "archivebox") { archive(company) }
        Divider()
        Button("Delete", systemImage: "trash", role: .destructive) { pendingDelete = company }
    }

    private func archive(_ company: String) {
        companies.removeAll { $0 == company }
        archived.append(company)
    }
}
