import SwiftUI
import Testing

@testable import MonarchUI

private let queue = [
    MonarchAiDraft(
        id: "1",
        title: "Reply to Alex Morgan",
        body: "Thanks for flagging this, Alex. The duplicate charge was refunded today.",
        context: "Alex: I was charged twice for my order #4821."
    ),
    MonarchAiDraft(id: "2", title: "Follow-up to Priya", body: "", status: .regenerating),
    MonarchAiDraft(id: "3", title: "Note for Sam", body: "Moved the call to Friday at 10.", status: .approved),
    MonarchAiDraft(id: "4", title: "Reply to Lee", body: "No reply needed.", status: .skipped),
    MonarchAiDraft(id: "5", title: "Reply to Jo", body: "Sent yesterday.", status: .sent),
    MonarchAiDraft(id: "6", title: "Reply to Kim", body: "Happy to help."),
]

enum MonarchAiDraftQueueFixtures: ComponentFixtures {
    static let id = "ai-draft-queue"
    static let fixtures = [
        Fixture("every status, titled, with send") {
            MonarchAiDraftQueue(
                queue,
                title: "Replies to review",
                onEdit: { _, _ in },
                onRegenerate: { _ in },
                onSkip: { _ in },
                onSend: { _ in },
                onApprove: { _ in }
            )
        },
        Fixture("approved without send, long title") {
            MonarchAiDraftQueue(
                [
                    MonarchAiDraft(
                        id: "a",
                        title: "Reply to the procurement team about the renewal of the enterprise agreement",
                        body: "We can confirm the renewal terms.",
                        status: .approved
                    )
                ],
                onEdit: { _, _ in },
                onRegenerate: { _ in },
                onSkip: { _ in },
                onApprove: { _ in }
            )
        },
        Fixture("empty") {
            MonarchAiDraftQueue([], onEdit: { _, _ in }, onRegenerate: { _ in }, onSkip: { _ in }, onApprove: { _ in })
        },
        Fixture("editor") {
            MonarchAiDraftQueueEditor(
                label: "Edit draft: Reply to Alex",
                text: .constant("Thanks for flagging this."),
                labels: MonarchAiDraftQueueLabels(),
                onSave: {},
                onCancel: {}
            )
        },
    ]
}

@Suite("MonarchAiDraftQueue")
struct MonarchAiDraftQueueTests {
    typealias Tokens = MonarchAiDraftQueueTokens

    @Test("Statuses have their own tone and symbol")
    func statusAppearance() {
        #expect(Tokens.tone(.draft) == .ai)
        #expect(Tokens.tone(.regenerating) == .ai)
        #expect(Tokens.tone(.approved) == .success)
        #expect(Tokens.tone(.sent) == .success)
        #expect(Tokens.tone(.skipped) == .neutral)
        let symbols = MonarchAiDraftStatus.allCases.map(Tokens.symbol)
        #expect(Set(symbols).count == MonarchAiDraftStatus.allCases.count)
        #expect(Tokens.bodyRole(.skipped) == \.textMuted)
        #expect(Tokens.bodyRole(.draft) == \.textDefault)
    }

    @Test("Approved, sent and skipped count as reviewed")
    func progress() {
        #expect(Tokens.reviewedCount(queue) == 3)
        #expect(Tokens.fraction(queue) == 0.5)
        #expect(Tokens.fraction([]) == 0)
        #expect(MonarchAiDraftQueueLabels().progress(3, 6) == "3 of 6 reviewed")
    }

    @Test("Each status offers its decisions; nothing while writing, sent or skipped")
    func actions() {
        #expect(Tokens.actions(.draft, canSend: true) == [.approve, .edit, .regenerate, .skip])
        #expect(Tokens.actions(.approved, canSend: true) == [.send, .edit])
        #expect(Tokens.actions(.approved, canSend: false) == [.edit])
        #expect(Tokens.actions(.regenerating, canSend: true).isEmpty)
        #expect(Tokens.actions(.sent, canSend: true).isEmpty)
        #expect(Tokens.actions(.skipped, canSend: true).isEmpty)
        #expect(Tokens.variant(.approve) == .primary)
        #expect(Tokens.variant(.send) == .primary)
        #expect(Tokens.variant(.skip) == .ghost)
        #expect(Tokens.advances(.approve) && Tokens.advances(.skip) && Tokens.advances(.send))
        #expect(!Tokens.advances(.edit) && !Tokens.advances(.regenerate))
    }

    @Test("The next draft to review wraps to the top and skips the current one")
    func next() {
        #expect(Tokens.next(after: "1", in: queue) == "6")
        #expect(Tokens.next(after: "6", in: queue) == "1")
        #expect(Tokens.next(after: "missing", in: queue) == "1")
        #expect(Tokens.next(after: "1", in: [queue[0], queue[2]]) == nil)
    }

    @Test("Edits are trimmed and never empty or unchanged")
    func edited() {
        #expect(Tokens.edited(" New text ", original: "Old") == "New text")
        #expect(Tokens.edited("Old\n", original: "Old") == nil)
        #expect(Tokens.edited("  ", original: "Old") == nil)
    }

    @Test("Status changes are announced; new drafts are not")
    func announcements() {
        let labels = MonarchAiDraftQueueLabels()
        let previous: [String: MonarchAiDraftStatus] = ["1": .draft, "2": .regenerating]
        let now = [
            MonarchAiDraft(id: "1", title: "Reply to Alex", body: "", status: .approved),
            MonarchAiDraft(id: "2", title: "Note for Priya", body: "", status: .draft),
            MonarchAiDraft(id: "3", title: "New", body: "", status: .draft),
        ]
        #expect(
            Tokens.announcements(previous: previous, drafts: now, labels: labels) == [
                "Approved: Reply to Alex", "New draft ready: Note for Priya",
            ]
        )
        #expect(Tokens.statuses(now)["3"] == .draft)
    }

    @Test("The body is read as an AI draft")
    func bodyLabel() {
        #expect(Tokens.bodyLabel("Hello", labels: MonarchAiDraftQueueLabels()) == "AI draft, Hello")
    }

    @Test("A draft made from a message takes its id and text")
    func fromMessage() {
        let message = MonarchAiConversationMessage(
            id: "msg-1",
            role: .assistant,
            parts: [.text(MonarchAiTextPart(text: "Hello ")), .text(MonarchAiTextPart(text: "Alex"))],
            createdAt: "2026-11-14T10:00:00Z"
        )
        let draft = MonarchAiDraft(message, title: "Reply to Alex")
        #expect(draft.id == "msg-1")
        #expect(draft.body == "Hello Alex")
        #expect(draft.status == .draft)
    }

    @Test("Own labels replace the built-in strings")
    func overrides() {
        let labels = MonarchAiDraftQueueLabels(
            label: "Queue",
            status: { $0 == .approved ? "OK" : "…" },
            approve: "Accept",
            editField: { "Change " + $0 }
        )
        #expect(labels.label == "Queue")
        #expect(labels.status(.approved) == "OK")
        #expect(labels.announce(.approved, "X") == "OK: X")
        #expect(Tokens.title(.approve, labels: labels) == "Accept")
        #expect(labels.editField("X") == "Change X")
    }

    @Test("The built-in strings come from the strings table")
    func strings() {
        typealias Strings = MonarchAiDraftQueueStrings
        #expect(Strings.label == "Drafts to review")
        #expect(Strings.progress(2, 5) == "2 of 5 reviewed")
        #expect(
            MonarchAiDraftStatus.allCases.map(Strings.status) == [
                "Needs review", "Writing", "Approved", "Sent", "Skipped",
            ]
        )
        #expect(Strings.writing == "AI is still writing…")
        #expect(Strings.editField("A") == "Edit draft: A")
        #expect(Strings.announce(.draft, title: "A", statusName: "Needs review") == "New draft ready: A")
        for value in [
            Strings.empty, Strings.aiDraft, Strings.context, Strings.approve, Strings.send, Strings.edit,
            Strings.regenerate, Strings.skip, Strings.save, Strings.cancel,
        ] {
            #expect(!value.hasPrefix("monarch."))
        }
    }
}
