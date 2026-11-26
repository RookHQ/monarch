import MonarchUI
import SwiftUI

struct MonarchAiDraftQueueSection: CataloguePage {
    static let title = "AI draft queue"

    @State private var drafts = [
        MonarchAiDraft(
            id: "1",
            title: "Reply to Alex Morgan",
            body: "Thanks for flagging this, Alex. The duplicate charge was refunded today; "
                + "it can take up to five days to show.",
            context: "Alex: I was charged twice for my order #4821."
        ),
        MonarchAiDraft(
            id: "2",
            title: "Follow-up to Priya Shah",
            body: "Hi Priya, here is the summary of Tuesday's call and the three next steps we agreed.",
            context: "Call notes from Tuesday."
        ),
        MonarchAiDraft(id: "3", title: "Note for Sam", body: "Moved the design review to Friday at 10."),
    ]

    var body: some View {
        CatalogueCaption("Approve, edit, regenerate, skip and send")
        MonarchAiDraftQueue(
            drafts,
            title: "Replies to review",
            onEdit: { id, body in update(id) { $0.body = body } },
            onRegenerate: { id in
                update(id) { $0.status = .regenerating }
                Task {
                    try? await Task.sleep(for: .seconds(1.5))
                    update(id) {
                        $0.body = "A new version of the reply, written again."
                        $0.status = .draft
                    }
                }
            },
            onSkip: { id in update(id) { $0.status = .skipped } },
            onSend: { id in update(id) { $0.status = .sent } },
            onApprove: { id in update(id) { $0.status = .approved } }
        )
    }

    private func update(_ id: String, _ change: (inout MonarchAiDraft) -> Void) {
        guard let index = drafts.firstIndex(where: { $0.id == id }) else { return }
        change(&drafts[index])
    }
}
