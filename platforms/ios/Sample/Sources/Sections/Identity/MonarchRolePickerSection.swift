import MonarchUI
import SwiftUI

struct MonarchRolePickerSection: CataloguePage {
    static let title = "Role picker"

    private let roles = [
        MonarchRoleOption("viewer", title: "Viewer", description: "Can see everything, change nothing."),
        MonarchRoleOption(
            "editor",
            title: "Editor",
            description: "Can edit content and settings of projects they are in.",
            capabilities: ["Edit pages", "Invite guests"]
        ),
        MonarchRoleOption("admin", title: "Admin", description: "Can manage people and billing."),
        MonarchRoleOption(
            "owner",
            title: "Owner",
            description: "Full control.",
            isDisabled: true,
            disabledReason: "Only owners can make someone an owner."
        ),
    ]

    @State private var role: String? = "editor"
    @State private var compactRole: String?

    var body: some View {
        CatalogueCaption("Cards")
        MonarchRolePicker(selection: $role, roles: roles, helper: "You can change this later.")

        CatalogueCaption("Compact")
        MonarchRolePicker(
            selection: $compactRole,
            roles: roles,
            error: compactRole == nil ? "Choose a role." : nil,
            layout: .compact
        )
    }
}
