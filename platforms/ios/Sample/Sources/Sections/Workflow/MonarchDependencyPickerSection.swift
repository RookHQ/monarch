import MonarchUI
import SwiftUI

struct MonarchDependencyPickerSection: CataloguePage {
    static let title = "Dependency picker"

    private static let issues = [
        MonarchDependencyItem(
            id: "12",
            key: "STR-12",
            title: "Fix login",
            status: MonarchStatusDefinition("In progress", tone: .info, systemImage: "circle.lefthalf.filled"),
            url: URL(string: "https://example.com/issues/12")
        ),
        MonarchDependencyItem(
            id: "14",
            key: "STR-14",
            title: "Update the pricing page",
            status: MonarchStatusDefinition("To do", tone: .neutral, systemImage: "circle")
        ),
        MonarchDependencyItem(
            id: "15",
            key: "STR-15",
            title: "Write the launch post",
            status: MonarchStatusDefinition("Done", tone: .success, systemImage: "checkmark.circle.fill")
        ),
        MonarchDependencyItem(id: "10", key: "STR-10", title: "Launch the new plans"),
    ]

    @State private var links = [MonarchDependencyLink(id: "l1", relation: .blockedBy, item: issues[0])]

    var body: some View {
        MonarchDependencyPicker(
            links: links,
            items: Self.issues,
            excludedIDs: ["10"],
            onAdd: { relation, item in
                links.append(MonarchDependencyLink(id: UUID().uuidString, relation: relation, item: item))
            },
            onRemove: { link in links.removeAll { $0.id == link.id } }
        )

        CatalogueCaption("Read only")
        MonarchDependencyPicker(links: links, isReadOnly: true)
    }
}
