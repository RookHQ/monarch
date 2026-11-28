@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiAgentPickerSection: CataloguePage {
    static let title = "AI agent picker"

    @State private var agent: String? = "research"
    @State private var gridAgent: String?

    private static let agents = [
        MonarchAiAgentPickerOption(
            MonarchAiAgentRef(id: "research", name: "Research agent", role: "Finds and summarises sources"),
            description: "Reads documents in this space and answers with citations.",
            capabilities: ["Summarises", "Cites sources"],
            tools: [MonarchAiAgentTool("Search documents"), MonarchAiAgentTool("Read file")]
        ),
        MonarchAiAgentPickerOption(
            MonarchAiAgentRef(id: "writer", name: "Writing agent", role: "Drafts and edits text"),
            description: "Drafts replies and tightens wording in the tone you choose.",
            capabilities: ["Drafts", "Rewrites"],
            tools: [MonarchAiAgentTool("Insert text")],
            state: .working
        ),
        MonarchAiAgentPickerOption(
            MonarchAiAgentRef(id: "scheduler", name: "Scheduling agent", role: "Plans meetings"),
            description: "Finds times that work and drafts invitations. Not enabled in this space.",
            isDisabled: true
        ),
    ]

    var body: some View {
        CatalogueCaption("List")
        MonarchAiAgentPicker("Agent", selection: $agent, agents: Self.agents)
        Text("Value: \(agent ?? "none")").monarchTextStyle(.caption)

        CatalogueCaption("Grid (two columns on a regular width)")
        MonarchAiAgentPicker(selection: $gridAgent, agents: Self.agents, layout: .grid)

        CatalogueCaption("No agents")
        MonarchAiAgentPicker("Agent", selection: .constant(nil), agents: [])
    }
}
