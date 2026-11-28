import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchAiTaskProgress``.
enum MonarchAiTaskProgressTokens {
    /// The gap between the sections of the card.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between items in a row.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the title and the status.
    static let headingSpacing: CGFloat = MonarchTokens.Space.space1
    /// The SF Symbol of the minimize button.
    static let minimizeSymbol = "chevron.down"
    /// The SF Symbol of the notify toggle.
    static let notifySymbol = "bell"
    /// The colour role of the title.
    static var title: MonarchColorRole { \.textDefault }
    /// The colour role of the meta line.
    static var muted: MonarchColorRole { \.textMuted }

    /// The tone of a status badge, as on the web: running is accent.
    static func tone(_ status: MonarchAiTaskStatus) -> MonarchTone {
        switch status {
        case .running: .ai
        case .paused: .warning
        case .done: .success
        case .failed: .danger
        case .cancelled: .neutral
        }
    }

    /// The edge along the card's leading side: the AI border, or the status's own once it has finished.
    static func edge(_ status: MonarchAiTaskStatus) -> MonarchColorRole {
        switch status {
        case .done: \.successBorder
        case .failed: \.dangerBorder
        case .paused: \.warningBorder
        default: \.aiBorder
        }
    }

    /// The SF Symbol of a status badge, so the status never depends on colour.
    static func symbol(_ status: MonarchAiTaskStatus) -> String {
        switch status {
        case .running: "arrow.triangle.2.circlepath"
        case .paused: "pause.circle"
        case .done: "checkmark.circle"
        case .failed: "xmark.octagon"
        case .cancelled: "slash.circle"
        }
    }

    /// Whether the task can still be cancelled or watched: running or paused.
    static func isActive(_ status: MonarchAiTaskStatus) -> Bool {
        status == .running || status == .paused
    }

    /// The completion from 0 to 1: the given progress, else the share of
    /// finished steps, else 1 when done; `nil` when unknown.
    static func fraction(progress: Double?, steps: [MonarchAiPlanStep], status: MonarchAiTaskStatus) -> Double? {
        let derived: Double?
        if let progress, progress.isFinite {
            derived = progress
        } else if !steps.isEmpty {
            derived =
                Double(steps.filter { MonarchAiChainOfStepsTokens.isFinished($0.state) }.count) / Double(steps.count)
        } else {
            derived = status == .done ? 1 : nil
        }
        return derived.map { min(1, max(0, $0)) }
    }

    /// The value of the bar: the fraction, or indeterminate (`nil`) while
    /// running without one, else empty.
    static func barValue(fraction: Double?, status: MonarchAiTaskStatus) -> Double? {
        if let fraction { return fraction }
        return status == .running ? nil : 0
    }

    /// The whole percentage of a fraction.
    static func percent(_ fraction: Double) -> Int {
        Int((fraction * 100).rounded())
    }

    /// The name of a status.
    static func statusName(_ status: MonarchAiTaskStatus, labels: MonarchAiTaskProgressLabels) -> String {
        labels.status?(status) ?? MonarchAiTaskProgressStrings.status(status)
    }

    /// The completion text: "40% complete".
    static func progressText(_ percent: Int, labels: MonarchAiTaskProgressLabels) -> String {
        labels.progressValue?(percent) ?? MonarchAiTaskProgressStrings.progressValue(percent)
    }

    /// The start text, "Started 5 minutes ago", relative to `now`.
    static func startedText(
        startedAt: Date,
        now: Date,
        locale: Locale,
        labels: MonarchAiTaskProgressLabels
    ) -> String {
        let relative = MonarchRelativeTime.string(startedAt, now: now, locale: locale, allowsFuture: true)
        return labels.started?(relative) ?? MonarchAiTaskProgressStrings.started(relative)
    }

    /// The time-left text, from an estimate in seconds: under a minute,
    /// whole minutes under an hour, then hours to one decimal.
    static func etaText(_ eta: TimeInterval, locale: Locale, labels: MonarchAiTaskProgressLabels) -> String {
        if let custom = labels.eta { return custom(eta) }
        if eta < 60 { return MonarchAiTaskProgressStrings.etaUnderMinute }
        let minutes = max(1, Int((eta / 60).rounded()))
        if minutes == 1 { return MonarchAiTaskProgressStrings.etaOneMinute }
        if minutes < 60 { return MonarchAiTaskProgressStrings.etaMinutes(minutes) }
        let hours = (Double(minutes) / 60).formatted(.number.precision(.fractionLength(0...1)).locale(locale))
        return MonarchAiTaskProgressStrings.etaHours(hours)
    }

    /// Whether the estimate is shown: while running, with a valid estimate.
    static func showsEta(_ eta: TimeInterval?, status: MonarchAiTaskStatus) -> Bool {
        guard let eta, eta.isFinite, eta >= 0 else { return false }
        return status == .running
    }

    /// The announcement for a status change, and whether it is urgent (a
    /// failure). `nil` for the first status and for no change.
    static func announcement(
        title: String,
        previous: MonarchAiTaskStatus?,
        status: MonarchAiTaskStatus,
        labels: MonarchAiTaskProgressLabels
    ) -> (text: String, isUrgent: Bool)? {
        guard let previous, previous != status else { return nil }
        let name = statusName(status, labels: labels)
        let text = labels.announce?(title, name) ?? MonarchAiTaskProgressStrings.announce(title, status: name)
        return (text, status == .failed)
    }
}
