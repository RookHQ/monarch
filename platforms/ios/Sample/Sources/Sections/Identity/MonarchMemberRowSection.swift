import MonarchUI
import SwiftUI

struct MonarchMemberRowSection: CataloguePage {
    static let title = "Member row"

    private struct Person: Identifiable {
        let id: String
        let name: String
        let email: String
        var role: String
        var isSelf = false
    }

    private struct DemoFailure: LocalizedError {
        var errorDescription: String? { "Only owners can make someone an owner." }
    }

    private let roles = [
        MonarchRoleOption("owner", title: "Owner"),
        MonarchRoleOption("admin", title: "Admin"),
        MonarchRoleOption("member", title: "Member"),
    ]

    @State private var people = [
        Person(id: "1", name: "Priya Raman", email: "priya@example.com", role: "owner", isSelf: true),
        Person(id: "2", name: "Jonas Weber", email: "jonas@example.com", role: "admin"),
        Person(id: "3", name: "Amara Okafor", email: "amara@example.com", role: "member"),
    ]
    @State private var isInviteShown = true

    var body: some View {
        CatalogueCaption("Editable roles. Making someone an owner fails, to show the error.")
        VStack(spacing: 0) {
            ForEach(people) { person in
                MonarchMemberRow(
                    person.name,
                    image: nil,
                    email: person.email,
                    presence: person.isSelf ? .online : nil,
                    role: person.role,
                    roles: roles,
                    isSelf: person.isSelf,
                    removeConsequences: ["Their open tasks go back to the team."],
                    onRoleChange: { role in
                        try await Task.sleep(for: .milliseconds(600))
                        if role == "owner" { throw DemoFailure() }
                        if let index = people.firstIndex(where: { $0.id == person.id }) {
                            people[index].role = role
                        }
                    },
                    onRemove: {
                        try await Task.sleep(for: .milliseconds(600))
                        people.removeAll { $0.id == person.id }
                    }
                )
                MonarchDivider()
            }
        }

        CatalogueCaption("Invited, suspended and read-only")
        if isInviteShown {
            MonarchMemberRow(
                "lee@example.com",
                image: nil,
                meta: "Invite expires in 6 days",
                status: .invited,
                role: "member",
                roles: roles,
                onResend: { try await Task.sleep(for: .milliseconds(600)) },
                onRemove: { isInviteShown = false }
            )
        }
        MonarchMemberRow(
            "Sam Carter",
            image: nil,
            email: "sam@example.com",
            status: .suspended,
            role: "member",
            roles: roles,
            badges: ["Guest"]
        )

        CatalogueCaption("In a List, with swipe actions")
        List {
            MonarchMemberRow(
                "Ana Lima",
                image: nil,
                email: "ana@example.com",
                role: "member",
                roles: roles,
                onRemove: {}
            )
            MonarchMemberRow("kai@example.com", image: nil, status: .invited, onResend: {}, onRemove: {})
        }
        .listStyle(.plain)
        .frame(minHeight: 180)
    }
}
