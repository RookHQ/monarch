import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchAiMessageFixtures: ComponentFixtures {
    static let id = "ai-message"

    static let utc = TimeZone(identifier: "UTC") ?? .gmt
    static let created = "2026-03-10T09:41:00.000Z"
    static let ledger = MonarchAiSourcePart(id: "s1", title: "Ledger, March", kind: .record)
    static let policy = MonarchAiSourcePart(
        id: "s2",
        title: "Collections policy",
        url: "https://example.com/policy",
        kind: .doc
    )

    static func message(
        _ role: MonarchAiRole,
        _ parts: [MonarchAiPart],
        agent: MonarchAiAgentRef? = nil,
        branch: MonarchAiBranchPosition? = nil,
        finishReason: MonarchAiFinishReason? = nil,
        isQueued: Bool = false
    ) -> MonarchAiConversationMessage {
        MonarchAiConversationMessage(
            id: "m-\(role.rawValue)",
            role: role,
            parts: parts,
            agent: agent,
            createdAt: created,
            branch: branch,
            finishReason: finishReason,
            isQueued: isQueued
        )
    }

    static let answer = message(
        .assistant,
        [
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
            .citation(MonarchAiCitationPart(id: "c2", sourceId: "s2", quote: "Remind after 14 days.")),
            .source(ledger),
            .source(policy),
        ],
        agent: MonarchAiAgentRef(name: "Ledger", role: "Finance agent")
    )

    static func utcTime<Content: View>(_ content: Content) -> some View {
        content.environment(\.timeZone, utc).environment(\.locale, Locale(identifier: "en_US"))
    }

    static let fixtures: [Fixture] = [
        Fixture("assistant: reasoning, tool call, text, citations, sources") {
            utcTime(MonarchAiMessage(answer, feedback: .constant(.up), isShareable: true, onRegenerate: {}))
        },
        Fixture("the person's message, editable, with a file") {
            utcTime(
                MonarchAiMessage(
                    message(
                        .user,
                        [
                            .text(MonarchAiTextPart(text: "Which invoices are overdue?")),
                            .file(
                                MonarchAiFilePart(name: "march-ledger.pdf", mediaType: "application/pdf", size: 182_000)
                            ),
                        ],
                        branch: MonarchAiBranchPosition(index: 1, count: 3)
                    ),
                    isEditing: .constant(false),
                    onBranch: { _ in }
                )
            )
        },
        Fixture("the person's message being edited") {
            MonarchAiMessage(
                message(.user, [.text(MonarchAiTextPart(text: "Which invoices are overdue?"))]),
                isEditing: .constant(true)
            )
        },
        Fixture("queued offline") {
            MonarchAiMessage(
                message(.user, [.text(MonarchAiTextPart(text: "Send the reminder."))], isQueued: true),
                onCancelQueued: {}
            )
        },
        Fixture("streaming, tool call waiting for approval") {
            MonarchAiMessage(
                message(
                    .assistant,
                    [
                        .text(MonarchAiTextPart(text: "I can send the reminder", state: .streaming)),
                        .toolCall(
                            MonarchAiToolCallPart(
                                id: "t2",
                                name: "sendEmail",
                                input: .object(["to": .string("billing@acme.test")]),
                                state: .pendingApproval,
                                risk: .high
                            )
                        ),
                    ]
                ),
                onApproveTool: { _, _ in },
                onDenyTool: { _, _ in }
            )
        },
        Fixture("error with retry and resume, plan and memory") {
            MonarchAiMessage(
                message(
                    .assistant,
                    [
                        .plan(
                            MonarchAiPlanPart(steps: [
                                MonarchAiPlanStep(id: "1", label: "Find invoices", state: .done),
                                MonarchAiPlanStep(id: "2", label: "Draft reminders", state: .failed),
                            ])
                        ),
                        .memory(MonarchAiMemoryPart(id: "mem1", action: .read, text: "Prefers email reminders")),
                        .error(
                            MonarchAiErrorPart(
                                code: "network",
                                message: "The connection dropped.",
                                isRetryable: true,
                                isResumable: true
                            )
                        ),
                    ]
                ),
                onRetry: {},
                onResume: {}
            )
        },
        Fixture("cut short, artifact, no actions, no time") {
            MonarchAiMessage(
                message(
                    .assistant,
                    [
                        .text(MonarchAiTextPart(text: "Here is the summary.")),
                        .artifact(
                            MonarchAiArtifactPart(id: "a1", kind: .document, title: "Overdue summary", version: 2)
                        ),
                    ],
                    finishReason: .length
                ),
                showsTimestamp: false,
                showsActions: false,
                onOpenArtifact: { _ in }
            )
        },
        Fixture("generative UI drawn by the app, extra menu item") {
            MonarchAiMessage(
                message(.assistant, [.ui(MonarchAiUiPart(component: "chart", props: .null))]),
                part: { _ in Text("Chart drawn by the app") },
                more: { Button("Report", systemImage: "flag") {} }
            )
        },
        Fixture("system message") {
            MonarchAiMessage(message(.system, [.text(MonarchAiTextPart(text: "The model changed to Fast."))]))
        },
        Fixture("long text") {
            MonarchAiMessage(
                message(.user, [.text(MonarchAiTextPart(text: String(repeating: "A very long question. ", count: 30)))])
            )
        },
    ]
}

@Suite("MonarchAiMessage")
struct MonarchAiMessageTests {
    private typealias Tokens = MonarchAiMessageTokens
    private typealias F = MonarchAiMessageFixtures

    @Test("Parts keep their order; sources become one list where the first stands; consecutive citations share a row")
    func blocks() {
        let blocks = Tokens.blocks(F.answer.parts, showsArtifacts: false, drawsCustomParts: false)
        #expect(blocks == [.part(0), .part(1), .part(2), .citations([3, 4]), .sources])
    }

    @Test("Citations of unknown sources, empty finished text and app-only parts are left out")
    func hidden() {
        let parts: [MonarchAiPart] = [
            .text(MonarchAiTextPart(text: "")),
            .text(MonarchAiTextPart(text: "", state: .streaming)),
            .citation(MonarchAiCitationPart(id: "c", sourceId: "missing")),
            .artifact(MonarchAiArtifactPart(id: "a", kind: .code, title: "Script", version: 1)),
            .ui(MonarchAiUiPart(component: "chart", props: .null)),
            .editProposal(MonarchAiEditProposalPart(id: "e", target: "doc", diff: .null)),
        ]
        #expect(Tokens.blocks(parts, showsArtifacts: false, drawsCustomParts: false) == [.part(1)])
        #expect(
            Tokens.blocks(parts, showsArtifacts: true, drawsCustomParts: true) == [
                .part(1), .part(3), .part(4), .part(5),
            ]
        )
    }

    @Test("The reply is done only when nothing streams and no tool call runs or waits")
    func replyDone() {
        #expect(Tokens.isReplyDone(F.answer))
        for state in [MonarchAiToolCallState.running, .pendingApproval, .awaitingResult] {
            let waiting = F.message(.assistant, [.toolCall(MonarchAiToolCallPart(id: "t", name: "x", state: state))])
            #expect(!Tokens.isReplyDone(waiting))
        }
        let streaming = F.message(.assistant, [.text(MonarchAiTextPart(text: "Hi", state: .streaming))])
        #expect(!Tokens.isReplyDone(streaming))
        #expect(Tokens.lastTextIndex(F.answer.parts) == 2)
        #expect(Tokens.lastTextIndex([]) == nil)
    }

    @Test("Approval is shown only for a waiting call when the app handles approvals")
    func approval() {
        let waiting = MonarchAiToolCallPart(id: "t", name: "x", state: .pendingApproval)
        let running = MonarchAiToolCallPart(id: "t", name: "x", state: .running)
        #expect(Tokens.showsApproval(waiting, handlesApproval: true))
        #expect(!Tokens.showsApproval(waiting, handlesApproval: false))
        #expect(!Tokens.showsApproval(running, handlesApproval: true))
    }

    @Test("A finish notice follows a finished assistant reply cut short, filtered or refused")
    func finishNotice() {
        let labels = MonarchAiMessageLabels()
        let text: [MonarchAiPart] = [.text(MonarchAiTextPart(text: "Partial"))]
        #expect(
            Tokens.finishNotice(F.message(.assistant, text, finishReason: .length), labels: labels)
                == "This answer was cut short because it reached the length limit."
        )
        #expect(
            Tokens.finishNotice(F.message(.assistant, text, finishReason: .contentFilter), labels: labels)
                == "This answer was stopped by a safety filter."
        )
        #expect(
            Tokens.finishNotice(F.message(.assistant, text, finishReason: .refusal), labels: labels)
                == "The assistant declined to answer this."
        )
        #expect(Tokens.finishNotice(F.message(.assistant, text, finishReason: .stop), labels: labels) == nil)
        #expect(Tokens.finishNotice(F.message(.user, text, finishReason: .length), labels: labels) == nil)
        let streaming: [MonarchAiPart] = [.text(MonarchAiTextPart(text: "Part", state: .streaming))]
        #expect(Tokens.finishNotice(F.message(.assistant, streaming, finishReason: .length), labels: labels) == nil)
        let custom = MonarchAiMessageLabels(finishNotice: { _ in "Gekürzt." })
        #expect(Tokens.finishNotice(F.message(.assistant, text, finishReason: .length), labels: custom) == "Gekürzt.")
    }

    @Test("VoiceOver names the message by who said it, and labels override")
    func spokenRole() {
        let none = MonarchAiMessageLabels()
        #expect(Tokens.spokenRole(.user, labels: none) == "You said")
        #expect(Tokens.spokenRole(.assistant, labels: none) == "Assistant said")
        #expect(Tokens.spokenRole(.system, labels: none) == "System message")
        #expect(Tokens.spokenRole(.tool, labels: none) == "System message")
        #expect(Tokens.spokenRole(.user, labels: MonarchAiMessageLabels(youSaid: "Du")) == "Du")
    }

    @Test("The header is read as the name, the role and the time")
    func header() {
        let none = MonarchAiMessageLabels()
        #expect(Tokens.name(agent: nil, labels: none) == "Assistant")
        #expect(Tokens.name(agent: MonarchAiAgentRef(name: ""), labels: none) == "Assistant")
        #expect(Tokens.name(agent: MonarchAiAgentRef(name: "Ledger"), labels: none) == "Ledger")
        #expect(Tokens.name(agent: nil, labels: MonarchAiMessageLabels(assistant: "Rook AI")) == "Rook AI")
        #expect(
            Tokens.headerLabel(name: "Ledger", agentRole: "Finance agent", time: "9:41 AM")
                == "Ledger, Finance agent, 9:41 AM"
        )
        #expect(Tokens.headerLabel(name: "Ledger", agentRole: "", time: nil) == "Ledger")
    }

    @Test("The time follows the locale and the time zone, and is hidden when asked or invalid")
    func time() {
        let utc = F.utc
        let tokyo = TimeZone(identifier: "Asia/Tokyo") ?? utc
        let us = Locale(identifier: "en_US")
        let de = Locale(identifier: "de_DE")
        let usTime = Tokens.time(F.created, isShown: true, locale: us, timeZone: utc)
        #expect(usTime?.contains("9:41") == true)
        #expect(usTime?.contains("AM") == true)
        let deTime = Tokens.time(F.created, isShown: true, locale: de, timeZone: utc)
        #expect(deTime?.contains("9:41") == true)
        #expect(deTime?.contains("AM") == false)
        #expect(Tokens.time(F.created, isShown: true, locale: de, timeZone: tokyo)?.contains("18:41") == true)
        #expect(Tokens.time(F.created, isShown: false, locale: us, timeZone: utc) == nil)
        #expect(Tokens.time("yesterday", isShown: true, locale: us, timeZone: utc) == nil)
    }

    @Test("Actions are hidden while editing, streaming, queued and for system messages")
    func actions() {
        let text: [MonarchAiPart] = [.text(MonarchAiTextPart(text: "Hi"))]
        #expect(Tokens.showsActions(F.message(.assistant, text), isShown: true, isEditing: false))
        #expect(Tokens.showsActions(F.message(.user, text), isShown: true, isEditing: false))
        #expect(!Tokens.showsActions(F.message(.assistant, text), isShown: false, isEditing: false))
        #expect(!Tokens.showsActions(F.message(.user, text), isShown: true, isEditing: true))
        #expect(!Tokens.showsActions(F.message(.user, text, isQueued: true), isShown: true, isEditing: false))
        #expect(!Tokens.showsActions(F.message(.system, text), isShown: true, isEditing: false))
        let streaming: [MonarchAiPart] = [.text(MonarchAiTextPart(text: "Hi", state: .streaming))]
        #expect(!Tokens.showsActions(F.message(.assistant, streaming), isShown: true, isEditing: false))
    }

    @Test("The branch switcher needs more than one alternative")
    func branch() {
        let text: [MonarchAiPart] = [.text(MonarchAiTextPart(text: "Hi"))]
        #expect(Tokens.branch(F.message(.user, text)) == nil)
        #expect(Tokens.branch(F.message(.user, text, branch: MonarchAiBranchPosition(index: 0, count: 1))) == nil)
        #expect(
            Tokens.branch(F.message(.user, text, branch: MonarchAiBranchPosition(index: 1, count: 2)))
                == MonarchAiBranchPosition(index: 1, count: 2)
        )
    }

    @Test("Sources are numbered from 1 in order and citations find them")
    func sources() {
        #expect(Tokens.sourceNumbers(F.answer.parts) == ["s1": 1, "s2": 2])
        #expect(Tokens.source("s2", in: F.answer.parts) == F.policy)
        #expect(Tokens.source("nope", in: F.answer.parts) == nil)
    }

    @Test("Only web addresses of files open; retry delays are in seconds")
    func filesAndErrors() {
        #expect(
            Tokens.fileURL(MonarchAiFilePart(name: "a", mediaType: "x", url: "https://example.com/a.pdf"))?.host
                == "example.com"
        )
        #expect(Tokens.fileURL(MonarchAiFilePart(name: "a", mediaType: "x", url: "data:text/plain;base64,SGk=")) == nil)
        #expect(Tokens.fileURL(MonarchAiFilePart(name: "a", mediaType: "x")) == nil)
        #expect(
            Tokens.retryAfter(MonarchAiErrorPart(code: "x", message: "", isRetryable: true, retryAfterMs: 2_500)) == 2.5
        )
        #expect(Tokens.retryAfter(MonarchAiErrorPart(code: "x", message: "", isRetryable: true)) == nil)
    }

    @Test("Built-in strings resolve")
    func strings() {
        let all = [
            MonarchAiMessageStrings.assistantSaid, MonarchAiMessageStrings.youSaid,
            MonarchAiMessageStrings.systemSaid, MonarchAiMessageStrings.assistant,
            MonarchAiMessageStrings.queued, MonarchAiMessageStrings.cancelQueued,
            MonarchAiMessageStrings.resume, MonarchAiMessageStrings.plan,
        ]
        for string in all {
            #expect(!string.hasPrefix("monarch."))
        }
        for reason in MonarchAiFinishReason.allCases {
            if let notice = MonarchAiMessageStrings.finishNotice(reason) {
                #expect(!notice.hasPrefix("monarch."))
            }
        }
        #expect(MonarchAiMessageStrings.queued == "Queued. It will send when you are back online.")
    }
}
