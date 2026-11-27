import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchEscalationPolicyEditor``.
enum MonarchEscalationPolicyEditorTokens {
    /// The gap between levels.
    static let listSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the parts of a level.
    static let levelSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap inside a line of text with an icon.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The fewest minutes a level waits.
    static let minimumMinutes = 1
    /// The step of the delay's stepper.
    static let stepMinutes = 5
    /// The add button's symbol.
    static let addSymbol = "plus"
    /// The remove button's symbol.
    static let removeSymbol = "xmark"
    /// The symbol of the no-targets warning and of a problem.
    static let warningSymbol = "exclamationmark.triangle"
    /// The warning when there are no targets.
    static var warningText: MonarchColorRole { \.warningStrong }
    /// The text of a problem.
    static var problemText: MonarchColorRole { \.dangerStrong }
    /// Muted text: the summary, the description and the max note.
    static var mutedText: MonarchColorRole { \.textMuted }

    /// What is wrong with each level: no targets, or (with cumulative
    /// timing) not later than the level before. Keyed by level id.
    static func problems(
        _ levels: [MonarchEscalationLevel],
        timing: MonarchEscalationPolicyTiming
    ) -> [String: MonarchEscalationLevelProblem] {
        var out: [String: MonarchEscalationLevelProblem] = [:]
        var previous: MonarchEscalationLevel?
        for level in levels {
            if level.targets.isEmpty {
                out[level.id] = .targets
            } else if timing == .cumulative, let previous, level.afterMinutes <= previous.afterMinutes {
                out[level.id] = .order
            }
            previous = level
        }
        return out
    }

    /// The delay of a new level: the last level's plus the step with
    /// cumulative timing, else the step.
    static func newLevelMinutes(
        after levels: [MonarchEscalationLevel],
        timing: MonarchEscalationPolicyTiming,
        step: Int
    ) -> Int {
        timing == .cumulative ? (levels.last?.afterMinutes ?? 0) + step : step
    }

    /// The chosen targets in the order of the targets list; ids that are not targets are dropped.
    static func ordered(_ chosen: Set<String>, targets: [MonarchEscalationTarget]) -> [String] {
        targets.map(\.id).filter { chosen.contains($0) }
    }

    /// The names a level notifies, joined with commas, or "nobody yet".
    static func names(
        _ level: MonarchEscalationLevel,
        targets: [MonarchEscalationTarget],
        labels: MonarchEscalationPolicyEditorLabels
    ) -> String {
        guard !level.targets.isEmpty else { return labels.nobody ?? MonarchEscalationPolicyEditorStrings.nobody }
        return level.targets.map { id in targets.first { $0.id == id }?.label ?? id }.joined(separator: ", ")
    }

    /// The visible delay: "15m", "1h 30m", "1d 2h".
    static func shortDuration(_ minutes: Int) -> String {
        MonarchDurationMath.format(minutes, largestUnit: .days)
    }

    /// The spoken delay: "1 hour, 30 minutes".
    static func spokenDuration(_ minutes: Int, locale: Locale) -> String {
        Duration.seconds(max(0, minutes) * 60).formatted(
            .units(allowed: [.days, .hours, .minutes], width: .wide).locale(locale)
        )
    }

    /// The summary of a level: "After 15m, notify On-call, #ops".
    static func summary(
        duration: String,
        names: String,
        labels: MonarchEscalationPolicyEditorLabels
    ) -> String {
        labels.summary?(duration, names) ?? MonarchEscalationPolicyEditorStrings.summary(duration, names)
    }

    /// The title of a level.
    static func levelTitle(_ position: Int, labels: MonarchEscalationPolicyEditorLabels) -> String {
        labels.level?(position) ?? MonarchEscalationPolicyEditorStrings.level(position)
    }

    /// The text of a problem.
    static func problemText(_ problem: MonarchEscalationLevelProblem, labels: MonarchEscalationPolicyEditorLabels)
        -> String
    {
        labels.problem?(problem) ?? MonarchEscalationPolicyEditorStrings.problem(problem)
    }

    /// Whether a level can be removed: only above the fewest levels.
    static func canRemove(count: Int, minLevels: Int) -> Bool { count > minLevels }

    /// Whether a level can be added: only below the most levels.
    static func canAdd(count: Int, maxLevels: Int) -> Bool { count < maxLevels }
}
