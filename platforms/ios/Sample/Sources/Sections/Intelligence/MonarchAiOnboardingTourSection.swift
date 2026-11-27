import MonarchUI
import SwiftUI

struct MonarchAiOnboardingTourSection: CataloguePage {
    static let title = "AI onboarding tour"

    @State private var state = "Open"
    @State private var showsSheet = false

    private let steps = [
        MonarchAiOnboardingTourStep(
            "What the assistant can see",
            body: "The page you have open, the records you select and any files you attach. "
                + "It does not see other workspaces.",
            systemImage: "eye"
        ),
        MonarchAiOnboardingTourStep(
            "What it can do",
            body: "Summarise pages, draft text and answer questions about your records. "
                + "It asks before it changes anything.",
            systemImage: "cpu"
        ),
        MonarchAiOnboardingTourStep(
            "How to turn it off",
            body: "Use Turn off AI below at any time, or switch it off per app in workspace settings.",
            systemImage: "switch.2"
        ),
    ]

    var body: some View {
        CatalogueCaption("Inline, in a card")
        if state == "Open" {
            MonarchCard {
                MonarchAiOnboardingTour(
                    steps,
                    onSkip: { state = "Introduction skipped." },
                    onDisable: { state = "AI turned off." },
                    onFinish: { state = "Introduction finished." }
                )
            }
        } else {
            Text(state).monarchTextStyle(.body)
            Button("Show again") { state = "Open" }
                .buttonStyle(.monarch(.secondary, size: .sm))
        }

        CatalogueCaption("In a sheet")
        Button("Start the introduction") { showsSheet = true }
            .buttonStyle(.monarch(.primary))
            .sheet(isPresented: $showsSheet) {
                MonarchAiOnboardingTour(
                    steps,
                    onSkip: { showsSheet = false },
                    onDisable: { showsSheet = false },
                    onFinish: { showsSheet = false }
                )
                .padding()
                .presentationDetents([.medium, .large])
            }
    }
}
