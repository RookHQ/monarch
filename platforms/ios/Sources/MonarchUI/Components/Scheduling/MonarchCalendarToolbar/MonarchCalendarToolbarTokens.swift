import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchCalendarToolbar``.
enum MonarchCalendarToolbarTokens {
    /// The type role of the range title.
    static let titleStyle: MonarchTextStyle = .heading
    /// The gap between controls.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the previous and next buttons.
    static let arrowSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The size of the buttons.
    static let buttonSize: MonarchButtonSize = .sm
    /// The SF Symbol of the previous button. A `backward` symbol, so it follows the reading direction.
    static let previousSymbol = "chevron.backward"
    /// The SF Symbol of the next button.
    static let nextSymbol = "chevron.forward"
    /// The SF Symbol after the current view's name in the switcher.
    static let switcherSymbol = "chevron.down"
    /// The colour of the title.
    static var titleColor: MonarchColorRole { \.textDefault }

    /// Whether the view switcher is shown: only when there is more than one view to choose.
    static func showsSwitcher(_ views: [MonarchCalendarToolbarView]) -> Bool {
        views.count > 1
    }

    /// The name of a view.
    static func viewName(_ view: MonarchCalendarToolbarView, labels: MonarchCalendarToolbarLabels) -> String {
        labels.viewName?(view) ?? MonarchCalendarToolbarStrings.viewName(view)
    }

    /// The spoken name of the previous button for a view, "Previous week".
    static func previousLabel(_ view: MonarchCalendarToolbarView, labels: MonarchCalendarToolbarLabels) -> String {
        let unit = labels.unit?(view) ?? MonarchCalendarToolbarStrings.unit(view)
        return labels.previous?(unit) ?? MonarchCalendarToolbarStrings.previous(unit)
    }

    /// The spoken name of the next button for a view, "Next week".
    static func nextLabel(_ view: MonarchCalendarToolbarView, labels: MonarchCalendarToolbarLabels) -> String {
        let unit = labels.unit?(view) ?? MonarchCalendarToolbarStrings.unit(view)
        return labels.next?(unit) ?? MonarchCalendarToolbarStrings.next(unit)
    }

    /// The Today button's title.
    static func today(_ labels: MonarchCalendarToolbarLabels) -> String {
        labels.today ?? MonarchCalendarToolbarStrings.today
    }

    /// The spoken name of the switcher.
    static func switcherLabel(_ labels: MonarchCalendarToolbarLabels) -> String {
        labels.view ?? MonarchCalendarToolbarStrings.view
    }

    /// The spoken name of the busy indicator.
    static func loading(_ labels: MonarchCalendarToolbarLabels) -> String {
        labels.loading ?? MonarchCalendarToolbarStrings.loading
    }
}
