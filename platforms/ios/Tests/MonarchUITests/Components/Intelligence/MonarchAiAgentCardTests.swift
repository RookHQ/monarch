import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let agent = MonarchAiAgentRef(id: "research", name: "Research agent", role: "Finds sources")
private let bare = MonarchAiAgentRef(name: "Helper")
private let tools = [
    MonarchAiAgentTool("Search documents", description: "Reads files in this space"),
    MonarchAiAgentTool("Send email"),
]

enum MonarchAiAgentCardFixtures: ComponentFixtures {
    static let id = "ai-agent-card"
    static let fixtures = [
        Fixture("full, every section, with actions") {
            MonarchAiAgentCard(
                agent,
                description: "Finds and summarises sources for your documents.",
                capabilities: ["Searches the web", "Drafts summaries", "Cites every claim"],
                tools: tools,
                dataAccess: ["Documents in this space", "Your calendar"],
                owner: "Platform team"
            ) {
                MonarchButton("Start a chat", variant: .primary, size: .sm) {}
                MonarchButton("Settings", variant: .secondary, size: .sm) {}
            }
        },
        Fixture("every state") {
            VStack {
                ForEach(MonarchAiAgentState.allCases, id: \.self) { state in
                    MonarchAiAgentCard(agent, description: "State \(state.rawValue)", state: state)
                }
            }
        },
        Fixture("compact") {
            MonarchAiAgentCard(
                agent,
                description: "Finds and summarises sources.",
                capabilities: ["Searches the web", "Drafts summaries"],
                tools: tools,
                dataAccess: ["Hidden in compact"],
                variant: .compact
            )
        },
        Fixture("name only") {
            MonarchAiAgentCard(bare)
        },
        Fixture("long text") {
            MonarchAiAgentCard(
                MonarchAiAgentRef(
                    name: "Quarterly compliance review and evidence collection agent",
                    role: "Collects evidence for audits across every connected system"
                ),
                description: String(repeating: "It reads policies and prepares evidence. ", count: 4),
                capabilities: ["Reads policies", "Prepares evidence folders for auditors"],
                owner: "Governance, risk and compliance team"
            )
        },
    ]
}

@Suite("MonarchAiAgentCard")
struct MonarchAiAgentCardTests {
    private typealias Tokens = MonarchAiAgentCardTokens
    private let labels = MonarchAiAgentCardLabels()

    @Test("The role line ends with AI agent")
    func subtitle() {
        #expect(Tokens.subtitle(agent, labels: labels) == "Finds sources · AI agent")
        #expect(Tokens.subtitle(bare, labels: labels) == "AI agent")
        #expect(Tokens.subtitle(MonarchAiAgentRef(name: "X", role: "  "), labels: labels) == "AI agent")
        #expect(Tokens.subtitle(agent, labels: MonarchAiAgentCardLabels(kind: "Bot")) == "Finds sources · Bot")
    }

    @Test("The full card lists the sections that have content, in order")
    func sections() {
        let all = Tokens.sections(
            variant: .full,
            capabilities: ["a"],
            tools: tools,
            dataAccess: ["d"],
            owner: "Team"
        )
        #expect(all == [.capabilities, .tools, .dataAccess, .owner])
        let some = Tokens.sections(variant: .full, capabilities: [], tools: tools, dataAccess: [], owner: "")
        #expect(some == [.tools])
        let compact = Tokens.sections(
            variant: .compact,
            capabilities: ["a"],
            tools: tools,
            dataAccess: ["d"],
            owner: "Team"
        )
        #expect(compact.isEmpty)
    }

    @Test("Only the compact card counts its tools")
    func toolCount() {
        #expect(Tokens.toolCount(1, variant: .compact, labels: labels) == "1 tool")
        #expect(Tokens.toolCount(3, variant: .compact, labels: labels) == "3 tools")
        #expect(Tokens.toolCount(0, variant: .compact, labels: labels) == nil)
        #expect(Tokens.toolCount(3, variant: .full, labels: labels) == nil)
        let custom = MonarchAiAgentCardLabels(toolCount: { "\($0) Werkzeuge" })
        #expect(Tokens.toolCount(2, variant: .compact, labels: custom) == "2 Werkzeuge")
    }

    @Test("The avatar is large on the full card and medium on the compact one")
    func avatarSize() {
        #expect(Tokens.avatarSize(.full) == .lg)
        #expect(Tokens.avatarSize(.compact) == .md)
    }

    @Test("Tools and the owner are read as one phrase each")
    func spokenText() {
        #expect(Tokens.toolLabel(tools[0]) == "Search documents, Reads files in this space")
        #expect(Tokens.toolLabel(tools[1]) == "Send email")
        #expect(Tokens.ownerLabel("Platform team", labels: labels) == "Owner, Platform team")
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchAiAgentCardStrings.capabilities == "What it can do")
        #expect(MonarchAiAgentCardStrings.dataAccess == "Data it can see")
        for value in [MonarchAiAgentCardStrings.tools, MonarchAiAgentCardStrings.owner] {
            #expect(!value.hasPrefix("monarch."))
        }
    }
}
