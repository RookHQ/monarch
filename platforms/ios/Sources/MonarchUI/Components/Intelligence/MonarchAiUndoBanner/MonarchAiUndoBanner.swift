import SwiftUI

/// An inline banner after the AI changed something on the person's behalf
/// ("AI changed 3 fields"): what changed, before and after, an Undo and a
/// link to the audit log. With `duration`, Undo is offered for a limited time.
///
/// ```swift
/// MonarchAiUndoBanner(
///     "AI changed 2 fields",
///     changes: [
///         MonarchAiChange("Status", before: "Open", after: "Closed"),
///         MonarchAiChange("Owner", before: "Ana", after: "Ben"),
///     ],
///     auditURL: URL(string: "https://app.example.com/audit/812"),
///     duration: .seconds(30),
///     onUndo: { try await store.revert(change) }
/// )
/// ```
///
/// Undo runs `onUndo`: the button shows a progress indicator until it
/// returns, then the banner says "Changes undone" and turns neutral. When
/// `onUndo` throws, it says "Couldn’t undo. Try again." and Undo stays. The
/// time limit pauses while the app is in the background and while the list
/// of changes is open, and does not run at all while VoiceOver or Switch
/// Control is on. For a timed message that floats over the screen, use the
/// undo toast recipe with ``MonarchToast`` instead.
///
/// Accessibility: the title is read as "AI change: " and the title. Each
/// change is one element read as "Status changed from Open to Closed"; the
/// old value is struck through and the arrow is decorative. Undo, the audit
/// link and Dismiss are separate controls with 44-point touch targets. The
/// result of an undo, a failure and the end of the time limit are shown as
/// text and announced. The bot icon is decorative.
public struct MonarchAiUndoBanner: View {
    private let title: String
    private let description: String?
    private let changes: [MonarchAiChange]
    private let auditURL: URL?
    private let duration: Duration?
    private let labels: MonarchAiUndoBannerLabels
    private let onUndo: @MainActor () async throws -> Void
    private let onExpire: (@MainActor () -> Void)?
    private let onDismiss: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.accessibilityVoiceOverEnabled) private var isVoiceOverOn
    @Environment(\.accessibilitySwitchControlEnabled) private var isSwitchControlOn
    @State private var undo = MonarchAiUndoBannerTokens.Undo.idle
    @State private var isExpired = false
    @State private var remaining: Duration?
    @State private var isShowingChanges = false

    /// An AI undo banner.
    ///
    /// - Parameters:
    ///   - title: What the AI did, in the past tense, such as "AI changed 3 fields". Already localised.
    ///   - description: One line of detail.
    ///   - changes: The fields that changed, before and after. More than three collapse into a disclosure.
    ///   - auditURL: The audit log entry of the change. Only `http` and `https` addresses are shown.
    ///   - duration: How long Undo stays available. `nil` keeps it until the banner is removed.
    ///   - labels: Replacements for the built-in strings.
    ///   - onUndo: Reverts the change. Throw to report a failure.
    ///   - onExpire: Runs once when `duration` runs out without an undo.
    ///   - onDismiss: Shows a dismiss button. Remove the banner here.
    public init(
        _ title: String,
        description: String? = nil,
        changes: [MonarchAiChange] = [],
        auditURL: URL? = nil,
        duration: Duration? = nil,
        labels: MonarchAiUndoBannerLabels = MonarchAiUndoBannerLabels(),
        onUndo: @escaping @MainActor () async throws -> Void,
        onExpire: (@MainActor () -> Void)? = nil,
        onDismiss: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.description = description
        self.changes = changes
        self.auditURL = MonarchAiUndoBannerTokens.safeURL(auditURL)
        self.duration = duration
        self.labels = labels
        self.onUndo = onUndo
        self.onExpire = onExpire
        self.onDismiss = onDismiss
        _remaining = State(initialValue: duration)
    }

    private var state: MonarchAiUndoBannerTokens.State {
        MonarchAiUndoBannerTokens.state(undo: undo, isExpired: isExpired)
    }

    private var isCountingDown: Bool {
        MonarchAiUndoBannerTokens.isCountingDown(
            hasDuration: duration != nil,
            state: state,
            isSceneActive: scenePhase == .active,
            isReadingChanges: isShowingChanges,
            isAssistiveTechnologyRunning: isVoiceOverOn || isSwitchControlOn
        )
    }

    /// The content of the view.
    public var body: some View {
        let surface = MonarchAiUndoBannerTokens.surface(state)
        let shape = RoundedRectangle(cornerRadius: MonarchAiUndoBannerTokens.cornerRadius, style: .continuous)
        HStack(alignment: .top, spacing: MonarchAiUndoBannerTokens.iconSpacing) {
            MonarchAiBotGlyph()
                .foregroundStyle(theme.colors.aiStrong)
            VStack(alignment: .leading, spacing: MonarchAiUndoBannerTokens.sectionSpacing) {
                VStack(alignment: .leading, spacing: MonarchAiUndoBannerTokens.changeSpacing) {
                    Text(title)
                        .monarchTextStyle(.bodyStrong)
                        .foregroundStyle(theme.colors.textDefault)
                        .accessibilityLabel(
                            MonarchAiUndoBannerTokens.titleLabel(
                                prefix: labels.aiPrefix ?? MonarchAiUndoBannerStrings.aiPrefix,
                                title: title
                            )
                        )
                    if let description, !description.isEmpty {
                        Text(description)
                            .monarchTextStyle(.body)
                            .foregroundStyle(theme.colors.textMuted)
                    }
                }
                changeList
                status
                actions
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if let onDismiss {
                Button(action: onDismiss) {
                    MonarchIcon(systemName: "xmark", size: .sm)
                        .foregroundStyle(theme.colors.textMuted)
                        .monarchMinimumHitTarget()
                }
                .buttonStyle(.plain)
                .accessibilityLabel(labels.dismiss ?? MonarchAiUndoBannerStrings.dismiss)
                .padding(.vertical, -MonarchAiUndoBannerTokens.verticalPadding)
            }
        }
        .padding(.horizontal, MonarchAiUndoBannerTokens.horizontalPadding)
        .padding(.vertical, MonarchAiUndoBannerTokens.verticalPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(shape.fill(theme.colors[role: surface.background]))
        .overlay(
            shape.strokeBorder(theme.colors[role: surface.border], lineWidth: MonarchAiUndoBannerTokens.borderWidth)
        )
        .monarchAnimation(.fast, value: state)
        .accessibilityElement(children: .contain)
        .task(id: isCountingDown) { await countDown() }
    }

    @ViewBuilder private var changeList: some View {
        if MonarchAiUndoBannerTokens.collapsesChanges(changes.count) {
            DisclosureGroup(
                labels.showChanges?(changes.count) ?? MonarchAiUndoBannerStrings.showChanges(changes.count),
                isExpanded: $isShowingChanges
            ) {
                rows
            }
            .disclosureGroupStyle(.monarch)
        } else if !changes.isEmpty {
            rows
        }
    }

    private var rows: some View {
        VStack(alignment: .leading, spacing: MonarchAiUndoBannerTokens.changeSpacing) {
            ForEach(Array(changes.enumerated()), id: \.offset) { _, change in
                (Text(change.label).foregroundStyle(theme.colors.textMuted)
                    + Text(verbatim: " ")
                    + Text(change.before).strikethrough().foregroundStyle(theme.colors.textMuted)
                    + Text(verbatim: " ")
                    + Text(Image(systemName: "arrow.forward")).foregroundStyle(theme.colors.textMuted)
                    + Text(verbatim: " ")
                    + Text(change.after).foregroundStyle(theme.colors.textDefault))
                    .monarchTextStyle(.caption)
                    .accessibilityLabel(
                        labels.change?(change.label, change.before, change.after)
                            ?? MonarchAiUndoBannerStrings.change(change.label, change.before, change.after)
                    )
            }
        }
    }

    @ViewBuilder private var status: some View {
        if let message = statusMessage {
            Text(message)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: MonarchAiUndoBannerTokens.statusColor(state)])
        }
    }

    private var statusMessage: String? {
        switch state {
        case .undone: labels.undone ?? MonarchAiUndoBannerStrings.undone
        case .failed: labels.undoFailed ?? MonarchAiUndoBannerStrings.undoFailed
        case .expired: labels.expired ?? MonarchAiUndoBannerStrings.expired
        case .active, .undoing: nil
        }
    }

    @ViewBuilder private var actions: some View {
        let showsUndo = MonarchAiUndoBannerTokens.showsUndo(state)
        if showsUndo || auditURL != nil {
            HStack(spacing: MonarchAiUndoBannerTokens.actionSpacing) {
                if showsUndo {
                    MonarchButton(
                        labels.undo ?? MonarchAiUndoBannerStrings.undo,
                        systemImage: "arrow.counterclockwise",
                        variant: .secondary,
                        size: .sm,
                        isLoading: state == .undoing,
                        action: runUndo
                    )
                }
                if let auditURL {
                    Link(labels.audit ?? MonarchAiUndoBannerStrings.audit, destination: auditURL)
                        .buttonStyle(.monarchLink(.standalone))
                        .monarchTextStyle(.label)
                }
            }
        }
    }

    private func runUndo() {
        guard MonarchAiUndoBannerTokens.canStartUndo(state) else { return }
        undo = .undoing
        Task {
            do {
                try await onUndo()
                undo = .undone
            } catch {
                undo = .failed
            }
            if let statusMessage {
                AccessibilityNotification.Announcement(statusMessage).post()
            }
        }
    }

    private func countDown() async {
        guard isCountingDown, let left = remaining else { return }
        let clock = ContinuousClock()
        let start = clock.now
        do {
            try await Task.sleep(for: left, clock: clock)
        } catch {
            // Paused: keep what is left for when the countdown resumes.
            remaining = max(.zero, left - (clock.now - start))
            return
        }
        remaining = .zero
        guard MonarchAiUndoBannerTokens.canStartUndo(state) else { return }
        isExpired = true
        onExpire?()
        AccessibilityNotification.Announcement(labels.expired ?? MonarchAiUndoBannerStrings.expired).post()
    }
}

#if DEBUG
    #Preview("MonarchAiUndoBanner") {
        MonarchPreviewMatrix {
            VStack {
                MonarchAiUndoBanner(
                    "AI changed 2 fields",
                    description: "On the Acme Ltd account",
                    changes: [
                        MonarchAiChange("Status", before: "Open", after: "Closed"),
                        MonarchAiChange("Owner", before: "Ana", after: "Ben"),
                    ],
                    auditURL: URL(string: "https://app.example.com/audit/812"),
                    onUndo: {},
                    onDismiss: {}
                )
                MonarchAiUndoBanner("AI archived 12 tickets", onUndo: {})
            }
        }
    }
#endif
