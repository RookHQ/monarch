import SwiftUI

/// One member of a workspace, space or channel: avatar, name, email, role,
/// status (active, invited, suspended) and confirmed actions.
///
/// ```swift
/// List(members) { member in
///     MonarchMemberRow(
///         member.name,
///         url: member.photoURL,
///         email: member.email,
///         role: member.role,
///         roles: roles,
///         onRoleChange: { role in try await team.setRole(role, for: member.id) },
///         onRemove: { try await team.remove(member.id) }
///     )
/// }
/// ```
///
/// The app passes the person, the roles allowed and the callbacks; who may
/// change what stays in the app. The role is a menu `Picker` when
/// `onRoleChange` is given (not on the signed-in person's row unless
/// `canChangeRole` says so); otherwise its title is plain text. While
/// `onRoleChange` runs the picker shows the new role and a progress
/// indicator; a thrown error puts the previous role back and shows the error
/// in the row. An invitation gets Resend invite, which confirms "Invite sent
/// again." when `onResend` returns. Remove (or Revoke invite) asks first in a
/// system confirmation dialog that lists `removeConsequences`. In a `List`,
/// the same actions are trailing swipe actions. Errors thrown with a
/// `LocalizedError` description are shown in its words.
///
/// Accessibility: the avatar, name, status, presence, email and meta are one
/// element read as the name ("Priya Raman, You") with the rest as its value,
/// so Invited, Suspended and presence never depend on colour. Every control is
/// named after the person: "Role for Jonas Weber", "Resend invite, Lee",
/// "Remove Jonas Weber". Outcomes and failures are announced. Controls keep
/// a 44-point touch target.
public struct MonarchMemberRow<Actions: View>: View {
    private let name: String
    private let image: Image?
    private var url: URL?
    private let email: String?
    private let meta: String?
    private let presence: MonarchPresenceStatus?
    private let status: MonarchMemberRowStatus
    private let statusTone: MonarchTone?
    private let role: String?
    private let roles: [MonarchRoleOption<String>]
    private let badges: [String]
    private let isSelf: Bool
    private let canChangeRole: Bool?
    private let canRemove: Bool?
    private let removeConsequences: [String]
    private let labels: MonarchMemberRowLabels
    private let onRoleChange: (@MainActor (String) async throws -> Void)?
    private let onResend: (@MainActor () async throws -> Void)?
    private let onRemove: (@MainActor () async throws -> Void)?
    private let actions: Actions

    @State private var pendingRole: String?
    @State private var isRolePending = false
    @State private var isResending = false
    @State private var isRemoving = false
    @State private var isConfirming = false
    @State private var notice: MonarchMemberRowTokens.Notice?
    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    /// A member row with the avatar from an image, and extra controls before Remove.
    ///
    /// - Parameters:
    ///   - name: The display name, already localised. It also seeds the avatar's initials and colour.
    ///   - image: The avatar image. Initials are shown without it.
    ///   - email: The email or handle under the name.
    ///   - meta: A line of extra detail: joined date, invite expiry, availability.
    ///   - presence: The presence dot on the avatar, also read after the name.
    ///   - status: Active, invited or suspended.
    ///   - statusTone: The tone of the status badge. `nil` keeps the default: info for invited, warning for suspended.
    ///   - role: The value of the member's role, one of `roles`.
    ///   - roles: The roles allowed here. Their titles also show for a role that cannot be changed.
    ///   - badges: Extra markers after the name, such as "Guest".
    ///   - isSelf: The row is the signed-in person: shows "(You)" and, by default, hides the role picker and Remove.
    ///   - canChangeRole: Shows the role picker. By default when `onRoleChange` is given and it is not your own row.
    ///   - canRemove: Shows Remove. By default when `onRemove` is given and it is not your own row.
    ///   - removeConsequences: Lines added to the remove confirmation, such as "3 assigned tickets go back to the queue".
    ///   - labels: Replacements for the built-in strings.
    ///   - onRoleChange: Changes the role. Throw to put the previous role back and show the error.
    ///   - onResend: Sends an invitation again. Shown for invited members. Throw to show the error.
    ///   - onRemove: Removes the member or revokes the invitation, after a confirmation. Throw to show the error.
    ///   - actions: More controls before Remove, such as a `Menu`. Name them after the member.
    public init(
        _ name: String,
        image: Image?,
        email: String? = nil,
        meta: String? = nil,
        presence: MonarchPresenceStatus? = nil,
        status: MonarchMemberRowStatus = .active,
        statusTone: MonarchTone? = nil,
        role: String? = nil,
        roles: [MonarchRoleOption<String>] = [],
        badges: [String] = [],
        isSelf: Bool = false,
        canChangeRole: Bool? = nil,
        canRemove: Bool? = nil,
        removeConsequences: [String] = [],
        labels: MonarchMemberRowLabels = MonarchMemberRowLabels(),
        onRoleChange: (@MainActor (String) async throws -> Void)? = nil,
        onResend: (@MainActor () async throws -> Void)? = nil,
        onRemove: (@MainActor () async throws -> Void)? = nil,
        @ViewBuilder actions: () -> Actions
    ) {
        self.name = name
        self.image = image
        self.url = nil
        self.email = email
        self.meta = meta
        self.presence = presence
        self.status = status
        self.statusTone = statusTone
        self.role = role
        self.roles = roles
        self.badges = badges
        self.isSelf = isSelf
        self.canChangeRole = canChangeRole
        self.canRemove = canRemove
        self.removeConsequences = removeConsequences
        self.labels = labels
        self.onRoleChange = onRoleChange
        self.onResend = onResend
        self.onRemove = onRemove
        self.actions = actions()
    }

    /// A member row with the avatar loaded from a URL, and extra controls before Remove.
    ///
    /// - Parameters:
    ///   - name: The display name, already localised. It also seeds the avatar's initials and colour.
    ///   - url: The avatar image URL, loaded with `AsyncImage`. Initials are shown while loading, on failure and without it.
    ///   - email: The email or handle under the name.
    ///   - meta: A line of extra detail: joined date, invite expiry, availability.
    ///   - presence: The presence dot on the avatar, also read after the name.
    ///   - status: Active, invited or suspended.
    ///   - statusTone: The tone of the status badge. `nil` keeps the default: info for invited, warning for suspended.
    ///   - role: The value of the member's role, one of `roles`.
    ///   - roles: The roles allowed here. Their titles also show for a role that cannot be changed.
    ///   - badges: Extra markers after the name, such as "Guest".
    ///   - isSelf: The row is the signed-in person: shows "(You)" and, by default, hides the role picker and Remove.
    ///   - canChangeRole: Shows the role picker. By default when `onRoleChange` is given and it is not your own row.
    ///   - canRemove: Shows Remove. By default when `onRemove` is given and it is not your own row.
    ///   - removeConsequences: Lines added to the remove confirmation, such as "3 assigned tickets go back to the queue".
    ///   - labels: Replacements for the built-in strings.
    ///   - onRoleChange: Changes the role. Throw to put the previous role back and show the error.
    ///   - onResend: Sends an invitation again. Shown for invited members. Throw to show the error.
    ///   - onRemove: Removes the member or revokes the invitation, after a confirmation. Throw to show the error.
    ///   - actions: More controls before Remove, such as a `Menu`. Name them after the member.
    public init(
        _ name: String,
        url: URL?,
        email: String? = nil,
        meta: String? = nil,
        presence: MonarchPresenceStatus? = nil,
        status: MonarchMemberRowStatus = .active,
        statusTone: MonarchTone? = nil,
        role: String? = nil,
        roles: [MonarchRoleOption<String>] = [],
        badges: [String] = [],
        isSelf: Bool = false,
        canChangeRole: Bool? = nil,
        canRemove: Bool? = nil,
        removeConsequences: [String] = [],
        labels: MonarchMemberRowLabels = MonarchMemberRowLabels(),
        onRoleChange: (@MainActor (String) async throws -> Void)? = nil,
        onResend: (@MainActor () async throws -> Void)? = nil,
        onRemove: (@MainActor () async throws -> Void)? = nil,
        @ViewBuilder actions: () -> Actions
    ) {
        self.init(
            name,
            image: nil,
            email: email,
            meta: meta,
            presence: presence,
            status: status,
            statusTone: statusTone,
            role: role,
            roles: roles,
            badges: badges,
            isSelf: isSelf,
            canChangeRole: canChangeRole,
            canRemove: canRemove,
            removeConsequences: removeConsequences,
            labels: labels,
            onRoleChange: onRoleChange,
            onResend: onResend,
            onRemove: onRemove,
            actions: actions
        )
        self.url = url
    }

    private var shownRole: String? { pendingRole ?? role }

    private var isRoleEditable: Bool {
        MonarchMemberRowTokens.isRoleEditable(
            canChangeRole: canChangeRole,
            hasHandler: onRoleChange != nil,
            isSelf: isSelf,
            roleCount: roles.count
        )
    }

    private var isRemovable: Bool {
        MonarchMemberRowTokens.isRemovable(canRemove: canRemove, hasHandler: onRemove != nil, isSelf: isSelf)
    }

    private var isResendable: Bool {
        MonarchMemberRowTokens.isResendable(status: status, hasHandler: onResend != nil)
    }

    /// The content of the view.
    public var body: some View {
        let confirmation = MonarchMemberRowTokens.confirmation(
            name: name,
            status: status,
            consequences: removeConsequences,
            labels: labels
        )
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .center, spacing: MonarchMemberRowTokens.controlSpacing) {
                identity
                controls
            }
            VStack(alignment: .leading, spacing: MonarchMemberRowTokens.stackSpacing) {
                identity
                controls
            }
        }
        .padding(.vertical, MonarchMemberRowTokens.verticalPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            if isRemovable && isEnabled && !isRemoving {
                Button {
                    isConfirming = true
                } label: {
                    Label(confirmation.confirm, systemImage: MonarchMemberRowTokens.removeSymbol)
                }
                .tint(theme.colors.dangerSolid)
            }
            if isResendable && isEnabled && !isResending {
                Button {
                    resend()
                } label: {
                    Label(
                        labels.resend ?? MonarchMemberRowStrings.resend,
                        systemImage: MonarchMemberRowTokens.resendSymbol
                    )
                }
                .tint(theme.colors.infoSolid)
            }
        }
        .confirmationDialog(confirmation.title, isPresented: $isConfirming, titleVisibility: .visible) {
            Button(confirmation.confirm, role: .destructive) { remove() }
            Button(labels.cancel ?? MonarchMemberRowStrings.cancel, role: .cancel) {}
        } message: {
            Text(confirmation.message)
        }
        .onChange(of: role) { _, _ in pendingRole = nil }
    }

    private var identity: some View {
        let roleTitle = MonarchMemberRowTokens.roleTitle(shownRole, roles: roles)
        return HStack(alignment: .top, spacing: MonarchMemberRowTokens.avatarSpacing) {
            avatar
            VStack(alignment: .leading, spacing: MonarchMemberRowTokens.lineSpacing) {
                MonarchFlowLayout(spacing: MonarchMemberRowTokens.nameSpacing) {
                    Text(name)
                        .monarchTextStyle(.body, weight: .medium)
                        .foregroundStyle(theme.colors[role: MonarchMemberRowTokens.nameRole(status)])
                        .lineLimit(1)
                        .truncationMode(.tail)
                    if isSelf {
                        Text(MonarchMemberRowTokens.selfMarker(labels))
                            .monarchTextStyle(.body)
                            .foregroundStyle(theme.colors.textMuted)
                    }
                    if let badge = MonarchMemberRowTokens.badge(status, tone: statusTone, labels: labels) {
                        MonarchBadge(badge.text, tone: badge.tone)
                    }
                    ForEach(badges, id: \.self) { MonarchBadge($0) }
                }
                if let email, !email.isEmpty {
                    Text(email)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                        .lineLimit(1)
                        .truncationMode(.middle)
                }
                if let meta, !meta.isEmpty {
                    Text(meta)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                if let notice {
                    HStack(alignment: .firstTextBaseline, spacing: MonarchMemberRowTokens.noticeIconSpacing) {
                        MonarchIcon(
                            systemName: notice.isError
                                ? MonarchMemberRowTokens.errorSymbol : MonarchMemberRowTokens.doneSymbol,
                            size: .sm
                        )
                        Text(notice.text).monarchTextStyle(.caption)
                    }
                    .foregroundStyle(theme.colors[role: MonarchMemberRowTokens.noticeRole(notice)])
                    .fixedSize(horizontal: false, vertical: true)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchMemberRowTokens.accessibilityLabel(name: name, isSelf: isSelf, labels: labels))
        .accessibilityValue(
            MonarchMemberRowTokens.accessibilityValue(
                status: status,
                presence: presence,
                badges: badges,
                readOnlyRole: isRoleEditable ? nil : roleTitle,
                email: email,
                meta: [meta, notice?.text].compactMap { $0 }.joined(separator: ", "),
                labels: labels
            )
        )
        // The same actions as the trailing swipe, reachable from the member's own element.
        .accessibilityActions {
            if isRemovable && isEnabled && !isRemoving {
                Button(MonarchMemberRowTokens.removeLabel(name: name, status: status, labels: labels)) {
                    isConfirming = true
                }
            }
            if isResendable && isEnabled && !isResending {
                Button(labels.resendLabel?(name) ?? MonarchMemberRowStrings.resendLabel(name)) {
                    resend()
                }
            }
        }
    }

    @ViewBuilder private var avatar: some View {
        if let url {
            MonarchAvatar(
                url: url,
                name: name,
                size: MonarchMemberRowTokens.avatarSize,
                status: presence,
                isDecorative: true
            )
        } else {
            MonarchAvatar(
                image: image,
                name: name,
                size: MonarchMemberRowTokens.avatarSize,
                status: presence,
                isDecorative: true
            )
        }
    }

    private var controls: some View {
        HStack(spacing: MonarchMemberRowTokens.controlSpacing) {
            roleControl
            if isResendable {
                MonarchButton(
                    labels.resend ?? MonarchMemberRowStrings.resend,
                    variant: .ghost,
                    size: .sm,
                    isLoading: isResending
                ) {
                    resend()
                }
                .accessibilityLabel(labels.resendLabel?(name) ?? MonarchMemberRowStrings.resendLabel(name))
            }
            actions
            if isRemovable {
                removeButton
            }
        }
        .fixedSize(horizontal: true, vertical: false)
    }

    @ViewBuilder private var roleControl: some View {
        if isRoleEditable {
            HStack(spacing: MonarchMemberRowTokens.noticeIconSpacing) {
                MonarchSelectField(
                    labels.roleLabel?(name) ?? MonarchMemberRowStrings.roleLabel(name),
                    selection: Binding(get: { shownRole ?? "" }, set: { changeRole($0) }),
                    valueText: roles.first { $0.value == shownRole }?.title,
                    prompt: labels.chooseRole ?? MonarchMemberRowStrings.chooseRole,
                    style: .compact
                ) {
                    if shownRole == nil {
                        Text(labels.chooseRole ?? MonarchMemberRowStrings.chooseRole).tag("")
                    }
                    ForEach(roles) { option in
                        Text(option.title).tag(option.value).disabled(option.isDisabled)
                    }
                }
                .frame(minWidth: MonarchMemberRowTokens.roleMinimumWidth)
                .disabled(isRolePending)
                if isRolePending {
                    ProgressView().controlSize(.small)
                }
            }
        } else if let title = MonarchMemberRowTokens.roleTitle(shownRole, roles: roles) {
            Text(title)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
                .accessibilityHidden(true)
        }
    }

    @ViewBuilder private var removeButton: some View {
        if isRemoving {
            ProgressView()
                .controlSize(.small)
                .monarchMinimumHitTarget()
        } else {
            Button {
                isConfirming = true
            } label: {
                MonarchIcon(systemName: MonarchMemberRowTokens.removeSymbol, size: .sm)
                    .foregroundStyle(isEnabled ? theme.colors.textMuted : theme.colors.textDisabled)
                    .monarchMinimumHitTarget()
            }
            .buttonStyle(.plain)
            .accessibilityLabel(MonarchMemberRowTokens.removeLabel(name: name, status: status, labels: labels))
        }
    }

    private func changeRole(_ next: String) {
        guard let onRoleChange, !next.isEmpty, next != shownRole, !isRolePending else { return }
        pendingRole = next
        notice = nil
        isRolePending = true
        Task { @MainActor in
            do {
                try await onRoleChange(next)
            } catch {
                pendingRole = nil
                fail(error)
            }
            isRolePending = false
        }
    }

    private func resend() {
        guard let onResend, !isResending else { return }
        notice = nil
        isResending = true
        Task { @MainActor in
            do {
                try await onResend()
                let message = labels.resent ?? MonarchMemberRowStrings.resent
                notice = .done(message)
                AccessibilityNotification.Announcement(message).post()
            } catch {
                fail(error)
            }
            isResending = false
        }
    }

    private func remove() {
        guard let onRemove, !isRemoving else { return }
        notice = nil
        isRemoving = true
        Task { @MainActor in
            do {
                try await onRemove()
            } catch {
                fail(error)
            }
            isRemoving = false
        }
    }

    private func fail(_ error: any Error) {
        let message = MonarchMemberRowTokens.failureMessage(error, labels: labels)
        notice = .error(message)
        AccessibilityNotification.Announcement(message).post()
    }
}

extension MonarchMemberRow where Actions == EmptyView {
    /// A member row with the avatar from an image.
    ///
    /// - Parameters:
    ///   - name: The display name, already localised. It also seeds the avatar's initials and colour.
    ///   - image: The avatar image. Initials are shown without it.
    ///   - email: The email or handle under the name.
    ///   - meta: A line of extra detail: joined date, invite expiry, availability.
    ///   - presence: The presence dot on the avatar, also read after the name.
    ///   - status: Active, invited or suspended.
    ///   - statusTone: The tone of the status badge. `nil` keeps the default: info for invited, warning for suspended.
    ///   - role: The value of the member's role, one of `roles`.
    ///   - roles: The roles allowed here. Their titles also show for a role that cannot be changed.
    ///   - badges: Extra markers after the name, such as "Guest".
    ///   - isSelf: The row is the signed-in person: shows "(You)" and, by default, hides the role picker and Remove.
    ///   - canChangeRole: Shows the role picker. By default when `onRoleChange` is given and it is not your own row.
    ///   - canRemove: Shows Remove. By default when `onRemove` is given and it is not your own row.
    ///   - removeConsequences: Lines added to the remove confirmation.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRoleChange: Changes the role. Throw to put the previous role back and show the error.
    ///   - onResend: Sends an invitation again. Shown for invited members. Throw to show the error.
    ///   - onRemove: Removes the member or revokes the invitation, after a confirmation. Throw to show the error.
    public init(
        _ name: String,
        image: Image?,
        email: String? = nil,
        meta: String? = nil,
        presence: MonarchPresenceStatus? = nil,
        status: MonarchMemberRowStatus = .active,
        statusTone: MonarchTone? = nil,
        role: String? = nil,
        roles: [MonarchRoleOption<String>] = [],
        badges: [String] = [],
        isSelf: Bool = false,
        canChangeRole: Bool? = nil,
        canRemove: Bool? = nil,
        removeConsequences: [String] = [],
        labels: MonarchMemberRowLabels = MonarchMemberRowLabels(),
        onRoleChange: (@MainActor (String) async throws -> Void)? = nil,
        onResend: (@MainActor () async throws -> Void)? = nil,
        onRemove: (@MainActor () async throws -> Void)? = nil
    ) {
        self.init(
            name,
            image: image,
            email: email,
            meta: meta,
            presence: presence,
            status: status,
            statusTone: statusTone,
            role: role,
            roles: roles,
            badges: badges,
            isSelf: isSelf,
            canChangeRole: canChangeRole,
            canRemove: canRemove,
            removeConsequences: removeConsequences,
            labels: labels,
            onRoleChange: onRoleChange,
            onResend: onResend,
            onRemove: onRemove,
            actions: { EmptyView() }
        )
    }

    /// A member row with the avatar loaded from a URL.
    ///
    /// - Parameters:
    ///   - name: The display name, already localised. It also seeds the avatar's initials and colour.
    ///   - url: The avatar image URL, loaded with `AsyncImage`. Initials are shown while loading, on failure and without it.
    ///   - email: The email or handle under the name.
    ///   - meta: A line of extra detail: joined date, invite expiry, availability.
    ///   - presence: The presence dot on the avatar, also read after the name.
    ///   - status: Active, invited or suspended.
    ///   - statusTone: The tone of the status badge. `nil` keeps the default: info for invited, warning for suspended.
    ///   - role: The value of the member's role, one of `roles`.
    ///   - roles: The roles allowed here. Their titles also show for a role that cannot be changed.
    ///   - badges: Extra markers after the name, such as "Guest".
    ///   - isSelf: The row is the signed-in person: shows "(You)" and, by default, hides the role picker and Remove.
    ///   - canChangeRole: Shows the role picker. By default when `onRoleChange` is given and it is not your own row.
    ///   - canRemove: Shows Remove. By default when `onRemove` is given and it is not your own row.
    ///   - removeConsequences: Lines added to the remove confirmation.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRoleChange: Changes the role. Throw to put the previous role back and show the error.
    ///   - onResend: Sends an invitation again. Shown for invited members. Throw to show the error.
    ///   - onRemove: Removes the member or revokes the invitation, after a confirmation. Throw to show the error.
    public init(
        _ name: String,
        url: URL?,
        email: String? = nil,
        meta: String? = nil,
        presence: MonarchPresenceStatus? = nil,
        status: MonarchMemberRowStatus = .active,
        statusTone: MonarchTone? = nil,
        role: String? = nil,
        roles: [MonarchRoleOption<String>] = [],
        badges: [String] = [],
        isSelf: Bool = false,
        canChangeRole: Bool? = nil,
        canRemove: Bool? = nil,
        removeConsequences: [String] = [],
        labels: MonarchMemberRowLabels = MonarchMemberRowLabels(),
        onRoleChange: (@MainActor (String) async throws -> Void)? = nil,
        onResend: (@MainActor () async throws -> Void)? = nil,
        onRemove: (@MainActor () async throws -> Void)? = nil
    ) {
        self.init(
            name,
            url: url,
            email: email,
            meta: meta,
            presence: presence,
            status: status,
            statusTone: statusTone,
            role: role,
            roles: roles,
            badges: badges,
            isSelf: isSelf,
            canChangeRole: canChangeRole,
            canRemove: canRemove,
            removeConsequences: removeConsequences,
            labels: labels,
            onRoleChange: onRoleChange,
            onResend: onResend,
            onRemove: onRemove,
            actions: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchMemberRow") {
        let roles = [
            MonarchRoleOption("owner", title: "Owner"),
            MonarchRoleOption("admin", title: "Admin"),
            MonarchRoleOption("member", title: "Member"),
        ]
        MonarchPreviewMatrix {
            VStack {
                MonarchMemberRow(
                    "Priya Raman",
                    image: nil,
                    email: "priya@example.com",
                    presence: .online,
                    role: "owner",
                    roles: roles,
                    isSelf: true,
                    onRoleChange: { _ in },
                    onRemove: {}
                )
                MonarchMemberRow(
                    "Jonas Weber",
                    image: nil,
                    email: "jonas@example.com",
                    role: "admin",
                    roles: roles,
                    onRoleChange: { _ in },
                    onRemove: {}
                )
                MonarchMemberRow(
                    "lee@example.com",
                    image: nil,
                    meta: "Invite expires in 6 days",
                    status: .invited,
                    role: "member",
                    roles: roles,
                    onResend: {},
                    onRemove: {}
                )
                MonarchMemberRow(
                    "Sam Carter",
                    image: nil,
                    email: "sam@example.com",
                    status: .suspended,
                    role: "member",
                    roles: roles
                )
            }
        }
    }
#endif
