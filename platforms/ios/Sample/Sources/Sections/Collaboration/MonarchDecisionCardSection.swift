import MonarchUI
import SwiftUI

struct MonarchDecisionCardSection: CataloguePage {
    static let title = "Decision card"

    @State private var decision = MonarchDecision(value: nil)
    @State private var decidedAt: Date?
    @State private var failsNextSave = false

    private let options = [
        MonarchDecisionOption("Keep", value: "keep", description: "Renews for another year.", tone: .success),
        MonarchDecisionOption("Review", value: "review", note: "The owner is asked to confirm.", tone: .warning),
        MonarchDecisionOption(
            "Let lapse",
            value: "lapse",
            description: "Access ends on the renewal date.",
            note: "A reason is required for this choice.",
            tone: .danger,
            isReasonRequired: true
        ),
    ]

    private struct SaveFailed: LocalizedError {
        var errorDescription: String? { "The renewal is locked by another person." }
    }

    var body: some View {
        MonarchDecisionCard(
            "Renewal decision",
            options: options,
            value: decision.value,
            reason: decision.reason,
            decidedAt: decidedAt,
            decidedBy: decidedAt == nil ? nil : "You",
            onDecide: { newDecision in
                try await Task.sleep(for: .seconds(1))
                if failsNextSave { throw SaveFailed() }
                decision = newDecision
                decidedAt = .now
            },
            context: {
                Text("12 of 40 seats were used in the last 90 days.").monarchTextStyle(.body)
            }
        )
        Toggle("Fail the next save", isOn: $failsNextSave)

        CatalogueCaption("Read-only, showing the decision saved above")
        MonarchDecisionCard(
            "Renewal decision",
            options: options,
            value: decision.value,
            reason: decision.reason,
            decidedAt: decidedAt,
            decidedBy: decidedAt == nil ? nil : "You",
            isReadOnly: true
        )
    }
}
