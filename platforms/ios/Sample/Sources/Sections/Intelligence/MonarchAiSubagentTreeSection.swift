@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiSubagentTreeSection: CataloguePage {
    static let title = "AI sub-agent tree"

    @State private var selection: String?
    @State private var last = "Select an agent."
    @State private var agents: [MonarchAiSubagent] = [
        MonarchAiSubagent(
            id: "lead",
            name: "Lead",
            role: "Write the market report",
            status: .working,
            activity: "Waiting for the researchers",
            stepsDone: 2,
            stepsTotal: 5,
            toolCalls: 4,
            startedAt: Date(timeIntervalSince1970: 1_772_000_000),
            children: [
                MonarchAiSubagent(
                    id: "researcher",
                    name: "Researcher",
                    role: "Find the three latest filings",
                    status: .working,
                    activity: "Reading the 2025 annual report",
                    stepsDone: 1,
                    toolCalls: 6,
                    partId: "call-7"
                ),
                MonarchAiSubagent(id: "analyst", name: "Analyst", role: "Compare the margins", status: .needsInput),
                MonarchAiSubagent(id: "writer", name: "Writer", status: .waiting),
            ]
        )
    ]

    var body: some View {
        MonarchAiSubagentTree(
            agents,
            selection: $selection,
            onRespond: { agent in last = "Respond to \(agent.name)" },
            onOpen: { agent in last = "Open \(agent.name)" },
            onOpenPart: { agent in last = "Show \(agent.partId ?? agent.name) in the conversation" },
            onStop: { agent in
                last = "Stopped \(agent.name)"
                setStatus(agent.id, .stopped)
            }
        )
        Text(last).monarchTextStyle(.caption)
        Button("Finish the researcher") { setStatus("researcher", .done) }
            .buttonStyle(.monarch(.secondary, size: .sm))
    }

    private func setStatus(_ id: String, _ status: MonarchAiSubagentStatus) {
        func update(_ list: inout [MonarchAiSubagent]) {
            for index in list.indices {
                if list[index].id == id { list[index].status = status }
                update(&list[index].children)
            }
        }
        update(&agents)
    }
}
