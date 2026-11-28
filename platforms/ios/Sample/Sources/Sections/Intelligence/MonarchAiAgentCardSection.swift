@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiAgentCardSection: CataloguePage {
    static let title = "AI agent card"

    private let agent = MonarchAiAgentRef(id: "research", name: "Research agent", role: "Finds sources")
    @State private var state = MonarchAiAgentState.idle
    @State private var chats = 0

    var body: some View {
        CatalogueCaption("Full")
        MonarchAiAgentCard(
            agent,
            description: "Finds and summarises sources for your documents.",
            capabilities: ["Searches the web", "Drafts summaries", "Cites every claim"],
            tools: [
                MonarchAiAgentTool("Search documents", description: "Reads files in this space"),
                MonarchAiAgentTool("Browse the web", description: "Opens public pages"),
            ],
            dataAccess: ["Documents in this space", "Your calendar"],
            owner: "Platform team",
            state: state
        ) {
            MonarchButton("Start a chat", variant: .primary, size: .sm) { chats += 1 }
        }
        Text("Chats started: \(chats)").monarchTextStyle(.caption)

        CatalogueCaption("State")
        Picker("State", selection: $state) {
            Text("Idle").tag(MonarchAiAgentState.idle)
            Text("Working").tag(MonarchAiAgentState.working)
            Text("Waiting").tag(MonarchAiAgentState.waiting)
            Text("Error").tag(MonarchAiAgentState.error)
        }
        .pickerStyle(.segmented)

        CatalogueCaption("Compact")
        MonarchAiAgentCard(
            agent,
            description: "Finds and summarises sources.",
            capabilities: ["Searches the web", "Drafts summaries"],
            tools: [MonarchAiAgentTool("Search documents"), MonarchAiAgentTool("Browse the web")],
            variant: .compact
        )
    }
}
