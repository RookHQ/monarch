import MonarchUI
import SwiftUI

/// Recipe: MonarchWidget around a compact MonarchActivityTimeline of the latest events.
struct ActivityWidgetSection: CataloguePage {
    static let title = "Activity widget (recipe)"

    @State private var marker = MonarchActivityTimelineMarker.dot
    @State private var isGrouped = false
    @State private var isRetrying = false
    @State private var showsAll = false

    private var events: [MonarchActivityTimelineEvent] {
        let now = Date()
        return [
            MonarchActivityTimelineEvent(
                id: "1",
                actor: MonarchActivityTimelineActor("Ada Moss"),
                verb: "closed",
                object: "Request 1042",
                time: now.addingTimeInterval(-1_200)
            ),
            MonarchActivityTimelineEvent(
                id: "2",
                actor: MonarchActivityTimelineActor("Renewal rule", kind: .automation),
                verb: "flagged",
                object: "Contract 88",
                time: now.addingTimeInterval(-6_900),
                tone: .warning
            ),
            MonarchActivityTimelineEvent(
                id: "3",
                actor: MonarchActivityTimelineActor("Bea Lund"),
                verb: "commented on",
                object: "Request 1039",
                time: now.addingTimeInterval(-13_500)
            ),
            MonarchActivityTimelineEvent(
                id: "4",
                actor: MonarchActivityTimelineActor("Cal Ortiz"),
                verb: "created",
                object: "Project Atlas",
                time: now.addingTimeInterval(-72_000)
            ),
        ]
    }

    var body: some View {
        CatalogueCaption("Options")
        Picker("Marker", selection: $marker) {
            Text("Dot").tag(MonarchActivityTimelineMarker.dot)
            Text("Avatar").tag(MonarchActivityTimelineMarker.avatar)
        }
        .pickerStyle(.segmented)
        Toggle("Group by day", isOn: $isGrouped)
            .monarchTextStyle(.body)

        CatalogueCaption("Recent activity")
        MonarchWidget(
            "Recent activity",
            systemImage: "clock.arrow.circlepath",
            onViewAll: { showsAll = true }
        ) {
            MonarchActivityTimeline(
                Array(events.prefix(6)),
                isGroupedByDay: isGrouped,
                marker: marker,
                density: .compact,
                labels: MonarchActivityTimelineLabels(list: "Recent activity", empty: "No activity yet")
            )
        }
        .sheet(isPresented: $showsAll) {
            NavigationStack {
                ScrollView {
                    MonarchActivityTimeline(events).padding()
                }
                .navigationTitle("Activity")
            }
        }

        CatalogueCaption("Loading")
        MonarchWidget("Recent activity") {
            MonarchActivityTimeline([], isGroupedByDay: false, density: .compact, isLoading: true)
        }

        CatalogueCaption("Empty")
        MonarchWidget("Recent activity") {
            MonarchActivityTimeline(
                [],
                isGroupedByDay: false,
                density: .compact,
                labels: MonarchActivityTimelineLabels(empty: "No activity yet")
            )
        }

        CatalogueCaption("Failed")
        MonarchWidget(
            "Recent activity",
            state: .failed(),
            isRetrying: isRetrying,
            onRetry: { isRetrying = true },
            content: { EmptyView() }
        )
    }
}
