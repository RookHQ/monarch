import MonarchUI
import SwiftUI

struct MonarchIncidentTimelineSection: CataloguePage {
    static let title = "Incident timeline"

    private static let start = Date(timeIntervalSinceReferenceDate: 800_000_000)

    @State private var updates = [
        MonarchIncidentUpdate(
            id: "1",
            stage: .investigating,
            body: "Checkout is failing for some users.",
            time: start,
            isAutomated: true
        ),
        MonarchIncidentUpdate(
            id: "2",
            stage: .identified,
            body: "A database migration locked the orders table. We are rolling it back.",
            time: start.addingTimeInterval(900),
            author: "Ada"
        ),
    ]

    var body: some View {
        MonarchIncidentTimeline(updates, isLive: true)
        MonarchButton("Post the next update", variant: .secondary) {
            let next: MonarchIncidentStage = updates.count == 2 ? .monitoring : .resolved
            updates.append(
                MonarchIncidentUpdate(
                    id: UUID().uuidString,
                    stage: next,
                    body: next == .monitoring ? "The rollback is done. We are watching error rates." : "Resolved.",
                    time: Self.start.addingTimeInterval(Double(updates.count) * 900)
                )
            )
        }
        .disabled(updates.count >= 4)

        CatalogueCaption("Compact, with a product stage")
        MonarchIncidentTimeline(
            [
                MonarchIncidentUpdate(
                    id: "m",
                    stage: "maintenance",
                    body: "The database moves to new hardware.",
                    time: Self.start
                )
            ],
            stages: ["maintenance": MonarchStatusDefinition("Scheduled maintenance", tone: .info)],
            density: .compact
        )

        CatalogueCaption("No updates")
        MonarchIncidentTimeline([])
    }
}
