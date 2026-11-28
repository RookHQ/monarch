import MonarchUI
import SwiftUI

/// Native adoption: `List` with `children:` and `OutlineGroup`, in the Monarch theme.
struct TreeViewSection: CataloguePage {
    static let title = "Tree view (List with children)"

    struct Node: Identifiable, Hashable {
        let id: String
        let name: String
        let systemImage: String
        var children: [Node]?
    }

    @State private var selection: Node.ID?

    private let nodes = [
        Node(
            id: "eng",
            name: "Engineering",
            systemImage: "folder",
            children: [
                Node(id: "web", name: "Web", systemImage: "person.2"),
                Node(
                    id: "mobile",
                    name: "Mobile",
                    systemImage: "folder",
                    children: [
                        Node(id: "ios", name: "iOS", systemImage: "person.2"),
                        Node(id: "android", name: "Android", systemImage: "person.2"),
                    ]
                ),
            ]
        ),
        Node(id: "design", name: "Design", systemImage: "folder", children: []),
        Node(id: "sales", name: "Sales", systemImage: "person.2"),
    ]

    var body: some View {
        CatalogueCaption("Single selection: \(selection ?? "none")")
        List(nodes, children: \.children, selection: $selection) { node in
            Label(node.name, systemImage: node.systemImage)
                .monarchTextStyle(.body)
        }
        .listStyle(.plain)
        .frame(minHeight: 320)
    }
}
