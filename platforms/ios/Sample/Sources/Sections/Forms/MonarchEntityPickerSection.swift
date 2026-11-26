import MonarchUI
import SwiftUI

struct MonarchEntityPickerSection: CataloguePage {
    static let title = "EntityPicker"

    private nonisolated static let people = [
        MonarchEntityPickerEntity(id: "ana", name: "Ana Silva", description: "Design", hint: "ana@example.com"),
        MonarchEntityPickerEntity(id: "sam", name: "Sam Lee", description: "Engineering", hint: "sam@example.com"),
        MonarchEntityPickerEntity(id: "ada", name: "Ada Byron", description: "Research", hint: "ada@example.com"),
        MonarchEntityPickerEntity(id: "kai", name: "Kai Moreno", description: "Support", isDisabled: true),
    ]

    private nonisolated static let directory =
        people + [
            MonarchEntityPickerEntity(id: "lin", name: "Lin Chen", description: "Finance"),
            MonarchEntityPickerEntity(id: "omar", name: "Omar Haddad", description: "Sales"),
            MonarchEntityPickerEntity(id: "priya", name: "Priya Nair", description: "Engineering"),
        ]

    @State private var owner: String? = "ana"
    @State private var reviewers: [String] = []
    @State private var project: String?
    @State private var projects = [
        MonarchEntityPickerEntity(id: "web", name: "Website relaunch", systemImage: "folder", shape: .square),
        MonarchEntityPickerEntity(id: "app", name: "Mobile app", systemImage: "folder", shape: .square),
    ]

    var body: some View {
        CatalogueCaption("One person, with Unassigned")
        MonarchEntityPicker(
            "Owner",
            selection: $owner,
            entities: Self.people,
            helper: "Gets the alerts for this record.",
            clearOptionTitle: "Unassigned"
        )

        CatalogueCaption("Several people from a server search")
        MonarchEntityPicker(
            "Reviewers",
            selection: $reviewers,
            entities: Array(Self.people.prefix(2)),
            selectedEntities: Self.directory,
            prompt: "Add reviewers",
            minSearchLength: 2,
            search: { query in
                // Stands in for a request to the app's own server.
                try await Task.sleep(for: .milliseconds(400))
                return Self.directory.filter { $0.name.localizedStandardContains(query) }
            }
        )

        CatalogueCaption("A record, with create")
        MonarchEntityPicker(
            "Project",
            selection: $project,
            entities: projects,
            prompt: "Choose a project",
            error: project == nil ? "Choose a project." : nil,
            isRequired: true,
            onCreate: { name in
                let created = MonarchEntityPickerEntity(
                    id: name.lowercased(),
                    name: name,
                    systemImage: "folder",
                    shape: .square
                )
                projects.append(created)
                return created
            }
        )

        CatalogueCaption("Disabled")
        MonarchEntityPicker("Owner", selection: .constant("sam"), entities: Self.people).disabled(true)
    }
}
