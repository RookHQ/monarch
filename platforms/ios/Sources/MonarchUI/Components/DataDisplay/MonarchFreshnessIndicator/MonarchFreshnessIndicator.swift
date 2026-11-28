import SwiftUI

/// How fresh a data set or a sync is: "Updated 3 minutes ago", turning to
/// warning and danger as it ages, with an optional refresh button.
///
/// ```swift
/// MonarchFreshnessIndicator(
///     updatedAt: lastSync,
///     source: "Directory",
///     staleAfter: .seconds(3600),
///     expiredAfter: .seconds(86_400)
/// ) {
///     try await directory.sync()
/// }
/// ```
///
/// The status is worked out from the age of `updatedAt` and the limits, and
/// the line is redrawn every minute and when the age passes a limit. Pass
/// `status` to override it, for example `checking` while the status itself
/// loads or `error` when the last sync failed. On a scrolling screen,
/// `refreshable(action:)` is the usual companion of the refresh button.
///
/// Accessibility: the dot is decorative; stale, expired and error are also
/// written as words in the tone's colour, so colour is never the only signal.
/// The line is one element read with pauses between its parts. When the
/// status turns stale, expired or error after the first draw, the status
/// words are announced. The refresh button is a separate standard button
/// with a 44-point touch target; while it runs it reads "Refreshing…" as its
/// value, and "Refreshed." or "Refresh failed." is announced at the end.
public struct MonarchFreshnessIndicator: View {
    private let updatedAt: Date?
    private let source: String?
    private let details: [String]
    private let error: String?
    private let statusOverride: MonarchFreshnessStatus?
    private let staleAfter: TimeInterval?
    private let expiredAfter: TimeInterval?
    private let variant: MonarchFreshnessIndicatorVariant
    private let isRefreshing: Bool
    private let labels: MonarchFreshnessIndicatorLabels
    private let onRefresh: (@MainActor () async throws -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @ScaledMetric(relativeTo: .caption) private var dotSize = MonarchFreshnessIndicatorTokens.dotSize
    @State private var isPending = false

    /// A freshness indicator.
    ///
    /// - Parameters:
    ///   - updatedAt: When the data was last updated or synced. `nil` means never.
    ///   - source: Names the data or its source, such as "Directory". Shown first.
    ///   - details: Further facts, already localised, appended with middle dots.
    ///   - error: The error text for the `error` status, or the last error of a sync.
    ///   - status: Overrides the status worked out from the age.
    ///   - staleAfter: The age after which the data is stale. `nil` never turns stale.
    ///   - expiredAfter: The age after which the data is expired. `nil` never expires.
    ///   - variant: A line of text, or a bordered bar.
    ///   - isRefreshing: Whether a refresh the app tracks is running.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRefresh: Refreshes the data. Adds a refresh button; the button
    ///     stays busy until the function returns, and the outcome is announced.
    public init(
        updatedAt: Date?,
        source: String? = nil,
        details: [String] = [],
        error: String? = nil,
        status: MonarchFreshnessStatus? = nil,
        staleAfter: Duration? = nil,
        expiredAfter: Duration? = nil,
        variant: MonarchFreshnessIndicatorVariant = .inline,
        isRefreshing: Bool = false,
        labels: MonarchFreshnessIndicatorLabels = MonarchFreshnessIndicatorLabels(),
        onRefresh: (@MainActor () async throws -> Void)? = nil
    ) {
        self.updatedAt = updatedAt
        self.source = source
        self.details = details
        self.error = error
        self.statusOverride = status
        self.staleAfter = MonarchFreshnessIndicatorTokens.seconds(staleAfter)
        self.expiredAfter = MonarchFreshnessIndicatorTokens.seconds(expiredAfter)
        self.variant = variant
        self.isRefreshing = isRefreshing
        self.labels = labels
        self.onRefresh = onRefresh
    }

    private var limits: [TimeInterval] { [staleAfter, expiredAfter].compactMap { $0 } }
    private var refreshing: Bool { isRefreshing || isPending }

    /// The content of the view.
    public var body: some View {
        TimelineView(MonarchFreshnessSchedule(updatedAt: updatedAt, limits: limits)) { timeline in
            content(now: timeline.date)
        }
    }

    private func status(now: Date) -> MonarchFreshnessStatus {
        statusOverride
            ?? MonarchFreshnessIndicatorTokens.status(
                updatedAt: updatedAt,
                now: now,
                staleAfter: staleAfter,
                expiredAfter: expiredAfter
            )
    }

    private func content(now: Date) -> some View {
        let status = status(now: now)
        let parts = MonarchFreshnessIndicatorTokens.parts(
            status: status,
            updatedAt: updatedAt,
            now: now,
            error: error,
            details: details,
            labels: labels,
            locale: locale
        )
        let isBanner = variant == .banner
        return ViewThatFits(in: .horizontal) {
            HStack(spacing: MonarchFreshnessIndicatorTokens.actionSpacing) {
                line(status: status, parts: parts)
                if onRefresh != nil {
                    Spacer(minLength: 0)
                    refreshButton
                }
            }
            VStack(alignment: .leading, spacing: MonarchFreshnessIndicatorTokens.actionSpacing) {
                line(status: status, parts: parts)
                if onRefresh != nil { refreshButton }
            }
        }
        .padding(.horizontal, isBanner ? MonarchFreshnessIndicatorTokens.bannerHorizontalPadding : 0)
        .padding(.vertical, isBanner ? MonarchFreshnessIndicatorTokens.bannerVerticalPadding : 0)
        .frame(maxWidth: isBanner ? .infinity : nil, alignment: .leading)
        .background {
            if isBanner {
                MonarchShapes.control.fill(theme.colors[role: MonarchFreshnessIndicatorTokens.bannerFill])
            }
        }
        .overlay {
            if isBanner {
                MonarchShapes.control
                    .strokeBorder(
                        theme.colors[role: MonarchFreshnessIndicatorTokens.bannerBorder],
                        lineWidth: MonarchShapes.borderWidth
                    )
                    .allowsHitTesting(false)
            }
        }
        .accessibilityElement(children: .contain)
        .onChange(of: status) { old, new in
            if let message = MonarchFreshnessIndicatorTokens.announcement(
                from: old,
                to: new,
                error: error,
                labels: labels
            ) {
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }

    private func line(status: MonarchFreshnessStatus, parts: [MonarchFreshnessIndicatorTokens.Part]) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchFreshnessIndicatorTokens.dotSpacing) {
            // The hidden text gives the dot the height and baseline of the first line.
            Text(verbatim: " ")
                .monarchTextStyle(MonarchFreshnessIndicatorTokens.textStyle)
                .hidden()
                .frame(width: dotSize)
                .overlay {
                    Circle()
                        .fill(theme.colors[role: MonarchFreshnessIndicatorTokens.dot(status)])
                        .frame(width: dotSize, height: dotSize)
                }
            composedText(parts)
                .monarchTextStyle(MonarchFreshnessIndicatorTokens.textStyle)
                .foregroundStyle(theme.colors[role: MonarchFreshnessIndicatorTokens.text])
                .fixedSize(horizontal: false, vertical: true)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchFreshnessIndicatorTokens.accessibilityLabel(source: source, parts: parts))
    }

    private func composedText(_ parts: [MonarchFreshnessIndicatorTokens.Part]) -> Text {
        let separator = MonarchFreshnessIndicatorTokens.separator
        var text = Text(verbatim: "")
        if let source, !source.isEmpty {
            text = Text(verbatim: source + separator)
        }
        for (index, part) in parts.enumerated() {
            if index > 0 { text = text + Text(verbatim: separator) }
            if let emphasis = part.emphasis {
                text = text + Text(verbatim: part.text).bold().foregroundStyle(theme.colors[role: emphasis])
            } else {
                text = text + Text(verbatim: part.text)
            }
        }
        return text
    }

    private var refreshButton: some View {
        let spoken = labels.refreshing ?? MonarchFreshnessIndicatorStrings.refreshing
        return MonarchButton(
            refreshing ? spoken : (labels.refresh ?? MonarchFreshnessIndicatorStrings.refresh),
            systemImage: MonarchFreshnessIndicatorTokens.refreshSymbol,
            variant: .ghost,
            size: .sm,
            isLoading: refreshing,
            loadingLabel: spoken
        ) {
            refresh()
        }
    }

    private func refresh() {
        guard let onRefresh, !refreshing else { return }
        isPending = true
        Task { @MainActor in
            let message: String
            do {
                try await onRefresh()
                message = labels.refreshed ?? MonarchFreshnessIndicatorStrings.refreshed
            } catch {
                message = labels.refreshFailed ?? MonarchFreshnessIndicatorStrings.refreshFailed
            }
            isPending = false
            AccessibilityNotification.Announcement(message).post()
        }
    }
}

#if DEBUG
    #Preview("MonarchFreshnessIndicator") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchFreshnessIndicator(updatedAt: .now.addingTimeInterval(-180), source: "Directory") {}
                MonarchFreshnessIndicator(
                    updatedAt: .now.addingTimeInterval(-7200),
                    staleAfter: .seconds(3600),
                    variant: .banner
                ) {}
                MonarchFreshnessIndicator(
                    updatedAt: .now.addingTimeInterval(-200_000),
                    staleAfter: .seconds(3600),
                    expiredAfter: .seconds(86_400)
                )
                MonarchFreshnessIndicator(updatedAt: nil)
                MonarchFreshnessIndicator(updatedAt: nil, status: .checking)
                MonarchFreshnessIndicator(
                    updatedAt: .now.addingTimeInterval(-600),
                    error: "Sync failed",
                    status: .error,
                    isRefreshing: true
                ) {}
            }
        }
    }
#endif
