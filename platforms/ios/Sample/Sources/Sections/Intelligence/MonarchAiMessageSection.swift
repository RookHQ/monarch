@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiMessageSection: CataloguePage {
    static let title = "AI message"

    @Environment(\.monarchTheme) private var theme
    @State private var feedback: MonarchAiMessageFeedback?
    @State private var isEditing = false
    @State private var question = "Which invoices are overdue?"
    @State private var version = 0
    @State private var decision = "None yet"

    private static let created = "2026-03-10T09:41:00.000Z"

    private var userMessage: MonarchAiConversationMessage {
        MonarchAiConversationMessage(
            id: "u1",
            role: .user,
            parts: [.text(MonarchAiTextPart(text: question))],
            createdAt: Self.created,
            branch: MonarchAiBranchPosition(index: version, count: 2)
        )
    }

    private static let answer = MonarchAiConversationMessage(
        id: "a1",
        role: .assistant,
        parts: [
            .reasoning(MonarchAiReasoningPart(text: "Look up unpaid invoices past their due date.", durationMs: 3_000)),
            .toolCall(
                MonarchAiToolCallPart(
                    id: "t1",
                    name: "searchInvoices",
                    input: .object(["status": .string("overdue")]),
                    state: .succeeded,
                    output: .number(2)
                )
            ),
            .text(MonarchAiTextPart(text: "Two invoices are overdue: **INV-204** and **INV-211**.")),
            .citation(MonarchAiCitationPart(id: "c1", sourceId: "s1")),
            .source(MonarchAiSourcePart(id: "s1", title: "Ledger, March", kind: .record)),
        ],
        agent: MonarchAiAgentRef(name: "Ledger", role: "Finance agent"),
        createdAt: "2026-03-10T09:41:05.000Z"
    )

    private static let approval = MonarchAiConversationMessage(
        id: "a2",
        role: .assistant,
        parts: [
            .text(MonarchAiTextPart(text: "I can send a reminder to Acme.")),
            .toolCall(
                MonarchAiToolCallPart(
                    id: "t2",
                    name: "sendEmail",
                    input: .object(["to": .string("billing@acme.test")]),
                    state: .pendingApproval,
                    risk: .high
                )
            ),
        ],
        createdAt: "2026-03-10T09:42:00.000Z"
    )

    private static let failed = MonarchAiConversationMessage(
        id: "a3",
        role: .assistant,
        parts: [
            .text(MonarchAiTextPart(text: "Drafting the reminder")),
            .error(
                MonarchAiErrorPart(
                    code: "network",
                    message: "The connection dropped.",
                    isRetryable: true,
                    isResumable: true
                )
            ),
        ],
        createdAt: "2026-03-10T09:43:00.000Z",
        finishReason: .length
    )

    var body: some View {
        CatalogueCaption("The person's message: Edit and versions")
        MonarchAiMessage(
            userMessage,
            isEditing: $isEditing,
            onEditSubmit: { question = $0 },
            onBranch: { version = $0 }
        )

        CatalogueCaption("An answer with reasoning, a tool call and a source")
        MonarchAiMessage(Self.answer, feedback: $feedback, isShareable: true, onRegenerate: {})

        CatalogueCaption("A tool call that waits for approval")
        MonarchAiMessage(
            Self.approval,
            onApproveTool: { id, _ in decision = "Approved \(id)" },
            onDenyTool: { id, _ in decision = "Denied \(id)" }
        )
        Text("Decision: \(decision)")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)

        CatalogueCaption("A dropped reply")
        MonarchAiMessage(Self.failed, onRetry: {}, onResume: {})
    }
}
