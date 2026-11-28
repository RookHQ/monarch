import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let calendar: Calendar = {
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = .gmt
    return calendar
}()

/// 2026-11-14 09:00 UTC.
private let now = Date(timeIntervalSince1970: 1_794_646_800)
private let day: TimeInterval = 86_400

private let items = [
    MonarchRenewalItem(
        id: "acme",
        name: "Acme support contract",
        status: "open",
        renewalDate: now.addingTimeInterval(day * 6),
        description: "Support · Acme Inc",
        amount: 12_000,
        currencyCode: "USD",
        amountNote: "per year",
        autoRenews: false,
        probability: 60,
        owner: "Priya",
        nextAction: "Send a quote",
        nextActionDate: now.addingTimeInterval(day * 2),
        isNextActionOverdue: true
    ),
    MonarchRenewalItem(
        id: "domain",
        name: "example.com",
        status: "in_progress",
        renewalDate: now.addingTimeInterval(day * 30),
        noticeDeadline: now.addingTimeInterval(day * 20),
        amount: 40,
        currencyCode: "EUR",
        autoRenews: true
    ),
    MonarchRenewalItem(
        id: "lapsed",
        name: "Design licence with a long name that wraps onto another line",
        status: "lost",
        renewalDate: now.addingTimeInterval(-day * 3),
        outcome: "Moved to another vendor"
    ),
    MonarchRenewalItem(id: "unknown", name: "Legacy hosting", status: "open", renewalDate: nil),
    MonarchRenewalItem(id: "custom", name: "Custom status", status: "pending_review", renewalDate: nil, daysUntil: 2),
]

private let filters = [
    MonarchRenewalFilter(value: "30", label: "30 days", count: 4),
    MonarchRenewalFilter(value: "90", label: "90 days", count: 9),
    MonarchRenewalFilter(value: "all", label: "All"),
]

enum MonarchRenewalsPanelFixtures: ComponentFixtures {
    static let id = "renewals-panel"
    static let fixtures = [
        Fixture("every status, filters, totals, actions") {
            MonarchRenewalsPanel(
                items,
                description: "Renewals in the next 90 days.",
                filters: filters,
                filter: .constant("30"),
                totals: [
                    MonarchRenewalTotal(currencyCode: "USD", amount: 12_000, count: 1, estimated: 2_000),
                    MonarchRenewalTotal(currencyCode: "EUR", amount: 40, count: 1),
                ],
                unpricedCount: 2,
                locale: Locale(identifier: "en_US"),
                now: now,
                calendar: calendar,
                onSelect: { _ in },
                onAction: { _ in },
                canAct: { $0.status != "lost" },
                actions: { MonarchButton("Check now", size: .sm) {} }
            )
        },
        Fixture("read only") {
            MonarchRenewalsPanel(Array(items.prefix(2)), now: now, calendar: calendar)
        },
        Fixture("loading") {
            MonarchRenewalsPanel([], isLoading: true)
        },
        Fixture("empty") {
            MonarchRenewalsPanel([])
        },
        Fixture("error, retrying") {
            MonarchRenewalsPanel([], errorMessage: "The renewals service is down.", isRetrying: true, onRetry: {})
        },
    ]
}

@Suite("MonarchRenewalsPanel")
struct MonarchRenewalsPanelTests {
    private let texts = MonarchRenewalsPanelTokens.texts(MonarchRenewalsPanelLabels())
    private let locale = Locale(identifier: "en_US")

    @Test("An error wins over loading, and a loaded empty list is the empty state")
    func cardState() {
        #expect(MonarchRenewalsPanelTokens.cardState(isLoading: true, errorMessage: "x", isEmpty: true) == .error)
        #expect(MonarchRenewalsPanelTokens.cardState(isLoading: true, errorMessage: nil, isEmpty: true) == .loading)
        #expect(MonarchRenewalsPanelTokens.cardState(isLoading: false, errorMessage: "", isEmpty: true) == .empty)
        #expect(MonarchRenewalsPanelTokens.cardState(isLoading: false, errorMessage: nil, isEmpty: false) == .ready)
    }

    @Test("Days are counted by calendar day, and the server's count wins")
    func days() {
        #expect(MonarchRenewalsPanelTokens.daysUntil(items[0], now: now, calendar: calendar) == 6)
        #expect(MonarchRenewalsPanelTokens.daysUntil(items[2], now: now, calendar: calendar) == -3)
        #expect(MonarchRenewalsPanelTokens.daysUntil(items[3], now: now, calendar: calendar) == nil)
        #expect(MonarchRenewalsPanelTokens.daysUntil(items[4], now: now, calendar: calendar) == 2)
        let lateTonight = MonarchRenewalItem(
            id: "t",
            name: "T",
            status: "open",
            renewalDate: now.addingTimeInterval(14 * 3_600)
        )
        #expect(MonarchRenewalsPanelTokens.daysUntil(lateTonight, now: now, calendar: calendar) == 0)
    }

    @Test("Only open renewals within the window, or past, are urgent")
    func urgent() {
        #expect(MonarchRenewalsPanelTokens.isUrgent(status: "open", days: 6, within: 14))
        #expect(MonarchRenewalsPanelTokens.isUrgent(status: "in_progress", days: -2, within: 14))
        #expect(!MonarchRenewalsPanelTokens.isUrgent(status: "open", days: 30, within: 14))
        #expect(!MonarchRenewalsPanelTokens.isUrgent(status: "renewed", days: 1, within: 14))
        #expect(!MonarchRenewalsPanelTokens.isUrgent(status: "open", days: nil, within: 14))
    }

    @Test("A row has its date, day count, notice, owner and overdue step")
    func row() {
        let first = MonarchRenewalsPanelTokens.row(
            items[0],
            now: now,
            calendar: calendar,
            locale: locale,
            urgentWithinDays: 14,
            showsOwner: true,
            texts: texts
        )
        #expect(first.date == "Nov 20, 2026")
        #expect(first.when == "in 6 days")
        #expect(first.isUrgent)
        #expect(first.renewal == "Manual renewal")
        #expect(first.probability == "60% likely")
        #expect(first.owner == "Priya")
        #expect(first.nextAction == "Overdue: Send a quote · Nov 16, 2026")
        let second = MonarchRenewalsPanelTokens.row(
            items[1],
            now: now,
            calendar: calendar,
            locale: locale,
            urgentWithinDays: 14,
            showsOwner: true,
            texts: texts
        )
        #expect(second.when == "Notice by Dec 4, 2026")
        #expect(!second.isUrgent)
        #expect(second.renewal == "Auto-renews")
        #expect(second.owner == "No owner")
        #expect(second.nextAction == "No next action")
        let unknown = MonarchRenewalsPanelTokens.row(
            items[3],
            now: now,
            calendar: calendar,
            locale: locale,
            urgentWithinDays: 14,
            showsOwner: false,
            texts: texts
        )
        #expect(unknown.date == "Unknown date")
        #expect(unknown.when == nil)
        #expect(unknown.owner == nil)
        #expect(unknown.nextAction == nil)
    }

    @Test("Owners are shown only when some renewal has one")
    func showsOwner() {
        #expect(MonarchRenewalsPanelTokens.showsOwner(items))
        #expect(!MonarchRenewalsPanelTokens.showsOwner(Array(items.suffix(3))))
    }

    @Test("VoiceOver reads the urgency, date, amount and status after the name")
    func accessibility() {
        let row = MonarchRenewalsPanelTokens.row(
            items[0],
            now: now,
            calendar: calendar,
            locale: locale,
            urgentWithinDays: 14,
            showsOwner: true,
            texts: texts
        )
        let amount = MonarchRenewalsPanelTokens.amount(items[0], locale: locale, texts: texts)
        #expect(amount == "$12,000.00")
        let value = MonarchRenewalsPanelTokens.accessibilityValue(
            items[0],
            row: row,
            amount: amount,
            status: "Open",
            texts: texts
        )
        #expect(
            value
                == "Support · Acme Inc, Due soon: Nov 20, 2026, in 6 days, $12,000.00, per year, Open, 60% likely, Manual renewal, Priya, Overdue: Send a quote · Nov 16, 2026"
        )
        #expect(MonarchRenewalsPanelTokens.amount(items[3], locale: locale, texts: texts) == "No price")
    }

    @Test("Built-in statuses resolve with words, tones and icons, and the app's registry wins")
    func statuses() {
        let registry = MonarchRenewalsPanelTokens.registry(nil)
        #expect(registry.resolve("open").label == "Open")
        #expect(registry.resolve("in_progress").tone == .warning)
        #expect(registry.resolve("void").label == "No longer due")
        let tones = MonarchRenewalsPanelTokens.builtInStatuses.map(\.symbol)
        #expect(Set(tones).count == tones.count)
        let custom = MonarchRenewalsPanelTokens.registry(
            MonarchStatusRegistry(["open": MonarchStatusDefinition("Up next", tone: .info)])
        )
        #expect(custom.resolve("open").label == "Up next")
        #expect(custom.resolve("lost").label == "Lost")
    }

    @Test("Filters show their counts and totals appear when there is something to total")
    func filtersAndTotals() {
        #expect(MonarchRenewalsPanelTokens.filterLabel(filters[0], locale: locale) == "30 days (4)")
        #expect(MonarchRenewalsPanelTokens.filterLabel(filters[2], locale: locale) == "All")
        #expect(!MonarchRenewalsPanelTokens.showsTotals([], unpricedCount: 0))
        #expect(MonarchRenewalsPanelTokens.showsTotals([], unpricedCount: 1))
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchRenewalsPanelStrings.daysUntil(0) == "today")
        #expect(MonarchRenewalsPanelStrings.daysUntil(1) == "in 1 day")
        #expect(MonarchRenewalsPanelStrings.daysUntil(12) == "in 12 days")
        #expect(MonarchRenewalsPanelStrings.daysUntil(-1) == "1 day ago")
        #expect(MonarchRenewalsPanelStrings.daysUntil(-3) == "3 days ago")
        #expect(MonarchRenewalsPanelStrings.totalIn("USD", 1) == "USD · 1 renewal")
        #expect(MonarchRenewalsPanelStrings.totalIn("USD", 4) == "USD · 4 renewals")
        #expect(MonarchRenewalsPanelStrings.probability(60) == "60% likely")
        #expect(MonarchRenewalsPanelStrings.actionFor("Update", "Acme") == "Update Acme")
        #expect(MonarchRenewalsPanelStrings.estimated("$1.00") == "Includes $1.00 estimated")
        for value in [
            texts.title, texts.list, texts.filters, texts.totals, texts.unknownDate, texts.unpriced,
            texts.autoRenews, texts.manualRenewal, texts.noOwner, texts.noNextAction, texts.overdue,
            texts.dueSoon, texts.unpricedCount, texts.unpricedHint, texts.action, texts.emptyTitle,
            texts.emptyMessage, texts.errorTitle, texts.loading,
        ] {
            #expect(!value.hasPrefix("monarch."))
        }
    }
}
