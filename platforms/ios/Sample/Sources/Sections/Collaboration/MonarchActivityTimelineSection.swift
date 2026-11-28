import MonarchUI
import SwiftUI

struct MonarchActivityTimelineSection: CataloguePage {
    static let title = "ActivityTimeline"

    @State private var marker = MonarchActivityTimelineMarker.dot
    @State private var density = MonarchActivityTimelineDensity.regular
    @State private var isGrouped = true
    @State private var pages = 1

    private var events: [MonarchActivityTimelineEvent] {
        let now = Date()
        let first = [
            MonarchActivityTimelineEvent(
                id: "1",
                actor: MonarchActivityTimelineActor("Ana Ruiz"),
                verb: "moved",
                object: "Acme renewal",
                time: now.addingTimeInterval(-1_800),
                tone: .info,
                tags: ["Negotiation"],
                details: "Stage: Proposal → Negotiation"
            ),
            MonarchActivityTimelineEvent(
                id: "2",
                actor: MonarchActivityTimelineActor("Follow-up rule", kind: .automation),
                verb: "sent a reminder to",
                object: "Ben Okafor",
                time: now.addingTimeInterval(-7_200),
                systemImage: "bolt",
                tone: .warning
            ),
            MonarchActivityTimelineEvent(
                id: "3",
                actor: MonarchActivityTimelineActor("Ben Okafor"),
                verb: "commented on",
                object: "Acme renewal",
                time: now.addingTimeInterval(-86_400),
                body: "Legal wants the new terms by Friday."
            ),
        ]
        let older = (0..<(pages - 1) * 3).map { index in
            MonarchActivityTimelineEvent(
                id: "old\(index)",
                verb: "Ticket created from email",
                time: now.addingTimeInterval(-86_400 * Double(index + 3)),
                tone: .success
            )
        }
        return first + older
    }

    var body: some View {
        CatalogueCaption("Options")
        Picker("Marker", selection: $marker) {
            Text("Dot").tag(MonarchActivityTimelineMarker.dot)
            Text("Avatar").tag(MonarchActivityTimelineMarker.avatar)
        }
        .pickerStyle(.segmented)
        Picker("Density", selection: $density) {
            Text("Regular").tag(MonarchActivityTimelineDensity.regular)
            Text("Compact").tag(MonarchActivityTimelineDensity.compact)
        }
        .pickerStyle(.segmented)
        Toggle("Group by day", isOn: $isGrouped)

        CatalogueCaption("A record's history")
        MonarchActivityTimeline(
            events,
            isGroupedByDay: isGrouped,
            marker: marker,
            density: density,
            hasMore: pages < 3,
            onLoadMore: {
                pages += 1
            }
        )

        CatalogueCaption("Loading, empty and failed")
        MonarchActivityTimeline([], isLoading: true)
        MonarchActivityTimeline([])
        MonarchActivityTimeline([], isFailed: true, onRetry: {})
    }
}
