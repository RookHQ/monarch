import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let assistant = MonarchAiAgentRef(id: "research", name: "Research agent", codename: "Ra")

enum MonarchAiConversationHeaderFixtures: ComponentFixtures {
    static let id = "ai-conversation-header"
    static let fixtures = [
        Fixture("title, agent, model and actions") {
            MonarchAiConversationHeader(
                "Quarterly planning notes",
                agent: assistant,
                model: "Fast model",
                onRename: { _ in },
                actions: {
                    Button("Share", systemImage: "square.and.arrow.up") {}
                    Button("Delete", systemImage: "trash", role: .destructive) {}
                }
            )
        },
        Fixture("rename only") {
            MonarchAiConversationHeader("Trip ideas", onRename: { _ in })
        },
        Fixture("plain title") {
            MonarchAiConversationHeader("Trip ideas", agent: assistant)
        },
        Fixture("untitled") {
            MonarchAiConversationHeader("  ", model: "Fast model")
        },
        Fixture("naming") {
            MonarchAiConversationHeader("", agent: assistant, isNaming: true, onRename: { _ in })
        },
        Fixture("long title") {
            MonarchAiConversationHeader(
                "A very long conversation title about the quarterly planning review and the hiring plan for next year",
                agent: MonarchAiAgentRef(name: "Planning and forecasting assistant for the finance team"),
                model: "Large reasoning model with long context",
                onRename: { _ in }
            )
        },
        Fixture("custom labels") {
            MonarchAiConversationHeader("", labels: MonarchAiConversationHeaderLabels(untitled: "Untitled chat"))
        },
    ]
}

@Suite("MonarchAiConversationHeader")
struct MonarchAiConversationHeaderTests {
    private typealias Tokens = MonarchAiConversationHeaderTokens
    private let labels = MonarchAiConversationHeaderLabels()

    @Test("The shown title is trimmed, falls back to untitled, and shows naming or a pending rename")
    func displayTitle() {
        #expect(
            Tokens.displayTitle("  Trip ideas ", isNaming: false, pendingTitle: nil, labels: labels) == "Trip ideas"
        )
        #expect(Tokens.displayTitle(" ", isNaming: false, pendingTitle: nil, labels: labels) == "New conversation")
        #expect(
            Tokens.displayTitle("Trip", isNaming: true, pendingTitle: nil, labels: labels) == "Naming conversation…"
        )
        #expect(Tokens.displayTitle("Trip", isNaming: false, pendingTitle: "Holiday", labels: labels) == "Holiday")
        #expect(
            Tokens.displayTitle("Trip", isNaming: true, pendingTitle: "Holiday", labels: labels)
                == "Naming conversation…"
        )
        let custom = MonarchAiConversationHeaderLabels(untitled: "Untitled", naming: "Thinking of a name")
        #expect(Tokens.displayTitle("", isNaming: false, pendingTitle: nil, labels: custom) == "Untitled")
        #expect(Tokens.displayTitle("", isNaming: true, pendingTitle: nil, labels: custom) == "Thinking of a name")
    }

    @Test("Only a trimmed, non-empty, changed title is saved")
    func renamed() {
        #expect(Tokens.renamedTitle("  Holiday  ", current: "Trip") == "Holiday")
        #expect(Tokens.renamedTitle("   ", current: "Trip") == nil)
        #expect(Tokens.renamedTitle("Trip ", current: " Trip") == nil)
        #expect(Tokens.renamedTitle("Trip", current: "") == "Trip")
    }

    @Test("The line under the title joins the agent and the model")
    func subtitle() {
        #expect(Tokens.subtitle(agent: assistant, model: "Fast model") == "Research agent · Fast model")
        #expect(Tokens.subtitle(agent: assistant, model: nil) == "Research agent")
        #expect(Tokens.subtitle(agent: nil, model: " Fast model ") == "Fast model")
        #expect(Tokens.subtitle(agent: nil, model: "  ") == nil)
        #expect(Tokens.subtitle(agent: nil, model: nil) == nil)
    }

    @Test("VoiceOver reads the assistant and the model as the value")
    func accessibilityValue() {
        #expect(
            Tokens.accessibilityValue(agent: assistant, model: "Fast model", labels: labels)
                == "AI assistant: Research agent. Model: Fast model"
        )
        #expect(Tokens.accessibilityValue(agent: nil, model: "Fast", labels: labels) == "Model: Fast")
        #expect(Tokens.accessibilityValue(agent: nil, model: nil, labels: labels).isEmpty)
        let custom = MonarchAiConversationHeaderLabels(agentPrefix: "Bot", modelPrefix: "Engine")
        #expect(
            Tokens.accessibilityValue(agent: assistant, model: "X", labels: custom) == "Bot: Research agent. Engine: X"
        )
    }

    @Test("The title is a menu only when there is something in it")
    func hasMenu() {
        #expect(Tokens.hasMenu(canRename: true, hasActions: false))
        #expect(Tokens.hasMenu(canRename: false, hasActions: true))
        #expect(!Tokens.hasMenu(canRename: false, hasActions: false))
    }

    @Test("Colour roles: the title in the default text colour, the rest muted")
    func roles() {
        #expect(Tokens.titleRole == \.textDefault)
        #expect(Tokens.secondaryRole == \.textMuted)
    }

    @Test("Built-in strings resolve")
    func builtIn() {
        let all = [
            MonarchAiConversationHeaderStrings.untitled, MonarchAiConversationHeaderStrings.rename,
            MonarchAiConversationHeaderStrings.titleLabel, MonarchAiConversationHeaderStrings.save,
            MonarchAiConversationHeaderStrings.cancel, MonarchAiConversationHeaderStrings.naming,
            MonarchAiConversationHeaderStrings.agentPrefix, MonarchAiConversationHeaderStrings.modelPrefix,
            MonarchAiConversationHeaderStrings.renameFailed, MonarchAiConversationHeaderStrings.menuHint,
        ]
        for string in all {
            #expect(!string.hasPrefix("monarch."))
        }
    }
}
