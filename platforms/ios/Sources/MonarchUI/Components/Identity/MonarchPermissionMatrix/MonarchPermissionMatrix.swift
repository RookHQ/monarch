import SwiftUI

/// Which permissions each role has: roles by permissions, editable with
/// checkboxes or read only, grouped, with cells that cannot change.
///
/// ```swift
/// MonarchPermissionMatrix(
///     selection: grants,
///     roles: [
///         MonarchPermissionMatrixRole(id: "admin", label: "Admin"),
///         MonarchPermissionMatrixRole(id: "member", label: "Member"),
///     ],
///     permissions: [
///         MonarchPermissionMatrixPermission(id: "invite", label: "Invite members", group: "People"),
///     ],
///     lockReason: { role, _ in role == "owner" ? "Owners can do everything." : nil }
/// )
/// ```
///
/// A two-axis grid does not fit a phone, so the `automatic` layout shows the
/// grid (roles as columns) only when it fits the width, as on an iPad or in
/// landscape, and otherwise one role at a time: a ``MonarchTabs`` strip of
/// roles, then that role's permissions as checkbox rows under their group
/// headings. Disable the whole matrix with `.disabled(true)`, for example
/// while saving.
///
/// Accessibility: each cell is a standard toggle (in the Monarch checkbox
/// style) named "<permission> for <role>" in the grid, and by the permission
/// in the compact layout, where the role is the selected tab. A locked cell
/// is disabled and its reason is shown and read as the hint. Read only, each
/// cell reads "Allowed" or "Not allowed" as its value, not only a mark.
/// Role and group headings are headers.
public struct MonarchPermissionMatrix: View {
    @Binding private var selection: [String: Set<String>]
    private let roles: [MonarchPermissionMatrixRole]
    private let permissions: [MonarchPermissionMatrixPermission]
    private let title: String?
    private let layout: MonarchPermissionMatrixLayout
    private let isReadOnly: Bool
    private let labels: MonarchPermissionMatrixLabels
    private let lockReason: (@MainActor (String, String) -> String?)?

    @Environment(\.monarchTheme) private var theme
    @State private var chosenRole: String?

    /// A permission matrix.
    ///
    /// - Parameters:
    ///   - title: A heading above the matrix.
    ///   - selection: The granted permission ids by role id.
    ///   - roles: The roles, in order.
    ///   - permissions: The permissions, in order. Those with the same group are listed together.
    ///   - layout: The grid, one role at a time, or the grid when it fits.
    ///   - isReadOnly: Shows the grants as marks and words, without checkboxes.
    ///   - labels: Replacements for the built-in strings.
    ///   - lockReason: Given a role id and a permission id, why the cell cannot change, or `nil` when it can.
    ///     An empty string uses the built-in reason "This can’t be changed".
    public init(
        _ title: String? = nil,
        selection: Binding<[String: Set<String>]>,
        roles: [MonarchPermissionMatrixRole],
        permissions: [MonarchPermissionMatrixPermission],
        layout: MonarchPermissionMatrixLayout = .automatic,
        isReadOnly: Bool = false,
        labels: MonarchPermissionMatrixLabels = MonarchPermissionMatrixLabels(),
        lockReason: (@MainActor (String, String) -> String?)? = nil
    ) {
        self.title = title
        self._selection = selection
        self.roles = roles
        self.permissions = permissions
        self.layout = layout
        self.isReadOnly = isReadOnly
        self.labels = labels
        self.lockReason = lockReason
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchPermissionMatrixTokens.sectionSpacing) {
            if let title, !title.isEmpty {
                Text(title)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
            }
            switch layout {
            case .automatic:
                ViewThatFits(in: .horizontal) {
                    grid
                    byRole
                }
            case .grid:
                ScrollView(.horizontal) { grid }
            case .byRole:
                byRole
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: Grid

    private var grid: some View {
        Grid(
            alignment: .leading,
            horizontalSpacing: MonarchPermissionMatrixTokens.columnSpacing,
            verticalSpacing: MonarchPermissionMatrixTokens.rowSpacing
        ) {
            GridRow(alignment: .bottom) {
                Text(labels.permissionHeader ?? MonarchPermissionMatrixStrings.permissionHeader)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
                ForEach(roles) { role in
                    roleHeading(role)
                        .gridColumnAlignment(.center)
                }
            }
            MonarchDivider()
            ForEach(MonarchPermissionMatrixTokens.groups(permissions)) { group in
                if let label = group.label {
                    groupHeading(label)
                }
                ForEach(group.permissions) { permission in
                    GridRow {
                        permissionText(permission)
                        ForEach(roles) { role in
                            cell(role: role, permission: permission, label: cellLabel(role, permission))
                        }
                    }
                }
            }
        }
        .fixedSize()
    }

    private func roleHeading(_ role: MonarchPermissionMatrixRole) -> some View {
        VStack(spacing: MonarchPermissionMatrixTokens.textSpacing) {
            Text(role.label)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textDefault)
            if let description = role.description, !description.isEmpty {
                Text(description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .multilineTextAlignment(.center)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }

    private func groupHeading(_ label: String) -> some View {
        Text(label)
            .textCase(.uppercase)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
            .padding(.top, MonarchPermissionMatrixTokens.iconSpacing)
            .accessibilityAddTraits(.isHeader)
    }

    private func permissionText(_ permission: MonarchPermissionMatrixPermission) -> some View {
        VStack(alignment: .leading, spacing: MonarchPermissionMatrixTokens.textSpacing) {
            Text(permission.label)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textDefault)
            if let description = permission.description, !description.isEmpty {
                Text(description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .frame(width: 96, alignment: .leading)
        .accessibilityElement(children: .combine)
    }

    private func cellLabel(_ role: MonarchPermissionMatrixRole, _ permission: MonarchPermissionMatrixPermission)
        -> String
    {
        MonarchPermissionMatrixTokens.cellLabel(permission: permission.label, role: role.label, labels: labels)
    }

    @ViewBuilder private func cell(
        role: MonarchPermissionMatrixRole,
        permission: MonarchPermissionMatrixPermission,
        label: String
    ) -> some View {
        let granted = MonarchPermissionMatrixTokens.isGranted(selection, role: role.id, permission: permission.id)
        if isReadOnly {
            mark(granted)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
                .accessibilityValue(MonarchPermissionMatrixTokens.grantText(granted, labels: labels))
        } else {
            let reason = MonarchPermissionMatrixTokens.lockReason(lockReason?(role.id, permission.id), labels: labels)
            HStack(spacing: MonarchPermissionMatrixTokens.iconSpacing) {
                Toggle(isOn: binding(role: role.id, permission: permission.id)) { EmptyView() }
                    .toggleStyle(.monarchCheckbox)
                    .disabled(reason != nil)
                    .accessibilityLabel(label)
                    .accessibilityHint(reason ?? "")
                if reason != nil {
                    MonarchIcon(systemName: MonarchPermissionMatrixTokens.lockSymbol, size: .sm)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .frame(maxWidth: .infinity, alignment: .center)
        }
    }

    private func mark(_ granted: Bool) -> some View {
        MonarchIcon(
            systemName: granted
                ? MonarchPermissionMatrixTokens.allowedSymbol : MonarchPermissionMatrixTokens.notAllowedSymbol
        )
        .foregroundStyle(theme.colors[role: MonarchPermissionMatrixTokens.markRole(isGranted: granted)])
        .monarchMinimumHitTarget()
    }

    private func binding(role: String, permission: String) -> Binding<Bool> {
        Binding(
            get: { MonarchPermissionMatrixTokens.isGranted(selection, role: role, permission: permission) },
            set: {
                selection = MonarchPermissionMatrixTokens.setting(selection, role: role, permission: permission, to: $0)
            }
        )
    }

    // MARK: One role at a time

    @ViewBuilder private var byRole: some View {
        if let shown = MonarchPermissionMatrixTokens.shownRole(chosenRole, roles: roles),
            let role = roles.first(where: { $0.id == shown })
        {
            VStack(alignment: .leading, spacing: MonarchPermissionMatrixTokens.rowSpacing) {
                MonarchTabs(
                    labels.rolePicker ?? MonarchPermissionMatrixStrings.rolePicker,
                    selection: Binding(get: { shown }, set: { chosenRole = $0 }),
                    items: roles.map { MonarchTabsItem($0.label, value: $0.id) }
                )
                if let description = role.description, !description.isEmpty {
                    Text(description)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                ForEach(MonarchPermissionMatrixTokens.groups(permissions)) { group in
                    if let label = group.label {
                        groupHeading(label)
                    }
                    ForEach(group.permissions) { permission in
                        roleRow(role: role, permission: permission)
                    }
                }
            }
        }
    }

    @ViewBuilder private func roleRow(
        role: MonarchPermissionMatrixRole,
        permission: MonarchPermissionMatrixPermission
    ) -> some View {
        let granted = MonarchPermissionMatrixTokens.isGranted(selection, role: role.id, permission: permission.id)
        if isReadOnly {
            HStack(alignment: .firstTextBaseline, spacing: MonarchPermissionMatrixTokens.rowSpacing) {
                mark(granted)
                permissionText(permission)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(permission.label)
            .accessibilityValue(MonarchPermissionMatrixTokens.grantText(granted, labels: labels))
        } else {
            let reason = MonarchPermissionMatrixTokens.lockReason(lockReason?(role.id, permission.id), labels: labels)
            VStack(alignment: .leading, spacing: MonarchPermissionMatrixTokens.textSpacing) {
                Toggle(isOn: binding(role: role.id, permission: permission.id)) {
                    permissionText(permission)
                }
                .toggleStyle(.monarchCheckbox)
                .disabled(reason != nil)
                .accessibilityHint(reason ?? "")
                if let reason {
                    HStack(alignment: .firstTextBaseline, spacing: MonarchPermissionMatrixTokens.iconSpacing) {
                        MonarchIcon(systemName: MonarchPermissionMatrixTokens.lockSymbol, size: .sm)
                        Text(reason)
                            .monarchTextStyle(.caption)
                    }
                    .foregroundStyle(theme.colors.textMuted)
                    .accessibilityHidden(true)
                }
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchPermissionMatrix") {
        let grants: Binding<[String: Set<String>]> = .constant(["owner": ["invite", "billing"], "admin": ["invite"]])
        let roles = [
            MonarchPermissionMatrixRole(id: "owner", label: "Owner"),
            MonarchPermissionMatrixRole(id: "admin", label: "Admin"),
            MonarchPermissionMatrixRole(id: "member", label: "Member"),
        ]
        let permissions = [
            MonarchPermissionMatrixPermission(id: "invite", label: "Invite members", group: "People"),
            MonarchPermissionMatrixPermission(id: "billing", label: "Manage billing", group: "Billing"),
        ]
        MonarchPreviewMatrix {
            VStack {
                MonarchPermissionMatrix(selection: grants, roles: roles, permissions: permissions, layout: .grid) {
                    role,
                    _ in role == "owner" ? "" : nil
                }
                MonarchPermissionMatrix(selection: grants, roles: roles, permissions: permissions, layout: .byRole)
                MonarchPermissionMatrix(
                    selection: grants,
                    roles: roles,
                    permissions: permissions,
                    layout: .grid,
                    isReadOnly: true
                )
            }
        }
    }
#endif
