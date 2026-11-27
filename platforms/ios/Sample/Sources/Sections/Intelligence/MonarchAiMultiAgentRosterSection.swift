@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiMultiAgentRosterSection: CataloguePage {
    static let title = "AI multi-agent roster"

    @State private var members = [
        MonarchAiMultiAgentRosterMember(
            MonarchAiAgentRef(id: "c", name: "Coordinator", role: "Plans the work"),
            state: .working,
            task: "Splitting the request"
        ),
        MonarchAiMultiAgentRosterMember(
            MonarchAiAgentRef(id: "r", name: "Research agent"),
            state: .waiting,
            task: "Needs access to the CRM"
        ),
        MonarchAiMultiAgentRosterMember(MonarchAiAgentRef(id: "w", name: "Writing agent"), state: .error),
        MonarchAiMultiAgentRosterMember(MonarchAiAgentRef(id: "q", name: "Review agent")),
        MonarchAiMultiAgentRosterMember(MonarchAiAgentRef(id: "f", name: "Fact checker"), state: .working),
    ]

    var body: some View {
        CatalogueCaption("List")
        MonarchAiMultiAgentRoster(members)
        Button("Advance the research agent") {
            members[1].state = members[1].state == .waiting ? .working : .waiting
        }
        .buttonStyle(.monarch(.secondary, size: .sm))

        CatalogueCaption("Stack")
        MonarchAiMultiAgentRoster(members, variant: .stack, maxVisible: 3)

        CatalogueCaption("Empty")
        MonarchAiMultiAgentRoster([])
    }
}
