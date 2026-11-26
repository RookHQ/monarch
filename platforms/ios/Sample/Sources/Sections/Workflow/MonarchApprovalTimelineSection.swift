import MonarchUI
import SwiftUI

struct MonarchApprovalTimelineSection: CataloguePage {
    static let title = "Approval timeline"

    @State private var isDecided = false

    private var steps: [MonarchApprovalTimelineStep] {
        let now = Date()
        return [
            MonarchApprovalTimelineStep(
                id: "0",
                label: "Submitted",
                status: .submitted,
                actor: MonarchApprovalPerson("Sam Lee"),
                date: now.addingTimeInterval(-86_400 * 2)
            ),
            MonarchApprovalTimelineStep(
                id: "1",
                label: "Reporting manager",
                status: .approved,
                actor: MonarchApprovalPerson("Ana Ruiz"),
                date: now.addingTimeInterval(-3_600),
                comment: "Fine by me."
            ),
            isDecided
                ? MonarchApprovalTimelineStep(
                    id: "2",
                    label: "Finance check",
                    status: .rejected,
                    actor: MonarchApprovalPerson("Jo Park"),
                    date: now,
                    comment: "Add the hotel receipt."
                )
                : MonarchApprovalTimelineStep(
                    id: "2",
                    label: "Finance check",
                    status: .pending,
                    approvers: [MonarchApprovalPerson("Jo Park"), MonarchApprovalPerson("Kim Tran")]
                ),
            MonarchApprovalTimelineStep(id: "3", label: "Director", status: .skipped, note: "Below the approval limit"),
            MonarchApprovalTimelineStep(id: "4", label: "Payout", status: isDecided ? .cancelled : .waiting),
        ]
    }

    var body: some View {
        Toggle("Finance sent it back", isOn: $isDecided)
        MonarchApprovalTimeline(steps)
        CatalogueCaption("Not submitted")
        MonarchApprovalTimeline([])
    }
}
