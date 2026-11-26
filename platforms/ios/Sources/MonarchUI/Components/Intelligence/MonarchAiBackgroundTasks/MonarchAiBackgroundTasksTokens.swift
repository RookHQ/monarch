import Foundation
import MonarchTokens
import SwiftUI

/// Tasks split into the tray's three sections, each sorted.
struct MonarchAiBackgroundTaskGroups: Equatable {
    /// Waiting on the person; the one that has waited longest first.
    var needsInput: [MonarchAiBackgroundTask] = []
    /// Running, then queued; oldest start first within each.
    var running: [MonarchAiBackgroundTask] = []
    /// Succeeded, failed or cancelled; most recently ended first.
    var finished: [MonarchAiBackgroundTask] = []
}

/// A row action of the tray.
enum MonarchAiBackgroundTaskAction: Equatable, CaseIterable {
    case respond, retry, open, cancel, dismiss
}

/// Component tokens and pure rules for ``MonarchAiBackgroundTasks``.
enum MonarchAiBackgroundTasksTokens {
    /// The gap between the status icon and the text of a row.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the parts of a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the lines of the row text.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the buttons of a row and of the tray button.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The padding above and below a row.
    static let rowPadding: CGFloat = MonarchTokens.Space.space1
    /// The SF Symbol of the tray button.
    static let buttonSymbol = "cpu"
    /// The SF Symbol of the dismiss button.
    static let dismissSymbol = "xmark"
    /// The SF Symbol of the empty state.
    static let emptySymbol = "tray"
    /// The tone of the "needs you" count.
    static let needsInputTone: MonarchTone = .warning
    /// The tone of the running count: accent, as on the web.
    static let runningTone: MonarchTone = .accent

    /// The colour of a task's title.
    static var title: MonarchColorRole { \.textDefault }
    /// The colour of the status line and of a step.
    static var muted: MonarchColorRole { \.textMuted }

    /// The SF Symbol of a status. A running task shows a spinner instead.
    static func symbol(_ status: MonarchAiBackgroundTaskStatus) -> String {
        switch status {
        case .queued: "clock"
        case .running: "arrow.triangle.2.circlepath"
        case .needsInput: "bell.fill"
        case .succeeded: "checkmark.circle.fill"
        case .failed: "exclamationmark.octagon.fill"
        case .cancelled: "minus.circle"
        }
    }

    /// The colour of a status icon.
    static func symbolColor(_ status: MonarchAiBackgroundTaskStatus) -> MonarchColorRole {
        switch status {
        case .queued, .cancelled: \.textMuted
        case .running: MonarchTonePalette.palette(for: runningTone).strong
        case .needsInput: \.warningStrong
        case .succeeded: \.successStrong
        case .failed: \.dangerStrong
        }
    }

    /// The colour of the detail line: the error in danger, otherwise muted.
    static func detailColor(_ status: MonarchAiBackgroundTaskStatus) -> MonarchColorRole {
        status == .failed ? \.dangerStrong : \.textMuted
    }

    // MARK: Grouping

    /// Splits tasks into "needs you", "running" and "finished" and sorts each
    /// section. Tasks without a usable time go last; ties keep the given order.
    static func groups(_ tasks: [MonarchAiBackgroundTask]) -> MonarchAiBackgroundTaskGroups {
        let indexed = Array(tasks.enumerated())
        func sorted(
            _ items: [(offset: Int, element: MonarchAiBackgroundTask)],
            by order: (MonarchAiBackgroundTask, MonarchAiBackgroundTask) -> Bool?
        ) -> [MonarchAiBackgroundTask] {
            items.sorted { lhs, rhs in order(lhs.element, rhs.element) ?? (lhs.offset < rhs.offset) }.map(\.element)
        }
        let needs = indexed.filter { $0.element.status == .needsInput }
        let running = indexed.filter { $0.element.status == .running || $0.element.status == .queued }
        let finished = indexed.filter { $0.element.status.isFinished }
        return MonarchAiBackgroundTaskGroups(
            needsInput: sorted(needs) { compare($0.startedAt, $1.startedAt, ascending: true) },
            running: sorted(running) { lhs, rhs in
                if lhs.status != rhs.status { return lhs.status == .running }
                return compare(lhs.startedAt, rhs.startedAt, ascending: true)
            },
            finished: sorted(finished) { compare($0.endedAt, $1.endedAt, ascending: false) }
        )
    }

    /// Orders two optional times; `nil` when they tie. A missing time goes last.
    private static func compare(_ lhs: Date?, _ rhs: Date?, ascending: Bool) -> Bool? {
        switch (lhs, rhs) {
        case (nil, nil): return nil
        case (nil, _): return false
        case (_, nil): return true
        case (let a?, let b?):
            if a == b { return nil }
            return ascending ? a < b : a > b
        }
    }

    // MARK: Text

    /// The name of a status.
    static func statusName(_ status: MonarchAiBackgroundTaskStatus, labels: MonarchAiBackgroundTasksLabels) -> String {
        labels.status?(status) ?? MonarchAiBackgroundTasksStrings.status(status)
    }

    /// The tray's title.
    static func title(_ labels: MonarchAiBackgroundTasksLabels) -> String {
        labels.title ?? MonarchAiBackgroundTasksStrings.title
    }

    /// A duration in short units: "42 sec", "3 min 12 sec", "1 hr 4 min". Negative reads as zero.
    static func elapsed(_ interval: TimeInterval, locale: Locale) -> String {
        let total = max(0, Int(interval.rounded(.down)))
        let hours = total / 3_600
        let minutes = (total % 3_600) / 60
        let seconds = total % 60
        func unit(_ value: Int, _ unit: UnitDuration) -> String {
            let key =
                unit == .hours
                ? "monarch.aiBackgroundTasks.unit.hours"
                : unit == .minutes ? "monarch.aiBackgroundTasks.unit.minutes" : "monarch.aiBackgroundTasks.unit.seconds"
            return MonarchStrings.string(key, table: "MonarchAiBackgroundTasks", Int64(value))
        }
        if hours > 0 { return "\(unit(hours, .hours)) \(unit(minutes, .minutes))" }
        if minutes > 0 { return "\(unit(minutes, .minutes)) \(unit(seconds, .seconds))" }
        return unit(seconds, .seconds)
    }

    /// "5 minutes ago" for a moment before `now`, in the locale.
    static func relative(_ date: Date, now: Date, locale: Locale) -> String {
        MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true)
    }

    /// The status line of a row: status, agent, then the elapsed time of a
    /// running task or the end of a finished one.
    static func meta(
        _ task: MonarchAiBackgroundTask,
        now: Date,
        locale: Locale,
        labels: MonarchAiBackgroundTasksLabels
    ) -> [String] {
        var parts = [statusName(task.status, labels: labels)]
        if let agent = task.agent, !agent.isEmpty { parts.append(agent) }
        if task.status == .running, let start = task.startedAt {
            let duration = elapsed(now.timeIntervalSince(start), locale: locale)
            parts.append(labels.elapsed?(duration) ?? MonarchAiBackgroundTasksStrings.elapsed(duration))
        }
        if task.status.isFinished, let end = task.endedAt {
            let text = relative(end, now: now, locale: locale)
            parts.append(labels.ended?(text) ?? MonarchAiBackgroundTasksStrings.ended(text))
        }
        return parts
    }

    /// The second line: the error of a failed task, the result of a succeeded
    /// one, or the step of one that has not finished.
    static func detail(_ task: MonarchAiBackgroundTask) -> String? {
        let text: String? =
            switch task.status {
            case .failed: task.error
            case .succeeded: task.result
            case .cancelled: nil
            case .queued, .running, .needsInput: task.step
            }
        guard let text, !text.isEmpty else { return nil }
        return text
    }

    /// The known completion from 0 to 1, or `nil` when unknown or forced indeterminate.
    static func fraction(_ task: MonarchAiBackgroundTask) -> Double? {
        guard let progress = task.progress, progress.isFinite, !task.isIndeterminate else { return nil }
        return min(1, max(0, progress))
    }

    /// Whether a row shows a bar: always while running, and for a waiting
    /// task only with a known amount.
    static func showsBar(_ task: MonarchAiBackgroundTask) -> Bool {
        task.status == .running || (!task.status.isFinished && fraction(task) != nil)
    }

    /// The actions a row offers, in display order, given which callbacks the app supplied.
    static func actions(
        for status: MonarchAiBackgroundTaskStatus,
        available: Set<MonarchAiBackgroundTaskAction>
    ) -> [MonarchAiBackgroundTaskAction] {
        MonarchAiBackgroundTaskAction.allCases.filter { action in
            guard available.contains(action) else { return false }
            switch action {
            case .respond: return status == .needsInput
            case .retry: return status == .failed
            case .open: return true
            case .cancel: return !status.isFinished
            case .dismiss: return status.isFinished
            }
        }
    }

    /// The visible title of an action.
    static func actionTitle(_ action: MonarchAiBackgroundTaskAction, labels: MonarchAiBackgroundTasksLabels) -> String {
        switch action {
        case .respond: labels.respond ?? MonarchAiBackgroundTasksStrings.respond
        case .retry: labels.retry ?? MonarchAiBackgroundTasksStrings.retry
        case .open: labels.open ?? MonarchAiBackgroundTasksStrings.open
        case .cancel: labels.cancel ?? MonarchAiBackgroundTasksStrings.cancel
        case .dismiss: labels.dismiss ?? MonarchAiBackgroundTasksStrings.dismiss
        }
    }

    /// The spoken name of an action: "Open: Summarize 40 contracts".
    static func actionLabel(
        _ action: MonarchAiBackgroundTaskAction,
        title: String,
        labels: MonarchAiBackgroundTasksLabels
    ) -> String {
        let name = actionTitle(action, labels: labels)
        return labels.actionLabel?(name, title) ?? MonarchAiBackgroundTasksStrings.actionLabel(name, title)
    }

    /// A section heading with its count.
    static func groupHeading(_ label: String, count: Int, locale: Locale, labels: MonarchAiBackgroundTasksLabels)
        -> String
    {
        labels.groupHeading?(label, count)
            ?? MonarchAiBackgroundTasksStrings.groupHeading(label, count.formatted(.number.locale(locale)))
    }

    /// The counts on the tray button: running (or queued when nothing runs), then needs you.
    static func buttonCounts(
        _ tasks: [MonarchAiBackgroundTask],
        locale: Locale,
        labels: MonarchAiBackgroundTasksLabels
    ) -> (activity: String?, needsInput: String?) {
        let running = tasks.filter { $0.status == .running }.count
        let queued = tasks.filter { $0.status == .queued }.count
        let needs = tasks.filter { $0.status == .needsInput }.count
        let number = { (count: Int) in count.formatted(.number.locale(locale)) }
        var activity: String?
        if running > 0 {
            activity = labels.runningCount?(running) ?? MonarchAiBackgroundTasksStrings.runningCount(number(running))
        } else if queued > 0 {
            activity = labels.queuedCount?(queued) ?? MonarchAiBackgroundTasksStrings.queuedCount(number(queued))
        }
        var attention: String?
        if needs > 0 {
            attention =
                labels.needsInputCount?(needs)
                ?? (needs == 1
                    ? MonarchAiBackgroundTasksStrings.needsInputCountOne
                    : MonarchAiBackgroundTasksStrings.needsInputCountOther(number(needs)))
        }
        return (activity, attention)
    }

    /// What VoiceOver reads for the tray button: the title and the counts.
    static func buttonLabel(_ tasks: [MonarchAiBackgroundTask], locale: Locale, labels: MonarchAiBackgroundTasksLabels)
        -> String
    {
        let counts = buttonCounts(tasks, locale: locale, labels: labels)
        return ([title(labels)] + [counts.activity, counts.needsInput].compactMap { $0 }).joined(separator: ", ")
    }

    // MARK: Announcements

    /// The tasks whose status changed into succeeded, failed or needs input
    /// since `before`. A task that was not there before is not news.
    static func news(
        before: [String: MonarchAiBackgroundTaskStatus],
        after tasks: [MonarchAiBackgroundTask]
    ) -> [MonarchAiBackgroundTask] {
        tasks.filter { task in
            guard let was = before[task.id], was != task.status else { return false }
            return task.status == .succeeded || task.status == .failed || task.status == .needsInput
        }
    }

    /// The statuses by task id, to compare with the next update.
    static func statuses(_ tasks: [MonarchAiBackgroundTask]) -> [String: MonarchAiBackgroundTaskStatus] {
        Dictionary(tasks.map { ($0.id, $0.status) }, uniquingKeysWith: { _, last in last })
    }

    /// The announcement of changed tasks, joined into one sentence list.
    static func announcement(_ tasks: [MonarchAiBackgroundTask], labels: MonarchAiBackgroundTasksLabels) -> String {
        tasks.map { task in
            let status = statusName(task.status, labels: labels)
            return labels.announce?(task.title, status) ?? MonarchAiBackgroundTasksStrings.announce(task.title, status)
        }
        .joined(separator: ". ")
    }

    /// Whether the clock should tick every second: a visible running task with a start time.
    static func ticks(_ tasks: [MonarchAiBackgroundTask]) -> Bool {
        tasks.contains { $0.status == .running && $0.startedAt != nil }
    }
}
