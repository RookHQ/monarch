import SwiftUI

/// Invitations that have not been accepted yet: who was invited, the role,
/// who sent it, when it was sent and when it expires, with Resend, Copy link
/// and Revoke behind a confirmation.
///
/// ```swift
/// MonarchPendingInvitesList(
///     invites,
///     onResend: { id in try await team.resendInvite(id) },
///     onRevoke: { id in try await team.revokeInvite(id) }
/// )
/// ```
///
/// The list is a ``MonarchSectionCard`` with one ``MonarchMemberRow`` per
/// invitation in its invited status, so Resend invite, the revoke
/// confirmation, the pending states and the error text are the member row's.
/// Copy link is a ``MonarchCopyButton`` and appears only for an invitation
/// with a `link` that has not expired. An expired invitation keeps its row,
/// with an "Expired" badge and Resend as the way forward. A loaded empty
/// list shows the empty state; `.loading` and `.error` show the section
/// card's states.
///
/// Accessibility: the title is a header. Each invitation is read as its
/// email, then "Invited" or "Expired", the role and the detail line
/// ("Invited by Sam Rivera · Sent 2 days ago · Expires in 5 days"). Every
/// button is named after the email address. A revoke is announced.
@_spi(Experimental) public struct MonarchPendingInvitesList: View {
    private let invites: [MonarchPendingInvite]
    private let title: String?
    private let description: String?
    private let state: MonarchSectionCardState
    private let isRetrying: Bool
    private let now: Date?
    private let labels: MonarchPendingInvitesListLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onResend: (@MainActor (String) async throws -> Void)?
    private let onRevoke: (@MainActor (String) async throws -> Void)?

    private typealias Tokens = MonarchPendingInvitesListTokens

    /// A list of pending invitations.
    ///
    /// - Parameters:
    ///   - invites: The invitations, in the order to show them.
    ///   - title: Replaces the built-in title "Pending invitations".
    ///   - description: A line under the title.
    ///   - state: Whether the invitations are loaded, loading or failed to load. A loaded empty list shows the
    ///     empty state.
    ///   - isRetrying: Whether a retry is in progress, in the error state.
    ///   - now: The reference time for relative times and expiry. Without it they follow the clock, updated
    ///     every minute.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Retries loading. Shows a retry button in the error state.
    ///   - onResend: Sends an invitation again, given its id. Without it rows have no Resend. Throw to show the
    ///     error in the row.
    ///   - onRevoke: Revokes an invitation, given its id, after a confirmation. Without it rows have no Revoke.
    ///     Throw to show the error in the row; remove the invitation from `invites` on success.
    public init(
        _ invites: [MonarchPendingInvite],
        title: String? = nil,
        description: String? = nil,
        state: MonarchSectionCardState = .ready,
        isRetrying: Bool = false,
        now: Date? = nil,
        labels: MonarchPendingInvitesListLabels = MonarchPendingInvitesListLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onResend: (@MainActor (String) async throws -> Void)? = nil,
        onRevoke: (@MainActor (String) async throws -> Void)? = nil
    ) {
        self.invites = invites
        self.title = title
        self.description = description
        self.state = state
        self.isRetrying = isRetrying
        self.now = now
        self.labels = labels
        self.onRetry = onRetry
        self.onResend = onResend
        self.onRevoke = onRevoke
    }

    /// The content of the view.
    public var body: some View {
        MonarchSectionCard(
            title ?? labels.title ?? MonarchPendingInvitesListStrings.title,
            description: description,
            state: Tokens.phase(state, invites: invites),
            isRetrying: isRetrying,
            labels: MonarchSectionCardLabels(
                loading: labels.loading ?? MonarchPendingInvitesListStrings.loading,
                emptyTitle: labels.emptyTitle ?? MonarchPendingInvitesListStrings.emptyTitle,
                errorTitle: labels.errorTitle ?? MonarchPendingInvitesListStrings.errorTitle
            ),
            onRetry: onRetry
        ) {
            if let now {
                rows(now: now)
            } else {
                MonarchRelativeTimeClock(dates: invites.flatMap { [$0.sentAt, $0.expiresAt] }) { current in
                    rows(now: current)
                }
            }
        }
    }

    private func rows(now: Date) -> some View {
        VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            ForEach(Array(invites.enumerated()), id: \.element.id) { index, invite in
                if index > 0 {
                    MonarchDivider()
                }
                MonarchPendingInvitesListRow(
                    invite: invite,
                    now: now,
                    labels: labels,
                    onResend: onResend,
                    onRevoke: onRevoke
                )
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.listLabel ?? MonarchPendingInvitesListStrings.listLabel)
    }
}

/// One invitation, laid out as on the web: the email with its role and Expired badges, the detail line, then
/// Copy link, Resend and Revoke as labelled buttons.
private struct MonarchPendingInvitesListRow: View {
    let invite: MonarchPendingInvite
    let now: Date
    let labels: MonarchPendingInvitesListLabels
    let onResend: (@MainActor (String) async throws -> Void)?
    let onRevoke: (@MainActor (String) async throws -> Void)?

    @Environment(\.locale) private var locale
    @Environment(\.monarchTheme) private var theme
    @State private var isResending = false

    private typealias Tokens = MonarchPendingInvitesListTokens

    var body: some View {
        let isExpired = Tokens.isExpired(invite, now: now)
        let link = Tokens.copyableLink(invite, isExpired: isExpired)
        let detail = Tokens.detail(invite, isExpired: isExpired, now: now, locale: locale, labels: labels)
        VStack(alignment: .leading, spacing: theme.spacing.space2) {
            MonarchFlowLayout(spacing: theme.spacing.space2) {
                Text(invite.email)
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors.textDefault)
                if let role = invite.role, !role.isEmpty {
                    MonarchBadge(role, size: .sm)
                }
                if isExpired {
                    MonarchBadge(
                        labels.expired ?? MonarchPendingInvitesListStrings.expired,
                        tone: .warning,
                        size: .sm
                    )
                }
            }
            if let detail {
                Text(detail)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            MonarchFlowLayout(spacing: theme.spacing.space2) {
                if let link {
                    MonarchCopyButton(
                        labels.copyLink ?? MonarchPendingInvitesListStrings.copyLink,
                        value: link,
                        target: Tokens.copyTarget(invite.email, labels: labels),
                        systemImage: Tokens.copySymbol,
                        variant: .ghost,
                        size: .sm
                    )
                }
                if let resendAction {
                    MonarchButton(
                        labels.resend ?? MonarchPendingInvitesListStrings.resend,
                        variant: .secondary,
                        size: .sm,
                        isLoading: isResending
                    ) {
                        Task {
                            isResending = true
                            defer { isResending = false }
                            try? await resendAction()
                        }
                    }
                }
                if let revokeAction {
                    MonarchButton(
                        labels.revoke ?? MonarchPendingInvitesListStrings.revoke,
                        variant: .dangerSubtle,
                        size: .sm
                    ) {
                        Task { try? await revokeAction() }
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
    }

    private var resendAction: (@MainActor () async throws -> Void)? {
        guard let onResend else { return nil }
        let id = invite.id
        return { try await onResend(id) }
    }

    private var revokeAction: (@MainActor () async throws -> Void)? {
        guard let onRevoke else { return nil }
        let id = invite.id
        let announcement = Tokens.revokedAnnouncement(invite.email, labels: labels)
        return {
            try await onRevoke(id)
            AccessibilityNotification.Announcement(announcement).post()
        }
    }
}

#if DEBUG
    #Preview("MonarchPendingInvitesList") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        MonarchPreviewMatrix {
            VStack {
                MonarchPendingInvitesList(
                    [
                        MonarchPendingInvite(
                            id: "1",
                            email: "ana@example.com",
                            role: "Member",
                            invitedBy: "Sam Rivera",
                            sentAt: now.addingTimeInterval(-86_400 * 2),
                            expiresAt: now.addingTimeInterval(86_400 * 5),
                            link: "https://example.com/join/1"
                        ),
                        MonarchPendingInvite(
                            id: "2",
                            email: "bo@example.com",
                            role: "Admin",
                            sentAt: now.addingTimeInterval(-86_400 * 9),
                            expiresAt: now.addingTimeInterval(-86_400 * 2)
                        ),
                    ],
                    now: now,
                    onResend: { _ in },
                    onRevoke: { _ in }
                )
                MonarchPendingInvitesList([], state: .loading)
                MonarchPendingInvitesList([])
                MonarchPendingInvitesList([], state: .error, onRetry: {})
            }
        }
    }
#endif
