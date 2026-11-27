import MonarchTokens
import SwiftUI

/// The apps a person signed in to with their account: logo, name, domain,
/// verified or suspended badges, when it was connected and last used, the
/// workspace and what it can access, with "Remove access" behind a
/// confirmation.
///
/// ```swift
/// MonarchConnectedAppsList(
///     apps,
///     state: isLoading ? .loading : .ready,
///     onRevoke: { app in
///         try await api.revokeAccess(app.id)
///         apps.removeAll { $0.id == app.id }
///     }
/// )
/// ```
///
/// The app loads the list and removes access in `onRevoke`. While it runs,
/// the row's button shows progress; if it throws, the error's description is
/// shown under the row and announced.
///
/// Accessibility: the header title is a header. Each app is one element
/// labelled with its name, whose value is its badges, domain, times and
/// workspace; its scopes are a group named "What Notes can access". The
/// remove button is named "Remove access for Notes". Removal and failures are
/// announced. Badges are text, not colour alone. Loading rows are hidden and
/// replaced by one "Loading connected apps" element.
public struct MonarchConnectedAppsList: View {
    private let apps: [MonarchConnectedApp]
    private let state: MonarchConnectedAppsListState
    private let title: String?
    private let description: String?
    private let isRetrying: Bool
    private let confirmsRevoke: Bool
    private let now: Date?
    private let labels: MonarchConnectedAppsListLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onRevoke: (@MainActor (MonarchConnectedApp) async throws -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @State private var pending: MonarchConnectedApp?
    @State private var revoking: Set<String> = []
    @State private var failures: [String: String] = [:]

    /// A connected apps list.
    ///
    /// - Parameters:
    ///   - apps: The connected apps.
    ///   - state: Whether the apps are loaded, loading or failed to load.
    ///   - title: Replaces the title "Connected apps". An empty string hides the header.
    ///   - description: Replaces the line under the title.
    ///   - isRetrying: Whether a retry is in flight, in the error state.
    ///   - confirmsRevoke: Whether to ask before removing access. Turn it off when the app asks itself.
    ///   - now: The reference time for relative times. The current time by default.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Retries loading. Shows a retry button in the error state.
    ///   - onRevoke: Removes an app's access. Shows "Remove access" on every row.
    public init(
        _ apps: [MonarchConnectedApp],
        state: MonarchConnectedAppsListState = .ready,
        title: String? = nil,
        description: String? = nil,
        isRetrying: Bool = false,
        confirmsRevoke: Bool = true,
        now: Date? = nil,
        labels: MonarchConnectedAppsListLabels = MonarchConnectedAppsListLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onRevoke: (@MainActor (MonarchConnectedApp) async throws -> Void)? = nil
    ) {
        self.apps = apps
        self.state = state
        self.title = title
        self.description = description
        self.isRetrying = isRetrying
        self.confirmsRevoke = confirmsRevoke
        self.now = now
        self.labels = labels
        self.onRetry = onRetry
        self.onRevoke = onRevoke
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchConnectedAppsListTokens.sectionSpacing) {
            let shownTitle = title ?? MonarchConnectedAppsListStrings.title
            if !shownTitle.isEmpty {
                MonarchSectionHeader(
                    shownTitle,
                    description: description ?? MonarchConnectedAppsListStrings.description
                )
            }
            switch MonarchConnectedAppsListTokens.content(state: state, count: apps.count) {
            case .loading:
                loadingRows
            case .error:
                MonarchErrorState(
                    labels.errorTitle ?? MonarchConnectedAppsListStrings.errorTitle,
                    size: .sm,
                    isRetrying: isRetrying,
                    onRetry: onRetry
                )
            case .empty:
                MonarchEmptyState(
                    labels.emptyTitle ?? MonarchConnectedAppsListStrings.emptyTitle,
                    message: labels.emptyMessage ?? MonarchConnectedAppsListStrings.emptyMessage,
                    systemImage: MonarchConnectedAppsListTokens.emptySymbol,
                    size: .sm
                )
            case .rows:
                list(apps)
            }
        }
        .alert(
            pending.map { labels.confirmTitle?($0.name) ?? MonarchConnectedAppsListStrings.confirmTitle($0.name) }
                ?? "",
            isPresented: Binding(get: { pending != nil }, set: { if !$0 { pending = nil } }),
            presenting: pending
        ) { app in
            Button(labels.revoke ?? MonarchConnectedAppsListStrings.revoke, role: .destructive) {
                revoke(app)
            }
            Button(labels.cancel ?? MonarchConnectedAppsListStrings.cancel, role: .cancel) {}
        } message: { _ in
            Text(labels.confirmMessage ?? MonarchConnectedAppsListStrings.confirmMessage)
        }
    }

    private func list(_ apps: [MonarchConnectedApp]) -> some View {
        let shape = RoundedRectangle(cornerRadius: MonarchConnectedAppsListTokens.radius, style: .continuous)
        return VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(apps.enumerated()), id: \.element.id) { index, app in
                if index > 0 { Divider().overlay(theme.colors.borderSubtle) }
                row(app)
            }
        }
        .background(shape.fill(theme.colors.bgSurface))
        .overlay(shape.strokeBorder(theme.colors.borderSubtle, lineWidth: MonarchShapes.borderWidth))
        .clipShape(shape)
    }

    private func row(_ app: MonarchConnectedApp) -> some View {
        let badges = MonarchConnectedAppsListTokens.badges(app, labels: labels)
        let meta = MonarchConnectedAppsListTokens.metaParts(app, now: now ?? Date(), locale: locale, labels: labels)
        return VStack(alignment: .leading, spacing: MonarchConnectedAppsListTokens.logoSpacing) {
            HStack(alignment: .top, spacing: MonarchConnectedAppsListTokens.logoSpacing) {
                MonarchFavicon(url: app.logoURL, name: app.name, size: .lg)
                VStack(alignment: .leading, spacing: MonarchConnectedAppsListTokens.lineSpacing) {
                    MonarchFlowLayout(spacing: MonarchConnectedAppsListTokens.badgeSpacing) {
                        Text(app.name)
                            .monarchTextStyle(.bodyStrong)
                            .foregroundStyle(
                                theme.colors[
                                    role: MonarchConnectedAppsListTokens.nameRole(isSuspended: app.isSuspended)
                                ]
                            )
                        if app.isVerified {
                            MonarchBadge(
                                labels.verified ?? MonarchConnectedAppsListStrings.verified,
                                tone: .success,
                                systemImage: MonarchConnectedAppsListTokens.verifiedSymbol
                            )
                        }
                        if app.isSuspended {
                            MonarchBadge(labels.suspended ?? MonarchConnectedAppsListStrings.suspended, tone: .warning)
                        }
                    }
                    if let domain = app.domain, !domain.isEmpty {
                        Text(domain)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors.textMuted)
                    }
                    if !meta.isEmpty {
                        Text(meta.joined(separator: MonarchConnectedAppsListTokens.metaSeparator))
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors.textMuted)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(app.name)
                .accessibilityValue(
                    MonarchConnectedAppsListTokens.accessibilityValue(
                        badges: badges,
                        domain: app.domain,
                        metaParts: meta
                    )
                )
            }
            if !app.scopes.isEmpty {
                scopes(app)
            }
            if let failure = failures[app.id] {
                HStack(alignment: .firstTextBaseline, spacing: MonarchConnectedAppsListTokens.scopeSpacing) {
                    MonarchIcon(systemName: MonarchConnectedAppsListTokens.errorSymbol, size: .sm)
                    Text(failure).monarchTextStyle(.caption)
                }
                .foregroundStyle(theme.colors.dangerStrong)
                .accessibilityElement(children: .combine)
            }
            if onRevoke != nil {
                MonarchButton(
                    labels.revoke ?? MonarchConnectedAppsListStrings.revoke,
                    variant: .dangerSubtle,
                    size: .sm,
                    isLoading: revoking.contains(app.id)
                ) {
                    start(app)
                }
                .accessibilityLabel(
                    labels.revokeLabel?(app.name) ?? MonarchConnectedAppsListStrings.revokeLabel(app.name)
                )
            }
        }
        .padding(MonarchConnectedAppsListTokens.rowPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func scopes(_ app: MonarchConnectedApp) -> some View {
        VStack(alignment: .leading, spacing: MonarchConnectedAppsListTokens.lineSpacing) {
            ForEach(Array(app.scopes.enumerated()), id: \.offset) { _, scope in
                HStack(alignment: .firstTextBaseline, spacing: MonarchConnectedAppsListTokens.scopeSpacing) {
                    // A dot before each permission, as the web draws them.
                    Circle()
                        .fill(theme.colors.textMuted)
                        .frame(width: MonarchTokens.Space.space1_5, height: MonarchTokens.Space.space1_5)
                        .accessibilityHidden(true)
                    Text(scope)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textDefault)
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.scopes?(app.name) ?? MonarchConnectedAppsListStrings.scopes(app.name))
    }

    private var loadingRows: some View {
        let placeholder = MonarchConnectedApp(
            id: "placeholder",
            name: "Connected app name",
            domain: "app.example.com",
            scopes: ["See your email address"]
        )
        return VStack(spacing: 0) {
            ForEach(0..<MonarchConnectedAppsListTokens.loadingRowCount, id: \.self) { _ in
                row(placeholder)
            }
        }
        .redacted(reason: .placeholder)
        .allowsHitTesting(false)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(labels.loading ?? MonarchConnectedAppsListStrings.loading)
    }

    private func start(_ app: MonarchConnectedApp) {
        failures[app.id] = nil
        if confirmsRevoke {
            pending = app
        } else {
            revoke(app)
        }
    }

    private func revoke(_ app: MonarchConnectedApp) {
        guard let onRevoke, !revoking.contains(app.id) else { return }
        revoking.insert(app.id)
        Task {
            do {
                try await onRevoke(app)
                AccessibilityNotification.Announcement(
                    labels.revoked?(app.name) ?? MonarchConnectedAppsListStrings.revoked(app.name)
                ).post()
            } catch {
                failures[app.id] = error.localizedDescription
                AccessibilityNotification.Announcement(error.localizedDescription).post()
            }
            revoking.remove(app.id)
        }
    }
}

#if DEBUG
    #Preview("MonarchConnectedAppsList") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchConnectedAppsList(
                    [
                        MonarchConnectedApp(
                            id: "notes",
                            name: "Notes",
                            domain: "notes.example.com",
                            isVerified: true,
                            scopes: ["See your email address", "See your name"],
                            connectedAt: Date(timeIntervalSinceNow: -86_400 * 3)
                        ),
                        MonarchConnectedApp(id: "old", name: "Old tool", isSuspended: true),
                    ],
                    onRevoke: { _ in }
                )
                MonarchConnectedAppsList([], state: .loading)
            }
        }
    }
#endif
