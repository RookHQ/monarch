import MonarchTokens
import SwiftUI

/// Recent security alerts for an account: new sign-ins, password changes,
/// blocked attempts. Each has a severity badge, a time, an unread mark and
/// optional follow-up, mark-as-read and dismiss actions.
///
/// ```swift
/// MonarchSecurityAlertCard(
///     alerts,
///     onMarkRead: { id in store.markRead(id) },
///     onMarkAllRead: { store.markAllRead() },
///     onDismiss: { id in store.dismiss(id) }
/// )
/// ```
///
/// The card is a ``MonarchCard`` with a header and a list. While loading it
/// shows a progress indicator; an empty list shows a ``MonarchEmptyState``;
/// a failure shows a ``MonarchErrorState`` with a retry. Actions appear only
/// with their handler. A follow-up with an `http`, `https` or `mailto` link
/// opens it; any other link is ignored, because alerts are often server data.
///
/// Accessibility: the title is a header. Each alert is one element read as
/// its title, with "Unread", the severity, the time and the description as
/// its value, so severity and unread state never depend on colour or the
/// icon. Its actions are separate buttons with 44-point touch targets; the
/// dismiss button is named "Dismiss" and the alert title. After a dismissal
/// VoiceOver announces it and moves to the next alert. Times update every minute.
public struct MonarchSecurityAlertCard: View {
    private let alerts: [MonarchSecurityAlert]
    private let title: String?
    private let description: String?
    private let state: MonarchSecurityAlertCardState
    private let isRetrying: Bool
    private let now: Date?
    private let labels: MonarchSecurityAlertCardLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onAction: (@MainActor (MonarchSecurityAlert) -> Void)?
    private let onMarkRead: (@MainActor (String) -> Void)?
    private let onMarkAllRead: (@MainActor () -> Void)?
    private let onDismiss: (@MainActor (String) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @AccessibilityFocusState private var focusedAlert: String?
    @State private var pendingDismissal: PendingDismissal?

    /// A security alert card.
    ///
    /// - Parameters:
    ///   - alerts: The alerts, newest first.
    ///   - title: Replaces the built-in title "Security alerts".
    ///   - description: Replaces the built-in line under the title.
    ///   - state: Whether the alerts are loaded, loading or failed to load.
    ///   - isRetrying: Whether a retry is in progress, in the failed state.
    ///   - now: The reference time for relative times. Without it they follow the clock, updated every minute.
    ///   - labels: Replacements for the built-in strings.
    ///   - onAction: Runs the follow-up of an alert that has an action title and no safe link.
    ///   - onMarkRead: Marks one alert read. Shows "Mark as read" on unread alerts.
    ///   - onMarkAllRead: Marks every alert read. Shows "Mark all as read" while any are unread.
    ///   - onDismiss: Dismisses an alert. Shows a dismiss button on every alert.
    ///   - onRetry: Retries loading. Shows "Try again" in the failed state.
    public init(
        _ alerts: [MonarchSecurityAlert],
        title: String? = nil,
        description: String? = nil,
        state: MonarchSecurityAlertCardState = .ready,
        isRetrying: Bool = false,
        now: Date? = nil,
        labels: MonarchSecurityAlertCardLabels = MonarchSecurityAlertCardLabels(),
        onAction: (@MainActor (MonarchSecurityAlert) -> Void)? = nil,
        onMarkRead: (@MainActor (String) -> Void)? = nil,
        onMarkAllRead: (@MainActor () -> Void)? = nil,
        onDismiss: (@MainActor (String) -> Void)? = nil,
        onRetry: (@MainActor () -> Void)? = nil
    ) {
        self.alerts = alerts
        self.title = title
        self.description = description
        self.state = state
        self.isRetrying = isRetrying
        self.now = now
        self.labels = labels
        self.onRetry = onRetry
        self.onAction = onAction
        self.onMarkRead = onMarkRead
        self.onMarkAllRead = onMarkAllRead
        self.onDismiss = onDismiss
    }

    /// The content of the view.
    public var body: some View {
        let phase = MonarchSecurityAlertCardTokens.phase(state, alerts: alerts)
        let unread = MonarchSecurityAlertCardTokens.unreadCount(alerts)
        MonarchCard {
            VStack(alignment: .leading, spacing: MonarchSecurityAlertCardTokens.sectionSpacing) {
                header(unread: phase == .list ? unread : 0, showsMarkAll: phase == .list && unread > 0)
                content(phase)
            }
        }
        .onChange(of: alerts.map(\.id)) { _, ids in
            handleDismissal(remaining: ids)
        }
    }

    private func header(unread: Int, showsMarkAll: Bool) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchSecurityAlertCardTokens.headerSpacing) {
            MonarchIcon(systemName: MonarchSecurityAlertCardTokens.headerSymbol)
                .foregroundStyle(theme.colors.textMuted)
                .frame(width: 28, height: 28)
                .background(
                    RoundedRectangle(cornerRadius: MonarchTokens.Radius.md, style: .continuous)
                        .fill(theme.colors.bgSubtle)
                )
            VStack(alignment: .leading, spacing: MonarchSecurityAlertCardTokens.textSpacing) {
                Text(title ?? labels.title ?? MonarchSecurityAlertCardStrings.title)
                    .monarchTextStyle(
                        MonarchSectionHeaderTokens.titleStyle(.md),
                        weight: MonarchSectionHeaderTokens.titleWeight
                    )
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
                Text(description ?? labels.description ?? MonarchSecurityAlertCardStrings.description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                if unread > 0 {
                    // Plain text under the description, as on the web.
                    Text(labels.unreadCount?(unread) ?? MonarchSecurityAlertCardStrings.unreadCount(unread))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if showsMarkAll, let onMarkAllRead {
                MonarchButton(
                    labels.markAllRead ?? MonarchSecurityAlertCardStrings.markAllRead,
                    variant: .ghost,
                    size: .sm,
                    action: onMarkAllRead
                )
            }
        }
    }

    @ViewBuilder private func content(_ phase: MonarchSecurityAlertCardTokens.Phase) -> some View {
        switch phase {
        case .loading:
            HStack(spacing: MonarchSecurityAlertCardTokens.headerSpacing) {
                ProgressView()
                Text(labels.loading ?? MonarchSecurityAlertCardStrings.loading)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            }
            .accessibilityElement(children: .combine)
            .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget)
        case .empty:
            MonarchEmptyState(
                labels.emptyTitle ?? MonarchSecurityAlertCardStrings.emptyTitle,
                message: labels.emptyDescription ?? MonarchSecurityAlertCardStrings.emptyDescription,
                systemImage: MonarchSecurityAlertCardTokens.emptySymbol,
                size: .sm
            )
        case .failed:
            MonarchErrorState(
                labels.errorTitle ?? MonarchSecurityAlertCardStrings.errorTitle,
                size: .sm,
                isRetrying: isRetrying,
                onRetry: onRetry
            )
        case .list:
            list
        }
    }

    private var list: some View {
        VStack(alignment: .leading, spacing: MonarchSecurityAlertCardTokens.rowSpacing) {
            ForEach(Array(alerts.enumerated()), id: \.element.id) { index, alert in
                row(alert, index: index)
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.listLabel ?? MonarchSecurityAlertCardStrings.listLabel)
    }

    private func row(_ alert: MonarchSecurityAlert, index: Int) -> some View {
        var dismiss: (@MainActor () -> Void)?
        if onDismiss != nil {
            dismiss = { dismissAlert(alert, at: index) }
        }
        var markRead: (@MainActor () -> Void)?
        if let onMarkRead, !alert.isRead {
            markRead = { onMarkRead(alert.id) }
        }
        var action: (@MainActor () -> Void)?
        if let onAction {
            action = { onAction(alert) }
        }
        return MonarchSecurityAlertRow(
            alert: alert,
            now: now,
            labels: labels,
            followUp: MonarchSecurityAlertCardTokens.followUp(alert, hasHandler: onAction != nil),
            onAction: action,
            onMarkRead: markRead,
            onDismiss: dismiss,
            focus: $focusedAlert
        )
    }

    private func dismissAlert(_ alert: MonarchSecurityAlert, at index: Int) {
        pendingDismissal = PendingDismissal(id: alert.id, index: index, title: alert.title)
        onDismiss?(alert.id)
    }

    private func handleDismissal(remaining ids: [String]) {
        guard let pending = pendingDismissal, !ids.contains(pending.id) else { return }
        pendingDismissal = nil
        AccessibilityNotification.Announcement(
            labels.dismissed?(pending.title) ?? MonarchSecurityAlertCardStrings.dismissed(pending.title)
        ).post()
        focusedAlert = MonarchFocusAfterRemoval.target(removedIndex: pending.index, remaining: ids)
    }

    /// The alert being dismissed, until it leaves `alerts`.
    private struct PendingDismissal: Equatable {
        let id: String
        let index: Int
        let title: String
    }
}

/// One alert: the severity icon, the unread mark, the text, the badge and time, and the actions.
private struct MonarchSecurityAlertRow: View {
    let alert: MonarchSecurityAlert
    let now: Date?
    let labels: MonarchSecurityAlertCardLabels
    let followUp: MonarchSecurityAlertCardTokens.FollowUp?
    let onAction: (@MainActor () -> Void)?
    let onMarkRead: (@MainActor () -> Void)?
    let onDismiss: (@MainActor () -> Void)?
    let focus: AccessibilityFocusState<String?>.Binding

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @ScaledMetric(relativeTo: .body) private var dotSize = MonarchSecurityAlertCardTokens.unreadDotSize

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchTokens.Radius.lg, style: .continuous)
        HStack(alignment: .top, spacing: MonarchSecurityAlertCardTokens.iconSpacing) {
            MonarchIcon(systemName: MonarchSecurityAlertCardTokens.symbol(alert.severity))
                .foregroundStyle(theme.colors[role: MonarchSecurityAlertCardTokens.iconRole(alert.severity)])
                .frame(width: 32, height: 32)
                .background(
                    Circle().fill(theme.colors[role: MonarchSecurityAlertCardTokens.iconFill(alert.severity)])
                )
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: MonarchSecurityAlertCardTokens.rowContentSpacing) {
                if let now {
                    summary(now: now)
                } else {
                    MonarchRelativeTimeClock(dates: [alert.date]) { current in
                        summary(now: current)
                    }
                }
                actions
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if let onDismiss {
                Button(action: onDismiss) {
                    MonarchIcon(systemName: MonarchSecurityAlertCardTokens.dismissSymbol, size: .sm)
                        .foregroundStyle(theme.colors.textMuted)
                        .monarchMinimumHitTarget()
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(
                    labels.dismiss?(alert.title) ?? MonarchSecurityAlertCardStrings.dismiss(alert.title)
                )
            }
        }
        .padding(MonarchTokens.Space.space3)
        .background(shape.fill(theme.colors.bgSubtle))
        .overlay(
            shape.strokeBorder(
                theme.colors[
                    role: alert.severity == .critical ? MonarchTonePalette.palette(for: .danger).border : \.borderSubtle
                ],
                lineWidth: 1
            )
        )
    }

    private func summary(now: Date) -> some View {
        let severity = labels.severity?(alert.severity) ?? MonarchSecurityAlertCardStrings.severity(alert.severity)
        let time = alert.date.map { MonarchSecurityAlertCardTokens.relativeTime($0, now: now, locale: locale) }
        return VStack(alignment: .leading, spacing: MonarchSecurityAlertCardTokens.rowContentSpacing) {
            HStack(alignment: .firstTextBaseline, spacing: MonarchSecurityAlertCardTokens.metaSpacing) {
                if !alert.isRead {
                    Circle()
                        .fill(theme.colors[role: MonarchSecurityAlertCardTokens.unreadRole])
                        .frame(width: dotSize, height: dotSize)
                }
                Text(alert.title)
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors.textDefault)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            if let description = alert.description, !description.isEmpty {
                Text(description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            MonarchFlowLayout(spacing: MonarchSecurityAlertCardTokens.metaSpacing) {
                MonarchBadge(
                    severity,
                    tone: MonarchSecurityAlertCardTokens.tone(alert.severity),
                    variant: MonarchSecurityAlertCardTokens.badgeVariant(alert.severity)
                )
                if let time {
                    Text(time)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(alert.title)
        .accessibilityValue(
            MonarchSecurityAlertCardTokens.accessibilityValue(
                unread: alert.isRead ? nil : (labels.unread ?? MonarchSecurityAlertCardStrings.unread),
                severity: severity,
                time: time,
                description: alert.description
            )
        )
        .accessibilityFocused(focus, equals: alert.id)
    }

    @ViewBuilder private var actions: some View {
        if followUp != nil || onMarkRead != nil {
            MonarchFlowLayout(spacing: MonarchSecurityAlertCardTokens.actionSpacing) {
                switch followUp {
                case .some(.link(let title, let url)):
                    Link(title, destination: url)
                        .buttonStyle(.monarch(.secondary, size: .sm))
                case .some(.button(let title)):
                    MonarchButton(title, size: .sm) { onAction?() }
                case .none:
                    EmptyView()
                }
                if let onMarkRead {
                    MonarchButton(
                        labels.markRead ?? MonarchSecurityAlertCardStrings.markRead,
                        variant: .ghost,
                        size: .sm,
                        action: onMarkRead
                    )
                }
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchSecurityAlertCard") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        MonarchPreviewMatrix {
            VStack {
                MonarchSecurityAlertCard(
                    [
                        MonarchSecurityAlert(
                            id: "1",
                            title: "New sign-in from Firefox on Linux",
                            description: "Berlin, Germany",
                            severity: .medium,
                            date: now.addingTimeInterval(-300),
                            actionTitle: "Review sessions",
                            actionURL: URL(string: "https://example.com/sessions")
                        ),
                        MonarchSecurityAlert(
                            id: "2",
                            title: "Password changed",
                            severity: .info,
                            date: now.addingTimeInterval(-86_400),
                            isRead: true
                        ),
                    ],
                    now: now,
                    onMarkRead: { _ in },
                    onMarkAllRead: {},
                    onDismiss: { _ in }
                )
                MonarchSecurityAlertCard([], state: .loading)
                MonarchSecurityAlertCard([])
                MonarchSecurityAlertCard([], state: .failed, onRetry: {})
            }
        }
    }
#endif
