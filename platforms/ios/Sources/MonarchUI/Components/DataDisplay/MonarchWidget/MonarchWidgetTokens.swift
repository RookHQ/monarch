import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and the rules of ``MonarchWidget``.
enum MonarchWidgetTokens {
    /// The gap between the header, the body and the footer.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the title and the description.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the icon, the title and the period.
    static let titleSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between header actions.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the placeholder lines.
    static let skeletonSpacing: CGFloat = MonarchTokens.Space.space2
    /// The height of a placeholder line at the default text size.
    static let skeletonLineHeight: CGFloat = MonarchTokens.Space.space3
    /// The widths of the three placeholder lines, as shares of the body width.
    static let skeletonWidths: [CGFloat] = [0.6, 0.9, 0.75]
    /// The weight of the title.
    static let titleWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The SF Symbol of the menu button.
    static let menuSymbol = "ellipsis"
    /// The SF Symbol after the view-all title.
    static let viewAllSymbol = "chevron.forward"
    /// The title's role.
    static var titleColor: MonarchColorRole { \.textDefault }
    /// The role of the description, the period and the footer.
    static var muted: MonarchColorRole { \.textMuted }
    /// The fill of the placeholder lines.
    static var skeletonFill: MonarchColorRole { \.bgMuted }

    /// Whether header actions move under the title: at the accessibility text sizes.
    static func stacksActions(at dynamicTypeSize: DynamicTypeSize) -> Bool {
        dynamicTypeSize.isAccessibilitySize
    }

    /// The failure message, or `nil` when the state is not a failure.
    static func errorMessage(_ state: MonarchWidgetState, labels: MonarchWidgetLabels) -> String? {
        guard case .failed(let message) = state else { return nil }
        return message ?? labels.error ?? MonarchWidgetStrings.error
    }

    /// The empty title, or `nil` when the state is not empty.
    static func emptyTitle(_ state: MonarchWidgetState, labels: MonarchWidgetLabels) -> String? {
        guard case .empty(let title) = state else { return nil }
        return title ?? labels.empty ?? MonarchWidgetStrings.empty
    }

    /// The spoken name of the loading placeholder.
    static func loadingLabel(title: String, labels: MonarchWidgetLabels) -> String {
        labels.loading?(title) ?? MonarchWidgetStrings.loading(title)
    }

    /// The spoken name of the menu button.
    static func menuLabel(title: String, labels: MonarchWidgetLabels) -> String {
        labels.menu?(title) ?? MonarchWidgetStrings.menu(title)
    }

    /// The visible title of the view-all button.
    static func viewAllTitle(labels: MonarchWidgetLabels) -> String {
        labels.viewAll ?? MonarchWidgetStrings.viewAll
    }

    /// The spoken name of the view-all button.
    static func viewAllLabel(title: String, labels: MonarchWidgetLabels) -> String {
        labels.viewAllLabel?(title) ?? MonarchWidgetStrings.viewAllLabel(title)
    }

    /// The last-updated line for a date, relative to `now`.
    static func updatedText(_ date: Date, now: Date, locale: Locale, labels: MonarchWidgetLabels) -> String {
        let relative = MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true)
        return labels.updated?(relative) ?? MonarchWidgetStrings.updated(relative)
    }

    /// Whether the footer is shown: never while loading, and only with something in it.
    static func showsFooter(state: MonarchWidgetState, footer: String?, updatedAt: Date?) -> Bool {
        state != .loading && (footer != nil || updatedAt != nil)
    }

    /// What is announced when the state changes: the failure message when
    /// the widget fails. `nil` otherwise.
    static func announcement(
        from old: MonarchWidgetState,
        to new: MonarchWidgetState,
        labels: MonarchWidgetLabels
    ) -> String? {
        guard old != new else { return nil }
        if case .failed = old { return nil }
        return errorMessage(new, labels: labels)
    }
}
