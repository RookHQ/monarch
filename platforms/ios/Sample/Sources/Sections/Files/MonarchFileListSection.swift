import MonarchUI
import SwiftUI

struct MonarchFileListSection: CataloguePage {
    static let title = "File list"

    @State private var files = [
        MonarchFileListItem(id: "1", name: "Q3 report.pdf", size: 2_400_000, meta: "Edited yesterday"),
        MonarchFileListItem(id: "2", name: "Budget 2027.xlsx", size: 48_000, meta: "Edited 3 days ago"),
        MonarchFileListItem(id: "3", name: "Launch video.mov", size: 120_000_000, badges: ["Processing"]),
        MonarchFileListItem(id: "4", name: "Brand assets.zip", size: 9_800_000, badges: ["Shared"]),
        MonarchFileListItem(id: "5", name: "Contract.pdf", size: 310_000, meta: "Locked by legal", isDisabled: true),
    ]
    @State private var selection: Set<String> = []
    @State private var layout = MonarchFileListLayout.list
    @State private var status = "Nothing opened."

    var body: some View {
        CatalogueCaption("Selectable, with bulk actions and a menu per file")
        Picker("Layout", selection: $layout) {
            Text("List").tag(MonarchFileListLayout.list)
            Text("Grid").tag(MonarchFileListLayout.grid)
        }
        .pickerStyle(.segmented)
        Text(status).monarchTextStyle(.caption)
        MonarchFileList(
            files,
            selection: $selection,
            layout: layout,
            label: "Project files",
            bulkActions: [
                MonarchBulkAction(id: "download", title: "Download", systemImage: "arrow.down.circle") {
                    status = "Downloading \(selection.count) files."
                },
                MonarchBulkAction(id: "delete", title: "Delete", systemImage: "trash", isDestructive: true) {
                    files.removeAll { selection.contains($0.id) }
                    selection = []
                },
            ],
            onOpen: { file in status = "Opened \(file.name)." },
            menu: { file in
                Button("Rename", systemImage: "pencil") { status = "Rename \(file.name)." }
                Button("Delete", systemImage: "trash", role: .destructive) {
                    files.removeAll { $0.id == file.id }
                }
            }
        )

        CatalogueCaption("Read only")
        MonarchFileList(Array(files.prefix(2)))

        CatalogueCaption("Empty")
        MonarchFileList([])
    }
}
