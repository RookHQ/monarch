@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchGoalTreeSection: CataloguePage {
    static let title = "Goal tree"

    @State private var goals = [
        MonarchGoal(
            id: "o1",
            title: "Grow repeat customers",
            progress: 0.45,
            status: .onTrack,
            ownerName: "Ana Ruiz",
            meta: "Q2"
        ),
        MonarchGoal(id: "k1", parentID: "o1", title: "Raise repeat rate to 35%", progress: 0.6, status: .onTrack),
        MonarchGoal(
            id: "k2",
            parentID: "o1",
            title: "Cut churn to 4%",
            progress: 0.2,
            status: .atRisk,
            ownerName: "Ben Okafor",
            alignedTo: "Retention plan (Sam Lee)"
        ),
        MonarchGoal(id: "o2", title: "Ship the mobile app", progress: 1, status: .done, ownerName: "Jo Park"),
        MonarchGoal(id: "k3", parentID: "o2", title: "Publish to the store", progress: 1, status: .done),
    ]
    @State private var isQuiet = true
    @State private var checkIns = 0

    var body: some View {
        Toggle("Quiet children", isOn: $isQuiet)
        Text("Check-ins: \(checkIns)")
        MonarchGoalTree(
            goals,
            isQuietChildren: isQuiet,
            onAddChild: { parent in
                let count = goals.filter { $0.parentID == parent.id }.count
                goals.append(
                    MonarchGoal(
                        id: UUID().uuidString,
                        parentID: parent.id,
                        title: "New key result \(count + 1)",
                        progress: 0,
                        status: .notStarted
                    )
                )
            },
            actions: { _ in
                Menu {
                    Button("Check in") { checkIns += 1 }
                } label: {
                    Label("More", systemImage: "ellipsis")
                }
                .labelStyle(.iconOnly)
                .buttonStyle(.monarch(.ghost, size: .sm))
            }
        )
        CatalogueCaption("No goals")
        MonarchGoalTree([])
    }
}
