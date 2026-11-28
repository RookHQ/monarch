import MonarchUI
import SwiftUI

struct MonarchFileCardSection: CataloguePage {
    static let title = "File card"

    private struct File: Identifiable {
        let id: String
        let size: Int64
        let meta: String
    }

    private static let files = [
        File(id: "Q3 report.pdf", size: 2_400_000, meta: "Edited yesterday"),
        File(id: "Budget 2027.xlsx", size: 48_000, meta: "Edited 3 days ago"),
        File(id: "Launch video.mov", size: 120_000_000, meta: "Uploaded today"),
        File(id: "Brand assets.zip", size: 9_800_000, meta: "Shared by Sam"),
    ]

    @Environment(\.monarchTheme) private var theme
    @State private var selection: Set<String> = []
    @State private var status = "Nothing opened."

    var body: some View {
        CatalogueCaption("Grid of cards with selection and actions")
        Text(status).monarchTextStyle(.caption)
        LazyVGrid(columns: [GridItem(.adaptive(minimum: theme.spacing.space24 + theme.spacing.space12))]) {
            ForEach(Self.files) { file in
                MonarchFileCard(
                    file.id,
                    isSelected: binding(file.id),
                    size: file.size,
                    meta: file.meta,
                    badges: file.id.hasSuffix(".zip") ? ["Shared"] : [],
                    onOpen: { status = "Opened \(file.id)." },
                    menu: {
                        Button("Rename", systemImage: "pencil") { status = "Rename \(file.id)." }
                        Button("Delete", systemImage: "trash", role: .destructive) { status = "Deleted \(file.id)." }
                    }
                )
            }
        }

        CatalogueCaption("Rows")
        ForEach(Self.files) { file in
            MonarchFileCard(
                file.id,
                size: file.size,
                meta: file.meta,
                layout: .row,
                onOpen: { status = "Opened \(file.id)." }
            )
        }
        MonarchFileCard("Locked.pdf", size: 1_000, layout: .row).disabled(true)
    }

    private func binding(_ id: String) -> Binding<Bool> {
        Binding(
            get: { selection.contains(id) },
            set: { isOn in
                if isOn { selection.insert(id) } else { selection.remove(id) }
            }
        )
    }
}
