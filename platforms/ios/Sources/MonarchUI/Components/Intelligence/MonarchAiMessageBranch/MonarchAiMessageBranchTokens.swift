import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiMessageBranch``.
enum MonarchAiMessageBranchTokens {
    /// The gap between the buttons and the position.
    static let spacing: CGFloat = MonarchTokens.Space.space0_5
    /// The least width of the position, so the buttons do not move as the digits change.
    static let positionMinWidth: CGFloat = MonarchTokens.Space.space10
    /// The SF Symbol of the previous button. It mirrors in right-to-left layouts.
    static let previousSymbol = "chevron.backward"
    /// The SF Symbol of the next button. It mirrors in right-to-left layouts.
    static let nextSymbol = "chevron.forward"

    /// How many versions there are: at least one.
    static func total(_ count: Int) -> Int {
        max(1, count)
    }

    /// The version shown, 0-based, kept inside the range.
    static func current(_ index: Int, count: Int) -> Int {
        min(max(0, index), total(count) - 1)
    }

    /// Whether the switcher is shown. With fewer than two versions it is not, unless asked.
    static func isVisible(count: Int, showsSingle: Bool) -> Bool {
        total(count) >= 2 || showsSingle
    }

    /// Whether there is a version before the one shown.
    static func canGoBack(index: Int, count: Int) -> Bool {
        current(index, count: count) > 0
    }

    /// Whether there is a version after the one shown.
    static func canGoForward(index: Int, count: Int) -> Bool {
        current(index, count: count) < total(count) - 1
    }

    /// The version a step leads to, or `nil` at either end.
    static func target(index: Int, count: Int, step: Int) -> Int? {
        let next = current(index, count: count) + step
        return next >= 0 && next < total(count) ? next : nil
    }

    /// The colour role of the position.
    static func positionRole(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }
}
