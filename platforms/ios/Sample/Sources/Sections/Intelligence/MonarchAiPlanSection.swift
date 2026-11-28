@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiPlanSection: CataloguePage {
    static let title = "AI plan"

    private static let proposed = [
        MonarchAiPlanStep(id: "find", label: "Find the three open invoices"),
        MonarchAiPlanStep(id: "draft", label: "Draft a reminder for each customer"),
        MonarchAiPlanStep(id: "send", label: "Send the reminders"),
    ]

    @State private var part = MonarchAiPlanPart(steps: Self.proposed)
    @State private var decision: MonarchAiPlanDecision = .pending
    @State private var round = 0

    var body: some View {
        CatalogueCaption("Edit, drag or remove steps, then approve")
        MonarchAiPlan(
            part,
            decision: decision,
            onReject: { decision = .rejected },
            onApprove: { steps in run(steps) }
        )
        .id(round)
        MonarchButton("Start again", variant: .ghost, size: .sm) {
            part = MonarchAiPlanPart(steps: Self.proposed)
            decision = .pending
            round += 1
        }

        CatalogueCaption("Read-only proposal")
        MonarchAiPlan(MonarchAiPlanPart(steps: Self.proposed), isEditable: false)
    }

    private func run(_ steps: [MonarchAiPlanStep]) {
        decision = .approved
        part = MonarchAiPlanPart(steps: steps)
        Task {
            for index in steps.indices {
                part.steps[index].state = .active
                try? await Task.sleep(for: .seconds(1))
                part.steps[index].state = .done
            }
        }
    }
}
