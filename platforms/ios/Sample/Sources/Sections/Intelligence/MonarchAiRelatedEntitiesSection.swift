import MonarchUI
import SwiftUI

struct MonarchAiRelatedEntitiesSection: CataloguePage {
    static let title = "AI related entities"

    private static let entities = [
        MonarchAiRelatedEntity(id: "jordan", label: "Jordan Lee", kind: .person, weight: 0.9),
        MonarchAiRelatedEntity(id: "acme", label: "Acme", kind: .org, weight: 0.7),
        MonarchAiRelatedEntity(id: "launch", label: "Q3 launch", kind: .project, weight: 0.4),
        MonarchAiRelatedEntity(id: "brief", label: "Launch brief", kind: .document, weight: 0.3),
        MonarchAiRelatedEntity(id: "deal", label: "Acme renewal", kind: .record, weight: 0.2),
    ]
    private static let relations = [
        MonarchAiEntityRelation(from: "jordan", to: "acme", label: "works at"),
        MonarchAiEntityRelation(from: "jordan", to: "launch", label: "leads"),
        MonarchAiEntityRelation(from: "deal", to: "acme"),
        MonarchAiEntityRelation(from: "brief", to: "launch", label: "describes"),
    ]

    @State private var variant: MonarchAiRelatedEntitiesVariant = .list
    @State private var selected: String?

    var body: some View {
        CatalogueCaption("Ranked list or radial graph; tap an entity")
        Picker("Layout", selection: $variant) {
            Text("List").tag(MonarchAiRelatedEntitiesVariant.list)
            Text("Graph").tag(MonarchAiRelatedEntitiesVariant.graph)
        }
        .pickerStyle(.segmented)
        MonarchAiRelatedEntities(Self.entities, relations: Self.relations, variant: variant) { entity in
            selected = entity.label
        }
        Text("Selected: \(selected ?? "none")").monarchTextStyle(.caption)

        CatalogueCaption("Nothing found")
        MonarchAiRelatedEntities([])
    }
}
