import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchAiAgentPickerFixtures: ComponentFixtures {
    static let id = "ai-agent-picker"

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
            tools: [MonarchAiAgentTool("Insert text")],
            state: .working
        ),
        MonarchAiAgentPickerOption(
            MonarchAiAgentRef(name: "Scheduling agent"),
            description: "Finds times that work. Not enabled in this space.",
            isDisabled: true
        ),
    ]

    static let fixtures = [
        Fixture("list, one chosen, busy and unavailable agents") {
            MonarchAiAgentPicker("Agent", selection: .constant("research"), agents: agents)
        },
        Fixture("grid, nothing chosen, no label") {
            MonarchAiAgentPicker(selection: .constant(nil), agents: agents, layout: .grid)
        },
        Fixture("empty") {
            MonarchAiAgentPicker("Agent", selection: .constant(nil), agents: [])
        },
        Fixture("every state of the avatar") {
            MonarchAiAgentPicker(
                "Agent",
                selection: .constant("waiting"),
                agents: MonarchAiAgentState.allCases.map { state in
                    MonarchAiAgentPickerOption(
                        MonarchAiAgentRef(
                            id: state.rawValue,
                            name: "Agent in the \(state.rawValue) state with a long name"
                        ),
                        state: state
                    )
                }
            )
        },
        Fixture("disabled") {
            MonarchAiAgentPicker("Agent", selection: .constant("writer"), agents: agents).disabled(true)
        },
    ]
}

@Suite("MonarchAiAgentPicker")
struct MonarchAiAgentPickerTests {
    private typealias Tokens = MonarchAiAgentPickerTokens

    private let research = MonarchAiAgentPickerOption(
        MonarchAiAgentRef(id: "research", name: "Research agent", role: "Finds sources"),
        description: "Reads documents.",
        capabilities: ["Summarises", "Cites sources"],
        tools: [MonarchAiAgentTool("Search"), MonarchAiAgentTool("Read")]
    )
    private let unavailable = MonarchAiAgentPickerOption(
        MonarchAiAgentRef(name: "Scheduling agent"),
        state: .working,
        isDisabled: true
    )

    @Test("An agent's key is its id, or its name without one")
    func key() {
        #expect(MonarchAiAgentPicker.key(for: MonarchAiAgentRef(id: "a1", name: "Ana")) == "a1")
        #expect(MonarchAiAgentPicker.key(for: MonarchAiAgentRef(name: "Ana")) == "Ana")
        #expect(research.id == "research")
        #expect(unavailable.id == "Scheduling agent")
    }

    @Test("Tapping chooses an available agent and leaves the choice for an unavailable one")
    func choosing() {
        #expect(Tokens.choosing(research, current: nil) == "research")
        #expect(Tokens.choosing(research, current: "research") == "research")
        #expect(Tokens.choosing(unavailable, current: "research") == "research")
        #expect(Tokens.choosing(unavailable, current: nil) == nil)
    }

    @Test("The grid has two columns only on a regular width at standard text sizes")
    func columns() {
        #expect(Tokens.columnCount(layout: .list, isCompactWidth: false, isAccessibilitySize: false) == 1)
        #expect(Tokens.columnCount(layout: .grid, isCompactWidth: false, isAccessibilitySize: false) == 2)
        #expect(Tokens.columnCount(layout: .grid, isCompactWidth: true, isAccessibilitySize: false) == 1)
        #expect(Tokens.columnCount(layout: .grid, isCompactWidth: false, isAccessibilitySize: true) == 1)
    }

    @Test("The chosen agent has the accent border and mark, and a different mark shape")
    func appearance() {
        let chosen = Tokens.appearance(isSelected: true, isPressed: false, isEnabled: true)
        let other = Tokens.appearance(isSelected: false, isPressed: false, isEnabled: true)
        let off = Tokens.appearance(isSelected: true, isPressed: false, isEnabled: false)
        #expect(chosen.border == \MonarchColorTokens.aiSolid)
        #expect(chosen.mark == \MonarchColorTokens.aiSolid)
        #expect(other.border == \MonarchColorTokens.borderDefault)
        #expect(off.mark == \MonarchColorTokens.textDisabled)
        #expect(
            Tokens.appearance(isSelected: false, isPressed: true, isEnabled: true).border
                == \MonarchColorTokens.borderStrong
        )
        #expect(Tokens.markSymbol(isSelected: true) != Tokens.markSymbol(isSelected: false))
    }

    @Test("VoiceOver reads the name and role, then the state, description, capabilities and tools")
    func accessibility() {
        let none = MonarchAiAgentPickerLabels()
        #expect(Tokens.accessibilityLabel(research, labels: none) == "Research agent, Finds sources · AI agent")
        #expect(
            Tokens.accessibilityValue(research, labels: none)
                == "Reads documents., Summarises, Cites sources, 2 tools"
        )
        #expect(Tokens.accessibilityLabel(unavailable, labels: none) == "Scheduling agent, AI agent")
        #expect(Tokens.accessibilityValue(unavailable, labels: none) == "Working, Unavailable")
        let custom = MonarchAiAgentPickerLabels(unavailable: "Off", card: MonarchAiAgentCardLabels(kind: "Bot"))
        #expect(Tokens.accessibilityLabel(unavailable, labels: custom) == "Scheduling agent, Bot")
        #expect(Tokens.accessibilityValue(unavailable, labels: custom).hasSuffix("Off"))
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchAiAgentPickerStrings.label == "Choose an agent")
        #expect(MonarchAiAgentPickerStrings.empty == "No agents are available.")
        #expect(MonarchAiAgentPickerStrings.unavailable == "Unavailable")
    }
}
