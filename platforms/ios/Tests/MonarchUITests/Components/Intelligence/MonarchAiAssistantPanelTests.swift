import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let dossier = MonarchAiAgentRef(id: "dossier", name: "Dossier", role: "Workspace assistant")

private let page = MonarchAiContextItem(id: "page", kind: .page, label: "Quarterly plan", tokens: 1_200)
private let file = MonarchAiContextItem(id: "file", kind: .file, label: "contract.pdf", tokens: 8_400)

private let starters = [
    MonarchAiStarterPrompt(id: "sum", title: "Summarize this page", prompt: "Summarize this page."),
    MonarchAiStarterPrompt(id: "week", title: "What changed this week?", prompt: "What changed this week?"),
]

private let history = [
    MonarchAiConversationMessage(
        id: "u1",
        role: .user,
        parts: [.text(MonarchAiTextPart(text: "When is invoice 42 due?"))],
        createdAt: "2026-11-05T09:05:00.000Z"
    ),
    MonarchAiConversationMessage(
        id: "a1",
        role: .assistant,
        parts: [.text(MonarchAiTextPart(text: "Invoice 42 is due on **4 December**."))],
        createdAt: "2026-11-05T09:05:02.000Z"
    ),
]

/// A transport that answers every prompt at once with the same text.
private struct EchoTransport: MonarchAiTransport {
    func stream(_ request: MonarchAiTransportRequest) -> MonarchAiEventStream {
        MonarchAiEventStream { continuation in
            continuation.yield(.textDelta("You asked: " + (request.messages.last?.text ?? "")))
            continuation.yield(.done())
            continuation.finish()
        }
    }
}

enum MonarchAiAssistantPanelFixtures: ComponentFixtures {
    static let id = "ai-assistant-panel"
    static let fixtures = [
        Fixture("sheet, empty, starters and context") {
            MonarchAiAssistantPanel(
                "Assistant",
                messages: [],
                isPresented: .constant(true),
                placement: .sheet,
                agent: dossier,
                context: [page, file],
                starters: starters,
                onRemoveContext: { _ in },
                onSend: { _ in }
            )
        },
        Fixture("sheet, conversation, header actions and footer") {
            MonarchAiAssistantPanel(
                "Assistant",
                messages: history,
                isPresented: .constant(true),
                placement: .sheet,
                agent: dossier,
                headerActions: { Button("New conversation", systemImage: "square.and.pencil") {} },
                toolbar: {},
                footer: { MonarchAiDisclaimer() },
                onSend: { _ in }
            )
        },
        Fixture("inline, running") {
            MonarchAiAssistantPanel(
                "Assistant",
                messages: history,
                isPresented: .constant(true),
                placement: .inline,
                status: .streaming,
                agent: dossier,
                context: [page],
                onStop: {},
                onSend: { _ in }
            )
        },
        Fixture("inline, empty, no agent") {
            MonarchAiAssistantPanel(
                "Assistant",
                messages: [],
                isPresented: .constant(true),
                placement: .inline,
                starters: starters,
                onSend: { _ in }
            )
        },
        Fixture("awaiting approval, starters disabled") {
            MonarchAiAssistantPanel(
                "Assistant",
                messages: [],
                isPresented: .constant(true),
                placement: .inline,
                status: .awaitingApproval,
                starters: starters,
                onSend: { _ in }
            )
        },
        Fixture("offline, slow and error") {
            MonarchAiAssistantPanel(
                "Assistant",
                messages: history,
                isPresented: .constant(true),
                placement: .inline,
                status: .error,
                error: MonarchAiConversationError(
                    code: "network",
                    message: "The assistant could not answer.",
                    isRetryable: true
                ),
                isOffline: true,
                queuedCount: 2,
                isSlow: true,
                onSend: { _ in }
            )
        },
        Fixture("long title and agent") {
            MonarchAiAssistantPanel(
                "Planning assistant for the quarterly review and the hiring plan",
                messages: [],
                isPresented: .constant(true),
                placement: .inline,
                agent: MonarchAiAgentRef(
                    name: "Planning and forecasting assistant",
                    role: "Finance team assistant with a long role"
                ),
                onSend: { _ in }
            )
        },
        Fixture("custom labels and empty state") {
            MonarchAiAssistantPanel(
                "Helper",
                messages: [],
                isPresented: .constant(true),
                placement: .sheet,
                labels: MonarchAiAssistantPanelLabels(
                    close: "Close helper",
                    inputLabel: "Ask the helper",
                    welcome: "What do you need?"
                ),
                onSend: { _ in }
            )
            .emptyState { Text("Ask about this page.") }
        },
        Fixture("automatic, wired to a conversation") {
            MonarchAiAssistantPanel(
                "Assistant",
                conversation: MonarchAiConversation(transport: EchoTransport(), initialMessages: history),
                isPresented: .constant(true),
                context: [page],
                starters: starters
            )
        },
    ]
}

@Suite("MonarchAiAssistantPanel")
struct MonarchAiAssistantPanelTests {
    private typealias Tokens = MonarchAiAssistantPanelTokens

    @Test("An explicit placement wins, then the presenting modifier, then the width")
    func placement() {
        #expect(Tokens.resolvedPlacement(.inline, presentedAs: .sheet, isCompactWidth: true) == .inline)
        #expect(Tokens.resolvedPlacement(.sheet, presentedAs: .inline, isCompactWidth: false) == .sheet)
        #expect(Tokens.resolvedPlacement(.automatic, presentedAs: .inline, isCompactWidth: true) == .inline)
        #expect(Tokens.resolvedPlacement(.automatic, presentedAs: .sheet, isCompactWidth: false) == .sheet)
        #expect(Tokens.resolvedPlacement(.automatic, presentedAs: nil, isCompactWidth: true) == .sheet)
        #expect(Tokens.resolvedPlacement(.automatic, presentedAs: nil, isCompactWidth: false) == .inline)
        #expect(Tokens.resolvedPlacement(.automatic, presentedAs: .automatic, isCompactWidth: false) == .inline)
        for placement in MonarchAiAssistantPanelPlacement.allCases {
            for compact in [true, false] {
                #expect(Tokens.resolvedPlacement(placement, presentedAs: nil, isCompactWidth: compact) != .automatic)
            }
        }
    }

    @Test("Send becomes Stop and the avatar works only while a reply runs")
    func busy() {
        let busy: Set<MonarchAiConversationStatus> = [.submitted, .streaming]
        for status in MonarchAiConversationStatus.allCases {
            #expect(Tokens.isBusy(status) == busy.contains(status))
            #expect(Tokens.avatarState(status) == (busy.contains(status) ? .working : .idle))
            #expect(Tokens.closesOnEscape(status) == !busy.contains(status))
        }
    }

    @Test("Starters are disabled while a reply runs or a tool call waits for approval")
    func startersDisabled() {
        let disabled: Set<MonarchAiConversationStatus> = [.submitted, .streaming, .awaitingApproval]
        for status in MonarchAiConversationStatus.allCases {
            #expect(Tokens.areStartersDisabled(status) == disabled.contains(status))
        }
    }

    @Test("The welcome shows only while the conversation is empty")
    func welcome() {
        #expect(Tokens.showsWelcome(messageCount: 0))
        #expect(!Tokens.showsWelcome(messageCount: 1))
    }

    @Test("The line under the title is the agent's name and role")
    func subtitle() {
        #expect(Tokens.subtitle(dossier) == "Dossier · Workspace assistant")
        #expect(Tokens.subtitle(MonarchAiAgentRef(name: "Dossier")) == "Dossier")
        #expect(Tokens.subtitle(MonarchAiAgentRef(name: " Dossier ", role: "  ")) == "Dossier")
        #expect(Tokens.subtitle(MonarchAiAgentRef(name: " ")) == nil)
        #expect(Tokens.subtitle(nil) == nil)
    }

    @Test("Without an agent the avatar is named Assistant, or the override")
    func headerAgent() {
        #expect(Tokens.headerAgent(dossier, labels: MonarchAiAssistantPanelLabels()) == dossier)
        #expect(Tokens.headerAgent(nil, labels: MonarchAiAssistantPanelLabels()).name == "Assistant")
        #expect(Tokens.headerAgent(nil, labels: MonarchAiAssistantPanelLabels(assistant: "Helper")).name == "Helper")
    }

    @Test("Colour roles: the title in the default text colour, the agent muted, on the surface")
    func roles() {
        #expect(Tokens.titleRole == \.textDefault)
        #expect(Tokens.secondaryRole == \.textMuted)
        #expect(Tokens.backgroundRole == \.bgSurface)
    }

    @Test("Built-in strings resolve to their English text")
    func builtIn() {
        #expect(MonarchAiAssistantPanelStrings.close == "Close assistant")
        #expect(MonarchAiAssistantPanelStrings.inputLabel == "Message the assistant")
        #expect(MonarchAiAssistantPanelStrings.context == "Context")
        #expect(MonarchAiAssistantPanelStrings.welcome == "How can I help?")
        #expect(MonarchAiAssistantPanelStrings.starters == "Suggested prompts")
        #expect(MonarchAiAssistantPanelStrings.assistant == "Assistant")
    }

    @Test("Labels default to nil and keep overrides")
    func labels() {
        let empty = MonarchAiAssistantPanelLabels()
        #expect(empty.close == nil && empty.inputLabel == nil && empty.context == nil)
        #expect(empty.welcome == nil && empty.starters == nil && empty.assistant == nil)
        let custom = MonarchAiAssistantPanelLabels(close: "Close helper", starters: "Ideas")
        #expect(custom.close == "Close helper")
        #expect(custom.starters == "Ideas")
    }
}
