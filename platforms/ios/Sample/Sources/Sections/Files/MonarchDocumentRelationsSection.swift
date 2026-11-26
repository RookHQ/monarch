@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchDocumentRelationsSection: CataloguePage {
    static let title = "Document relations"

    private struct LinkFailure: LocalizedError {
        var errorDescription: String? { "Archived documents can’t be linked." }
    }

    private static let library = [
        MonarchDocumentRelationTarget(id: "d1", title: "Q3 plan", description: "Finance", fileName: "q3-plan.pdf"),
        MonarchDocumentRelationTarget(id: "d2", title: "Roadmap", description: "Product"),
        MonarchDocumentRelationTarget(id: "d3", title: "Billing specification", description: "Engineering"),
        MonarchDocumentRelationTarget(id: "d4", title: "Pricing sheet", description: "Sales", fileName: "pricing.xlsx"),
        MonarchDocumentRelationTarget(id: "d5", title: "Old plan (archived)", description: "Finance"),
    ]

    @State private var relations = [
        MonarchDocumentRelation(id: "r1", type: "parent-of", target: Self.library[0]),
        MonarchDocumentRelation(id: "r2", type: "parent-of", direction: .incoming, target: Self.library[1]),
    ]
    @State private var opened: String?
    @State private var failsRemove = false

    var body: some View {
        CatalogueCaption("Options")
        Toggle("Fail the next removal", isOn: $failsRemove)
            .monarchTextStyle(.body)

        CatalogueCaption("A document's relations")
        MonarchDocumentRelations(
            relations,
            candidates: Self.library,
            showsEmpty: true,
            onOpen: { opened = $0.title },
            onAdd: { targetID, type in
                guard let target = Self.library.first(where: { $0.id == targetID }) else { return }
                if target.id == "d5" { throw LinkFailure() }
                relations.append(MonarchDocumentRelation(id: UUID().uuidString, type: type, target: target))
            },
            onRemove: { relationID in
                if failsRemove {
                    failsRemove = false
                    throw LinkFailure()
                }
                relations.removeAll { $0.id == relationID }
            }
        )
        if let opened {
            Text("Opened \(opened)").monarchTextStyle(.caption)
        }

        CatalogueCaption("Read only")
        MonarchDocumentRelations(relations)
    }
}
