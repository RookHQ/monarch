import MonarchUI
import SwiftUI

struct MonarchOnboardingChecklistSection: CataloguePage {
    static let title = "Onboarding checklist"

    @State private var steps = [
        MonarchOnboardingStep(id: "profile", title: "Complete your profile", isDone: true),
        MonarchOnboardingStep(
            id: "invite",
            title: "Invite your team",
            description: "Work together on shared projects."
        ),
        MonarchOnboardingStep(id: "calendar", title: "Connect a calendar", description: "See bookings in one place."),
    ]
    @State private var isExpanded = true
    @State private var isDismissed = false

    var body: some View {
        if isDismissed {
            Button("Show the checklist again") { isDismissed = false }
                .buttonStyle(.monarch(.secondary))
        } else {
            MonarchOnboardingChecklist(
                "Get started",
                steps: steps,
                description: "A few steps to set up your workspace.",
                isExpanded: $isExpanded,
                onDismiss: { isDismissed = true },
                action: { step in
                    Button("Mark done") { markDone(step.id) }
                        .buttonStyle(.monarch(.secondary, size: .sm))
                }
            )
        }
        Button("Reset steps") { reset() }
            .buttonStyle(.monarch(.ghost))

        CatalogueCaption("Complete, not collapsible")
        MonarchOnboardingChecklist(
            "Set up payments",
            steps: [
                MonarchOnboardingStep(id: "business", title: "Verify your business", isDone: true),
                MonarchOnboardingStep(id: "bank", title: "Add a bank account", isDone: true),
            ],
            isCollapsible: false
        )
    }

    private func markDone(_ id: String) {
        if let index = steps.firstIndex(where: { $0.id == id }) {
            steps[index].isDone = true
        }
    }

    private func reset() {
        for index in steps.indices.dropFirst() {
            steps[index].isDone = false
        }
    }
}
