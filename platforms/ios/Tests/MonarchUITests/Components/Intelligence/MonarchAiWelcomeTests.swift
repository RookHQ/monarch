import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiWelcomeFixtures: ComponentFixtures {
    static let id = "ai-welcome"

    static let agent = MonarchAiAgentRef(name: "Dossier assistant", role: "Writing assistant")
    static let starters = [
        MonarchAiStarterPrompt(id: "sum", title: "Summarise this page", prompt: "Summarise this page."),
        MonarchAiStarterPrompt(id: "next", title: "Suggest next steps", prompt: "What should I do next?"),
    ]

    static let fixtures = [
        Fixture("complete") {
            MonarchAiWelcome(
                "How can I help?",
                agent: agent,
                description: "I can read the page you are on and the documents you share with me.",
                capabilities: ["Summarise long documents", "Draft replies in your tone"],
                starters: { MonarchAiStarterPrompts(starters) { _ in } },
                footer: { MonarchAiDisclaimer() }
            )
        },
        Fixture("title only") {
            MonarchAiWelcome("Ask about your projects")
        },
        Fixture("no agent, starters") {
            MonarchAiWelcome("Start a conversation", description: "Ask anything.") {
                MonarchAiStarterPrompts(starters, layout: .list) { _ in }
            }
        },
        Fixture("long text") {
            MonarchAiWelcome(
                "A greeting that is long enough to wrap onto several lines at the largest text size",
                agent: MonarchAiAgentRef(name: "An assistant with a long display name", codename: "Lo"),
                description: String(repeating: "A description that wraps. ", count: 6),
                capabilities: [String(repeating: "A capability that wraps across lines. ", count: 3), "  "]
            )
        },
    ]
}

@Suite("MonarchAiWelcome")
struct MonarchAiWelcomeTests {
    @Test("Blank capabilities are left out")
    func capabilities() {
        #expect(MonarchAiWelcomeTokens.capabilities(["Draft", " ", "", "Summarise"]) == ["Draft", "Summarise"])
        #expect(MonarchAiWelcomeTokens.capabilities([]).isEmpty)
    }

    @Test("The agent line reads the prefix, the name and the role")
    func agentLabel() {
        let agent = MonarchAiAgentRef(name: "Dossier assistant", role: "Writing assistant")
        #expect(
            MonarchAiWelcomeTokens.agentLabel(agent, prefix: nil)
                == "AI assistant: Dossier assistant, Writing assistant"
        )
        #expect(
            MonarchAiWelcomeTokens.agentLabel(MonarchAiAgentRef(name: "Ra"), prefix: "Assistant") == "Assistant: Ra"
        )
        #expect(
            MonarchAiWelcomeTokens.agentLabel(MonarchAiAgentRef(name: "Ra", role: ""), prefix: nil)
                == "AI assistant: Ra"
        )
    }

    @Test("The bot mark and check marks use the AI roles")
    func colours() {
        #expect(MonarchAiWelcomeTokens.markFill == \MonarchColorTokens.aiSubtle)
        #expect(MonarchAiWelcomeTokens.markGlyph == \MonarchColorTokens.aiStrong)
        #expect(MonarchAiWelcomeTokens.checkRole == \MonarchColorTokens.aiStrong)
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAiWelcomeStrings.capabilities == "What I can help with")
        #expect(MonarchAiWelcomeStrings.agentPrefix == "AI assistant")
        #expect(MonarchAiWelcomeStrings.agentLabel(prefix: "A", name: "B") == "A: B")
    }

    @Test("Starters and capabilities make the welcome taller")
    @MainActor
    func layout() {
        let bare = Host.layout(MonarchAiWelcome("How can I help?"))
        let full = Host.layout(
            MonarchAiWelcome("How can I help?", capabilities: ["Draft replies"]) {
                MonarchAiStarterPrompts(MonarchAiWelcomeFixtures.starters, layout: .list) { _ in }
            }
        )
        #expect(full.height > bare.height)
    }
}
