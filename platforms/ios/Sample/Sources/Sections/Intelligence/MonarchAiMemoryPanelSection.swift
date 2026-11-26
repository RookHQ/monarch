@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiMemoryPanelSection: CataloguePage {
    static let title = "AI memory panel"

    private static let ada = MonarchAiMemoryEntity(kind: .person, name: "Ada Lovelace")
    private static let rook = MonarchAiMemoryEntity(kind: .organization, name: "Rook")
    private static let launch = MonarchAiMemoryEntity(kind: .project, name: "Launch")

    private static let initial = [
        MonarchAiMemoryItem(id: "1", text: "Prefers summaries under 200 words", entity: ada, updatedAt: Date()),
        MonarchAiMemoryItem(
            id: "2",
            text: "Works in Lisbon",
            entity: ada,
            updatedAt: Date().addingTimeInterval(-86_400)
        ),
        MonarchAiMemoryItem(id: "3", text: "Fiscal year starts in April", entity: rook, updatedAt: Date()),
        MonarchAiMemoryItem(id: "4", text: "Ships on Mondays", entity: launch, updatedAt: Date()),
    ]

    @State private var memories = initial
    @State private var isOpen = false

    var body: some View {
        CatalogueCaption("A settings screen, opened in a sheet")
        Button("Manage memories") { isOpen = true }
            .buttonStyle(.monarch(.secondary))
            .sheet(isPresented: $isOpen) {
                NavigationStack {
                    MonarchAiMemoryPanel(
                        memories,
                        onEdit: { id, text in
                            if let index = memories.firstIndex(where: { $0.id == id }) {
                                memories[index].text = text
                                memories[index].updatedAt = Date()
                            }
                        },
                        onForget: { id in memories.removeAll { $0.id == id } },
                        onForgetMany: { ids in memories.removeAll { ids.contains($0.id) } }
                    )
                    .navigationTitle("Memories")
                }
            }
        Button("Restore the sample memories") { memories = Self.initial }
            .buttonStyle(.monarch(.ghost, size: .sm))
        Text("\(memories.count) remembered").monarchTextStyle(.caption)
    }
}
