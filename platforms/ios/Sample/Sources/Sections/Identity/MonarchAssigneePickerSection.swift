import MonarchUI
import SwiftUI

struct MonarchAssigneePickerSection: CataloguePage {
    static let title = "Assignee picker"

    @State private var assignee: String?
    @State private var reviewers: [String] = ["ana"]
    @State private var searched: String?

    nonisolated private static let people = [
        MonarchAssigneePerson(id: "ana", name: "Ana Silva", email: "ana@example.com", hint: "3 open"),
        MonarchAssigneePerson(id: "sam", name: "Sam Lee", email: "sam@example.com"),
        MonarchAssigneePerson(id: "kim", name: "Kim Park", email: "kim@example.com", hint: "Away", isDisabled: true),
        MonarchAssigneePerson(id: "lee", name: "Lee Moreau", email: "lee@example.com"),
    ]

    var body: some View {
        CatalogueCaption("One assignee, signed in as Sam")
        MonarchAssigneePicker("Assignee", selection: $assignee, people: Self.people, currentUserID: "sam")
        Text("Value: \(assignee ?? "none")").monarchTextStyle(.caption)

        CatalogueCaption("Several assignees")
        MonarchAssigneePicker("Reviewers", selection: $reviewers, people: Self.people, currentUserID: "sam")
        Text("Value: \(reviewers.joined(separator: ", "))").monarchTextStyle(.caption)

        CatalogueCaption("Searched on a server")
        MonarchAssigneePicker(
            "Assignee",
            selection: $searched,
            people: [],
            selectedPeople: Self.people,
            currentUserID: "sam",
            minSearchLength: 1,
            search: { query in
                try await Task.sleep(for: .milliseconds(400))
                return Self.people.filter { $0.name.localizedCaseInsensitiveContains(query) }
            }
        )

        CatalogueCaption("Required, with an error")
        MonarchAssigneePicker(
            "Owner",
            selection: .constant(nil),
            people: Self.people,
            error: "Choose who owns this.",
            isRequired: true,
            allowsUnassigned: false
        )
    }
}
