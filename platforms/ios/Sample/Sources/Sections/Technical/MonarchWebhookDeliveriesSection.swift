@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchWebhookDeliveriesSection: CataloguePage {
    static let title = "Webhook deliveries"

    private struct RedeliveryFailed: Error {}

    private static func page(_ start: Int, now: Date) -> [MonarchWebhookDelivery] {
        (start..<start + 5).map { index in
            let failed = index.isMultiple(of: 3)
            return MonarchWebhookDelivery(
                id: "d\(index)",
                status: failed ? "retrying" : "succeeded",
                eventType: failed ? "invoice.payment_failed" : "invoice.paid",
                eventID: "evt_\(1_000 + index)",
                responseCode: failed ? 503 : 200,
                error: failed ? "The endpoint is unavailable." : nil,
                durationMilliseconds: failed ? 10_000 : Double(120 + index * 7),
                attempts: failed ? 3 : 1,
                date: now.addingTimeInterval(Double(-index * 900)),
                nextAttempt: failed ? now.addingTimeInterval(600) : nil
            )
        }
    }

    @State private var now = Date()
    @State private var deliveries: [MonarchWebhookDelivery] = []
    @State private var isLoadingMore = false
    @State private var failsRedelivery = false

    var body: some View {
        Toggle("Fail redeliveries", isOn: $failsRedelivery).monarchTextStyle(.body)
        MonarchWebhookDeliveries(
            deliveries: deliveries,
            eventTypes: ["invoice.paid", "invoice.payment_failed"],
            counts: ["succeeded": 1_204, "retrying": 3],
            hasMore: deliveries.count < 20,
            isLoadingMore: isLoadingMore,
            now: now,
            onRedeliver: { _ in
                try await Task.sleep(for: .seconds(1))
                if failsRedelivery { throw RedeliveryFailed() }
            },
            onLoadMore: loadMore
        )
        .onAppear {
            if deliveries.isEmpty { deliveries = Self.page(1, now: now) }
        }
        CatalogueCaption("First run")
        MonarchWebhookDeliveries(
            deliveries: [],
            isFilterable: false,
            emptyActionTitle: "Send a test event",
            onEmptyAction: {}
        )
    }

    private func loadMore() {
        guard !isLoadingMore else { return }
        isLoadingMore = true
        Task {
            try? await Task.sleep(for: .seconds(1))
            deliveries += Self.page(deliveries.count + 1, now: now)
            isLoadingMore = false
        }
    }
}
