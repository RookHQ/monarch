import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchGoalTree``.
enum MonarchGoalTreeTokens {
    /// The space between sibling goals.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the toggle, the goal and its actions.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the lines of one goal.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between items of the heading.
    static let headingSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the bar and the percentage.
    static let progressSpacing: CGFloat = MonarchTokens.Space.space2
    /// How far children are indented under their parent: half the toggle's width.
    static let childIndent: CGFloat = MonarchAccessibility.minimumHitTarget / 2
    /// How far the add-child button is indented, so it lines up with the children's titles.
    static let addButtonIndent: CGFloat = MonarchAccessibility.minimumHitTarget + columnSpacing
    /// How far the chevron turns when the goal is open, in degrees.
    static let openRotation: Double = 90
    /// The SF Symbol of the toggle; it turns a quarter when open.
    static let chevronSymbol = "chevron.forward"
    /// The SF Symbol of the add-child button.
    static let addSymbol = "plus"
    /// The SF Symbol before an aligned goal.
    static let alignedSymbol = "link"
    /// The SF Symbol of the empty tree.
    static let emptySymbol = "target"
    /// The separator between parts of what VoiceOver reads.
    static let separator = ", "

    /// The chevron's turn: a quarter toward the children when open, mirrored
    /// in right-to-left layouts because rotation is not.
    static func chevronRotation(isExpanded: Bool, layoutDirection: LayoutDirection) -> Angle {
        MonarchDisclosureChevron.rotation(isExpanded: isExpanded, layoutDirection: layoutDirection)
    }

    /// The tone of a status badge.
    static func tone(_ status: MonarchGoalStatus) -> MonarchTone {
        switch status {
        case .notStarted: .neutral
        case .onTrack: .info
        case .atRisk: .warning
        case .offTrack: .danger
        case .done: .success
        }
    }

    /// The tone of a goal's progress bar, as the web's `BAR_TONE`: the accent while on track.
    static func barTone(_ status: MonarchGoalStatus) -> MonarchTone {
        switch status {
        case .notStarted: .neutral
        case .onTrack: .accent
        case .atRisk: .warning
        case .offTrack: .danger
        case .done: .success
        }
    }

    /// The SF Symbol of a status badge, so the status has a shape as well as a colour.
    static func symbol(_ status: MonarchGoalStatus) -> String {
        switch status {
        case .notStarted: "circle.dashed"
        case .onTrack: "chart.line.uptrend.xyaxis"
        case .atRisk: "exclamationmark.triangle"
        case .offTrack: "xmark.octagon"
        case .done: "checkmark.circle"
        }
    }

    /// The tree: top-level goals in order, and each goal's children in order.
    /// A goal whose parent is missing or itself is top-level.
    static func structure(_ goals: [MonarchGoal]) -> (roots: [MonarchGoal], children: [String: [MonarchGoal]]) {
        let ids = Set(goals.map(\.id))
        var roots: [MonarchGoal] = []
        var children: [String: [MonarchGoal]] = [:]
        for goal in goals {
            if let parent = goal.parentID, parent != goal.id, ids.contains(parent) {
                children[parent, default: []].append(goal)
            } else {
                roots.append(goal)
            }
        }
        return (roots, children)
    }

    /// The children of a goal, or none when the goal is already on the path, so a cycle cannot recurse.
    static func children(of goal: MonarchGoal, in children: [String: [MonarchGoal]], trail: Set<String>)
        -> [MonarchGoal]
    {
        trail.contains(goal.id) ? [] : children[goal.id] ?? []
    }

    /// The goals open at first: every goal with children.
    static func defaultExpanded(_ goals: [MonarchGoal]) -> Set<String> {
        Set(structure(goals).children.keys)
    }

    /// Progress as a whole percentage from 0 to 100.
    static func percent(_ progress: Double) -> Int {
        guard progress.isFinite else { return 0 }
        return Int((min(max(progress, 0), 1) * 100).rounded())
    }

    /// Progress clamped to 0 through 1 for the bar.
    static func fraction(_ progress: Double) -> Double {
        Double(percent(progress)) / 100
    }

    /// Whether the status badge is shown: always on top-level goals; on children only
    /// when off the quiet path (at risk, off track, done), unless the tree is not quiet.
    static func showsBadge(_ status: MonarchGoalStatus, depth: Int, isQuietChildren: Bool) -> Bool {
        depth == 0 || !isQuietChildren || (status != .onTrack && status != .notStarted)
    }

    /// Whether the add-child button is under the goal: top-level goals by default.
    static func addsChild(
        _ goal: MonarchGoal,
        depth: Int,
        hasHandler: Bool,
        canAddChild: ((MonarchGoal, Int) -> Bool)?
    ) -> Bool {
        guard hasHandler else { return false }
        return canAddChild?(goal, depth) ?? (depth == 0)
    }

    /// The expanded set after a goal is opened or closed.
    static func toggled(_ expanded: Set<String>, id: String, isExpanded: Bool) -> Set<String> {
        var next = expanded
        if isExpanded { next.insert(id) } else { next.remove(id) }
        return next
    }

    /// What VoiceOver reads for a goal: title, status, owner, meta, alignment and,
    /// when closed, the number of children. The progress is the value.
    static func accessibilityLabel(
        _ goal: MonarchGoal,
        childCount: Int,
        isExpanded: Bool,
        labels: MonarchGoalTreeLabels
    ) -> String {
        var parts = [goal.title, labels.status(goal.status)]
        if let owner = goal.ownerName, !owner.isEmpty { parts.append(owner) }
        if let meta = goal.meta, !meta.isEmpty { parts.append(meta) }
        if let aligned = goal.alignedTo, !aligned.isEmpty { parts.append("\(labels.alignedTo) \(aligned)") }
        if !isExpanded, childCount > 0 { parts.append(labels.childCount(childCount)) }
        return parts.joined(separator: separator)
    }

    /// The progress as VoiceOver reads it: the percentage in the locale.
    static func accessibilityValue(_ progress: Double, locale: Locale) -> String {
        fraction(progress).formatted(.percent.precision(.fractionLength(0)).locale(locale))
    }
}
