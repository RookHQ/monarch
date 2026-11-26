import MonarchUI
import SwiftUI

struct MonarchAiWelcomeSection: CataloguePage {
    static let title = "AI welcome"

    @State private var chosen = "Choose a starter."

    private let starters = [
        MonarchAiStarterPrompt(
            id: "sum",
            title: "Summarise this page",
            prompt: "Summarise this page.",
            systemImage: "doc.text"
        ),
        MonarchAiStarterPrompt(
            id: "next",
            title: "Suggest next steps",
            prompt: "What should I do next on this project?",
            systemImage: "arrow.forward.circle"
        ),
    ]

    var body: some View {
        Text(chosen)
            .monarchTextStyle(.body)

        CatalogueCaption("In an empty conversation")
        MonarchAiWelcome(
            "How can I help?",
            agent: MonarchAiAgentRef(name: "Dossier assistant", role: "Writing assistant"),
            description: "I can read the page you are on and the documents you share with me.",
            starters: {
                MonarchAiStarterPrompts(starters) { starter in
                    chosen = starter.prompt
                }
            },
            footer: { MonarchAiDisclaimer() }
        )

        CatalogueCaption("With capabilities, no agent")
        MonarchAiWelcome(
            "Ask about your projects",
            description: "Answers come from the projects you can open.",
            capabilities: [
                "Find a project by name or owner", "Summarise what changed this week", "Draft a status update",
            ]
        )
    }
}
