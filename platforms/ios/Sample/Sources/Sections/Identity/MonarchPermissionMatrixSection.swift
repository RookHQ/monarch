import MonarchUI
import SwiftUI

struct MonarchPermissionMatrixSection: CataloguePage {
    static let title = "Permission matrix"

    private let roles = [
        MonarchPermissionMatrixRole(id: "owner", label: "Owner", description: "Everything"),
        MonarchPermissionMatrixRole(id: "admin", label: "Admin"),
        MonarchPermissionMatrixRole(id: "member", label: "Member"),
    ]
    private let permissions = [
        MonarchPermissionMatrixPermission(
            id: "invite",
            label: "Invite members",
            description: "Send invites by email.",
            group: "People"
        ),
        MonarchPermissionMatrixPermission(id: "remove", label: "Remove members", group: "People"),
        MonarchPermissionMatrixPermission(id: "billing", label: "Manage billing", group: "Billing"),
        MonarchPermissionMatrixPermission(id: "api", label: "Create API keys", group: "Developers"),
    ]

    @State private var grants: [String: Set<String>] = [
        "owner": ["invite", "remove", "billing", "api"],
        "admin": ["invite", "remove"],
    ]
    @State private var layout = MonarchPermissionMatrixLayout.automatic
    @State private var isReadOnly = false

    var body: some View {
        Picker("Layout", selection: $layout) {
            Text("Automatic").tag(MonarchPermissionMatrixLayout.automatic)
            Text("Grid").tag(MonarchPermissionMatrixLayout.grid)
            Text("By role").tag(MonarchPermissionMatrixLayout.byRole)
        }
        .pickerStyle(.segmented)
        Toggle("Read only", isOn: $isReadOnly)

        CatalogueCaption("The owner's permissions are locked")
        MonarchPermissionMatrix(
            "Workspace roles",
            selection: $grants,
            roles: roles,
            permissions: permissions,
            layout: layout,
            isReadOnly: isReadOnly
        ) { role, _ in
            role == "owner" ? "Owners can do everything." : nil
        }
    }
}
