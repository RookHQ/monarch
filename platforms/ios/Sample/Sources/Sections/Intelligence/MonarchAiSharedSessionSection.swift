@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiSharedSessionSection: CataloguePage {
    static let title = "AI shared session"

    @State private var queue = [
        MonarchAiSharedSessionRequest(id: "r1", participantID: "sam", preview: "Compare the Q3 numbers with last year"),
        MonarchAiSharedSessionRequest(id: "r2", participantID: "maya", preview: "Draft the summary for the board"),
        MonarchAiSharedSessionRequest(id: "r3", participantID: "jo", preview: "List the open risks"),
    ]
    @State private var following: Set<String> = []
    @State private var invited = false
    @State private var accessRequested = false

    private let participants = [
        MonarchAiSharedSessionParticipant(id: "maya", name: "Maya", role: .owner, isYou: true),
        MonarchAiSharedSessionParticipant(id: "sam", name: "Sam Lee", activity: .typing),
        MonarchAiSharedSessionParticipant(id: "jo", name: "Jo", presence: .idle, activity: .reviewing),
        MonarchAiSharedSessionParticipant(id: "kim", name: "Kim", presence: .away),
        MonarchAiSharedSessionParticipant(id: "ra", name: "Research agent", kind: .agent),
    ]

    var body: some View {
        CatalogueCaption("As the owner")
        MonarchAiSharedSession(
            participants,
            answering: MonarchAiSharedSessionAnswering(
                participantID: "ra",
                forParticipantID: "sam",
                label: "Summarise the notes"
            ),
            queue: queue,
            maxVisible: 3,
            following: $following,
            onWithdraw: { id in queue.removeAll { $0.id == id } },
            onReorder: { ids, _ in
                queue = ids.compactMap { id in queue.first { $0.id == id } }
            },
            onRemove: { id in queue.removeAll { $0.id == id } },
            onInvite: { invited = true }
        )
        if invited {
            Text("The product's invite flow opens here.").monarchTextStyle(.caption)
        }

        CatalogueCaption("As a viewer")
        MonarchAiSharedSession(
            [
                MonarchAiSharedSessionParticipant(id: "v", name: "Alex", role: .viewer, isYou: true),
                MonarchAiSharedSessionParticipant(id: "maya", name: "Maya", role: .owner),
            ],
            isAccessRequested: accessRequested,
            onRequestAccess: { accessRequested = true }
        )
    }
}
