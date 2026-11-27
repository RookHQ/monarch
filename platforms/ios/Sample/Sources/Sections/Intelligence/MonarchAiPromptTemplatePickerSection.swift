import MonarchUI
import SwiftUI

struct MonarchAiPromptTemplatePickerSection: CataloguePage {
    static let title = "AI prompt template picker"

    @State private var showsPicker = false
    @State private var draft = ""

    private let templates = [
        MonarchAiPromptTemplate(
            id: "reply",
            title: "Reply to a customer",
            body: "Write a friendly reply to {{customer_name}} about {{topic}}.",
            description: "A short, warm reply to a support message.",
            category: "support"
        ),
        MonarchAiPromptTemplate(
            id: "brief",
            title: "Project brief",
            body: "Write a one-page brief for {{project}}.{{notes}}",
            description: "Goals, scope and risks on one page.",
            category: "writing",
            variables: [
                MonarchAiPromptTemplateVariable(
                    "project",
                    label: "Project",
                    isRequired: true,
                    placeholder: "Northwind"
                ),
                MonarchAiPromptTemplateVariable("notes", label: "Notes", isMultiline: true),
            ]
        ),
        MonarchAiPromptTemplate(
            id: "summary",
            title: "Summarise this page",
            body: "Summarise this page in five bullet points.",
            category: "writing"
        ),
    ]

    var body: some View {
        CatalogueCaption("Opens in a sheet and puts the prompt in the field")
        MonarchTextField("Message", text: $draft, prompt: "Ask anything")
        Button("Choose a template") { showsPicker = true }
            .buttonStyle(.monarch(.secondary))
            .sheet(isPresented: $showsPicker) {
                MonarchAiPromptTemplatePicker(
                    templates,
                    categories: [
                        MonarchAiPromptTemplateCategory(id: "support", label: "Support"),
                        MonarchAiPromptTemplateCategory(id: "writing", label: "Writing"),
                    ],
                    onCancel: { showsPicker = false }
                ) { text, _, _ in
                    draft = text
                    showsPicker = false
                }
            }
    }
}
