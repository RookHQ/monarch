import MonarchTokens
import SwiftUI

/// Component tokens and the rules of ``MonarchBulkActionBar``.
enum MonarchBulkActionBarTokens {
    /// Padding inside the bar.
    static let padding: CGFloat = MonarchTokens.Space.space3
    /// The gap between the count, the actions and the clear button.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The weight of the count.
    static let countWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The SF Symbol of the clear button.
    static let clearSymbol = "xmark"
    /// The SF Symbol after the More title.
    static let moreSymbol = "chevron.down"
    /// The fill of the bar.
    static var background: MonarchColorRole { \.bgSurface }
    /// The border of the bar.
    static var border: MonarchColorRole { \.borderSubtle }
    /// The colour of the count.
    static var countColor: MonarchColorRole { \.textDefault }

    /// The visible count.
    static func countText(count: Int, total: Int?, labels: MonarchBulkActionBarLabels) -> String {
        labels.selected?(count, total) ?? MonarchBulkActionBarStrings.selected(count, total: total)
    }

    /// Whether the bar is shown: while something is selected.
    static func isOpen(count: Int) -> Bool { count > 0 }

    /// The actions shown as buttons and those in the More menu.
    static func split(_ actions: [MonarchBulkAction], maxVisible: Int) -> (
        visible: [MonarchBulkAction], overflow: [MonarchBulkAction]
    ) {
        let cut = min(max(0, maxVisible), actions.count)
        return (Array(actions[..<cut]), Array(actions[cut...]))
    }

    /// Whether every action is blocked: the app is busy or an action is running.
    static func isBlocked(isBusy: Bool, busyActionID: String?) -> Bool {
        isBusy || busyActionID != nil
    }

    /// Whether one action's button is disabled. The running action stays
    /// enabled so it keeps focus while it shows progress.
    static func isDisabled(_ action: MonarchBulkAction, isBusy: Bool, busyActionID: String?) -> Bool {
        if action.isDisabled { return true }
        if busyActionID == action.id { return false }
        return isBlocked(isBusy: isBusy, busyActionID: busyActionID)
    }

    /// The button variant of an action.
    static func variant(_ action: MonarchBulkAction) -> MonarchButtonVariant {
        action.isDestructive ? .danger : .secondary
    }

    /// What is announced when the count changes: the new count, or
    /// "Selection cleared" when it drops to zero. `nil` when it is unchanged.
    static func announcement(from old: Int, to new: Int, total: Int?, labels: MonarchBulkActionBarLabels) -> String? {
        guard old != new else { return nil }
        if new > 0 { return countText(count: new, total: total, labels: labels) }
        return labels.cleared ?? MonarchBulkActionBarStrings.cleared
    }
}
