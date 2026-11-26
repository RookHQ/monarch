import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private enum WebhookSamples {
    static let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
    static let locale = Locale(identifier: "en_US")
    static var calendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .gmt
        calendar.locale = locale
        return calendar
    }

    static let delivered = MonarchWebhookDelivery(
        id: "1",
        status: "succeeded",
        eventType: "invoice.paid",
        eventID: "evt_1Q2w3E",
        destination: "https://api.example.com/hooks",
        responseCode: 200,
        durationMilliseconds: 184,
        attempts: 1,
        date: now.addingTimeInterval(-120)
    )
    static let retrying = MonarchWebhookDelivery(
        id: "2",
        status: "retrying",
        eventType: "invoice.payment_failed",
        eventID: "evt_9Z8x7C",
        responseCode: 500,
        error: "The endpoint answered with a server error.",
        durationMilliseconds: 1_204.4,
        attempts: 3,
        date: now.addingTimeInterval(-600),
        nextAttempt: now.addingTimeInterval(300)
    )
    static let pending = MonarchWebhookDelivery(
        id: "3",
        status: "pending",
        eventType: "customer.created",
        attempts: 0,
        date: now.addingTimeInterval(-30)
    )
    static let timedOut = MonarchWebhookDelivery(
        id: "4",
        status: "failed",
        eventType: "customer.deleted",
        eventID: "evt_T1med",
        error: "No response within 10 seconds.",
        attempts: 5,
        date: now.addingTimeInterval(-86_400)
    )
    static let all = [delivered, retrying, pending, timedOut]
}

enum MonarchWebhookDeliveriesFixtures: ComponentFixtures {
    static let id = "webhook-deliveries"
    static let fixtures = [
        Fixture("every built-in status with counts, filters and destination") {
            MonarchWebhookDeliveries(
                deliveries: WebhookSamples.all + [
                    MonarchWebhookDelivery(
                        id: "5",
                        status: "sending",
                        eventType: "invoice.paid",
                        attempts: 1,
                        date: WebhookSamples.now
                    ),
                    MonarchWebhookDelivery(
                        id: "6",
                        status: "gone",
                        eventType: "invoice.paid",
                        attempts: 16,
                        date: WebhookSamples.now
                    ),
                ],
                eventTypes: ["invoice.paid", "invoice.payment_failed"],
                showsDestination: true,
                counts: ["succeeded": 1_204, "retrying": 3, "failed": 1],
                now: WebhookSamples.now,
                onRedeliver: { _ in }
            )
        },
        Fixture("loading") {
            MonarchWebhookDeliveries(deliveries: [], isLoading: true)
        },
        Fixture("load error") {
            MonarchWebhookDeliveries(deliveries: [], loadError: "Deliveries did not load.", onRetry: {})
        },
        Fixture("first run with an action, no filter bar") {
            MonarchWebhookDeliveries(
                deliveries: [],
                isFilterable: false,
                emptyActionTitle: "Send a test event",
                onEmptyAction: {}
            )
        },
        Fixture("filtered to nothing") {
            MonarchWebhookDeliveries(
                deliveries: WebhookSamples.all,
                filters: .constant(MonarchWebhookDeliveryFilters(query: "evt_missing")),
                now: WebhookSamples.now
            )
        },
        Fixture("paging footer, loading and failed") {
            VStack {
                MonarchWebhookDeliveries(
                    deliveries: [WebhookSamples.delivered],
                    hasMore: true,
                    isLoadingMore: true,
                    now: WebhookSamples.now,
                    onLoadMore: {}
                )
                MonarchWebhookDeliveries(
                    deliveries: [WebhookSamples.retrying],
                    isFilterable: false,
                    hasMore: true,
                    isLoadMoreFailed: true,
                    now: WebhookSamples.now,
                    onLoadMore: {}
                )
            }
        },
        Fixture("custom status and long event names") {
            MonarchWebhookDeliveries(
                deliveries: [
                    MonarchWebhookDelivery(
                        id: "x",
                        status: "discarded",
                        eventType: "subscription.schedule.phase_advanced.with_a_very_long_name",
                        eventID: "evt_0123456789abcdefghijklmnop",
                        attempts: 2,
                        date: WebhookSamples.now
                    )
                ],
                statuses: MonarchStatusRegistry(["discarded": MonarchStatusDefinition("Discarded", tone: .neutral)]),
                now: WebhookSamples.now
            )
        },
    ]
}

@Suite("MonarchWebhookDeliveries")
@MainActor
struct MonarchWebhookDeliveriesTests {
    private let text = MonarchWebhookDeliveriesText(labels: MonarchWebhookDeliveriesLabels())
    private let locale = WebhookSamples.locale

    @Test("Filters match the status, the event type, an exact event ID or part of the type")
    func filtering() {
        let all = WebhookSamples.all
        #expect(MonarchWebhookDeliveryFilters().apply(to: all) == all)
        #expect(!MonarchWebhookDeliveryFilters().isActive)
        #expect(MonarchWebhookDeliveryFilters(status: "retrying").apply(to: all).map(\.id) == ["2"])
        #expect(MonarchWebhookDeliveryFilters(eventType: "invoice.paid").apply(to: all).map(\.id) == ["1"])
        #expect(MonarchWebhookDeliveryFilters(query: " EVT_9z8X7c ").apply(to: all).map(\.id) == ["2"])
        #expect(MonarchWebhookDeliveryFilters(query: "evt_9Z8").apply(to: all).isEmpty)
        #expect(MonarchWebhookDeliveryFilters(query: "invoice").apply(to: all).map(\.id) == ["1", "2"])
        #expect(MonarchWebhookDeliveryFilters(status: "failed", query: "customer").apply(to: all).map(\.id) == ["4"])
        #expect(MonarchWebhookDeliveryFilters(query: "  ").isActive == false)
        #expect(MonarchWebhookDeliveryFilters(eventType: "x").isActive)
    }

    @Test("Built-in statuses have their own label, tone and symbol; the app's are merged over them")
    func registry() {
        let registry = MonarchWebhookDeliveriesTokens.registry(nil)
        #expect(registry.resolve("succeeded").label == "Delivered")
        #expect(registry.resolve("succeeded").tone == .success)
        #expect(registry.resolve("retrying").tone == .warning)
        #expect(registry.resolve("failed").tone == .danger)
        #expect(registry.resolve("gone").label == "Gave up")
        #expect(registry.resolve("pending").tone == .neutral)
        #expect(registry.resolve("sending").tone == .info)
        let symbols = MonarchWebhookDeliveriesTokens.builtInOrder.compactMap { registry.resolve($0).systemImage }
        #expect(Set(symbols).count == MonarchWebhookDeliveriesTokens.builtInOrder.count)
        let merged = MonarchWebhookDeliveriesTokens.registry(
            MonarchStatusRegistry([
                "failed": MonarchStatusDefinition("Bounced", tone: .danger),
                "discarded": MonarchStatusDefinition("Discarded", tone: .neutral),
            ])
        )
        #expect(merged.resolve("failed").label == "Bounced")
        #expect(merged.resolve("succeeded").label == "Delivered")
        #expect(
            MonarchWebhookDeliveriesTokens.statusOptions(nil, registry: merged)
                == ["pending", "sending", "succeeded", "retrying", "failed", "gone", "discarded"]
        )
        #expect(MonarchWebhookDeliveriesTokens.statusOptions(["failed"], registry: merged) == ["failed"])
    }

    @Test("Pending and sending deliveries cannot be redelivered unless the app says so")
    func redeliverable() {
        #expect(MonarchWebhookDeliveriesTokens.canRedeliver(WebhookSamples.delivered, rule: nil))
        #expect(MonarchWebhookDeliveriesTokens.canRedeliver(WebhookSamples.retrying, rule: nil))
        #expect(!MonarchWebhookDeliveriesTokens.canRedeliver(WebhookSamples.pending, rule: nil))
        #expect(!MonarchWebhookDeliveriesTokens.canRedeliver(WebhookSamples.delivered, rule: { $0.status == "failed" }))
        #expect(MonarchWebhookDeliveriesTokens.canRedeliver(WebhookSamples.timedOut, rule: { $0.status == "failed" }))
    }

    @Test("The response line")
    func outcome() {
        func outcome(_ delivery: MonarchWebhookDelivery) -> String? {
            MonarchWebhookDeliveriesTokens.outcome(delivery, text: text, locale: locale)
        }
        #expect(outcome(WebhookSamples.delivered) == "HTTP 200 · 184 ms")
        #expect(outcome(WebhookSamples.retrying) == "HTTP 500 · 1,204 ms")
        #expect(outcome(WebhookSamples.pending) == "Not attempted yet")
        #expect(outcome(WebhookSamples.timedOut) == nil)
        let silent = MonarchWebhookDelivery(
            id: "s",
            status: "failed",
            eventType: "a",
            attempts: 1,
            date: WebhookSamples.now
        )
        #expect(outcome(silent) == "No response")
        #expect(
            MonarchWebhookDeliveriesTokens.outcome(
                WebhookSamples.retrying,
                text: text,
                locale: Locale(identifier: "de_DE")
            ) == "HTTP 500 · 1.204 ms"
        )
    }

    @Test("The time line shows the next attempt for pending and retrying deliveries")
    func when() {
        func when(_ delivery: MonarchWebhookDelivery) -> String {
            MonarchWebhookDeliveriesTokens.when(
                delivery,
                now: WebhookSamples.now,
                text: text,
                locale: locale,
                calendar: WebhookSamples.calendar
            )
        }
        #expect(when(WebhookSamples.retrying) == "Next attempt in 5 minutes")
        #expect(when(WebhookSamples.delivered) == "2 minutes ago")
        // The shared relative-time rule (the web's `numeric: 'auto'`) writes one day as "yesterday".
        #expect(when(WebhookSamples.timedOut) == "yesterday")
    }

    @Test("Counts follow the filter order and use the status names")
    func counts() {
        let registry = MonarchWebhookDeliveriesTokens.registry(nil)
        let order = MonarchWebhookDeliveriesTokens.statusOptions(nil, registry: registry)
        let line = MonarchWebhookDeliveriesTokens.countLine(
            ["failed": 2, "succeeded": 1_204, "custom": 1],
            order: order,
            registry: registry,
            text: text,
            locale: locale
        )
        #expect(line == "1,204 delivered · 2 failed · 1 custom")
        #expect(
            MonarchWebhookDeliveriesTokens.countLine([:], order: order, registry: registry, text: text, locale: locale)
                == nil
        )
    }

    @Test("A row is read as one sentence")
    func rowLabel() {
        let registry = MonarchWebhookDeliveriesTokens.registry(nil)
        func label(_ delivery: MonarchWebhookDelivery, destination: Bool = false) -> String {
            MonarchWebhookDeliveriesTokens.rowLabel(
                delivery,
                registry: registry,
                now: WebhookSamples.now,
                text: text,
                locale: locale,
                calendar: WebhookSamples.calendar,
                showsDestination: destination
            )
        }
        #expect(
            label(WebhookSamples.retrying)
                == "invoice.payment_failed, evt_9Z8x7C, Retrying, HTTP 500 · 1,204 ms, The endpoint answered with a server error., 3 attempts, Next attempt in 5 minutes"
        )
        #expect(
            label(WebhookSamples.delivered, destination: true)
                == "invoice.paid, evt_1Q2w3E, Delivered, https://api.example.com/hooks, HTTP 200 · 184 ms, 1 attempt, 2 minutes ago"
        )
        #expect(
            label(WebhookSamples.pending) == "customer.created, Pending, Not attempted yet, 0 attempts, 30 seconds ago"
        )
    }

    @Test("Loading wins over an error, an error over emptiness")
    func content() {
        typealias T = MonarchWebhookDeliveriesTokens
        #expect(T.content(isLoading: true, hasError: true, rowCount: 3, isFiltered: true) == .loading)
        #expect(T.content(isLoading: false, hasError: true, rowCount: 3, isFiltered: false) == .failed)
        #expect(T.content(isLoading: false, hasError: false, rowCount: 0, isFiltered: true) == .filteredEmpty)
        #expect(T.content(isLoading: false, hasError: false, rowCount: 0, isFiltered: false) == .empty)
        #expect(T.content(isLoading: false, hasError: false, rowCount: 2, isFiltered: true) == .rows)
    }

    @Test("Replacements win over the built-in strings")
    func overrides() {
        let custom = MonarchWebhookDeliveriesText(
            labels: MonarchWebhookDeliveriesLabels(
                redeliver: "Send again",
                httpStatus: { "Status \($0)" },
                duration: { "\(Int($0))ms" },
                attempts: { "×\($0)" },
                count: { "\($1): \($0)" }
            )
        )
        #expect(custom(\.redeliver, "redeliver") == "Send again")
        #expect(
            MonarchWebhookDeliveriesTokens.outcome(WebhookSamples.delivered, text: custom, locale: locale)
                == "Status 200 · 184ms"
        )
        #expect(custom.attempts(3, locale: locale) == "×3")
        #expect(custom.count(4, status: "Failed", locale: locale) == "Failed: 4")
    }

    @Test("The built-in strings resolve")
    func strings() {
        let keys: [(KeyPath<MonarchWebhookDeliveriesLabels, String?>, String, String)] = [
            (\.label, "label", "Webhook deliveries"),
            (\.anyStatus, "anyStatus", "Any status"),
            (\.search, "search", "Find by event ID"),
            (\.redeliver, "redeliver", "Redeliver"),
            (\.redeliverQueued, "redeliverQueued", "Redelivery queued."),
            (\.redeliverFailed, "redeliverFailed", "Could not redeliver. Try again."),
            (\.emptyTitle, "emptyTitle", "No deliveries yet"),
            (\.filteredTitle, "filteredTitle", "No deliveries match"),
            (\.detailsHint, "detailsHint", "Shows the delivery details."),
            (\.detailsTitle, "detailsTitle", "Delivery"),
            (\.close, "close", "Close"),
        ]
        for (path, key, expected) in keys {
            #expect(text(path, key) == expected)
        }
        #expect(text.attempts(1, locale: locale) == "1 attempt")
        #expect(text.nextAttempt("in 1 hour") == "Next attempt in 1 hour")
    }
}
