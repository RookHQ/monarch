import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let april12 = Date(timeIntervalSince1970: 1_775_952_000)
private let march4 = Date(timeIntervalSince1970: 1_772_582_400)
private let utc = TimeZone.gmt
private let english = Locale(identifier: "en_US")

private let rows = [
    MonarchSubscription(
        "sub_1",
        name: "Acme Pro",
        plan: "Team plan",
        quantity: 3,
        customer: "Acme Inc",
        status: "active",
        amount: 49,
        currencyCode: "USD",
        monthly: .amount(49, currencyCode: "USD"),
        renewalDate: april12,
        cancelsAt: march4
    ),
    MonarchSubscription(
        "sub_2",
        name: "Globex Support",
        customer: "Globex",
        status: "past_due",
        amount: 1_200,
        currencyCode: "EUR",
        interval: .year,
        monthly: .notConverted,
        renewalDate: april12,
        renewsAutomatically: false
    ),
    MonarchSubscription(
        "sub_3",
        name: "Initech onboarding",
        plan: "Setup",
        status: "trial",
        amount: 500,
        currencyCode: "GBP",
        interval: nil
    ),
    MonarchSubscription(
        "sub_4",
        name: "Umbrella quarterly",
        plan: "Analytics",
        customer: "Umbrella",
        status: "paused",
        amount: nil,
        currencyCode: "USD",
        intervalCount: 3,
        monthly: .notConverted,
        changesAt: march4
    ),
]

enum MonarchSubscriptionsTableFixtures: ComponentFixtures {
    static let id = "subscriptions-table"
    static let fixtures = [
        Fixture("stacked rows with every status and monthly kind") {
            MonarchSubscriptionsTable(
                rows + [
                    MonarchSubscription("d", name: "Draft deal", status: "draft", amount: 10, currencyCode: "USD"),
                    MonarchSubscription("c", name: "Old plan", status: "cancelled", amount: 10, currencyCode: "USD"),
                    MonarchSubscription("e", name: "Expired plan", status: "expired", amount: 10, currencyCode: "USD"),
                    MonarchSubscription(
                        "u",
                        name: "Unknown status",
                        status: "on_hold",
                        amount: 10,
                        currencyCode: "USD"
                    ),
                ]
            )
        },
        Fixture("selectable rows, customer hidden") {
            MonarchSubscriptionsTable(rows, isCustomerShown: false, onSelect: { _ in })
        },
        Fixture("table at regular width, selectable") {
            MonarchSubscriptionsTable(rows, onSelect: { _ in })
                .environment(\.horizontalSizeClass, .regular)
        },
        Fixture("table without monthly or customer columns") {
            MonarchSubscriptionsTable(rows, isCustomerShown: false, isMonthlyShown: false)
                .environment(\.horizontalSizeClass, .regular)
        },
        Fixture("empty and custom registry and labels") {
            VStack {
                MonarchSubscriptionsTable([])
                MonarchSubscriptionsTable(
                    [
                        MonarchSubscription(
                            "x",
                            name: "Lagerverwaltung",
                            status: "active",
                            amount: 99,
                            currencyCode: "EUR"
                        )
                    ],
                    registry: MonarchStatusRegistry(["active": MonarchStatusDefinition("Aktiv", tone: .success)]),
                    labels: MonarchSubscriptionsTableLabels(
                        customPrice: "Individueller Preis",
                        evergreen: "Unbefristet"
                    )
                )
            }
        },
        Fixture("long names") {
            MonarchSubscriptionsTable([
                MonarchSubscription(
                    "long",
                    name: "Enterprise analytics, data retention and premium support for every regional subsidiary",
                    plan: "Enterprise analytics with unlimited dashboards",
                    quantity: 1_250,
                    customer: "The International Society of Very Long Organisation Names",
                    status: "active",
                    amount: 1_234_567.89,
                    currencyCode: "USD",
                    interval: .week,
                    intervalCount: 2,
                    renewalDate: april12
                )
            ])
        },
    ]
}

@Suite("MonarchSubscriptionsTable")
struct MonarchSubscriptionsTableTests {
    private let texts = MonarchSubscriptionsTableTokens.texts(MonarchSubscriptionsTableLabels())

    @Test("Billing periods: singular, plural, quarter and one time")
    func intervals() {
        #expect(MonarchSubscriptionsTableTokens.intervalText(.month, count: 1) == "month")
        #expect(MonarchSubscriptionsTableTokens.intervalText(.month, count: 3) == "quarter")
        #expect(MonarchSubscriptionsTableTokens.intervalText(.month, count: 6) == "6 months")
        #expect(MonarchSubscriptionsTableTokens.intervalText(.week, count: 2) == "2 weeks")
        #expect(MonarchSubscriptionsTableTokens.intervalText(.day, count: 1) == "day")
        #expect(MonarchSubscriptionsTableTokens.intervalText(.year, count: 2) == "2 years")
        #expect(MonarchSubscriptionsTableTokens.intervalText(nil, count: 1) == "one time")
        #expect(texts.priceSuffix("month") == "/ month")
        let zero = MonarchSubscription(
            "z",
            name: "Z",
            status: "active",
            amount: 1,
            currencyCode: "USD",
            intervalCount: 0
        )
        #expect(MonarchSubscriptionsTableTokens.period(zero, texts: texts) == "month")
    }

    @Test("The meta line has quantity, plan and scheduled changes")
    func meta() {
        #expect(
            MonarchSubscriptionsTableTokens.meta(rows[0], texts: texts, locale: english, timeZone: utc)
                == "3 × Team plan · cancels Mar 4, 2026"
        )
        #expect(
            MonarchSubscriptionsTableTokens.meta(rows[1], texts: texts, locale: english, timeZone: utc)
                == "Custom price"
        )
        #expect(
            MonarchSubscriptionsTableTokens.meta(rows[3], texts: texts, locale: english, timeZone: utc)
                == "Analytics · changes Mar 4, 2026"
        )
    }

    @Test("Renewal: renews, ends or evergreen")
    func renewal() {
        #expect(
            MonarchSubscriptionsTableTokens.renewal(rows[0], texts: texts, locale: english, timeZone: utc)
                == "Renews Apr 12, 2026"
        )
        #expect(
            MonarchSubscriptionsTableTokens.renewal(rows[1], texts: texts, locale: english, timeZone: utc)
                == "Ends Apr 12, 2026"
        )
        #expect(
            MonarchSubscriptionsTableTokens.renewal(rows[2], texts: texts, locale: english, timeZone: utc)
                == "Evergreen"
        )
        #expect(
            MonarchSubscriptionsTableTokens.secondaryLine("Acme", renewal: "Evergreen", showsCustomer: true)
                == "Acme · Evergreen"
        )
        #expect(
            MonarchSubscriptionsTableTokens.secondaryLine("Acme", renewal: "Evergreen", showsCustomer: false)
                == "Evergreen"
        )
        #expect(
            MonarchSubscriptionsTableTokens.secondaryLine(nil, renewal: "Evergreen", showsCustomer: true) == "Evergreen"
        )
    }

    @Test("Not converted shows only for live subscriptions; otherwise a dash")
    func monthly() {
        #expect(MonarchSubscriptionsTableTokens.monthly(rows[0]) == .amount(49, currencyCode: "USD"))
        #expect(MonarchSubscriptionsTableTokens.monthly(rows[1]) == .notConverted)
        #expect(MonarchSubscriptionsTableTokens.monthly(rows[2]) == .none)
        #expect(MonarchSubscriptionsTableTokens.monthly(rows[3]) == .none)
        #expect(MonarchSubscriptionsTableTokens.showsMonthly(nil, subscriptions: rows))
        #expect(!MonarchSubscriptionsTableTokens.showsMonthly(nil, subscriptions: [rows[2]]))
        #expect(!MonarchSubscriptionsTableTokens.showsMonthly(false, subscriptions: rows))
        #expect(MonarchSubscriptionsTableTokens.showsMonthly(true, subscriptions: [rows[2]]))
    }

    @Test("Prices are spoken per period, as one time, or as no amount")
    func spokenPrice() {
        #expect(
            MonarchSubscriptionsTableTokens.spokenPrice(rows[0], texts: texts, locale: english) == "$49.00 per month"
        )
        #expect(
            MonarchSubscriptionsTableTokens.spokenPrice(rows[1], texts: texts, locale: english) == "€1,200.00 per year"
        )
        #expect(
            MonarchSubscriptionsTableTokens.spokenPrice(rows[2], texts: texts, locale: english) == "£500.00, one time"
        )
        #expect(
            MonarchSubscriptionsTableTokens.spokenPrice(rows[3], texts: texts, locale: english)
                == "No amount per quarter"
        )
    }

    @Test("A row is read with every column, in both layouts")
    func rowLabel() {
        let label = MonarchSubscriptionsTableTokens.rowLabel(
            rows[0],
            statusLabel: "Active",
            texts: texts,
            locale: english,
            timeZone: utc,
            showsCustomer: true,
            showsMonthly: true
        )
        #expect(
            label
                == "Acme Pro, 3 × Team plan · cancels Mar 4, 2026, Acme Inc, Active, $49.00 per month, Monthly $49.00, Renews Apr 12, 2026"
        )
        let past = MonarchSubscriptionsTableTokens.rowLabel(
            rows[1],
            statusLabel: "Past due",
            texts: texts,
            locale: english,
            timeZone: utc,
            showsCustomer: false,
            showsMonthly: true
        )
        #expect(
            past
                == "Globex Support, Custom price, Past due, €1,200.00 per year, Monthly Not converted, Ends Apr 12, 2026"
        )
        #expect(
            MonarchSubscriptionsTableTokens.rowHint(rows[1], texts: texts, showsMonthly: true)
                == "No exchange rate is available, so this is left out of the monthly total."
        )
        #expect(MonarchSubscriptionsTableTokens.rowHint(rows[1], texts: texts, showsMonthly: false) == nil)
        #expect(MonarchSubscriptionsTableTokens.rowHint(rows[0], texts: texts, showsMonthly: true) == nil)
    }

    @Test("Built-in statuses have their tones and words; the app's registry wins")
    func statuses() {
        let builtIn = MonarchSubscriptionsTable.statuses
        #expect(
            builtIn.resolve("active")
                == MonarchStatusDefinition("Active", tone: .success, systemImage: "checkmark.circle.fill")
        )
        #expect(builtIn.resolve("past_due").label == "Past due")
        #expect(builtIn.resolve("past_due").tone == .warning)
        #expect(builtIn.resolve("cancelled").tone == .danger)
        #expect(builtIn.resolve("trial").tone == .info)
        #expect(
            Set(builtIn.definitions.keys) == ["draft", "trial", "active", "past_due", "paused", "cancelled", "expired"]
        )
        let symbols = MonarchSubscriptionsTableTokens.builtInStatuses.map(\.symbol)
        #expect(Set(symbols).count == symbols.count)
        let merged = MonarchSubscriptionsTableTokens.registry(
            MonarchStatusRegistry(["active": MonarchStatusDefinition("Live", tone: .success)])
        )
        #expect(merged.resolve("active").label == "Live")
        #expect(merged.resolve("paused").label == "Paused")
    }

    @Test("Rows stack at compact width and at accessibility sizes; links use the link colour")
    func layout() {
        #expect(MonarchSubscriptionsTableTokens.usesStackedRows(isRegularWidth: false, isAccessibilitySize: false))
        #expect(MonarchSubscriptionsTableTokens.usesStackedRows(isRegularWidth: true, isAccessibilitySize: true))
        #expect(!MonarchSubscriptionsTableTokens.usesStackedRows(isRegularWidth: true, isAccessibilitySize: false))
        #expect(MonarchSubscriptionsTableTokens.nameRole(isSelectable: true) == \MonarchColorTokens.textLink)
        #expect(MonarchSubscriptionsTableTokens.nameRole(isSelectable: false) == \MonarchColorTokens.textDefault)
    }

    @Test("Replacements win over the built-in strings")
    func labels() {
        let custom = MonarchSubscriptionsTableTokens.texts(
            MonarchSubscriptionsTableLabels(
                caption: "Abonnements",
                interval: { _, count in "\(count) Monat" },
                renews: { "Verlängert am \($0)" }
            )
        )
        #expect(custom.caption == "Abonnements")
        #expect(custom.interval(.month, 1) == "1 Monat")
        #expect(custom.renews("12.04.2026") == "Verlängert am 12.04.2026")
        #expect(custom.evergreen == "Evergreen")
    }

    @Test("The built-in strings resolve")
    func strings() {
        typealias S = MonarchSubscriptionsTableStrings
        #expect(S.caption == "Subscriptions")
        #expect(
            [S.subscription, S.customer, S.status, S.price, S.monthly, S.renewal]
                == ["Subscription", "Customer", "Status", "Price", "Monthly", "Renewal"]
        )
        #expect(S.customPrice == "Custom price")
        #expect(S.quantity(3) == "3 ×")
        #expect(S.interval(.year) == "year")
        #expect(S.intervals(.day, count: 5) == "5 days")
        #expect(S.quarter == "quarter")
        #expect(S.oneTime == "one time")
        #expect(S.priceSuffix("week") == "/ week")
        #expect(S.spokenPrice("$1.00", "day") == "$1.00 per day")
        #expect(S.spokenOneTimePrice("$1.00") == "$1.00, one time")
        #expect(S.cancels("x") == "cancels x")
        #expect(S.changes("x") == "changes x")
        #expect(S.renews("x") == "Renews x")
        #expect(S.ends("x") == "Ends x")
        #expect(S.evergreen == "Evergreen")
        #expect(S.notConverted == "Not converted")
        #expect(S.notConvertedHint.hasPrefix("No exchange rate"))
        #expect(S.noValue == "—")
        #expect(S.emptyTitle == "No subscriptions")
        #expect(S.emptyMessage == "Subscriptions appear here once they are created.")
        #expect(S.status("expired") == "Expired")
    }

    // The size class can be set only on iOS; the host ignores it.
    #if os(iOS)
        /// An iPad in portrait, where the regular size class is real. The
        /// test canvas is a 390-point phone, which never has a regular width.
        private static let regularWidth: CGFloat = 820

        /// The size of the view laid out `width` points wide.
        @MainActor
        private func layout(_ view: some View, width: CGFloat) -> CGSize {
            Host.layout(view.frame(width: width))
        }

        @Test("The table at regular width is shorter than stacked rows")
        @MainActor
        func tableIsShorter() {
            let width = Self.regularWidth
            let stacked = layout(MonarchSubscriptionsTable(rows), width: width)
            let table = layout(
                MonarchSubscriptionsTable(rows).environment(\.horizontalSizeClass, .regular),
                width: width
            )
            #expect(table.height < stacked.height)
        }

        // Before, a regular size class always drew the table: at 390 points
        // its six columns wrapped to 1 369 points against 400 for the blocks.
        @Test("A regular size class without room for the columns stacks the rows")
        @MainActor
        func narrowRegularStacks() {
            let stacked = Host.layout(MonarchSubscriptionsTable(rows))
            let regular = Host.layout(MonarchSubscriptionsTable(rows).environment(\.horizontalSizeClass, .regular))
            #expect(abs(regular.height - stacked.height) < 1)
        }
    #endif

    #if os(iOS)
        @Test("A selectable row keeps a 44-point touch target")
        @MainActor
        func hitTarget() {
            let short = MonarchSubscription("s", name: "A", plan: "B", status: "active", amount: 1, currencyCode: "USD")
            let size = Host.layout(MonarchSubscriptionsTable([short], onSelect: { _ in }))
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
