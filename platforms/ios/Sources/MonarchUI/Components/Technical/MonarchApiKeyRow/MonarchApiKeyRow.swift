import SwiftUI

/// One API key in a list: its name and status, the visible start of the key,
/// when it was created, last used and expires, its scopes, and Rotate and
/// Revoke actions that ask for confirmation first.
///
/// ```swift
/// List(keys) { key in
///     MonarchApiKeyRow(
///         key.name,
///         prefix: key.prefix,
///         createdAt: key.createdAt,
///         lastUsedAt: key.lastUsedAt,
///         scopes: key.scopes,
///         onRotate: { try await api.rotate(key) },
///         onRevoke: { try await api.revoke(key) }
///     )
/// }
/// ```
///
/// The secret itself is never shown: only the prefix, followed by a mask.
/// The actions are in a menu at the trailing edge, as list rows do on iOS.
/// Rotate asks in a confirmation dialog and Revoke in an alert, which can
/// ask for a phrase to be typed first. While the app's action runs the row
/// shows its progress, and a failure is shown in the row with a retry.
/// Show the new secret after a rotation with the app's own sheet.
///
/// Relative times follow the clock, updated every minute, unless `now` is
/// given.
///
/// Accessibility: the name and status are read together; the prefix is read
/// as "Key starting with sk_live_4f2a"; the times as one line; the scopes as
/// "Scopes" and every scope, including those behind "+2". The menu is named
/// "Actions for Reporting job" and has a 44-point touch target. Progress
/// and failures are announced. The status has a name, so it does not rely on
/// colour.
public struct MonarchApiKeyRow<Actions: View>: View {
    private let name: String
    private let prefix: String
    private let createdAt: Date
    private let lastUsedAt: Date?
    private let expiresAt: Date?
    private let scopes: [String]
    private let maxScopes: Int
    private let status: MonarchApiKeyStatus
    private let now: Date?
    private let revokeConfirmPhrase: String?
    private let labels: MonarchApiKeyRowLabels
    private let onRotate: (@MainActor () async throws -> Void)?
    private let onRevoke: (@MainActor () async throws -> Void)?
    private let actions: Actions
    private let hasActions: Bool

    @State private var operation = MonarchApiKeyRowTokens.OperationState()
    @State private var isConfirmingRotate = false
    @State private var isConfirmingRevoke = false
    @State private var typedPhrase = ""
    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// An API key row with more actions in its menu.
    ///
    /// - Parameters:
    ///   - name: The key's name, such as "Reporting job".
    ///   - prefix: The visible, non-secret start of the key, such as `sk_live_4f2a`.
    ///   - createdAt: When the key was created.
    ///   - lastUsedAt: When the key was last used. Without it the row says "Never used".
    ///   - expiresAt: When the key expires, if it does.
    ///   - scopes: Scope names, such as `["items:read", "items:write"]`.
    ///   - maxScopes: How many scopes are shown before "+N".
    ///   - status: Active, expiring, expired or revoked. Expired and revoked keys have no actions.
    ///   - now: The reference time for relative times. Without it they follow the clock.
    ///   - revokeConfirmPhrase: A phrase, usually the name, to type before revoking.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRotate: Rotates the key after confirmation. Without it there is no Rotate.
    ///   - onRevoke: Revokes the key after confirmation. Without it there is no Revoke.
    ///   - actions: More menu items after Rotate and Revoke, such as Edit.
    public init(
        _ name: String,
        prefix: String,
        createdAt: Date,
        lastUsedAt: Date? = nil,
        expiresAt: Date? = nil,
        scopes: [String] = [],
        maxScopes: Int = 3,
        status: MonarchApiKeyStatus = .active,
        now: Date? = nil,
        revokeConfirmPhrase: String? = nil,
        labels: MonarchApiKeyRowLabels = MonarchApiKeyRowLabels(),
        onRotate: (@MainActor () async throws -> Void)? = nil,
        onRevoke: (@MainActor () async throws -> Void)? = nil,
        @ViewBuilder actions: () -> Actions
    ) {
        self.name = name
        self.prefix = prefix
        self.createdAt = createdAt
        self.lastUsedAt = lastUsedAt
        self.expiresAt = expiresAt
        self.scopes = scopes
        self.maxScopes = maxScopes
        self.status = status
        self.now = now
        self.revokeConfirmPhrase = revokeConfirmPhrase
        self.labels = labels
        self.onRotate = onRotate
        self.onRevoke = onRevoke
        self.actions = actions()
        self.hasActions = Actions.self != EmptyView.self
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchApiKeyRowTokens.lineSpacing) {
            titleLine
            Text(prefix + MonarchApiKeyRowTokens.mask)
                .monarchTextStyle(.code)
                .foregroundStyle(theme.colors.textMuted)
                .lineLimit(1)
                .truncationMode(.middle)
                .accessibilityLabel(MonarchApiKeyRowTokens.prefixAccessibility(prefix, labels: labels))
            metaLine
            scopeLine
            operationLine
            actionRow
        }
        .padding(.vertical, MonarchApiKeyRowTokens.verticalPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .confirmationDialog(
            MonarchApiKeyRowTokens.confirmTitle(.rotate, name: name, labels: labels),
            isPresented: $isConfirmingRotate,
            titleVisibility: .visible,
            actions: {
                Button(MonarchApiKeyRowTokens.confirmButton(.rotate, labels: labels)) { run(.rotate) }
                Button(MonarchApiKeyRowTokens.cancel(labels: labels), role: .cancel) {}
            },
            message: {
                Text(MonarchApiKeyRowTokens.confirmMessage(.rotate, labels: labels))
            }
        )
        .alert(
            MonarchApiKeyRowTokens.confirmTitle(.revoke, name: name, labels: labels),
            isPresented: $isConfirmingRevoke,
            actions: {
                if let revokeConfirmPhrase {
                    TextField(
                        MonarchApiKeyRowTokens.phrasePrompt(revokeConfirmPhrase, labels: labels),
                        text: $typedPhrase
                    )
                }
                Button(MonarchApiKeyRowTokens.confirmButton(.revoke, labels: labels), role: .destructive) {
                    if MonarchApiKeyRowTokens.canConfirmRevoke(typed: typedPhrase, phrase: revokeConfirmPhrase) {
                        run(.revoke)
                    }
                }
                .disabled(!MonarchApiKeyRowTokens.canConfirmRevoke(typed: typedPhrase, phrase: revokeConfirmPhrase))
                Button(MonarchApiKeyRowTokens.cancel(labels: labels), role: .cancel) {}
            },
            message: {
                Text(MonarchApiKeyRowTokens.confirmMessage(.revoke, labels: labels))
            }
        )
    }

    private var titleLine: some View {
        HStack(alignment: .center, spacing: MonarchApiKeyRowTokens.inlineSpacing) {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: MonarchApiKeyRowTokens.inlineSpacing) { nameAndStatus }
                VStack(alignment: .leading, spacing: MonarchApiKeyRowTokens.inlineSpacing) { nameAndStatus }
            }
            .accessibilityElement(children: .combine)
            .frame(maxWidth: .infinity, alignment: .leading)
            // Rotate and Revoke are buttons under the key, as on the web; the menu keeps the app's own actions.
            if MonarchApiKeyRowTokens.showsMenu(
                status: status,
                canRotate: false,
                canRevoke: false,
                hasActions: hasActions
            ) {
                menu
            }
        }
    }

    @ViewBuilder
    private var nameAndStatus: some View {
        Text(name)
            .monarchTextStyle(.bodyStrong)
            .foregroundStyle(theme.colors.textDefault)
            .fixedSize(horizontal: false, vertical: true)
        MonarchStatusBadge(MonarchApiKeyRowTokens.statusDefinition(status, labels: labels), size: .sm)
    }

    private var menu: some View {
        Menu {
            actions
        } label: {
            Label(MonarchApiKeyRowTokens.actionsLabel(name: name, labels: labels), systemImage: "ellipsis")
                .labelStyle(.iconOnly)
        }
        .menuStyle(.button)
        .buttonStyle(.monarch(.ghost, size: .sm))
        .fixedSize()
        .disabled(operation.running != nil)
        .accessibilityLabel(MonarchApiKeyRowTokens.actionsLabel(name: name, labels: labels))
    }

    /// Rotate and Revoke, which the web shows as buttons at the end of the row.
    @ViewBuilder
    private var actionRow: some View {
        if MonarchApiKeyRowTokens.isLive(status), onRotate != nil || onRevoke != nil {
            HStack(spacing: MonarchApiKeyRowTokens.inlineSpacing) {
                Spacer(minLength: 0)
                if onRotate != nil {
                    MonarchButton(
                        MonarchApiKeyRowTokens.menuTitle(.rotate, labels: labels),
                        variant: .secondary,
                        size: .sm
                    ) {
                        isConfirmingRotate = true
                    }
                }
                if onRevoke != nil {
                    MonarchButton(
                        MonarchApiKeyRowTokens.menuTitle(.revoke, labels: labels),
                        variant: .dangerSubtle,
                        size: .sm
                    ) {
                        typedPhrase = ""
                        isConfirmingRevoke = true
                    }
                }
            }
            .disabled(operation.running != nil)
        }
    }

    @ViewBuilder
    private var metaLine: some View {
        if let now {
            metaText(now: now)
        } else {
            MonarchRelativeTimeClock(dates: [createdAt, lastUsedAt, expiresAt]) { current in
                metaText(now: current)
            }
        }
    }

    private func metaText(now: Date) -> some View {
        Text(
            MonarchApiKeyRowTokens.meta(
                createdAt: createdAt,
                lastUsedAt: lastUsedAt,
                expiresAt: expiresAt,
                now: now,
                locale: locale,
                labels: labels
            )
            .joined(separator: MonarchApiKeyRowTokens.metaSeparator)
        )
        .monarchTextStyle(.caption)
        .foregroundStyle(theme.colors.textMuted)
        .fixedSize(horizontal: false, vertical: true)
    }

    @ViewBuilder
    private var scopeLine: some View {
        let spoken = MonarchApiKeyRowTokens.scopesAccessibility(scopes, labels: labels)
        if scopes.isEmpty {
            Text(spoken.value)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
        } else {
            let split = MonarchApiKeyRowTokens.scopes(scopes, max: maxScopes)
            MonarchFlowLayout(spacing: MonarchApiKeyRowTokens.inlineSpacing) {
                ForEach(split.shown, id: \.self) { scope in
                    MonarchBadge(scope, variant: .outline)
                }
                if !split.hidden.isEmpty {
                    MonarchBadge(MonarchApiKeyRowStrings.moreScopes(split.hidden.count), variant: .outline)
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(spoken.label)
            .accessibilityValue(spoken.value)
        }
    }

    @ViewBuilder
    private var operationLine: some View {
        if let running = operation.running {
            HStack(spacing: MonarchApiKeyRowTokens.inlineSpacing) {
                ProgressView()
                    .controlSize(.small)
                Text(MonarchApiKeyRowTokens.progress(running, labels: labels))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            .accessibilityElement(children: .combine)
        } else if let failed = operation.failed, let message = operation.message {
            MonarchInlineErrorRetry(message) {
                run(failed)
            }
        }
    }

    private func run(_ kind: MonarchApiKeyRowTokens.Operation) {
        let handler = kind == .rotate ? onRotate : onRevoke
        guard let handler, operation.begin(kind) else { return }
        AccessibilityNotification.Announcement(MonarchApiKeyRowTokens.progress(kind, labels: labels)).post()
        Task { @MainActor in
            do {
                try await handler()
                operation.succeed()
            } catch {
                let message = MonarchApiKeyRowTokens.failureMessage(kind, error: error, labels: labels)
                operation.fail(message: message)
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }
}

extension MonarchApiKeyRow where Actions == EmptyView {
    /// An API key row.
    ///
    /// - Parameters:
    ///   - name: The key's name, such as "Reporting job".
    ///   - prefix: The visible, non-secret start of the key, such as `sk_live_4f2a`.
    ///   - createdAt: When the key was created.
    ///   - lastUsedAt: When the key was last used. Without it the row says "Never used".
    ///   - expiresAt: When the key expires, if it does.
    ///   - scopes: Scope names, such as `["items:read", "items:write"]`.
    ///   - maxScopes: How many scopes are shown before "+N".
    ///   - status: Active, expiring, expired or revoked. Expired and revoked keys have no actions.
    ///   - now: The reference time for relative times. Without it they follow the clock.
    ///   - revokeConfirmPhrase: A phrase, usually the name, to type before revoking.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRotate: Rotates the key after confirmation. Without it there is no Rotate.
    ///   - onRevoke: Revokes the key after confirmation. Without it there is no Revoke.
    public init(
        _ name: String,
        prefix: String,
        createdAt: Date,
        lastUsedAt: Date? = nil,
        expiresAt: Date? = nil,
        scopes: [String] = [],
        maxScopes: Int = 3,
        status: MonarchApiKeyStatus = .active,
        now: Date? = nil,
        revokeConfirmPhrase: String? = nil,
        labels: MonarchApiKeyRowLabels = MonarchApiKeyRowLabels(),
        onRotate: (@MainActor () async throws -> Void)? = nil,
        onRevoke: (@MainActor () async throws -> Void)? = nil
    ) {
        self.init(
            name,
            prefix: prefix,
            createdAt: createdAt,
            lastUsedAt: lastUsedAt,
            expiresAt: expiresAt,
            scopes: scopes,
            maxScopes: maxScopes,
            status: status,
            now: now,
            revokeConfirmPhrase: revokeConfirmPhrase,
            labels: labels,
            onRotate: onRotate,
            onRevoke: onRevoke,
            actions: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchApiKeyRow") {
        let now = Date(timeIntervalSince1970: 1_794_666_600)
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchApiKeyRow(
                    "Reporting job",
                    prefix: "sk_live_4f2a",
                    createdAt: now.addingTimeInterval(-90 * 86_400),
                    lastUsedAt: now.addingTimeInterval(-7_200),
                    scopes: ["items:read", "items:write", "reports:read", "users:read", "billing:read"],
                    now: now,
                    onRotate: {},
                    onRevoke: {}
                )
                MonarchApiKeyRow("Old CI key", prefix: "sk_test_91bc", createdAt: now, status: .revoked, now: now)
            }
        }
    }
#endif
