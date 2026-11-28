import MonarchTokens
import SwiftUI

/// The places an account is signed in: one row per browser or app with its
/// device, location and times, a "This device" badge, Sign out on each row
/// and Sign out other devices in the header, each behind a confirmation.
///
/// ```swift
/// MonarchSessionList(
///     sessions,
///     onSignOut: { id in try await account.signOut(session: id) },
///     onSignOutOthers: { ids in try await account.signOut(sessions: ids) }
/// )
/// ```
///
/// The list is a ``MonarchSectionCard``: it shows skeleton rows while
/// loading, a ``MonarchEmptyState`` for an empty list and a
/// ``MonarchErrorState`` with a retry on failure. Sign out appears on every
/// row with `onSignOut`, including the current one; Sign out other devices
/// appears with `onSignOutOthers` when another session exists. Both ask
/// first with a system alert unless `confirmsSignOut` is `false`. While a
/// handler runs its button is busy and the other buttons wait; a thrown
/// error shows a danger notice in the card. The app removes signed-out
/// sessions from `sessions`.
///
/// Accessibility: the title is a header. Each session is one element read
/// as its label, then "This device", the detail and the times, so the
/// current session never depends on the badge colour. Sign out is a
/// separate button named after the session ("Sign out Safari on iPhone").
/// Results and failures are announced. Times update every minute unless
/// `now` is given.
@_spi(Experimental) public struct MonarchSessionList: View {
    private let sessions: [MonarchSession]
    private let title: String?
    private let description: String?
    private let state: MonarchSessionListState
    private let isRetrying: Bool
    private let confirmsSignOut: Bool
    private let now: Date?
    private let footnote: String?
    private let labels: MonarchSessionListLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onSignOut: (@MainActor (String) async throws -> Void)?
    private let onSignOutOthers: (@MainActor ([String]) async throws -> Void)?

    @Environment(\.monarchTheme) private var theme
    @State private var pending: Pending?
    @State private var busy: Pending?
    @State private var failure: String?

    private typealias Tokens = MonarchSessionListTokens
    private typealias Strings = MonarchSessionListStrings

    /// What a confirmation or a running sign-out is about.
    private enum Pending: Hashable {
        case one(MonarchSession)
        case others
    }

    /// A session list.
    ///
    /// - Parameters:
    ///   - sessions: The person's sessions, the current one included, in the order to show them.
    ///   - title: Replaces the built-in title "Where you’re signed in".
    ///   - description: Replaces the built-in line under the title.
    ///   - state: Whether the sessions are loaded, loading or failed to load.
    ///   - isRetrying: Whether a retry is in progress, in the failed state.
    ///   - confirmsSignOut: Asks before signing out. Set `false` when the handlers open the app's own confirmation.
    ///   - now: The reference time for relative times. Without it they follow the clock, updated every minute.
    ///   - footnote: A note in the card's footer, such as how long open apps stay signed in.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Retries loading. Shows "Try again" in the failed state.
    ///   - onSignOut: Signs one session out, given its id. Shows Sign out on every row. Throw to show the failure.
    ///   - onSignOutOthers: Signs out every session except the current one, given their ids. Shows Sign out
    ///     other devices in the header. Throw to show the failure.
    public init(
        _ sessions: [MonarchSession],
        title: String? = nil,
        description: String? = nil,
        state: MonarchSessionListState = .ready,
        isRetrying: Bool = false,
        confirmsSignOut: Bool = true,
        now: Date? = nil,
        footnote: String? = nil,
        labels: MonarchSessionListLabels = MonarchSessionListLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onSignOut: (@MainActor (String) async throws -> Void)? = nil,
        onSignOutOthers: (@MainActor ([String]) async throws -> Void)? = nil
    ) {
        self.sessions = sessions
        self.title = title
        self.description = description
        self.state = state
        self.isRetrying = isRetrying
        self.confirmsSignOut = confirmsSignOut
        self.now = now
        self.footnote = footnote
        self.labels = labels
        self.onRetry = onRetry
        self.onSignOut = onSignOut
        self.onSignOutOthers = onSignOutOthers
    }

    /// The content of the view.
    public var body: some View {
        let phase = Tokens.phase(state, sessions: sessions)
        Group {
            if let footnote, !footnote.isEmpty {
                MonarchSectionCard(
                    resolvedTitle,
                    description: resolvedDescription,
                    state: Tokens.cardState(phase),
                    isRetrying: isRetrying,
                    labels: cardLabels,
                    onRetry: onRetry,
                    content: { cardContent },
                    actions: { headerActions(phase) },
                    footer: {
                        Text(footnote)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors.textMuted)
                            .multilineTextAlignment(.trailing)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                )
            } else {
                MonarchSectionCard(
                    resolvedTitle,
                    description: resolvedDescription,
                    state: Tokens.cardState(phase),
                    isRetrying: isRetrying,
                    labels: cardLabels,
                    onRetry: onRetry,
                    content: { cardContent },
                    actions: { headerActions(phase) }
                )
            }
        }
        .alert(
            pending.map(confirmTitle) ?? "",
            isPresented: Binding(
                get: { pending != nil },
                set: { if !$0 { pending = nil } }
            ),
            presenting: pending,
            actions: { target in
                Button(confirmButton(target), role: .destructive) { run(target) }
                Button(labels.cancel ?? Strings.cancel, role: .cancel) {}
            },
            message: { target in Text(confirmMessage(target)) }
        )
    }

    private var resolvedTitle: String { title ?? labels.title ?? Strings.title }
    private var resolvedDescription: String { description ?? labels.description ?? Strings.description }

    private var cardLabels: MonarchSectionCardLabels {
        MonarchSectionCardLabels(
            loading: labels.loading ?? Strings.loading,
            emptyTitle: labels.emptyTitle ?? Strings.emptyTitle,
            errorTitle: labels.errorTitle ?? Strings.errorTitle
        )
    }

    @ViewBuilder private var cardContent: some View {
        if let failure {
            MonarchInlineNotice(failure, tone: .danger, variant: .plain)
        }
        list
    }

    @ViewBuilder private func headerActions(_ phase: MonarchSessionListTokens.Phase) -> some View {
        if Tokens.showsSignOutOthers(sessions, phase: phase, hasHandler: onSignOutOthers != nil) {
            MonarchButton(
                labels.signOutOthers ?? Strings.signOutOthers,
                size: .sm,
                isLoading: busy == .others
            ) {
                start(.others)
            }
            .disabled(busy != nil && busy != .others)
        }
    }

    private var list: some View {
        VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            ForEach(Array(sessions.enumerated()), id: \.element.id) { index, session in
                row(session)
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.listLabel ?? Strings.listLabel)
    }

    private func row(_ session: MonarchSession) -> some View {
        let shape = RoundedRectangle(cornerRadius: MonarchTokens.Radius.lg, style: .continuous)
        return rowLayout(session)
            .padding(MonarchTokens.Space.space3)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(shape.fill(theme.colors.bgSubtle))
            .overlay(shape.strokeBorder(theme.colors.borderSubtle, lineWidth: 1))
    }

    private func rowLayout(_ session: MonarchSession) -> some View {
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .center, spacing: Tokens.iconSpacing) {
                summary(session)
                signOutButton(session)
            }
            VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                summary(session)
                signOutButton(session)
            }
        }
    }

    @ViewBuilder private func summary(_ session: MonarchSession) -> some View {
        if let now {
            MonarchSessionRow(session: session, now: now, labels: labels)
        } else {
            MonarchRelativeTimeClock(dates: [session.signedIn, session.lastActive]) { current in
                MonarchSessionRow(session: session, now: current, labels: labels)
            }
        }
    }

    @ViewBuilder private func signOutButton(_ session: MonarchSession) -> some View {
        if onSignOut != nil {
            MonarchButton(
                labels.signOut ?? Strings.signOut,
                variant: .ghost,
                size: .sm,
                isLoading: busy == .one(session)
            ) {
                start(.one(session))
            }
            .disabled(busy != nil && busy != .one(session))
            .accessibilityLabel(labels.signOutLabel?(session.label) ?? Strings.signOutLabel(session.label))
        }
    }

    private var othersCount: Int { Tokens.otherIDs(sessions).count }

    private func confirmTitle(_ target: Pending) -> String {
        switch target {
        case .one(let session):
            labels.confirmTitle?(session.label, session.isCurrent)
                ?? Strings.confirmTitle(session.label, isCurrent: session.isCurrent)
        case .others:
            labels.othersTitle ?? Strings.othersTitle
        }
    }

    private func confirmMessage(_ target: Pending) -> String {
        switch target {
        case .one(let session):
            labels.confirmMessage?(session.isCurrent) ?? Strings.confirmMessage(isCurrent: session.isCurrent)
        case .others:
            labels.othersMessage?(othersCount) ?? Strings.othersMessage(othersCount)
        }
    }

    private func confirmButton(_ target: Pending) -> String {
        switch target {
        case .one: labels.signOut ?? Strings.signOut
        case .others: labels.signOutOthers ?? Strings.signOutOthers
        }
    }

    private func start(_ target: Pending) {
        guard busy == nil else { return }
        if confirmsSignOut {
            pending = target
        } else {
            run(target)
        }
    }

    private func run(_ target: Pending) {
        guard busy == nil else { return }
        failure = nil
        busy = target
        let ids = Tokens.otherIDs(sessions)
        Task { @MainActor in
            do {
                let message: String
                switch target {
                case .one(let session):
                    try await onSignOut?(session.id)
                    message = labels.signedOut?(session.label) ?? Strings.signedOut(session.label)
                case .others:
                    try await onSignOutOthers?(ids)
                    message = labels.signedOutOthers?(ids.count) ?? Strings.signedOutOthers(ids.count)
                }
                AccessibilityNotification.Announcement(message).post()
            } catch {
                let message: String
                switch target {
                case .one(let session):
                    message = labels.signOutFailed?(session.label) ?? Strings.signOutFailed(session.label)
                case .others:
                    message = labels.signOutOthersFailed ?? Strings.signOutOthersFailed
                }
                failure = message
                AccessibilityNotification.Announcement(message).post()
            }
            busy = nil
        }
    }
}

/// One session: the icon, the label and badge, the detail and the times.
private struct MonarchSessionRow: View {
    let session: MonarchSession
    let now: Date
    let labels: MonarchSessionListLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    var body: some View {
        let current = session.isCurrent ? labels.current ?? MonarchSessionListStrings.current : nil
        let detail = session.detail.flatMap { $0.isEmpty ? nil : $0 }
        let times = MonarchSessionListTokens.times(
            session,
            now: now,
            locale: locale,
            signedIn: labels.signedIn ?? MonarchSessionListStrings.signedIn,
            lastActive: labels.lastActive ?? MonarchSessionListStrings.lastActive
        )
        HStack(alignment: .center, spacing: MonarchSessionListTokens.iconSpacing) {
            MonarchIcon(systemName: MonarchSessionListTokens.symbol(session.kind))
                .foregroundStyle(theme.colors.textMuted)
                .frame(width: 32, height: 32)
                .background(
                    RoundedRectangle(cornerRadius: MonarchTokens.Radius.md, style: .continuous)
                        .fill(theme.colors.bgMuted)
                )
            VStack(alignment: .leading, spacing: MonarchSessionListTokens.lineSpacing) {
                MonarchFlowLayout(spacing: MonarchSessionListTokens.metaSpacing) {
                    Text(session.label)
                        .monarchTextStyle(.bodyStrong)
                        .foregroundStyle(theme.colors.textDefault)
                    if let current {
                        MonarchBadge(current, tone: .success)
                    }
                }
                if let detail {
                    Text(detail)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                if !times.isEmpty {
                    Text(times.joined(separator: "  ·  "))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(session.label)
        .accessibilityValue(MonarchSessionListTokens.accessibilityValue(current: current, detail: detail, times: times))
    }
}

#if DEBUG
    #Preview("MonarchSessionList") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        MonarchPreviewMatrix {
            VStack {
                MonarchSessionList(
                    [
                        MonarchSession(
                            id: "1",
                            label: "Safari on iPhone",
                            kind: .mobile,
                            detail: "Berlin, Germany",
                            signedIn: now.addingTimeInterval(-86_400 * 12),
                            isCurrent: true
                        ),
                        MonarchSession(
                            id: "2",
                            label: "Chrome on Windows",
                            detail: "203.0.113.7",
                            signedIn: now.addingTimeInterval(-86_400 * 40),
                            lastActive: now.addingTimeInterval(-3_600)
                        ),
                    ],
                    now: now,
                    footnote: "Open apps stay signed in for up to an hour.",
                    onSignOut: { _ in },
                    onSignOutOthers: { _ in }
                )
                MonarchSessionList([], state: .loading)
                MonarchSessionList([])
                MonarchSessionList([], state: .failed, onRetry: {})
            }
        }
    }
#endif
