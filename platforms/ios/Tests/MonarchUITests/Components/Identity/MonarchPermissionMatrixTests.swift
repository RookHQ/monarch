import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

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
    MonarchPermissionMatrixPermission(id: "export", label: "Export all workspace data to a file", group: "People"),
    MonarchPermissionMatrixPermission(id: "api", label: "Create API keys"),
]

private let grants: [String: Set<String>] = [
    "owner": ["invite", "remove", "billing", "export", "api"],
    "admin": ["invite", "remove"],
]

@MainActor private func ownerLock(_ role: String, _ permission: String) -> String? {
    role == "owner" ? "" : (permission == "billing" && role == "member" ? "Members never see billing." : nil)
}

enum MonarchPermissionMatrixFixtures: ComponentFixtures {
    static let id = "permission-matrix"
    static let fixtures = [
        Fixture("automatic, editable, locked") {
            MonarchPermissionMatrix(
                "Workspace roles",
                selection: .constant(grants),
                roles: roles,
                permissions: permissions,
                lockReason: ownerLock
            )
        },
        Fixture("grid") {
            MonarchPermissionMatrix(
                selection: .constant(grants),
                roles: roles,
                permissions: permissions,
                layout: .grid,
                lockReason: ownerLock
            )
        },
        Fixture("by role") {
            MonarchPermissionMatrix(
                selection: .constant(grants),
                roles: roles,
                permissions: permissions,
                layout: .byRole,
                lockReason: ownerLock
            )
        },
        Fixture("read only grid") {
            MonarchPermissionMatrix(
                selection: .constant(grants),
                roles: roles,
                permissions: permissions,
                layout: .grid,
                isReadOnly: true
            )
        },
        Fixture("read only by role") {
            MonarchPermissionMatrix(
                selection: .constant(grants),
                roles: roles,
                permissions: permissions,
                layout: .byRole,
                isReadOnly: true
            )
        },
        Fixture("disabled") {
            MonarchPermissionMatrix(selection: .constant(grants), roles: roles, permissions: permissions)
                .disabled(true)
        },
        Fixture("no roles") {
            MonarchPermissionMatrix(selection: .constant([:]), roles: [], permissions: permissions)
        },
    ]
}

@Suite("MonarchPermissionMatrix")
struct MonarchPermissionMatrixTests {
    private let labels = MonarchPermissionMatrixLabels()

    @Test("Permissions are grouped in the order groups first appear")
    func groups() {
        let groups = MonarchPermissionMatrixTokens.groups(permissions)
        #expect(groups.map(\.label) == ["People", "Billing", nil])
        #expect(groups[0].permissions.map(\.id) == ["invite", "remove", "export"])
        #expect(groups[2].permissions.map(\.id) == ["api"])
        #expect(MonarchPermissionMatrixTokens.groups([]).isEmpty)
    }

    @Test("Setting a cell changes only that role")
    func setting() {
        let granted = MonarchPermissionMatrixTokens.setting(grants, role: "member", permission: "invite", to: true)
        #expect(granted["member"] == ["invite"])
        #expect(granted["admin"] == grants["admin"])
        let revoked = MonarchPermissionMatrixTokens.setting(granted, role: "admin", permission: "remove", to: false)
        #expect(revoked["admin"] == ["invite"])
        #expect(MonarchPermissionMatrixTokens.isGranted(revoked, role: "admin", permission: "invite"))
        #expect(!MonarchPermissionMatrixTokens.isGranted(revoked, role: "admin", permission: "remove"))
        #expect(!MonarchPermissionMatrixTokens.isGranted(revoked, role: "guest", permission: "invite"))
    }

    @Test("An empty lock reason uses the built-in one")
    func lockReason() {
        #expect(MonarchPermissionMatrixTokens.lockReason(nil, labels: labels) == nil)
        #expect(MonarchPermissionMatrixTokens.lockReason("", labels: labels) == "This can’t be changed")
        #expect(
            MonarchPermissionMatrixTokens.lockReason("Owners can do everything.", labels: labels)
                == "Owners can do everything."
        )
        let custom = MonarchPermissionMatrixLabels(locked: "Fixed")
        #expect(MonarchPermissionMatrixTokens.lockReason("", labels: custom) == "Fixed")
    }

    @Test("Cells are named by permission and role and read their grant in words")
    func accessibility() {
        #expect(
            MonarchPermissionMatrixTokens.cellLabel(permission: "Invite members", role: "Admin", labels: labels)
                == "Invite members for Admin"
        )
        let custom = MonarchPermissionMatrixLabels(cellLabel: { "\($1): \($0)" })
        #expect(
            MonarchPermissionMatrixTokens.cellLabel(permission: "Invite", role: "Admin", labels: custom)
                == "Admin: Invite"
        )
        #expect(MonarchPermissionMatrixTokens.grantText(true, labels: labels) == "Allowed")
        #expect(MonarchPermissionMatrixTokens.grantText(false, labels: labels) == "Not allowed")
    }

    @Test("Read-only marks differ in colour as well as symbol and words")
    func marks() {
        #expect(MonarchPermissionMatrixTokens.markRole(isGranted: true) == \MonarchColorTokens.successStrong)
        #expect(MonarchPermissionMatrixTokens.markRole(isGranted: false) == \MonarchColorTokens.textSubtle)
        #expect(MonarchPermissionMatrixTokens.allowedSymbol != MonarchPermissionMatrixTokens.notAllowedSymbol)
    }

    @Test("The compact layout shows the chosen role while it exists")
    func shownRole() {
        #expect(MonarchPermissionMatrixTokens.shownRole(nil, roles: roles) == "owner")
        #expect(MonarchPermissionMatrixTokens.shownRole("member", roles: roles) == "member")
        #expect(MonarchPermissionMatrixTokens.shownRole("gone", roles: roles) == "owner")
        #expect(MonarchPermissionMatrixTokens.shownRole("member", roles: []) == nil)
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchPermissionMatrixStrings.permissionHeader == "Permission")
        #expect(MonarchPermissionMatrixStrings.rolePicker == "Role")
        #expect(MonarchPermissionMatrixStrings.cellLabel(permission: "A", role: "B") == "A for B")
        #expect(MonarchPermissionMatrixStrings.locked == "This can’t be changed")
    }
}
