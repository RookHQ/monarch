import MonarchUI
import SwiftUI

struct MonarchAiAgentRunTimelineSection: CataloguePage {
    static let title = "AI agent run timeline"

    @State private var opened = "Open an event and show it in the conversation"

    private static let start = Date.now

    private static let events = [
        MonarchAiRunEvent(
            id: "1",
            kind: .model,
            name: "rook-large",
            startedAt: start,
            endedAt: start.addingTimeInterval(1.2),
            inputTokens: 1_200,
            outputTokens: 310,
            costMicros: 4_100,
            partId: "text-1"
        ),
        MonarchAiRunEvent(
            id: "2",
            kind: .tool,
            name: "search_invoices",
            startedAt: start.addingTimeInterval(1.3),
            endedAt: start.addingTimeInterval(2.9),
            detail: "{\"customer\": \"C-104\", \"results\": 12}",
            partId: "call-1"
        ),
        MonarchAiRunEvent(
            id: "3",
            kind: .handoff,
            name: "Billing agent",
            startedAt: start.addingTimeInterval(3),
            endedAt: start.addingTimeInterval(3.05)
        ),
        MonarchAiRunEvent(
            id: "4",
            kind: .model,
            name: "rook-small",
            startedAt: start.addingTimeInterval(3.1),
            inputTokens: 640,
            costMicros: 900
        ),
    ]

    var body: some View {
        CatalogueCaption("A run with an event still running")
        MonarchAiAgentRunTimeline(Self.events, now: Self.start.addingTimeInterval(4.4)) { event in
            opened = "Show \(event.partId ?? event.messageId ?? event.id) in the conversation"
        }
        Text(opened).monarchTextStyle(.caption)

        CatalogueCaption("No events")
        MonarchAiAgentRunTimeline([])
    }
}
