import MonarchUI
import SwiftUI

struct MonarchAiStarterPromptsSection: CataloguePage {
    static let title = "AI starter prompts"

    @State private var chosen = "Choose a starter."

    private let starters = [
        MonarchAiStarterPrompt(
            id: "summarise",
            title: "Summarise a document",
            prompt: "Summarise the attached report in five bullet points.",
            systemImage: "doc.text"
        ),
        MonarchAiStarterPrompt(
            id: "draft",
            title: "Draft a reply",
            prompt: "Write a polite reply to this email.",
            systemImage: "envelope"
        ),
        MonarchAiStarterPrompt(
            id: "plan",
            title: "Plan my week",
            prompt: "Turn my open tasks into a plan for the week, with the most urgent ones first.",
            systemImage: "calendar"
        ),
        MonarchAiStarterPrompt(id: "brief", title: "Brief me", prompt: "What changed since yesterday?"),
    ]

    var body: some View {
        Text(chosen)
            .monarchTextStyle(.body)

        CatalogueCaption("Grid")
        MonarchAiStarterPrompts(starters) { starter in
            chosen = starter.prompt
        }

        CatalogueCaption("List, titles only")
        MonarchAiStarterPrompts(starters, layout: .list, hidesPrompt: true) { starter in
            chosen = starter.prompt
        }

        CatalogueCaption("Disabled")
        MonarchAiStarterPrompts(Array(starters.prefix(2)), layout: .list) { _ in }
            .disabled(true)
    }
}
