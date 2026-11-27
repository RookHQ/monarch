import MonarchUI
import SwiftUI

struct MonarchAiKnowledgeScopeSection: CataloguePage {
    static let title = "AI knowledge scope"

    private static let sources = [
        MonarchAiKnowledgeSource(
            id: "handbook",
            label: "Handbook",
            kind: .space,
            description: "3 documents",
            children: [
                MonarchAiKnowledgeSource(id: "policies", label: "Policies", kind: .document),
                MonarchAiKnowledgeSource(id: "benefits", label: "Benefits", kind: .document),
                MonarchAiKnowledgeSource(
                    id: "travel",
                    label: "Travel",
                    kind: .space,
                    children: [
                        MonarchAiKnowledgeSource(id: "flights", label: "Flights", kind: .document),
                        MonarchAiKnowledgeSource(id: "hotels", label: "Hotels", kind: .document),
                    ]
                ),
            ]
        ),
        MonarchAiKnowledgeSource(id: "crm", label: "Sales CRM", kind: .app, description: "Connected"),
        MonarchAiKnowledgeSource(id: "wiki", label: "Engineering wiki", kind: .space),
    ]

    @State private var selection = ["policies", "crm"]
    @State private var isDisabled = false

    var body: some View {
        CatalogueCaption("Check sources; a group selects everything in it")
        MonarchAiKnowledgeScope(Self.sources, selection: $selection, startsExpanded: ["handbook"])
            .disabled(isDisabled)
        Toggle("Disabled", isOn: $isDisabled)
        Text("Selection: \(selection.joined(separator: ", "))").monarchTextStyle(.caption)

        CatalogueCaption("No sources")
        MonarchAiKnowledgeScope([], selection: .constant([]))
    }
}
