import MonarchUI
import SwiftUI

struct MonarchTagInputSection: CataloguePage {
    static let title = "Tag input"

    @State private var skills = ["Research", "TypeScript"]
    @State private var recipients: [String] = []
    @State private var keywords = ["quarterly", "draft"]

    var body: some View {
        CatalogueCaption("Suggestions while typing, at most 5")
        MonarchTagInput(
            "Skills",
            tags: $skills,
            prompt: "Add a skill",
            helper: "Separate skills with commas.",
            suggestions: ["Accessibility", "Copywriting", "Data analysis", "Illustration", "Research", "TypeScript"],
            maxCount: 5
        )

        CatalogueCaption("Validated email addresses, split on commas, spaces and semicolons")
        MonarchTagInput(
            "Recipients",
            tags: $recipients,
            prompt: "name@example.com",
            separators: [",", " ", ";"],
            normalize: { $0.trimmingCharacters(in: .whitespaces).lowercased() },
            validate: { tag, _ in tag.contains("@") ? nil : "Enter an email address like name@example.com." }
        )

        CatalogueCaption("Read only and disabled")
        MonarchTagInput("Labels", tags: .constant(["Billing", "Priority"]), isReadOnly: true)
        MonarchTagInput("Keywords", tags: $keywords).disabled(true)
    }
}
