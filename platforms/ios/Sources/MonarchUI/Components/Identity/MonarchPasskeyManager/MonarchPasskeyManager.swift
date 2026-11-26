import MonarchTokens
import SwiftUI

/// A person's passkeys: when each was added and last used, a Synced badge,
/// rename, remove behind a confirmation, and Add a passkey.
///
/// ```swift
/// MonarchPasskeyManager(
///     passkeys,
///     onAdd: { try await account.registerPasskey() },
///     onRename: { id, name in try await account.renamePasskey(id, to: name) },
///     onRemove: { id in try await account.removePasskey(id) }
/// )
/// ```
///
/// Monarch manages the list only. Creating a passkey stays in the app:
/// `onAdd` runs `ASAuthorizationController` with the app's relying party and
/// the server round trip. When the person cancels the system sheet
/// (`ASAuthorizationError.canceled` or a `CancellationError`), the card shows
/// a neutral note; another error shows its `LocalizedError` description or a
/// built-in failure. Rename asks for the new name in a system alert with a
/// text field; Remove asks in a system alert with a destructive button, with
/// an extra warning when `isLastSignInMethod` and only one passkey is left.
///
/// Accessibility: the title is a header. Each passkey is one element read as
/// its name, then Synced, the detail and the times. Rename and Remove are
/// separate buttons named after the passkey. Adding, removing and failures
/// are announced. Times update every minute unless `now` is given.
public struct MonarchPasskeyManager: View {
    private let passkeys: [MonarchPasskey]
    private let title: String?
    private let description: String?
    private let state: MonarchPasskeyManagerState
    private let isRetrying: Bool
    private let isSupported: Bool
    private let isLastSignInMethod: Bool
    private let now: Date?
    private let labels: MonarchPasskeyManagerLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onAdd: (@MainActor () async throws -> Void)?
    private let onRename: (@MainActor (String, String) async throws -> Void)?
    private let onRemove: (@MainActor (String) async throws -> Void)?

    @Environment(\.monarchTheme) private var theme
    @State private var isAdding = false
    @State private var busyID: String?
    @State private var notice: MonarchPasskeyManagerTokens.Notice?
    @State private var renaming: MonarchPasskey?
    @State private var draft = ""
    @State private var removing: MonarchPasskey?

    /// A passkey manager.
    ///
    /// - Parameters:
    ///   - passkeys: The person's passkeys, in the order to show them.
    ///   - title: Replaces the built-in title "Passkeys".
    ///   - description: Replaces the built-in line under the title.
    ///   - state: Whether the passkeys are loaded, loading or failed to load.
    ///   - isRetrying: Whether a retry is in progress, in the failed state.
    ///   - isSupported: Whether this device can create passkeys. When `false`, Add is disabled and a warning
    ///     says why.
    ///   - isLastSignInMethod: Passkeys are the person's only way to sign in, so removing the last one warns.
    ///   - now: The reference time for relative times. Without it they follow the clock, updated every minute.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Retries loading. Shows "Try again" in the failed state.
    ///   - onAdd: Runs the app's passkey registration. Shows "Add a passkey". Throw to show the failure.
    ///   - onRename: Saves a new name, given the id and the trimmed name. Shows "Rename". Throw to show the failure.
    ///   - onRemove: Removes a passkey after a confirmation. Shows "Remove". Throw to show the failure.
    public init(
        _ passkeys: [MonarchPasskey],
        title: String? = nil,
        description: String? = nil,
        state: MonarchPasskeyManagerState = .ready,
        isRetrying: Bool = false,
        isSupported: Bool = true,
        isLastSignInMethod: Bool = false,
        now: Date? = nil,
        labels: MonarchPasskeyManagerLabels = MonarchPasskeyManagerLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onAdd: (@MainActor () async throws -> Void)? = nil,
        onRename: (@MainActor (String, String) async throws -> Void)? = nil,
        onRemove: (@MainActor (String) async throws -> Void)? = nil
    ) {
        self.passkeys = passkeys
        self.title = title
        self.description = description
        self.state = state
        self.isRetrying = isRetrying
        self.isSupported = isSupported
        self.isLastSignInMethod = isLastSignInMethod
        self.now = now
        self.labels = labels
        self.onRetry = onRetry
        self.onAdd = onAdd
        self.onRename = onRename
        self.onRemove = onRemove
    }

    /// The content of the view.
    public var body: some View {
        MonarchCard {
            VStack(alignment: .leading, spacing: MonarchPasskeyManagerTokens.sectionSpacing) {
                header
                if !isSupported {
                    MonarchInlineNotice(
                        labels.unsupported ?? MonarchPasskeyManagerStrings.unsupported,
                        tone: .warning,
                        variant: .plain
                    )
                }
                if let notice {
                    MonarchInlineNotice(notice.text, tone: notice.tone, variant: .plain)
                }
                content(MonarchPasskeyManagerTokens.phase(state, passkeys: passkeys))
            }
        }
        .alert(
            labels.renameTitle ?? MonarchPasskeyManagerStrings.renameTitle,
            isPresented: Binding(
                get: { renaming != nil },
                set: { if !$0 { renaming = nil } }
            ),
            presenting: renaming,
            actions: { passkey in
                TextField(labels.nameField ?? MonarchPasskeyManagerStrings.nameField, text: $draft)
                Button(labels.save ?? MonarchPasskeyManagerStrings.save) {
                    rename(passkey)
                }
                .disabled(MonarchPasskeyManagerTokens.renamed(draft, from: passkey.name) == nil)
                Button(labels.cancel ?? MonarchPasskeyManagerStrings.cancel, role: .cancel) {}
            },
            message: { passkey in
                Text(passkey.name)
            }
        )
        .alert(
            removing.map { labels.confirmTitle?($0.name) ?? MonarchPasskeyManagerStrings.confirmTitle($0.name) }
                ?? "",
            isPresented: Binding(
                get: { removing != nil },
                set: { if !$0 { removing = nil } }
            ),
            presenting: removing,
            actions: { passkey in
                Button(labels.confirmRemove ?? MonarchPasskeyManagerStrings.confirmRemove, role: .destructive) {
                    remove(passkey)
                }
                Button(labels.cancel ?? MonarchPasskeyManagerStrings.cancel, role: .cancel) {}
            },
            message: { _ in
                Text(
                    MonarchPasskeyManagerTokens.confirmMessage(
                        isLastMethod: MonarchPasskeyManagerTokens.isLastMethod(
                            passkeyCount: passkeys.count,
                            isLastSignInMethod: isLastSignInMethod
                        ),
                        labels: labels
                    )
                )
            }
        )
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: MonarchPasskeyManagerTokens.textSpacing) {
            HStack(alignment: .center, spacing: MonarchPasskeyManagerTokens.metaSpacing) {
                Text(title ?? labels.title ?? MonarchPasskeyManagerStrings.title)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
                    .frame(maxWidth: .infinity, alignment: .leading)
                addButton
            }
            Text(description ?? labels.description ?? MonarchPasskeyManagerStrings.description)
                .monarchTextStyle(.bodySmall)
                .foregroundStyle(theme.colors.textMuted)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder private var addButton: some View {
        if onAdd != nil {
            MonarchButton(
                labels.add ?? MonarchPasskeyManagerStrings.add,
                variant: .primary,
                size: .sm,
                isLoading: isAdding,
                action: add
            )
            .disabled(!isSupported)
        }
    }

    @ViewBuilder private func content(_ phase: MonarchPasskeyManagerTokens.Phase) -> some View {
        switch phase {
        case .loading:
            HStack(spacing: MonarchPasskeyManagerTokens.metaSpacing) {
                ProgressView()
                Text(labels.loading ?? MonarchPasskeyManagerStrings.loading)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            }
            .accessibilityElement(children: .combine)
            .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget)
        case .empty:
            MonarchEmptyState(
                labels.emptyTitle ?? MonarchPasskeyManagerStrings.emptyTitle,
                message: labels.emptyMessage ?? MonarchPasskeyManagerStrings.emptyMessage,
                systemImage: MonarchPasskeyManagerTokens.emptySymbol,
                size: .sm
            )
        case .failed:
            MonarchErrorState(
                labels.errorTitle ?? MonarchPasskeyManagerStrings.errorTitle,
                size: .sm,
                isRetrying: isRetrying,
                onRetry: onRetry
            )
        case .list:
            VStack(alignment: .leading, spacing: MonarchPasskeyManagerTokens.metaSpacing) {
                ForEach(passkeys) { passkey in
                    HStack(alignment: .center, spacing: MonarchPasskeyManagerTokens.iconSpacing) {
                        summary(passkey)
                        removeButton(passkey)
                    }
                    .padding(MonarchPasskeyManagerTokens.iconSpacing)
                    .background(
                        theme.colors.bgSubtle,
                        in: RoundedRectangle(cornerRadius: MonarchTokens.Radius.lg, style: .continuous)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: MonarchTokens.Radius.lg, style: .continuous)
                            .strokeBorder(theme.colors.borderSubtle, lineWidth: 1)
                    )
                }
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel(labels.listLabel ?? MonarchPasskeyManagerStrings.listLabel)
        }
    }

    @ViewBuilder private func summary(_ passkey: MonarchPasskey) -> some View {
        if let now {
            MonarchPasskeyRow(passkey: passkey, now: now, labels: labels, onRename: renameAction(passkey))
        } else {
            MonarchRelativeTimeClock(dates: [passkey.created, passkey.lastUsed]) { current in
                MonarchPasskeyRow(passkey: passkey, now: current, labels: labels, onRename: renameAction(passkey))
            }
        }
    }

    /// Renaming is the name itself: tap it, or use the Rename action. No separate button, as on the web.
    private func renameAction(_ passkey: MonarchPasskey) -> (@MainActor () -> Void)? {
        guard onRename != nil, busyID == nil else { return nil }
        return { startRename(passkey) }
    }

    @ViewBuilder private func removeButton(_ passkey: MonarchPasskey) -> some View {
        if onRemove != nil {
            MonarchButton(
                labels.remove ?? MonarchPasskeyManagerStrings.remove,
                variant: .ghost,
                size: .sm,
                isLoading: busyID == passkey.id
            ) {
                removing = passkey
            }
            .accessibilityLabel(
                labels.removeLabel?(passkey.name) ?? MonarchPasskeyManagerStrings.removeLabel(passkey.name)
            )
            .disabled(busyID != nil)
        }
    }

    private func startRename(_ passkey: MonarchPasskey) {
        draft = passkey.name
        renaming = passkey
    }

    private func add() {
        guard let onAdd, MonarchPasskeyManagerTokens.canAdd(isSupported: isSupported, isAdding: isAdding) else {
            return
        }
        notice = nil
        isAdding = true
        Task { @MainActor in
            do {
                try await onAdd()
                announce(labels.added ?? MonarchPasskeyManagerStrings.added)
            } catch {
                let failure = MonarchPasskeyManagerTokens.addFailure(error, labels: labels)
                notice = failure
                announce(failure.text)
            }
            isAdding = false
        }
    }

    private func rename(_ passkey: MonarchPasskey) {
        guard let onRename, let name = MonarchPasskeyManagerTokens.renamed(draft, from: passkey.name) else { return }
        run(passkey) {
            try await onRename(passkey.id, name)
        } failure: {
            labels.renameFailed?(passkey.name) ?? MonarchPasskeyManagerStrings.renameFailed(passkey.name)
        }
    }

    private func remove(_ passkey: MonarchPasskey) {
        guard let onRemove else { return }
        run(passkey) {
            try await onRemove(passkey.id)
            announce(labels.removed?(passkey.name) ?? MonarchPasskeyManagerStrings.removed(passkey.name))
        } failure: {
            labels.removeFailed?(passkey.name) ?? MonarchPasskeyManagerStrings.removeFailed(passkey.name)
        }
    }

    private func run(
        _ passkey: MonarchPasskey,
        _ work: @escaping @MainActor () async throws -> Void,
        failure: @escaping @MainActor () -> String
    ) {
        guard busyID == nil else { return }
        notice = nil
        busyID = passkey.id
        Task { @MainActor in
            do {
                try await work()
            } catch {
                let message = failure()
                notice = MonarchPasskeyManagerTokens.Notice(text: message, tone: .danger)
                announce(message)
            }
            busyID = nil
        }
    }

    private func announce(_ message: String) {
        AccessibilityNotification.Announcement(message).post()
    }
}

/// One passkey: the icon, the name and badge, the detail and the times.
private struct MonarchPasskeyRow: View {
    let passkey: MonarchPasskey
    let now: Date
    let labels: MonarchPasskeyManagerLabels
    var onRename: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    var body: some View {
        let synced = passkey.isSynced ? labels.synced ?? MonarchPasskeyManagerStrings.synced : nil
        let times = MonarchPasskeyManagerTokens.times(passkey, now: now, locale: locale, labels: labels)
        HStack(alignment: .center, spacing: MonarchPasskeyManagerTokens.iconSpacing) {
            MonarchIcon(systemName: MonarchPasskeyManagerTokens.symbol)
                .foregroundStyle(theme.colors.textMuted)
                .frame(width: MonarchPasskeyManagerTokens.iconTile, height: MonarchPasskeyManagerTokens.iconTile)
                .background(
                    theme.colors.bgMuted,
                    in: RoundedRectangle(cornerRadius: MonarchTokens.Radius.md, style: .continuous)
                )
            VStack(alignment: .leading, spacing: MonarchPasskeyManagerTokens.textSpacing) {
                MonarchFlowLayout(spacing: MonarchPasskeyManagerTokens.metaSpacing) {
                    Text(passkey.name)
                        .monarchTextStyle(.label)
                        .foregroundStyle(theme.colors.textDefault)
                        .onTapGesture { onRename?() }
                    if let synced {
                        MonarchBadge(synced, tone: .info, size: .sm)
                    }
                }
                if let detail = passkey.detail, !detail.isEmpty {
                    Text(detail)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                Text(times.joined(separator: " \u{00B7} "))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(passkey.name)
        .accessibilityValue(
            MonarchPasskeyManagerTokens.accessibilityValue(synced: synced, detail: passkey.detail, times: times)
        )
        .accessibilityAction(named: labels.rename ?? MonarchPasskeyManagerStrings.rename) { onRename?() }
        .contextMenu {
            if let onRename {
                Button(labels.rename ?? MonarchPasskeyManagerStrings.rename, action: onRename)
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchPasskeyManager") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        MonarchPreviewMatrix {
            VStack {
                MonarchPasskeyManager(
                    [
                        MonarchPasskey(
                            id: "1",
                            name: "iPhone",
                            created: now.addingTimeInterval(-86_400 * 60),
                            lastUsed: now.addingTimeInterval(-3_600),
                            isSynced: true,
                            detail: "iCloud Keychain"
                        ),
                        MonarchPasskey(id: "2", name: "YubiKey", created: now.addingTimeInterval(-86_400 * 200)),
                    ],
                    now: now,
                    onAdd: {},
                    onRename: { _, _ in },
                    onRemove: { _ in }
                )
                MonarchPasskeyManager([], isSupported: false, onAdd: {})
                MonarchPasskeyManager([], state: .loading)
                MonarchPasskeyManager([], state: .failed, onRetry: {})
            }
        }
    }
#endif
