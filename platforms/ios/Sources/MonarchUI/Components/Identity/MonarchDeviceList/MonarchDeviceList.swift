import SwiftUI

/// The devices that have accessed an account: name, system and browser,
/// location, when each was last used and first seen, "This device" and
/// "Blocked" badges, and an optional Remove action behind a confirmation.
///
/// ```swift
/// MonarchDeviceList(devices, onRemove: { id in
///     try await account.removeDevice(id)
/// })
/// ```
///
/// The list is a ``MonarchCard`` with a header and one row per device.
/// While loading it shows a progress indicator; an empty list shows a
/// ``MonarchEmptyState``; a failure shows a ``MonarchErrorState`` with a
/// retry. Remove appears on every device except the current one, only with
/// `onRemove`. It asks first with a system alert unless `confirmsRemoval` is
/// `false` (for an app that opens its own revoke flow). While `onRemove`
/// runs the row's button is busy; a thrown error shows a danger notice in
/// the card.
///
/// Accessibility: the title is a header. Each device is one element read as
/// its name, then the badges, the system, browser and location and the
/// times, so "Blocked" and "This device" never depend on colour or the icon.
/// The Remove button is separate and named after the device ("Remove Work
/// laptop"). Removal and its failure are announced. Times update every
/// minute unless `now` is given.
public struct MonarchDeviceList<RowActions: View>: View {
    private let devices: [MonarchDevice]
    private let title: String?
    private let description: String?
    private let state: MonarchDeviceListState
    private let isRetrying: Bool
    private let confirmsRemoval: Bool
    private let now: Date?
    private let labels: MonarchDeviceListLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onRemove: (@MainActor (String) async throws -> Void)?
    private let rowActions: (MonarchDevice) -> RowActions

    @Environment(\.monarchTheme) private var theme
    @State private var pending: MonarchDevice?
    @State private var removingID: String?
    @State private var failure: String?

    /// A device list with extra controls at the end of each row, such as a
    /// block or rename menu.
    ///
    /// - Parameters:
    ///   - devices: The devices, in the order to show them.
    ///   - title: Replaces the built-in title "Devices".
    ///   - description: Replaces the built-in line under the title.
    ///   - state: Whether the devices are loaded, loading or failed to load.
    ///   - isRetrying: Whether a retry is in progress, in the failed state.
    ///   - confirmsRemoval: Asks before removing. Set `false` when `onRemove` opens the app's own confirmation.
    ///   - now: The reference time for relative times. Without it they follow the clock, updated every minute.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Retries loading. Shows "Try again" in the failed state.
    ///   - onRemove: Signs the device out and forgets it. Shows Remove on every device except the current one.
    ///     Throw to show the failure.
    ///   - rowActions: Extra controls at the end of a row.
    public init(
        _ devices: [MonarchDevice],
        title: String? = nil,
        description: String? = nil,
        state: MonarchDeviceListState = .ready,
        isRetrying: Bool = false,
        confirmsRemoval: Bool = true,
        now: Date? = nil,
        labels: MonarchDeviceListLabels = MonarchDeviceListLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onRemove: (@MainActor (String) async throws -> Void)? = nil,
        @ViewBuilder rowActions: @escaping (MonarchDevice) -> RowActions
    ) {
        self.devices = devices
        self.title = title
        self.description = description
        self.state = state
        self.isRetrying = isRetrying
        self.confirmsRemoval = confirmsRemoval
        self.now = now
        self.labels = labels
        self.onRetry = onRetry
        self.onRemove = onRemove
        self.rowActions = rowActions
    }

    /// The content of the view.
    public var body: some View {
        MonarchCard {
            VStack(alignment: .leading, spacing: MonarchDeviceListTokens.sectionSpacing) {
                header
                if let failure {
                    MonarchInlineNotice(failure, tone: .danger, variant: .plain)
                }
                content(MonarchDeviceListTokens.phase(state, devices: devices))
            }
        }
        .alert(
            pending.map { confirmTitle($0) } ?? "",
            isPresented: Binding(
                get: { pending != nil },
                set: { if !$0 { pending = nil } }
            ),
            presenting: pending,
            actions: { device in
                Button(labels.remove ?? MonarchDeviceListStrings.remove, role: .destructive) {
                    remove(device)
                }
                Button(labels.cancel ?? MonarchDeviceListStrings.cancel, role: .cancel) {}
            },
            message: { _ in
                Text(labels.confirmMessage ?? MonarchDeviceListStrings.confirmMessage)
            }
        )
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: MonarchDeviceListTokens.textSpacing) {
            Text(title ?? labels.title ?? MonarchDeviceListStrings.title)
                .monarchTextStyle(.subheading)
                .foregroundStyle(theme.colors.textDefault)
                .accessibilityAddTraits(.isHeader)
            Text(description ?? labels.description ?? MonarchDeviceListStrings.description)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder private func content(_ phase: MonarchDeviceListTokens.Phase) -> some View {
        switch phase {
        case .loading:
            HStack(spacing: MonarchDeviceListTokens.metaSpacing) {
                ProgressView()
                Text(labels.loading ?? MonarchDeviceListStrings.loading)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            }
            .accessibilityElement(children: .combine)
            .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget)
        case .empty:
            MonarchEmptyState(
                labels.emptyTitle ?? MonarchDeviceListStrings.emptyTitle,
                message: labels.emptyMessage ?? MonarchDeviceListStrings.emptyMessage,
                systemImage: MonarchDeviceListTokens.emptySymbol,
                size: .sm
            )
        case .failed:
            MonarchErrorState(
                labels.errorTitle ?? MonarchDeviceListStrings.errorTitle,
                size: .sm,
                isRetrying: isRetrying,
                onRetry: onRetry
            )
        case .list:
            VStack(alignment: .leading, spacing: MonarchDeviceListTokens.rowSpacing) {
                ForEach(Array(devices.enumerated()), id: \.element.id) { _, device in
                    row(device)
                }
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel(labels.listLabel ?? MonarchDeviceListStrings.listLabel)
        }
    }

    private func row(_ device: MonarchDevice) -> some View {
        let name = MonarchDeviceListTokens.displayName(device, labels: labels)
        let canRemove = MonarchDeviceListTokens.canRemove(device, hasHandler: onRemove != nil)
        // Each device is a quiet rounded tile, as on the web.
        return ViewThatFits(in: .horizontal) {
            HStack(alignment: .center, spacing: MonarchDeviceListTokens.iconSpacing) {
                summary(device, name: name)
                actions(device, name: name, canRemove: canRemove)
            }
            VStack(alignment: .leading, spacing: MonarchDeviceListTokens.lineSpacing) {
                summary(device, name: name)
                actions(device, name: name, canRemove: canRemove)
            }
        }
        .padding(MonarchDeviceListTokens.iconSpacing)
        .background(MonarchShapes.container.fill(theme.colors.bgMuted))
        .overlay(MonarchShapes.container.strokeBorder(theme.colors.borderSubtle, lineWidth: MonarchShapes.borderWidth))
    }

    @ViewBuilder private func summary(_ device: MonarchDevice, name: String) -> some View {
        if let now {
            MonarchDeviceRow(device: device, name: name, now: now, labels: labels)
        } else {
            MonarchRelativeTimeClock(dates: [device.lastUsed, device.firstSeen]) { current in
                MonarchDeviceRow(device: device, name: name, now: current, labels: labels)
            }
        }
    }

    private func actions(_ device: MonarchDevice, name: String, canRemove: Bool) -> some View {
        HStack(spacing: MonarchDeviceListTokens.metaSpacing) {
            rowActions(device)
            if canRemove {
                MonarchButton(
                    labels.remove ?? MonarchDeviceListStrings.remove,
                    variant: .ghost,
                    size: .sm,
                    isLoading: removingID == device.id
                ) {
                    if confirmsRemoval {
                        pending = device
                    } else {
                        remove(device)
                    }
                }
                .disabled(removingID != nil && removingID != device.id)
                .accessibilityLabel(labels.removeLabel?(name) ?? MonarchDeviceListStrings.removeLabel(name))
            }
        }
    }

    private func confirmTitle(_ device: MonarchDevice) -> String {
        let name = MonarchDeviceListTokens.displayName(device, labels: labels)
        return labels.confirmTitle?(name) ?? MonarchDeviceListStrings.confirmTitle(name)
    }

    private func remove(_ device: MonarchDevice) {
        guard let onRemove, removingID == nil else { return }
        let name = MonarchDeviceListTokens.displayName(device, labels: labels)
        failure = nil
        removingID = device.id
        Task { @MainActor in
            do {
                try await onRemove(device.id)
                AccessibilityNotification.Announcement(
                    labels.removed?(name) ?? MonarchDeviceListStrings.removed(name)
                ).post()
            } catch {
                let message = labels.removeFailed?(name) ?? MonarchDeviceListStrings.removeFailed(name)
                failure = message
                AccessibilityNotification.Announcement(message).post()
            }
            removingID = nil
        }
    }
}

extension MonarchDeviceList where RowActions == EmptyView {
    /// A device list.
    ///
    /// - Parameters:
    ///   - devices: The devices, in the order to show them.
    ///   - title: Replaces the built-in title "Devices".
    ///   - description: Replaces the built-in line under the title.
    ///   - state: Whether the devices are loaded, loading or failed to load.
    ///   - isRetrying: Whether a retry is in progress, in the failed state.
    ///   - confirmsRemoval: Asks before removing. Set `false` when `onRemove` opens the app's own confirmation.
    ///   - now: The reference time for relative times. Without it they follow the clock, updated every minute.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Retries loading. Shows "Try again" in the failed state.
    ///   - onRemove: Signs the device out and forgets it. Shows Remove on every device except the current one.
    ///     Throw to show the failure.
    public init(
        _ devices: [MonarchDevice],
        title: String? = nil,
        description: String? = nil,
        state: MonarchDeviceListState = .ready,
        isRetrying: Bool = false,
        confirmsRemoval: Bool = true,
        now: Date? = nil,
        labels: MonarchDeviceListLabels = MonarchDeviceListLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onRemove: (@MainActor (String) async throws -> Void)? = nil
    ) {
        self.init(
            devices,
            title: title,
            description: description,
            state: state,
            isRetrying: isRetrying,
            confirmsRemoval: confirmsRemoval,
            now: now,
            labels: labels,
            onRetry: onRetry,
            onRemove: onRemove,
            rowActions: { _ in EmptyView() }
        )
    }
}

/// One device: the icon, the name and badges, the detail line and the times.
private struct MonarchDeviceRow: View {
    let device: MonarchDevice
    let name: String
    let now: Date
    let labels: MonarchDeviceListLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    var body: some View {
        let badges = MonarchDeviceListTokens.badges(device, labels: labels)
        let detail = MonarchDeviceListTokens.detail(device)
        let times = MonarchDeviceListTokens.times(device, now: now, locale: locale, labels: labels)
        HStack(alignment: .center, spacing: MonarchDeviceListTokens.iconSpacing) {
            MonarchIcon(systemName: MonarchDeviceListTokens.symbol(device.kind))
                .foregroundStyle(theme.colors[role: MonarchDeviceListTokens.iconRole(isBlocked: device.isBlocked)])
                .frame(
                    width: MonarchAccessibility.minimumHitTarget * 0.75,
                    height: MonarchAccessibility.minimumHitTarget * 0.75
                )
                .background(
                    MonarchShapes.control.fill(device.isBlocked ? theme.colors.dangerSubtle : theme.colors.bgSurface)
                )
            VStack(alignment: .leading, spacing: MonarchDeviceListTokens.lineSpacing) {
                MonarchFlowLayout(spacing: MonarchDeviceListTokens.metaSpacing) {
                    Text(name)
                        .monarchTextStyle(.bodyStrong)
                        .foregroundStyle(theme.colors.textDefault)
                    ForEach(badges) { badge in
                        MonarchBadge(badge.text, tone: badge.tone)
                    }
                }
                if let detail {
                    Text(detail)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                if !times.isEmpty {
                    Text(times.joined(separator: " \u{00B7} "))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(name)
        .accessibilityValue(
            MonarchDeviceListTokens.accessibilityValue(badges: badges.map(\.text), detail: detail, times: times)
        )
    }
}

#if DEBUG
    #Preview("MonarchDeviceList") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        MonarchPreviewMatrix {
            VStack {
                MonarchDeviceList(
                    [
                        MonarchDevice(
                            id: "1",
                            name: "MacBook Pro",
                            os: "macOS 15",
                            browser: "Safari",
                            location: "Berlin, Germany",
                            lastUsed: now,
                            isCurrent: true
                        ),
                        MonarchDevice(
                            id: "2",
                            name: "Pixel 8",
                            kind: .mobile,
                            os: "Android 15",
                            lastUsed: now.addingTimeInterval(-86_400 * 3),
                            isBlocked: true
                        ),
                    ],
                    now: now,
                    onRemove: { _ in }
                )
                MonarchDeviceList([], state: .loading)
                MonarchDeviceList([])
                MonarchDeviceList([], state: .failed, onRetry: {})
            }
        }
    }
#endif
