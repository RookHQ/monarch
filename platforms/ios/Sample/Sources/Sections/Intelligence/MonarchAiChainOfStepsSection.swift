import MonarchUI
import SwiftUI

struct MonarchAiChainOfStepsSection: CataloguePage {
    static let title = "AI chain of steps"

    private static let labels = [
        "Read the attached contract", "Search past invoices", "Compare the totals", "Draft the summary",
    ]

    @State private var steps = Self.start()
    @State private var isRunning = false

    var body: some View {
        CatalogueCaption("Run the steps; the list folds when they finish")
        MonarchAiChainOfSteps(steps, details: ["search": "12 invoices"])
        MonarchButton(isRunning ? "Running" : "Run", variant: .primary, size: .sm, isLoading: isRunning) {
            run()
        }

        CatalogueCaption("Finished with a failure, open")
        MonarchAiChainOfSteps(
            [
                MonarchAiPlanStep(id: "a", label: "Look up the customer", state: .done),
                MonarchAiPlanStep(id: "b", label: "Check the payment", state: .failed),
                MonarchAiPlanStep(id: "c", label: "Email the account owner", state: .skipped),
            ],
            details: ["b": "The payment service did not answer."],
            startsExpanded: true,
            announces: false
        )
    }

    private static func start() -> [MonarchAiPlanStep] {
        labels.enumerated().map {
            MonarchAiPlanStep(id: ["read", "search", "compare", "draft"][$0.offset], label: $0.element)
        }
    }

    private func run() {
        steps = Self.start()
        isRunning = true
        Task {
            for index in steps.indices {
                steps[index].state = .active
                try? await Task.sleep(for: .seconds(1))
                steps[index].state = .done
            }
            isRunning = false
        }
    }
}
