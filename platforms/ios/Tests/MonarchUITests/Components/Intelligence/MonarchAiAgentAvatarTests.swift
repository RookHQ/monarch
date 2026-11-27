import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiAgentAvatarFixtures: ComponentFixtures {
    static let id = "ai-agent-avatar"
    static let agent = MonarchAiAgentRef(id: "research", name: "Research agent", codename: "Ra")

    static let fixtures = [
        Fixture("every size, idle") {
            HStack {
                ForEach(MonarchAvatarSize.allCases, id: \.self) { size in
                    MonarchAiAgentAvatar(agent, size: size)
                }
            }
        },
        Fixture("every state, with state text") {
            VStack(alignment: .leading) {
                ForEach(MonarchAiAgentState.allCases, id: \.self) { state in
                    MonarchAiAgentAvatar(agent, state: state, size: .lg, showsState: true)
                }
            }
        },
        Fixture("image, decorative, working") {
            MonarchAiAgentAvatar(agent, image: Image(systemName: "sparkles"), state: .working, isDecorative: true)
        },
        Fixture("no letters in the name") {
            MonarchAiAgentAvatar(MonarchAiAgentRef(name: "🤖"), state: .waiting, size: .sm)
        },
        Fixture("long state text") {
            MonarchAiAgentAvatar(
                agent,
                state: .waiting,
                showsState: true,
                labels: MonarchAiAgentAvatarLabels(state: { _ in "Waiting for your approval of the refund" })
            )
        },
    ]
}

@Suite("MonarchAiAgentAvatar")
struct MonarchAiAgentAvatarTests {
    private let labels = MonarchAiAgentAvatarLabels()

    @Test("The name, the kind and a non-idle state are read")
    func label() {
        #expect(
            MonarchAiAgentAvatarTokens.accessibilityLabel(
                name: "Research agent",
                state: .idle,
                isDecorative: false,
                labels: labels
            ) == "Research agent, AI agent"
        )
        #expect(
            MonarchAiAgentAvatarTokens.accessibilityLabel(
                name: "Research agent",
                state: .working,
                isDecorative: false,
                labels: labels
            ) == "Research agent, AI agent, Working"
        )
        #expect(
            MonarchAiAgentAvatarTokens.accessibilityLabel(
                name: "Research agent",
                state: .waiting,
                isDecorative: false,
                labels: labels
            ) == "Research agent, AI agent, Waiting for you"
        )
    }

    @Test("A decorative avatar is hidden unless it has a state")
    func decorative() {
        #expect(
            MonarchAiAgentAvatarTokens.accessibilityLabel(name: "A", state: .idle, isDecorative: true, labels: labels)
                == nil
        )
        #expect(
            MonarchAiAgentAvatarTokens.accessibilityLabel(name: "A", state: .error, isDecorative: true, labels: labels)
                == "Error"
        )
    }

    @Test("Labels replace the kind and the states")
    func overrides() {
        let custom = MonarchAiAgentAvatarLabels(kind: "Agent IA", state: { $0 == .working ? "Au travail" : "?" })
        #expect(
            MonarchAiAgentAvatarTokens.accessibilityLabel(
                name: "Rex",
                state: .working,
                isDecorative: false,
                labels: custom
            ) == "Rex, Agent IA, Au travail"
        )
    }

    @Test("Every built-in string resolves")
    func strings() {
        #expect(MonarchAiAgentAvatarStrings.kind == "AI agent")
        let names = MonarchAiAgentState.allCases.map { MonarchAiAgentAvatarTokens.stateName($0, labels: labels) }
        #expect(names == ["Idle", "Working", "Waiting for you", "Error"])
    }

    @Test("Each state has its own ring, and error its own badge")
    func rings() {
        #expect(MonarchAiAgentAvatarTokens.ring(.idle) == .none)
        #expect(MonarchAiAgentAvatarTokens.ring(.working) == .moving(\.aiSolid, \.aiBorder))
        #expect(MonarchAiAgentAvatarTokens.ring(.waiting) == .dashed(\.warningStrong))
        #expect(MonarchAiAgentAvatarTokens.ring(.error) == .solid(\.dangerStrong))
        #expect(MonarchAiAgentAvatarTokens.badge(.error).fill == \.dangerSolid)
        #expect(MonarchAiAgentAvatarTokens.badge(.working).fill == \.aiSolid)
        #expect(MonarchAiAgentAvatarTokens.badge(.idle).glyph == \.aiOnSolid)
    }

    @Test("The working ring turns once per period, from an injected time")
    func ringAngle() {
        let period = MonarchAiAgentAvatarTokens.ringPeriod
        #expect(MonarchAiAgentAvatarTokens.ringAngle(at: 0).degrees == 0)
        #expect(abs(MonarchAiAgentAvatarTokens.ringAngle(at: period / 4).degrees - 90) < 0.0001)
        #expect(abs(MonarchAiAgentAvatarTokens.ringAngle(at: period * 3.5).degrees - 180) < 0.0001)
        #expect(abs(MonarchAiAgentAvatarTokens.ringAngle(at: -period / 4).degrees - 270) < 0.0001)
    }

    @Test("Initials come from the codename, else the name; no letters gives the chip symbol")
    func initials() {
        #expect(
            MonarchAiAgentAvatarTokens.initialsSource(MonarchAiAgentRef(name: "Research agent", codename: "Ra")) == "Ra"
        )
        #expect(
            MonarchAiAgentAvatarTokens.initialsSource(MonarchAiAgentRef(name: "Research agent", codename: "  "))
                == "Research agent"
        )
        #expect(MonarchAiAgentAvatarTokens.fallbackSymbol(MonarchAiAgentRef(name: "Research agent")) == nil)
        #expect(MonarchAiAgentAvatarTokens.fallbackSymbol(MonarchAiAgentRef(name: "🤖")) == "cpu")
    }

    @Test("Only web addresses are loaded")
    func imageURL() {
        #expect(
            MonarchAiAgentAvatarTokens.imageURL(MonarchAiAgentRef(name: "A", avatar: "https://example.com/a.png"))
                != nil
        )
        #expect(MonarchAiAgentAvatarTokens.imageURL(MonarchAiAgentRef(name: "A", avatar: "javascript:alert(1)")) == nil)
        #expect(MonarchAiAgentAvatarTokens.imageURL(MonarchAiAgentRef(name: "A")) == nil)
    }
}
