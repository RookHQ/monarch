import MonarchUI
import SwiftUI

struct MonarchVersionHistoryPanelSection: CataloguePage {
    static let title = "VersionHistoryPanel"

    private static let texts: [String: [String]] = [
        "v3": ["Ship on Monday.", "Tell support.", "Update the status page."],
        "v2": ["Ship on Friday.", "Tell support.", "Update the status page."],
        "v1": ["Ship on Friday.", "Tell support."],
    ]

    @State private var versions: [MonarchVersionHistoryPanelVersion] = {
        let now = Date()
        return [
            MonarchVersionHistoryPanelVersion(
                id: "v3",
                date: now.addingTimeInterval(-300),
                authorName: "Ana Silva",
                note: "Launch copy"
            ),
            MonarchVersionHistoryPanelVersion(
                id: "v2",
                date: now.addingTimeInterval(-7_200),
                authorName: "Sam Lee",
                isAutomatic: true
            ),
            MonarchVersionHistoryPanelVersion(
                id: "v1",
                date: now.addingTimeInterval(-172_800),
                authorName: "Ada Byron"
            ),
        ]
    }()
    @State private var current = ["Ship on Monday.", "Tell support.", "Update the status page."]
    @State private var showsHistory = false
    @State private var selection: String?
    @State private var selectedLines: [String]?

    var body: some View {
        CatalogueCaption("In a sheet")
        MonarchButton("Show history", systemImage: "clock") { showsHistory = true }
            .sheet(isPresented: $showsHistory) {
                MonarchVersionHistoryPanel(
                    versions: versions,
                    selection: $selection,
                    currentLines: current,
                    selectedLines: selectedLines,
                    onSaveVersion: { name in
                        versions.insert(
                            MonarchVersionHistoryPanelVersion(
                                id: UUID().uuidString,
                                date: Date(),
                                authorName: "You",
                                note: name.isEmpty ? nil : name
                            ),
                            at: 0
                        )
                    },
                    onRestore: { id in
                        current = Self.texts[id] ?? current
                    },
                    onClose: { showsHistory = false }
                )
                .task(id: selection) {
                    selectedLines = nil
                    guard let selection else { return }
                    // Stands in for loading the version from the app's server.
                    try? await Task.sleep(for: .milliseconds(300))
                    selectedLines = Self.texts[selection] ?? current
                }
            }

        CatalogueCaption("The content now")
        ForEach(current, id: \.self) { line in
            Text(line).monarchTextStyle(.body)
        }
    }
}
