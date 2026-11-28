import MonarchTokens
import SwiftUI

/// The tray of work agents do in the background: tasks grouped into "Needs
/// you", "Running" and "Finished", each with its status, step, progress,
/// elapsed or end time and actions.
///
/// It is a `List` with sections, made to be shown in a sheet from a
/// ``MonarchAiBackgroundTasksButton``, or pushed as a screen. It only reports
/// and asks: every action is a callback. Work that must outlive the app is a
/// system notification or a Live Activity in the product; `onStatusChange`
/// is where to post one.
///
/// ```swift
/// .sheet(isPresented: $showsTasks) {
///     NavigationStack {
///         MonarchAiBackgroundTasks(tasks: tasks, onOpen: { open($0) }, onCancel: { cancel($0) })
///     }
///     .presentationDetents([.medium, .large])
/// }
/// ```
///
/// Accessibility: section headings are headers with their counts. Each row
/// reads its title and status line as one element, then its actions, which
/// are named with the task ("Open: Summarize 40 contracts"); Cancel and
/// Dismiss are also swipe actions. A task that succeeds, fails or starts
/// needing input is announced once. Status is an icon and a word, never
/// colour alone. Without `now` the elapsed time ticks every second while a
/// task runs, or every minute with Reduce Motion.
@_spi(Experimental) public struct MonarchAiBackgroundTasks: View {
    private let tasks: [MonarchAiBackgroundTask]
    private let now: Date?
    private let labels: MonarchAiBackgroundTasksLabels
    private let onOpen: (@MainActor (MonarchAiBackgroundTask) -> Void)?
    private let onCancel: (@MainActor (MonarchAiBackgroundTask) -> Void)?
    private let onRetry: (@MainActor (MonarchAiBackgroundTask) -> Void)?
    private let onRespond: (@MainActor (MonarchAiBackgroundTask) -> Void)?
    private let onDismiss: (@MainActor (MonarchAiBackgroundTask) -> Void)?
    private let onClearFinished: (@MainActor ([MonarchAiBackgroundTask]) -> Void)?
    private let onStatusChange: (@MainActor (MonarchAiBackgroundTask) -> Void)?

    @Environment(\.locale) private var locale
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.monarchAiPopoverCard) private var isCard
    @Environment(\.monarchTheme) private var theme

    /// A tray of background tasks.
    ///
    /// - Parameters:
    ///   - tasks: Every background task, in any order; the tray groups and sorts them.
    ///   - now: The reference time for elapsed and end times. Pass it for previews and tests;
    ///     without it the tray reads the clock.
    ///   - labels: Replacements for the built-in strings.
    ///   - onOpen: Shows Open on every task, for example to go to its conversation.
    ///   - onCancel: Shows Cancel on queued, running and waiting tasks.
    ///   - onRetry: Shows Retry on failed tasks.
    ///   - onRespond: Shows Respond on tasks that need input.
    ///   - onDismiss: Shows Dismiss on finished tasks.
    ///   - onClearFinished: Shows "Clear finished" while there are finished tasks; receives them.
    ///   - onStatusChange: Runs when a task succeeds, fails or starts needing input, for
    ///     example to post a local notification.
    public init(
        tasks: [MonarchAiBackgroundTask],
        now: Date? = nil,
        labels: MonarchAiBackgroundTasksLabels = MonarchAiBackgroundTasksLabels(),
        onOpen: (@MainActor (MonarchAiBackgroundTask) -> Void)? = nil,
        onCancel: (@MainActor (MonarchAiBackgroundTask) -> Void)? = nil,
        onRetry: (@MainActor (MonarchAiBackgroundTask) -> Void)? = nil,
        onRespond: (@MainActor (MonarchAiBackgroundTask) -> Void)? = nil,
        onDismiss: (@MainActor (MonarchAiBackgroundTask) -> Void)? = nil,
        onClearFinished: (@MainActor ([MonarchAiBackgroundTask]) -> Void)? = nil,
        onStatusChange: (@MainActor (MonarchAiBackgroundTask) -> Void)? = nil
    ) {
        self.tasks = tasks
        self.now = now
        self.labels = labels
        self.onOpen = onOpen
        self.onCancel = onCancel
        self.onRetry = onRetry
        self.onRespond = onRespond
        self.onDismiss = onDismiss
        self.onClearFinished = onClearFinished
        self.onStatusChange = onStatusChange
    }

    private var tickInterval: TimeInterval {
        MonarchAiBackgroundTasksTokens.ticks(tasks) && !reduceMotion ? 1 : 60
    }

    /// The content of the view.
    public var body: some View {
        Group {
            if let now {
                list(now: now)
            } else {
                TimelineView(.periodic(from: .now, by: tickInterval)) { context in
                    list(now: context.date)
                }
            }
        }
        .navigationTitle(isCard ? "" : MonarchAiBackgroundTasksTokens.title(labels))
        .onChange(of: MonarchAiBackgroundTasksTokens.statuses(tasks)) { before, _ in
            let news = MonarchAiBackgroundTasksTokens.news(before: before, after: tasks)
            guard !news.isEmpty else { return }
            AccessibilityNotification.Announcement(MonarchAiBackgroundTasksTokens.announcement(news, labels: labels))
                .post()
            for task in news { onStatusChange?(task) }
        }
    }

    private var available: Set<MonarchAiBackgroundTaskAction> {
        var set = Set<MonarchAiBackgroundTaskAction>()
        if onOpen != nil { set.insert(.open) }
        if onCancel != nil { set.insert(.cancel) }
        if onRetry != nil { set.insert(.retry) }
        if onRespond != nil { set.insert(.respond) }
        if onDismiss != nil { set.insert(.dismiss) }
        return set
    }

    private func list(now: Date) -> some View {
        let groups = MonarchAiBackgroundTasksTokens.groups(tasks)
        if isCard { return AnyView(card(groups, now: now)) }
        return AnyView(plainList(groups, now: now))
    }

    private func card(_ groups: MonarchAiBackgroundTaskGroups, now: Date) -> some View {
        VStack(alignment: .leading, spacing: MonarchTokens.Space.space3) {
            HStack {
                Text(MonarchAiBackgroundTasksTokens.title(labels))
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
                Spacer()
                if let onClearFinished, !groups.finished.isEmpty {
                    Button(labels.clearFinished ?? MonarchAiBackgroundTasksStrings.clearFinished) {
                        onClearFinished(groups.finished)
                    }
                    .buttonStyle(.plain)
                    .monarchTextStyle(.caption, weight: .semibold)
                    .foregroundStyle(theme.colors.textDefault)
                    .monarchMinimumHitTarget()
                }
            }
            if tasks.isEmpty {
                MonarchEmptyState(
                    labels.empty ?? MonarchAiBackgroundTasksStrings.empty,
                    systemImage: MonarchAiBackgroundTasksTokens.emptySymbol,
                    size: .sm
                )
                .frame(maxWidth: .infinity)
            }
            cardGroup(labels.needsInputGroup ?? MonarchAiBackgroundTasksStrings.needsInputGroup, groups.needsInput, now)
            cardGroup(labels.runningGroup ?? MonarchAiBackgroundTasksStrings.runningGroup, groups.running, now)
            cardGroup(labels.finishedGroup ?? MonarchAiBackgroundTasksStrings.finishedGroup, groups.finished, now)
        }
        .padding(MonarchTokens.Space.space4)
        .background(theme.colors.bgSurface, in: RoundedRectangle(cornerRadius: MonarchTokens.Radius.lg))
        .overlay(
            RoundedRectangle(cornerRadius: MonarchTokens.Radius.lg).stroke(theme.colors.borderDefault, lineWidth: 1)
        )
    }

    @ViewBuilder
    private func cardGroup(_ title: String, _ tasks: [MonarchAiBackgroundTask], _ now: Date) -> some View {
        if !tasks.isEmpty {
            VStack(alignment: .leading, spacing: MonarchTokens.Space.space2) {
                Text(
                    MonarchAiBackgroundTasksTokens.groupHeading(
                        title,
                        count: tasks.count,
                        locale: locale,
                        labels: labels
                    )
                )
                .monarchTextStyle(.caption, weight: .semibold)
                .foregroundStyle(theme.colors.textMuted)
                .accessibilityAddTraits(.isHeader)
                ForEach(tasks) { task in
                    MonarchAiBackgroundTasksRow(
                        task: task,
                        now: now,
                        labels: labels,
                        actions: MonarchAiBackgroundTasksTokens.actions(for: task.status, available: available),
                        perform: { perform($0, on: task) }
                    )
                }
            }
        }
    }

    private func plainList(_ groups: MonarchAiBackgroundTaskGroups, now: Date) -> some View {
        List {
            if tasks.isEmpty {
                MonarchEmptyState(
                    labels.empty ?? MonarchAiBackgroundTasksStrings.empty,
                    systemImage: MonarchAiBackgroundTasksTokens.emptySymbol,
                    size: .sm
                )
                .frame(maxWidth: .infinity)
            }
            section(
                labels.needsInputGroup ?? MonarchAiBackgroundTasksStrings.needsInputGroup,
                tasks: groups.needsInput,
                now: now
            )
            section(
                labels.runningGroup ?? MonarchAiBackgroundTasksStrings.runningGroup,
                tasks: groups.running,
                now: now
            )
            section(
                labels.finishedGroup ?? MonarchAiBackgroundTasksStrings.finishedGroup,
                tasks: groups.finished,
                now: now,
                clears: true
            )
        }
        #if os(iOS)
            .listStyle(.insetGrouped)
        #endif
        .monarchCanvas()
    }

    @ViewBuilder
    private func section(_ title: String, tasks: [MonarchAiBackgroundTask], now: Date, clears: Bool = false)
        -> some View
    {
        if !tasks.isEmpty {
            Section {
                ForEach(tasks) { task in
                    MonarchAiBackgroundTasksRow(
                        task: task,
                        now: now,
                        labels: labels,
                        actions: MonarchAiBackgroundTasksTokens.actions(for: task.status, available: available),
                        perform: { perform($0, on: task) }
                    )
                }
            } header: {
                HStack {
                    Text(
                        MonarchAiBackgroundTasksTokens.groupHeading(
                            title,
                            count: tasks.count,
                            locale: locale,
                            labels: labels
                        )
                    )
                    .accessibilityAddTraits(.isHeader)
                    Spacer()
                    if clears, let onClearFinished {
                        Button(labels.clearFinished ?? MonarchAiBackgroundTasksStrings.clearFinished) {
                            onClearFinished(tasks)
                        }
                        .monarchMinimumHitTarget()
                    }
                }
            }
        }
    }

    private func perform(_ action: MonarchAiBackgroundTaskAction, on task: MonarchAiBackgroundTask) {
        switch action {
        case .respond: onRespond?(task)
        case .retry: onRetry?(task)
        case .open: onOpen?(task)
        case .cancel: onCancel?(task)
        case .dismiss: onDismiss?(task)
        }
    }
}

#if DEBUG
    #Preview("MonarchAiBackgroundTasks") {
        let now = Date(timeIntervalSince1970: 1_794_650_000)
        MonarchPreviewMatrix {
            NavigationStack {
                MonarchAiBackgroundTasks(
                    tasks: [
                        MonarchAiBackgroundTask(
                            "Summarize 40 contracts",
                            id: "1",
                            status: .running,
                            agent: "Research agent",
                            progress: 0.3,
                            step: "Reading contract 12 of 40",
                            startedAt: now.addingTimeInterval(-192)
                        ),
                        MonarchAiBackgroundTask("Draft replies", id: "2", status: .needsInput, step: "Which tone?"),
                        MonarchAiBackgroundTask(
                            "Clean the CRM",
                            id: "3",
                            status: .failed,
                            endedAt: now.addingTimeInterval(-300),
                            error: "The CRM did not answer."
                        ),
                    ],
                    now: now,
                    onOpen: { _ in },
                    onCancel: { _ in },
                    onRetry: { _ in },
                    onDismiss: { _ in }
                )
            }
        }
    }
#endif
