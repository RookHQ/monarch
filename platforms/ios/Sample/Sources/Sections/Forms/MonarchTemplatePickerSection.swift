import MonarchUI
import SwiftUI

struct MonarchTemplatePickerSection: CataloguePage {
    static let title = "Template picker"

    private static let templates = [
        MonarchTemplatePickerItem(
            id: "notes",
            name: "Meeting notes",
            description: "Agenda, decisions and actions.",
            category: "meetings",
            systemImage: "person.3",
            keywords: ["minutes"]
        ),
        MonarchTemplatePickerItem(
            id: "retro",
            name: "Retrospective",
            description: "What went well and what to change.",
            category: "meetings",
            systemImage: "arrow.triangle.2.circlepath"
        ),
        MonarchTemplatePickerItem(
            id: "brief",
            name: "Project brief",
            description: "Goals, scope and owners.",
            category: "planning",
            badge: "New",
            systemImage: "doc.richtext"
        ),
        MonarchTemplatePickerItem(
            id: "roadmap",
            name: "Roadmap",
            description: "Themes by quarter.",
            category: "planning",
            systemImage: "map"
        ),
        MonarchTemplatePickerItem(
            id: "okr",
            name: "Quarterly goals",
            description: "Available on a higher plan.",
            category: "planning",
            systemImage: "target",
            isDisabled: true
        ),
    ]
    private static let categories = [
        MonarchTemplatePickerCategory(id: "meetings", label: "Meetings"),
        MonarchTemplatePickerCategory(id: "planning", label: "Planning"),
    ]

    @State private var selection: String?
    @State private var query = ""
    @State private var showsSheet = false

    var body: some View {
        CatalogueCaption("In the page")
        Text(selection.map { "Chosen: \($0)" } ?? "Nothing chosen yet.").monarchTextStyle(.caption)
        MonarchTemplatePicker(
            "Templates",
            selection: $selection,
            templates: Self.templates,
            categories: Self.categories,
            blank: .blank(description: "Start from an empty page.")
        )

        CatalogueCaption("In a sheet, with search")
        MonarchButton("New document", systemImage: "plus", variant: .primary) { showsSheet = true }
            .sheet(isPresented: $showsSheet) {
                NavigationStack {
                    ScrollView {
                        MonarchTemplatePicker(
                            "Templates",
                            selection: $selection,
                            templates: Self.templates,
                            categories: Self.categories,
                            query: query,
                            blank: .blank()
                        )
                        .padding()
                    }
                    .searchable(text: $query, prompt: "Search templates")
                    .navigationTitle("New document")
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Create") { showsSheet = false }.disabled(selection == nil)
                        }
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { showsSheet = false }
                        }
                    }
                }
            }
    }
}
