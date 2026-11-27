import MonarchUI
import SwiftUI

struct MonarchAiMemoryChipSection: CataloguePage {
    static let title = "AI memory chip"

    @State private var memory: MonarchAiMemoryPart? = MonarchAiMemoryPart(
        id: "m1",
        action: .write,
        text: "Prefers summaries under 200 words",
        scope: "Ada Lovelace"
    )

    var body: some View {
        CatalogueCaption("Saved in a thread, with Edit and Forget")
        if let memory {
            MonarchAiMemoryChip(
                memory,
                onEdit: { _, text in self.memory?.text = text },
                onForget: { _ in self.memory = nil }
            )
        } else {
            Text("Forgotten.").monarchTextStyle(.body)
            Button("Restore") {
                memory = MonarchAiMemoryPart(id: "m1", action: .write, text: "Prefers summaries under 200 words")
            }
            .buttonStyle(.monarch(.secondary, size: .sm))
        }

        CatalogueCaption("Used and forgotten")
        MonarchAiMemoryChip(MonarchAiMemoryPart(id: "m2", action: .read, text: "Works in Lisbon"))
        MonarchAiMemoryChip(MonarchAiMemoryPart(id: "m3", action: .forget, text: "Has a dog called Pixel"))

        CatalogueCaption("Rows in a list of memories")
        MonarchAiMemoryChip(
            MonarchAiMemoryPart(id: "m4", action: .write, text: "Writes in British English", scope: "Workspace"),
            variant: .row,
            showsScope: true,
            onForget: { _ in }
        )
        MonarchAiMemoryChip(
            MonarchAiMemoryPart(id: "m5", action: .write, text: "Reports go to the finance channel"),
            variant: .row,
            onEdit: { _, _ in },
            onForget: { _ in }
        )
    }
}
