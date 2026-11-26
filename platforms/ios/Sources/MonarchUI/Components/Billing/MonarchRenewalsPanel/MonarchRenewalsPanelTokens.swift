import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchRenewalsPanel``.
enum MonarchRenewalsPanelTokens {
    /// The gap between the filters, the totals and the list.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between rows, around the divider.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of a row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between items on one line.
    static let inlineSpacing: CGFloat = MonarchTokens.Space.space2
    /// The padding inside a total's tile.
    static let tilePadding: CGFloat = MonarchTokens.Space.space3
    /// How far a tile's amount may shrink before it is cut, so three tiles fit a phone.
    static let tileScale: CGFloat = 0.7
    /// The SF Symbol before the date of an urgent renewal.
    static let urgentSymbol = "exclamationmark.triangle.fill"
    /// The SF Symbol before an overdue next step.
    static let overdueSymbol = "clock.badge.exclamationmark"
    /// The SF Symbol after a row that opens the renewal.
    static let disclosureSymbol = "chevron.forward"
    /// Open renewals this many days away or fewer are urgent.
    static let urgentWithinDays = 14
    /// The statuses that count as open.
    static let openStatuses: Set<String> = ["open", "in_progress"]
    /// The separator between parts of a line.
    static let separator = " · "

    /// The built-in renewal statuses, their tones and their symbols.
    static let builtInStatuses: [(key: String, tone: MonarchTone, symbol: String)] = [
        ("open", .info, "circle"),
        ("in_progress", .warning, "circle.lefthalf.filled"),
        ("renewed", .success, "checkmark.circle.fill"),
        ("lost", .danger, "xmark.circle.fill"),
        ("void", .neutral, "minus.circle"),
    ]

    /// The built-in statuses with their localised labels.
    static var statuses: MonarchStatusRegistry {
        MonarchStatusRegistry(
            Dictionary(
                uniqueKeysWithValues: builtInStatuses.map {
                    (
                        $0.key,
                        MonarchStatusDefinition(
                            MonarchRenewalsPanelStrings.status($0.key),
                            tone: $0.tone,
                            systemImage: $0.symbol
                        )
                    )
                }
            )
        )
    }

    /// The built-in statuses overridden by the app's registry.
    static func registry(_ app: MonarchStatusRegistry?) -> MonarchStatusRegistry {
        app.map { statuses.merging($0) } ?? statuses
    }

    /// What the card shows.
    static func cardState(isLoading: Bool, errorMessage: String?, isEmpty: Bool) -> MonarchSectionCardState {
        if let errorMessage, !errorMessage.isEmpty { return .error }
        if isLoading { return .loading }
        return isEmpty ? .empty : .ready
    }

    /// Whole days from `now` to the renewal, by calendar day: the server's
    /// count when given, otherwise worked out in the calendar's time zone.
    static func daysUntil(_ item: MonarchRenewalItem, now: Date, calendar: Calendar) -> Int? {
        if let days = item.daysUntil { return days }
        guard let date = item.renewalDate else { return nil }
        let from = calendar.startOfDay(for: now)
        let to = calendar.startOfDay(for: date)
        return calendar.dateComponents([.day], from: from, to: to).day
    }

    /// Whether a status counts as open.
    static func isOpen(_ status: String) -> Bool {
        openStatuses.contains(status)
    }

    /// Whether a renewal is urgent: open and due within the window, or past.
    static func isUrgent(status: String, days: Int?, within window: Int) -> Bool {
        guard isOpen(status), let days else { return false }
        return days <= window
    }

    /// The medium date, in the calendar's time zone and the locale.
    static func date(_ date: Date, locale: Locale, calendar: Calendar) -> String {
        var style = Date.FormatStyle(date: .abbreviated, time: .omitted)
        style.locale = locale
        style.calendar = calendar
        style.timeZone = calendar.timeZone
        return date.formatted(style)
    }

    /// Every built-in string, with the app's replacements applied.
    struct Texts {
        var title: String
        var list: String
        var filters: String
        var totals: String
        var daysUntil: (Int) -> String
        var noticeBy: (String) -> String
        var unknownDate: String
        var unpriced: String
        var autoRenews: String
        var manualRenewal: String
        var noOwner: String
        var noNextAction: String
        var overdue: String
        var dueSoon: String
        var probability: (Int) -> String
        var totalIn: (String, Int) -> String
        var estimated: (String) -> String
        var unpricedCount: String
        var unpricedHint: String
        var columnItem: String
        var columnRenewal: String
        var columnAmount: String
        var columnStatus: String
        var action: String
        var actionFor: (String, String) -> String
        var emptyTitle: String
        var emptyMessage: String
        var errorTitle: String
        var loading: String
    }

    /// The strings to show.
    static func texts(_ labels: MonarchRenewalsPanelLabels) -> Texts {
        typealias Strings = MonarchRenewalsPanelStrings
        return Texts(
            title: labels.title ?? Strings.title,
            list: labels.list ?? Strings.list,
            filters: labels.filters ?? Strings.filters,
            totals: labels.totals ?? Strings.totals,
            daysUntil: labels.daysUntil ?? Strings.daysUntil,
            noticeBy: labels.noticeBy ?? Strings.noticeBy,
            unknownDate: labels.unknownDate ?? Strings.unknownDate,
            unpriced: labels.unpriced ?? Strings.unpriced,
            autoRenews: labels.autoRenews ?? Strings.autoRenews,
            manualRenewal: labels.manualRenewal ?? Strings.manualRenewal,
            noOwner: labels.noOwner ?? Strings.noOwner,
            noNextAction: labels.noNextAction ?? Strings.noNextAction,
            overdue: labels.overdue ?? Strings.overdue,
            dueSoon: labels.dueSoon ?? Strings.dueSoon,
            probability: labels.probability ?? Strings.probability,
            totalIn: labels.totalIn ?? Strings.totalIn,
            estimated: labels.estimated ?? Strings.estimated,
            unpricedCount: labels.unpricedCount ?? Strings.unpricedCount,
            unpricedHint: labels.unpricedHint ?? Strings.unpricedHint,
            columnItem: labels.columnItem ?? Strings.columnItem,
            columnRenewal: labels.columnRenewal ?? Strings.columnRenewal,
            columnAmount: labels.columnAmount ?? Strings.columnAmount,
            columnStatus: labels.columnStatus ?? Strings.columnStatus,
            action: labels.action ?? Strings.action,
            actionFor: labels.actionFor ?? Strings.actionFor,
            emptyTitle: labels.emptyTitle ?? Strings.emptyTitle,
            emptyMessage: labels.emptyMessage ?? Strings.emptyMessage,
            errorTitle: labels.errorTitle ?? Strings.errorTitle,
            loading: labels.loading ?? Strings.loading
        )
    }

    /// The text of one renewal, worked out once for the view and VoiceOver.
    struct Row: Equatable {
        /// The date, or the unknown-date text.
        var date: String
        /// The notice deadline or the day count.
        var when: String?
        /// Whether the urgent icon is shown.
        var isUrgent: Bool
        /// The auto-renew note.
        var renewal: String?
        /// The probability.
        var probability: String?
        /// The owner line, when the panel shows owners.
        var owner: String?
        /// The next step line, with the overdue prefix, when the panel shows owners.
        var nextAction: String?
    }

    /// The text of a renewal.
    static func row(
        _ item: MonarchRenewalItem,
        now: Date?,
        calendar: Calendar,
        locale: Locale,
        urgentWithinDays: Int,
        showsOwner: Bool,
        texts: Texts
    ) -> Row {
        let days = now.flatMap { daysUntil(item, now: $0, calendar: calendar) } ?? item.daysUntil
        let when: String? =
            if let notice = item.noticeDeadline {
                texts.noticeBy(date(notice, locale: locale, calendar: calendar))
            } else {
                days.map(texts.daysUntil)
            }
        var nextAction: String?
        if showsOwner {
            var line = item.nextAction ?? texts.noNextAction
            if item.nextAction != nil, let due = item.nextActionDate {
                line += separator + date(due, locale: locale, calendar: calendar)
            }
            nextAction = item.isNextActionOverdue ? "\(texts.overdue): \(line)" : line
        }
        return Row(
            date: item.renewalDate.map { date($0, locale: locale, calendar: calendar) } ?? texts.unknownDate,
            when: when,
            isUrgent: isUrgent(status: item.status, days: days, within: urgentWithinDays),
            renewal: item.autoRenews.map { $0 ? texts.autoRenews : texts.manualRenewal },
            probability: item.probability.map(texts.probability),
            owner: showsOwner ? item.owner ?? texts.noOwner : nil,
            nextAction: nextAction
        )
    }

    /// Whether the owner and next step lines are shown: some renewal has one.
    static func showsOwner(_ items: [MonarchRenewalItem]) -> Bool {
        items.contains { $0.owner != nil || $0.nextAction != nil || $0.nextActionDate != nil }
    }

    /// What VoiceOver reads after the name: the description, the urgency,
    /// the date and day count, the amount, the status, the probability, the
    /// renewal note, the owner and next step and the outcome.
    static func accessibilityValue(
        _ item: MonarchRenewalItem,
        row: Row,
        amount: String,
        status: String,
        texts: Texts
    ) -> String {
        let date = row.isUrgent ? "\(texts.dueSoon): \(row.date)" : row.date
        return [
            item.description, date, row.when, amount, item.amountNote, status, row.probability,
            row.renewal, row.owner, row.nextAction, item.outcome,
        ]
        .compactMap { $0 }
        .filter { !$0.isEmpty }
        .joined(separator: ", ")
    }

    /// The amount as text: the formatted money, or the unpriced text.
    static func amount(_ item: MonarchRenewalItem, locale: Locale, texts: Texts) -> String {
        guard let amount = item.amount, let code = item.currencyCode else { return texts.unpriced }
        return MonarchMoney.formatted(amount, currencyCode: code, locale: locale)
    }

    /// The label of a filter option, with its count.
    static func filterLabel(_ filter: MonarchRenewalFilter, locale: Locale) -> String {
        guard let count = filter.count else { return filter.label }
        return "\(filter.label) (\(count.formatted(.number.locale(locale))))"
    }

    /// Whether the totals are shown.
    static func showsTotals(_ totals: [MonarchRenewalTotal], unpricedCount: Int) -> Bool {
        !totals.isEmpty || unpricedCount > 0
    }
}
