import SwiftUI

/// A running stopwatch for time tracking: a panel with Start and Stop, or
/// a compact chip for a toolbar, with "I stopped earlier" and a confirmed
/// Discard.
///
/// ```swift
/// MonarchRunningTimer(
///     startedAt: entry.startedAt,
///     label: "ENG-142",
///     onStart: { try await timer.start() },
///     onStop: { try await timer.stop() },
///     onStopAt: { try await timer.stop(at: $0) },
///     onDiscard: { try await timer.discard() }
/// ) {
///     ProjectPicker(selection: $project)
/// }
/// ```
///
/// The timer runs while `startedAt` is set; the app owns that state and
/// changes it when an action succeeds. Each action is async: its button
/// shows a spinner until it returns, and a thrown error is shown and
/// announced. The panel shows a warning once the timer has run longer than
/// `warnAfter`. Showing the timer outside the app (a Live Activity or a
/// notification) stays in the product.
///
/// Accessibility: the clock is one element, read as the label and the
/// time in words with the state as its value; it updates frequently and
/// does not announce every second. Starting and stopping are announced.
/// The indicator's stop and menu buttons have names and 44-point touch
/// targets. The state is a word and an icon as well as a colour.
public struct MonarchRunningTimer<Fields: View>: View {
    private let startedAt: Date?
    private let label: String?
    private let variant: MonarchRunningTimerVariant
    private let warnAfter: TimeInterval?
    private let isStartDisabled: Bool
    private let isPending: Bool
    private let showsWhenStopped: Bool
    private let now: Date?
    private let labels: MonarchRunningTimerLabels
    private let onStart: (@MainActor () async throws -> Void)?
    private let onStop: (@MainActor () async throws -> Void)?
    private let onStopAt: (@MainActor (Date) async throws -> Void)?
    private let onDiscard: (@MainActor () async throws -> Void)?
    private let onOpen: (@MainActor () -> Void)?
    private let fields: Fields

    @State private var busy: MonarchRunningTimerAction?
    @State private var failure: String?
    @State private var isConfirmingDiscard = false
    @State private var stopSheetNow: Date?

    /// A running timer with fields for the next entry.
    ///
    /// - Parameters:
    ///   - startedAt: When the running timer started, or `nil` when no
    ///     timer runs.
    ///   - label: What is being timed, such as an issue key. Shown under
    ///     the panel's clock and in the indicator.
    ///   - variant: `panel` or `indicator`.
    ///   - warnAfter: Shows a warning once the timer has run this many
    ///     seconds. 12 hours by default; `nil` turns it off.
    ///   - isStartDisabled: Whether starting is not allowed yet, for
    ///     example until a required project is chosen.
    ///   - isPending: Whether an action runs outside the component; the
    ///     buttons show as busy.
    ///   - showsWhenStopped: Whether the indicator stays visible, at
    ///     0:00:00, while no timer runs.
    ///   - now: A fixed reference time, which also stops the clock. Use it
    ///     for a snapshot, for example in a preview.
    ///   - labels: Replacements for the built-in strings.
    ///   - onStart: Starts a timer. Adds Start to the stopped panel.
    ///   - onStop: Stops the timer and logs the time. Adds Stop.
    ///   - onStopAt: Stops the timer at an earlier time. Adds "I stopped
    ///     earlier", which asks for the time.
    ///   - onDiscard: Throws the running time away. Adds Discard, confirmed
    ///     first.
    ///   - onOpen: Opens what is being timed. Makes the indicator's clock a
    ///     button.
    ///   - fields: Fields for the entry being timed, such as a project
    ///     picker. Shown in the panel only.
    public init(
        startedAt: Date?,
        label: String? = nil,
        variant: MonarchRunningTimerVariant = .panel,
        warnAfter: TimeInterval? = 12 * 3600,
        isStartDisabled: Bool = false,
        isPending: Bool = false,
        showsWhenStopped: Bool = false,
        now: Date? = nil,
        labels: MonarchRunningTimerLabels = MonarchRunningTimerLabels(),
        onStart: (@MainActor () async throws -> Void)? = nil,
        onStop: (@MainActor () async throws -> Void)? = nil,
        onStopAt: (@MainActor (Date) async throws -> Void)? = nil,
        onDiscard: (@MainActor () async throws -> Void)? = nil,
        onOpen: (@MainActor () -> Void)? = nil,
        @ViewBuilder fields: () -> Fields
    ) {
        self.startedAt = startedAt
        self.label = label
        self.variant = variant
        self.warnAfter = warnAfter
        self.isStartDisabled = isStartDisabled
        self.isPending = isPending
        self.showsWhenStopped = showsWhenStopped
        self.now = now
        self.labels = labels
        self.onStart = onStart
        self.onStop = onStop
        self.onStopAt = onStopAt
        self.onDiscard = onDiscard
        self.onOpen = onOpen
        self.fields = fields()
    }

    /// The content of the view.
    public var body: some View {
        let ticking = MonarchRunningTimerTokens.isTicking(startedAt: startedAt, now: now)
        TimelineView(MonarchRunningTimerSchedule(startedAt: ticking ? startedAt : nil)) { context in
            content(now: now ?? max(context.date, .now))
        }
        .onChange(of: startedAt != nil) { _, isRunning in
            if !isRunning {
                isConfirmingDiscard = false
                stopSheetNow = nil
            }
            failure = nil
            AccessibilityNotification.Announcement(
                MonarchRunningTimerTokens.stateText(isRunning: isRunning, labels: labels)
            )
            .post()
        }
        .alert(
            MonarchRunningTimerStrings.discardTitle(labels),
            isPresented: $isConfirmingDiscard,
            actions: {
                Button(MonarchRunningTimerStrings.discardConfirm(labels), role: .destructive) {
                    if let onDiscard { run(.discard, onDiscard) }
                }
                Button(MonarchRunningTimerStrings.cancel(labels), role: .cancel) {}
            },
            message: {
                Text(MonarchRunningTimerStrings.discardMessage(labels))
            }
        )
        .sheet(isPresented: isStoppingEarlier) {
            if let startedAt, let sheetNow = stopSheetNow {
                MonarchRunningTimerStopSheet(
                    startedAt: startedAt,
                    now: sheetNow,
                    isSaving: busy == .stopAt,
                    failure: failure,
                    labels: labels,
                    onCancel: { stopSheetNow = nil },
                    onSave: { date in
                        guard let onStopAt else { return }
                        run(.stopAt, { try await onStopAt(date) }, onSuccess: { stopSheetNow = nil })
                    }
                )
            }
        }
    }

    private var isStoppingEarlier: Binding<Bool> {
        Binding(
            get: { stopSheetNow != nil },
            set: { isPresented in
                if !isPresented, busy != .stopAt { stopSheetNow = nil }
            }
        )
    }

    @ViewBuilder private func content(now: Date) -> some View {
        let seconds = startedAt.map { MonarchRunningTimerTokens.elapsedSeconds(startedAt: $0, now: now) } ?? 0
        let isLong = MonarchRunningTimerTokens.isLongRunning(startedAt: startedAt, now: now, warnAfter: warnAfter)
        switch variant {
        case .panel:
            MonarchRunningTimerPanel(
                isRunning: startedAt != nil,
                seconds: seconds,
                label: label,
                isLongRunning: isLong,
                warnAfter: warnAfter,
                failure: stopSheetNow == nil ? failure : nil,
                labels: labels,
                actions: actions(now: now),
                fields: fields
            )
        case .indicator:
            if startedAt != nil || showsWhenStopped {
                MonarchRunningTimerIndicator(
                    isRunning: startedAt != nil,
                    seconds: seconds,
                    label: label ?? MonarchRunningTimerStrings.defaultLabel(labels),
                    isLongRunning: isLong,
                    failure: stopSheetNow == nil ? failure : nil,
                    labels: labels,
                    actions: actions(now: now)
                )
            }
        }
    }

    private func actions(now: Date) -> MonarchRunningTimerActions {
        let isRunning = startedAt != nil
        let isBusy = isPending || busy != nil
        var start: (@MainActor () -> Void)?
        var stop: (@MainActor () -> Void)?
        var stopEarlier: (@MainActor () -> Void)?
        var discard: (@MainActor () -> Void)?
        if !isRunning, let onStart {
            start = { run(.start, onStart) }
        }
        if isRunning, let onStop {
            stop = { run(.stop, onStop) }
        }
        if isRunning, onStopAt != nil {
            stopEarlier = {
                failure = nil
                stopSheetNow = now
            }
        }
        if isRunning, onDiscard != nil {
            discard = { isConfirmingDiscard = true }
        }
        return MonarchRunningTimerActions(
            busy: busy,
            isPending: isPending,
            start: start,
            isStartEnabled: !isStartDisabled && !(isBusy && busy != .start),
            stop: stop,
            stopEarlier: stopEarlier,
            discard: discard,
            open: isRunning ? onOpen : nil
        )
    }

    private func run(
        _ action: MonarchRunningTimerAction,
        _ work: @escaping @MainActor () async throws -> Void,
        onSuccess: (@MainActor () -> Void)? = nil
    ) {
        guard busy == nil else { return }
        busy = action
        failure = nil
        Task {
            do {
                try await work()
                onSuccess?()
            } catch {
                let message = MonarchRunningTimerTokens.failureMessage(for: error, labels: labels)
                failure = message
                AccessibilityNotification.Announcement(message).post()
            }
            busy = nil
        }
    }
}

extension MonarchRunningTimer where Fields == EmptyView {
    /// A running timer without fields.
    ///
    /// - Parameters:
    ///   - startedAt: When the running timer started, or `nil` when no
    ///     timer runs.
    ///   - label: What is being timed, such as an issue key. Shown under
    ///     the panel's clock and in the indicator.
    ///   - variant: `panel` or `indicator`.
    ///   - warnAfter: Shows a warning once the timer has run this many
    ///     seconds. 12 hours by default; `nil` turns it off.
    ///   - isStartDisabled: Whether starting is not allowed yet.
    ///   - isPending: Whether an action runs outside the component; the
    ///     buttons show as busy.
    ///   - showsWhenStopped: Whether the indicator stays visible, at
    ///     0:00:00, while no timer runs.
    ///   - now: A fixed reference time, which also stops the clock.
    ///   - labels: Replacements for the built-in strings.
    ///   - onStart: Starts a timer. Adds Start to the stopped panel.
    ///   - onStop: Stops the timer and logs the time. Adds Stop.
    ///   - onStopAt: Stops the timer at an earlier time. Adds "I stopped
    ///     earlier".
    ///   - onDiscard: Throws the running time away. Adds Discard, confirmed
    ///     first.
    ///   - onOpen: Opens what is being timed. Makes the indicator's clock a
    ///     button.
    public init(
        startedAt: Date?,
        label: String? = nil,
        variant: MonarchRunningTimerVariant = .panel,
        warnAfter: TimeInterval? = 12 * 3600,
        isStartDisabled: Bool = false,
        isPending: Bool = false,
        showsWhenStopped: Bool = false,
        now: Date? = nil,
        labels: MonarchRunningTimerLabels = MonarchRunningTimerLabels(),
        onStart: (@MainActor () async throws -> Void)? = nil,
        onStop: (@MainActor () async throws -> Void)? = nil,
        onStopAt: (@MainActor (Date) async throws -> Void)? = nil,
        onDiscard: (@MainActor () async throws -> Void)? = nil,
        onOpen: (@MainActor () -> Void)? = nil
    ) {
        self.init(
            startedAt: startedAt,
            label: label,
            variant: variant,
            warnAfter: warnAfter,
            isStartDisabled: isStartDisabled,
            isPending: isPending,
            showsWhenStopped: showsWhenStopped,
            now: now,
            labels: labels,
            onStart: onStart,
            onStop: onStop,
            onStopAt: onStopAt,
            onDiscard: onDiscard,
            onOpen: onOpen,
            fields: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchRunningTimer") {
        MonarchPreviewMatrix {
            let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
            VStack(alignment: .leading) {
                MonarchRunningTimer(startedAt: nil, onStart: {})
                MonarchRunningTimer(
                    startedAt: now.addingTimeInterval(-249),
                    label: "ENG-142 Fix the export",
                    now: now,
                    onStop: {},
                    onStopAt: { _ in },
                    onDiscard: {}
                )
                MonarchRunningTimer(startedAt: now.addingTimeInterval(-50_000), now: now, onStop: {})
                MonarchRunningTimer(
                    startedAt: now.addingTimeInterval(-249),
                    label: "ENG-142",
                    variant: .indicator,
                    now: now,
                    onStop: {},
                    onDiscard: {},
                    onOpen: {}
                )
                MonarchRunningTimer(startedAt: nil, variant: .indicator, showsWhenStopped: true)
            }
        }
    }
#endif
