@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchOrgChartSection: CataloguePage {
    static let title = "Org chart"

    @State private var people = [
        MonarchOrgChartNode(id: "ana", name: "Ana Silva", title: "Chief executive"),
        MonarchOrgChartNode(id: "jo", parentID: "ana", name: "Jo Park", title: "Head of engineering"),
        MonarchOrgChartNode(id: "sam", parentID: "ana", name: "Sam Lee", title: "Head of finance", meta: "Finance"),
        MonarchOrgChartNode(
            id: "noor",
            parentID: "ana",
            name: "Noor Ali",
            title: "People lead",
            meta: "Acting",
            isDashed: true
        ),
        MonarchOrgChartNode(id: "ola", parentID: "jo", name: "Ola Berg", title: "Engineer"),
        MonarchOrgChartNode(id: "kai", parentID: "jo", name: "Kai Moss", title: "Engineer"),
        MonarchOrgChartNode(id: "open", parentID: "jo", name: "Product designer", meta: "Engineering", kind: .vacancy),
        MonarchOrgChartNode(id: "mia", parentID: "sam", name: "Mia Chen", title: "Accountant"),
        MonarchOrgChartNode(id: "leo", parentID: "noor", name: "Leo Grant", title: "Recruiter", childCount: 2),
    ]
    @State private var selectedID: String?
    @State private var moving: MonarchOrgChartNode?

    var body: some View {
        CatalogueCaption("Chart: tap a card, open teams with the chips, pinch to zoom, drag a card onto a new manager")
        chart(.chart)
        Text("Selected: \(people.first { $0.id == selectedID }?.name ?? "nobody")")
            .monarchTextStyle(.caption)

        CatalogueCaption("List: the phone and large text form")
        chart(.list)
    }

    private func chart(_ layout: MonarchOrgChartLayout) -> some View {
        MonarchOrgChart(
            people,
            focusID: selectedID,
            dottedLines: [MonarchOrgChartDottedLine(from: "mia", to: "jo")],
            layout: layout,
            onSelect: { person in selectedID = person.id },
            onLoadChildren: { manager in
                // Stands in for a network request in the catalogue.
                try await Task.sleep(for: .milliseconds(400))
                people.append(
                    MonarchOrgChartNode(
                        id: "\(manager.id)-a",
                        parentID: manager.id,
                        name: "Ivy Moss",
                        title: "Coordinator"
                    )
                )
                people.append(
                    MonarchOrgChartNode(
                        id: "\(manager.id)-b",
                        parentID: manager.id,
                        name: "Tom Reed",
                        title: "Coordinator"
                    )
                )
            },
            onMoveRequest: { person, target in
                switch target {
                case .manager(let id): move(person.id, to: id)
                case .topLevel: move(person.id, to: nil)
                case .unspecified: moving = person
                }
            }
        )
        .frame(height: 440)
        .confirmationDialog(
            "Move \(moving?.name ?? "")",
            isPresented: Binding(get: { moving != nil }, set: { if !$0 { moving = nil } }),
            presenting: moving
        ) { person in
            ForEach(managers(for: person)) { manager in
                Button("Under \(manager.name)") { move(person.id, to: manager.id) }
            }
            Button("Cancel", role: .cancel) {}
        }
    }

    /// People the person can move under: not themselves, not their team, not open positions.
    private func managers(for person: MonarchOrgChartNode) -> [MonarchOrgChartNode] {
        people.filter { candidate in
            candidate.kind == .person && candidate.id != person.parentID && !isInTeam(candidate.id, of: person.id)
        }
    }

    private func isInTeam(_ id: String, of managerID: String) -> Bool {
        var current: String? = id
        var steps = 0
        while let value = current, steps <= people.count {
            if value == managerID { return true }
            current = people.first { $0.id == value }?.parentID
            steps += 1
        }
        return false
    }

    private func move(_ id: String, to managerID: String?) {
        guard let index = people.firstIndex(where: { $0.id == id }) else { return }
        people[index].parentID = managerID
    }
}
