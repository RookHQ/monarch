import Foundation
import SwiftUI

/// A persistent bar shown while a support or admin person acts as someone
/// else: "You are viewing as Sam · Stop".
///
/// It cannot be dismissed; the only way out is Stop. Put it in
/// `safeAreaInset(edge: .top)` of the root view so it stays above every
/// screen while the session lasts:
///
/// ```swift
/// RootView()
///     .safeAreaInset(edge: .top, spacing: 0) {
///         if let session {
///             MonarchImpersonationBanner(name: session.name, detail: session.email, startedAt: session.start) {
///                 try await session.stop()
///             }
///         }
///     }
/// ```
///
/// Remove the banner when the session has ended. While `onStop` runs the
/// button is busy; a failure shows its message in the banner.
///
/// Accessibility: the text is one element ("You are viewing as Sam (sam@…),
/// Started 5 minutes ago, Reason: …") and is announced when the banner
/// appears. The Stop button is named "Stop viewing as" and the name, and has
/// a 44-point touch target. The icon is decorative. A failure is announced
/// and shown with an icon and text.
public struct MonarchImpersonationBanner: View {
    private let name: String
    private let detail: String?
    private let reason: String?
    private let startedAt: Date?
    private let isStopping: Bool?
    private let labels: MonarchImpersonationBannerLabels
    private let onStop: @MainActor () async throws -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @State private var isRunning = false
    @State private var errorMessage: String?

    /// An impersonation banner.
    ///
    /// - Parameters:
    ///   - name: The person being viewed as.
    ///   - detail: A second identifier, such as their email, after the name.
    ///   - reason: Why the session started, such as a ticket number.
    ///   - startedAt: When the session started, shown as a relative time.
    ///   - isStopping: Controls the busy state when the app tracks the stop itself. Tracked from `onStop` when `nil`.
    ///   - labels: Replacements for the built-in strings.
    ///   - onStop: Ends the session. Throw to show the failure; the banner stays.
    public init(
        name: String,
        detail: String? = nil,
        reason: String? = nil,
        startedAt: Date? = nil,
        isStopping: Bool? = nil,
        labels: MonarchImpersonationBannerLabels = MonarchImpersonationBannerLabels(),
        onStop: @escaping @MainActor () async throws -> Void
    ) {
        self.name = name
        self.detail = detail
        self.reason = reason
        self.startedAt = startedAt
        self.isStopping = isStopping
        self.labels = labels
        self.onStop = onStop
    }

    /// A banner showing a failed stop, for tests and previews.
    init(name: String, errorMessage: String) {
        self.init(name: name, onStop: {})
        _errorMessage = State(initialValue: errorMessage)
    }

    private var stopping: Bool { isStopping ?? isRunning }

    /// The content of the view.
    public var body: some View {
        let appearance = MonarchImpersonationBannerTokens.appearance
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .center, spacing: MonarchImpersonationBannerTokens.spacing) {
                content(appearance)
                Spacer(minLength: 0)
                stopButton(fullWidth: false)
            }
            VStack(alignment: .leading, spacing: MonarchImpersonationBannerTokens.spacing) {
                content(appearance)
                stopButton(fullWidth: true)
            }
        }
        .padding(.horizontal, MonarchImpersonationBannerTokens.horizontalPadding)
        .padding(.vertical, MonarchImpersonationBannerTokens.verticalPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.colors[role: appearance.background])
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(theme.colors[role: appearance.edge])
                .frame(height: MonarchImpersonationBannerTokens.edgeHeight)
        }
        .accessibilityElement(children: .contain)
        .onAppear {
            let text = MonarchImpersonationBannerTokens.titleText(name: name, labels: labels)
            AccessibilityNotification.Announcement(text).post()
        }
        .onChange(of: errorMessage) { _, message in
            if let message {
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }

    private func content(_ appearance: MonarchImpersonationBannerTokens.Appearance) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchImpersonationBannerTokens.spacing) {
            MonarchIcon(systemName: MonarchImpersonationBannerTokens.symbol)
                .foregroundStyle(theme.colors[role: appearance.icon])
            VStack(alignment: .leading, spacing: MonarchImpersonationBannerTokens.textSpacing) {
                Text(title(appearance))
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: appearance.text])
                    .fixedSize(horizontal: false, vertical: true)
                meta(appearance)
                if let errorMessage {
                    HStack(alignment: .firstTextBaseline, spacing: MonarchImpersonationBannerTokens.textSpacing) {
                        MonarchIcon(systemName: MonarchImpersonationBannerTokens.errorSymbol, size: .sm)
                        Text(errorMessage)
                            .monarchTextStyle(.caption)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .foregroundStyle(theme.colors[role: appearance.text])
                }
            }
        }
        .accessibilityElement(children: .combine)
    }

    private func title(_ appearance: MonarchImpersonationBannerTokens.Appearance) -> AttributedString {
        var text: AttributedString
        if let parts = MonarchImpersonationBannerTokens.titleParts(name: name, labels: labels) {
            var bold = AttributedString(name)
            bold.inlinePresentationIntent = .stronglyEmphasized
            text = AttributedString(parts.prefix) + bold + AttributedString(parts.suffix)
        } else {
            text = AttributedString(MonarchImpersonationBannerTokens.titleText(name: name, labels: labels))
        }
        if let detail, !detail.isEmpty {
            var extra = AttributedString(" (\(detail))")
            extra.swiftUI.foregroundColor = theme.colors[role: appearance.muted]
            text += extra
        }
        return text.monarchStrongFaces(theme: theme, style: .body)
    }

    @ViewBuilder private func meta(_ appearance: MonarchImpersonationBannerTokens.Appearance) -> some View {
        let reasonText = MonarchImpersonationBannerTokens.reasonText(reason, labels: labels)
        if startedAt != nil || reasonText != nil {
            MonarchRelativeTimeClock(dates: [startedAt]) { now in
                Text(metaText(reason: reasonText, now: now))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: appearance.muted])
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private func metaText(reason: String?, now: Date) -> String {
        var parts: [String] = []
        if let startedAt {
            let started = labels.started ?? MonarchImpersonationBannerStrings.started
            parts.append("\(started) \(MonarchRelativeTime.string(startedAt, now: now, locale: locale))")
        }
        if let reason { parts.append(reason) }
        return parts.joined(separator: " · ")
    }

    private func stopButton(fullWidth: Bool) -> some View {
        MonarchButton(
            labels.stop ?? MonarchImpersonationBannerStrings.stop,
            variant: .warning,
            size: .sm,
            isLoading: stopping,
            fullWidth: fullWidth
        ) {
            stop()
        }
        .accessibilityLabel(MonarchImpersonationBannerTokens.stopLabel(name: name, labels: labels))
    }

    private func stop() {
        guard !stopping else { return }
        errorMessage = nil
        isRunning = true
        let labels = labels
        Task { @MainActor in
            do {
                try await onStop()
            } catch {
                errorMessage = MonarchImpersonationBannerTokens.errorMessage(error, labels: labels)
            }
            isRunning = false
        }
    }
}

#if DEBUG
    #Preview("MonarchImpersonationBanner") {
        MonarchPreviewMatrix {
            VStack {
                MonarchImpersonationBanner(
                    name: "Sam Lee",
                    detail: "sam@example.com",
                    reason: "Ticket 4821",
                    startedAt: .now.addingTimeInterval(-300)
                ) {}
                MonarchImpersonationBanner(name: "Sam Lee", isStopping: true) {}
                MonarchImpersonationBanner(name: "Sam Lee", errorMessage: "Couldn’t stop. Try again.")
            }
        }
    }
#endif
