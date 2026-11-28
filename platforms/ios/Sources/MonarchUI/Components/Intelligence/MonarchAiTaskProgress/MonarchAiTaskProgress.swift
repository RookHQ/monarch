import SwiftUI

/// A long-running assistant task in a card: the title and status, a progress
/// bar, when it started and how long is left, the steps it works through,
/// and controls to cancel, minimize or be notified when it finishes.
///
/// ```swift
/// MonarchAiTaskProgress(
///     "Summarising 40 documents",
///     status: task.status,
///     startedAt: task.startedAt,
///     steps: task.steps,
///     eta: task.secondsLeft,
///     isNotifying: $notifyWhenDone,
///     onCancel: { task.cancel() }
/// )
/// ```
///
/// The progress is the given `progress`, else the share of finished steps;
/// with neither, the bar is indeterminate while running. The steps are a
/// ``MonarchAiChainOfSteps`` built from ``MonarchAiPlanStep``. The status is
/// a ``MonarchBadge`` with an icon and its name. The start time is relative
/// to `now`, or to the current minute when `now` is `nil`.
///
/// Accessibility: the card is a container named by the title, which has the
/// header trait. The bar is named "<title> progress" with "40% complete" as
/// its value. A status change is announced ("Summarising 40 documents:
/// Done"), a failure with high priority; the first status is not. The
/// minimize button is named "Minimize"; the notify toggle is a standard
/// toggle with a selected state. Every control has a 44-point touch target.
public struct MonarchAiTaskProgress: View {
    private let title: String
    private let status: MonarchAiTaskStatus
    private let startedAt: Date
    private let steps: [MonarchAiPlanStep]
    private let progress: Double?
    private let eta: TimeInterval?
    private let isNotifying: Binding<Bool>?
    private let now: Date?
    private let labels: MonarchAiTaskProgressLabels
    private let onMinimize: (@MainActor () -> Void)?
    private let onCancel: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// A task progress card.
    ///
    /// - Parameters:
    ///   - title: What the task is, such as "Summarising 40 documents". Already localised.
    ///   - status: Where the task is.
    ///   - startedAt: When the task started.
    ///   - steps: The steps, from a plan part. None by default: a task may only have a bar.
    ///   - progress: The completion from 0 to 1. By default the share of finished steps.
    ///   - eta: The estimated time left, in seconds, shown while running.
    ///   - isNotifying: Whether "Notify me when done" is on. The toggle is shown when given.
    ///   - now: The reference time for the start text. The current minute when `nil`.
    ///   - labels: Replacements for the built-in strings.
    ///   - onMinimize: Shows a Minimize button, for example to dock the task in a tray.
    ///   - onCancel: Shows a Cancel button while the task runs or is paused.
    public init(
        _ title: String,
        status: MonarchAiTaskStatus,
        startedAt: Date,
        steps: [MonarchAiPlanStep] = [],
        progress: Double? = nil,
        eta: TimeInterval? = nil,
        isNotifying: Binding<Bool>? = nil,
        now: Date? = nil,
        labels: MonarchAiTaskProgressLabels = MonarchAiTaskProgressLabels(),
        onMinimize: (@MainActor () -> Void)? = nil,
        onCancel: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.status = status
        self.startedAt = startedAt
        self.steps = steps
        self.progress = progress
        self.eta = eta
        self.isNotifying = isNotifying
        self.now = now
        self.labels = labels
        self.onMinimize = onMinimize
        self.onCancel = onCancel
    }

    /// The content of the view.
    public var body: some View {
        let fraction = MonarchAiTaskProgressTokens.fraction(progress: progress, steps: steps, status: status)
        MonarchCard {
            VStack(alignment: .leading, spacing: MonarchAiTaskProgressTokens.sectionSpacing) {
                header
                bar(fraction: fraction)
                meta(fraction: fraction)
                if !steps.isEmpty {
                    MonarchAiChainOfSteps(steps)
                }
                if MonarchAiTaskProgressTokens.isActive(status), onCancel != nil || isNotifying != nil {
                    actions
                }
            }
        }
        .monarchLeadingEdge(MonarchAiTaskProgressTokens.edge(status))
        .accessibilityElement(children: .contain)
        .accessibilityLabel(title)
        .onChange(of: status) { previous, newStatus in
            announce(previous: previous, status: newStatus)
        }
    }

    private var header: some View {
        HStack(alignment: .top, spacing: MonarchAiTaskProgressTokens.itemSpacing) {
            VStack(alignment: .leading, spacing: MonarchAiTaskProgressTokens.headingSpacing) {
                Text(title)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(theme.colors[role: MonarchAiTaskProgressTokens.title])
                    .accessibilityAddTraits(.isHeader)
                MonarchBadge(
                    MonarchAiTaskProgressTokens.statusName(status, labels: labels),
                    tone: MonarchAiTaskProgressTokens.tone(status),
                    systemImage: MonarchAiTaskProgressTokens.symbol(status)
                )
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if let onMinimize {
                Button {
                    onMinimize()
                } label: {
                    Label(
                        labels.minimize ?? MonarchAiTaskProgressStrings.minimize,
                        systemImage: MonarchAiTaskProgressTokens.minimizeSymbol
                    )
                    .labelStyle(.iconOnly)
                }
                .buttonStyle(.monarch(.ghost, size: .sm))
            }
        }
    }

    private func bar(fraction: Double?) -> some View {
        let value = MonarchAiTaskProgressTokens.barValue(fraction: fraction, status: status)
        return ProgressView(value: value)
            .progressViewStyle(.monarchLinear)
            .accessibilityLabel(
                labels.progressLabel?(title) ?? MonarchAiTaskProgressStrings.progressLabel(title)
            )
            .accessibilityValue(
                value.map {
                    MonarchAiTaskProgressTokens.progressText(MonarchAiTaskProgressTokens.percent($0), labels: labels)
                } ?? MonarchAiTaskProgressTokens.statusName(status, labels: labels)
            )
    }

    private func meta(fraction: Double?) -> some View {
        Group {
            if let now {
                metaText(fraction: fraction, now: now)
            } else {
                TimelineView(MonarchRelativeTimeSchedule(dates: [startedAt])) { context in
                    metaText(fraction: fraction, now: context.date)
                }
            }
        }
        .monarchTextStyle(.caption)
        .foregroundStyle(theme.colors[role: MonarchAiTaskProgressTokens.muted])
    }

    private func metaText(fraction: Double?, now: Date) -> some View {
        let parts = [
            MonarchAiTaskProgressTokens.startedText(startedAt: startedAt, now: now, locale: locale, labels: labels),
            fraction.map {
                MonarchAiTaskProgressTokens.progressText(MonarchAiTaskProgressTokens.percent($0), labels: labels)
            },
            MonarchAiTaskProgressTokens.showsEta(eta, status: status)
                ? eta.map { MonarchAiTaskProgressTokens.etaText($0, locale: locale, labels: labels) } : nil,
        ]
        return Text(parts.compactMap { $0 }.joined(separator: " · "))
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var actions: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: MonarchAiTaskProgressTokens.itemSpacing) { actionButtons }
            VStack(alignment: .leading, spacing: MonarchAiTaskProgressTokens.itemSpacing) { actionButtons }
        }
        .frame(maxWidth: .infinity, alignment: .trailing)
    }

    @ViewBuilder private var actionButtons: some View {
        if let isNotifying {
            Toggle(isOn: isNotifying) {
                Label(
                    labels.notify ?? MonarchAiTaskProgressStrings.notify,
                    systemImage: MonarchAiTaskProgressTokens.notifySymbol
                )
            }
            .toggleStyle(.monarchButton(.ghost, size: .sm))
        }
        if let onCancel {
            MonarchButton(labels.cancel ?? MonarchAiTaskProgressStrings.cancel, variant: .secondary, size: .sm) {
                onCancel()
            }
        }
    }

    private func announce(previous: MonarchAiTaskStatus, status: MonarchAiTaskStatus) {
        guard
            let announcement = MonarchAiTaskProgressTokens.announcement(
                title: title,
                previous: previous,
                status: status,
                labels: labels
            )
        else { return }
        var text = AttributedString(announcement.text)
        text.accessibilitySpeechAnnouncementPriority = announcement.isUrgent ? .high : .default
        AccessibilityNotification.Announcement(text).post()
    }
}

#if DEBUG
    #Preview("MonarchAiTaskProgress") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiTaskProgress(
                    "Summarising 40 documents",
                    status: .running,
                    startedAt: Date(timeIntervalSinceNow: -300),
                    steps: [
                        MonarchAiPlanStep(id: "read", label: "Read the documents", state: .done),
                        MonarchAiPlanStep(id: "group", label: "Group them by topic", state: .active),
                        MonarchAiPlanStep(id: "write", label: "Write the summary"),
                    ],
                    eta: 180,
                    isNotifying: .constant(true),
                    onMinimize: {},
                    onCancel: {}
                )
                MonarchAiTaskProgress("Export the report", status: .failed, startedAt: Date(timeIntervalSinceNow: -60))
            }
        }
    }
#endif
