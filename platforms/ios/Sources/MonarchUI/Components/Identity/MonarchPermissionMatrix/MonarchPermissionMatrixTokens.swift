import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchPermissionMatrix``.
enum MonarchPermissionMatrixTokens {
    /// The gap between the parts of the matrix.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between grid columns.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between grid rows and between compact rows.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between a name and the line under it.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between a cell's mark and its lock.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The SF Symbol of a granted cell when read only.
    static let allowedSymbol = "checkmark"
    /// The SF Symbol of a cell that is not granted when read only.
    static let notAllowedSymbol = "minus"
    /// The SF Symbol of a locked cell.
    static let lockSymbol = "lock.fill"

    /// The permissions listed under one group heading.
    struct Group: Equatable, Identifiable {
        /// The heading, or `nil` for permissions without a group.
        let label: String?
        /// The permissions, in their order.
        let permissions: [MonarchPermissionMatrixPermission]
        var id: String { label ?? "" }
    }

    /// The permissions by group, groups in the order they first appear.
    static func groups(_ permissions: [MonarchPermissionMatrixPermission]) -> [Group] {
        var order: [String?] = []
        var rows: [String?: [MonarchPermissionMatrixPermission]] = [:]
        for permission in permissions {
            if rows[permission.group] == nil {
                order.append(permission.group)
                rows[permission.group] = []
            }
            rows[permission.group, default: []].append(permission)
        }
        return order.map { Group(label: $0, permissions: rows[$0] ?? []) }
    }

    /// Whether a role has a permission.
    static func isGranted(_ selection: [String: Set<String>], role: String, permission: String) -> Bool {
        selection[role]?.contains(permission) ?? false
    }

    /// The selection with one cell changed. Other roles are untouched.
    static func setting(
        _ selection: [String: Set<String>],
        role: String,
        permission: String,
        to isGranted: Bool
    ) -> [String: Set<String>] {
        var next = selection
        var granted = next[role] ?? []
        if isGranted {
            granted.insert(permission)
        } else {
            granted.remove(permission)
        }
        next[role] = granted
        return next
    }

    /// The reason a cell is locked, or `nil` when it can change. An empty
    /// reason from the app becomes the built-in one.
    static func lockReason(_ reason: String?, labels: MonarchPermissionMatrixLabels) -> String? {
        guard let reason else { return nil }
        return reason.isEmpty ? labels.locked ?? MonarchPermissionMatrixStrings.locked : reason
    }

    /// The spoken name of a cell: "Invite members for Admin".
    static func cellLabel(permission: String, role: String, labels: MonarchPermissionMatrixLabels) -> String {
        labels.cellLabel?(permission, role)
            ?? MonarchPermissionMatrixStrings.cellLabel(permission: permission, role: role)
    }

    /// Allowed or Not allowed, for a read-only cell.
    static func grantText(_ isGranted: Bool, labels: MonarchPermissionMatrixLabels) -> String {
        isGranted
            ? labels.allowed ?? MonarchPermissionMatrixStrings.allowed
            : labels.notAllowed ?? MonarchPermissionMatrixStrings.notAllowed
    }

    /// The colour role of a read-only mark. The words say the same.
    static func markRole(isGranted: Bool) -> MonarchColorRole {
        isGranted ? \.successStrong : \.textSubtle
    }

    /// The role shown in the compact layout: the chosen one while it exists, else the first.
    static func shownRole(_ chosen: String?, roles: [MonarchPermissionMatrixRole]) -> String? {
        if let chosen, roles.contains(where: { $0.id == chosen }) { return chosen }
        return roles.first?.id
    }
}
