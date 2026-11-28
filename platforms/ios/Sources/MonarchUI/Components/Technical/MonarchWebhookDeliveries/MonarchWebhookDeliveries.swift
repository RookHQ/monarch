import Foundation
import SwiftUI

/// The delivery log of a webhook endpoint, or any outbound integration: status, event, response,
/// attempts and time per delivery, with filters, paging, a details sheet and Redeliver.
///
/// ```swift
/// ScrollView {
///     MonarchWebhookDeliveries(
///         deliveries: model.deliveries,
///         eventTypes: ["invoice.paid", "invoice.failed"],
///         hasMore: model.cursor != nil,
///         isLoadingMore: model.isLoadingPage,
///         onRedeliver: { delivery in try await api.redeliver(delivery.id) },
///         onLoadMore: { model.loadNextPage() }
///     )
///     .padding()
/// }
/// ```
///
/// The iPhone pattern for the web's table: a list of rows, each a button that opens a sheet with
/// every fact (the log-drawer recipe) and a Redeliver button. Filters are menu pickers for the
/// status and the event type and a field that finds an event ID. The view filters the deliveries
/// itself unless `filtersOnServer` is set, in which case the app sends the filters with its next
/// request. Data and actions come from the app; nothing is fetched here. Put the view in a
/// `ScrollView`.
///
/// Accessibility: each row is one button read as a sentence ("invoice.paid, evt_42, Failed,
/// HTTP 500 · 1,204 ms, 3 attempts, 5 minutes ago") with the hint "Shows the delivery details.",
/// and Redeliver as a custom action. A redelivery's outcome is announced. States are badge words
/// with a symbol, never colour alone. Touch targets are at least 44 points.
@_spi(Experimental) public struct MonarchWebhookDeliveries: View {
    private let deliveries: [MonarchWebhookDelivery]
    private let externalFilters: Binding<MonarchWebhookDeliveryFilters>?
    private let statuses: MonarchStatusRegistry?
    private let filterStatuses: [String]?
    private let eventTypes: [String]
    private let isFilterable: Bool
    private let filtersOnServer: Bool
    private let showsDestination: Bool
    private let counts: [String: Int]
    private let isLoading: Bool
    private let loadError: String?
    private let hasMore: Bool
    private let isLoadingMore: Bool
    private let isLoadMoreFailed: Bool
    private let now: Date?
    private let emptyActionTitle: String?
    private let text: MonarchWebhookDeliveriesText
    private let canRedeliver: ((MonarchWebhookDelivery) -> Bool)?
    private let onRedeliver: (@MainActor (MonarchWebhookDelivery) async throws -> Void)?
    private let onSelect: (@MainActor (MonarchWebhookDelivery) -> Void)?
    private let onRetry: (@MainActor () -> Void)?
    private let onEmptyAction: (@MainActor () -> Void)?
    private let onLoadMore: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.calendar) private var calendar
    @State private var ownFilters = MonarchWebhookDeliveryFilters()
    @State private var selectedID: String?
    @State private var pending: Set<String> = []
    @State private var results: [String: Bool] = [:]

    /// A webhook delivery log.
    ///
    /// - Parameters:
    ///   - deliveries: The deliveries, newest first.
    ///   - filters: The filter values, when the app keeps them. Kept inside otherwise.
    ///   - statuses: Status labels, tones and symbols, merged over the built-in ones.
    ///   - filterStatuses: The statuses the status filter offers. By default every known status.
    ///   - eventTypes: The event types the event type filter offers. The filter is hidden when empty.
    ///   - isFilterable: Whether the filter bar shows.
    ///   - filtersOnServer: Whether the app filters: the deliveries are shown as given.
    ///   - showsDestination: Whether each delivery's destination shows.
    ///   - counts: Totals per status key, shown as "12 delivered · 3 retrying".
    ///   - isLoading: The first page is loading.
    ///   - loadError: The list failed to load: the message, shown with Try again.
    ///   - hasMore: There is another page.
    ///   - isLoadingMore: The next page is loading.
    ///   - isLoadMoreFailed: Loading the next page failed.
    ///   - now: The time relative times are measured from. By default the time of drawing.
    ///   - emptyActionTitle: A button in the first-run empty state, such as "Send a test event".
    ///   - labels: Replacements for the built-in strings.
    ///   - canRedeliver: Which deliveries can be sent again. By default all but pending and sending.
    ///   - onRedeliver: Sends a delivery again. Redeliver is hidden without it. The button is busy
    ///     until it returns; a thrown error is announced as a failure.
    ///   - onSelect: Opens a delivery in the app's own screen instead of the built-in sheet.
    ///   - onRetry: Runs when Try again is activated after a load error.
    ///   - onEmptyAction: Runs when the empty state's button is activated.
    ///   - onLoadMore: Loads the next page. No footer without it.
    public init(
        deliveries: [MonarchWebhookDelivery],
        filters: Binding<MonarchWebhookDeliveryFilters>? = nil,
        statuses: MonarchStatusRegistry? = nil,
        filterStatuses: [String]? = nil,
        eventTypes: [String] = [],
        isFilterable: Bool = true,
        filtersOnServer: Bool = false,
        showsDestination: Bool = false,
        counts: [String: Int] = [:],
        isLoading: Bool = false,
        loadError: String? = nil,
        hasMore: Bool = false,
        isLoadingMore: Bool = false,
        isLoadMoreFailed: Bool = false,
        now: Date? = nil,
        emptyActionTitle: String? = nil,
        labels: MonarchWebhookDeliveriesLabels = MonarchWebhookDeliveriesLabels(),
        canRedeliver: ((MonarchWebhookDelivery) -> Bool)? = nil,
        onRedeliver: (@MainActor (MonarchWebhookDelivery) async throws -> Void)? = nil,
        onSelect: (@MainActor (MonarchWebhookDelivery) -> Void)? = nil,
        onRetry: (@MainActor () -> Void)? = nil,
        onEmptyAction: (@MainActor () -> Void)? = nil,
        onLoadMore: (@MainActor () -> Void)? = nil
    ) {
        self.deliveries = deliveries
        self.externalFilters = filters
        self.statuses = statuses
        self.filterStatuses = filterStatuses
        self.eventTypes = eventTypes
        self.isFilterable = isFilterable
        self.filtersOnServer = filtersOnServer
        self.showsDestination = showsDestination
        self.counts = counts
        self.isLoading = isLoading
        self.loadError = loadError
        self.hasMore = hasMore
        self.isLoadingMore = isLoadingMore
        self.isLoadMoreFailed = isLoadMoreFailed
        self.now = now
        self.emptyActionTitle = emptyActionTitle
        self.text = MonarchWebhookDeliveriesText(labels: labels)
        self.canRedeliver = canRedeliver
        self.onRedeliver = onRedeliver
        self.onSelect = onSelect
        self.onRetry = onRetry
        self.onEmptyAction = onEmptyAction
        self.onLoadMore = onLoadMore
    }

    private var filters: Binding<MonarchWebhookDeliveryFilters> {
        externalFilters ?? $ownFilters
    }

    private var registry: MonarchStatusRegistry {
        MonarchWebhookDeliveriesTokens.registry(statuses)
    }

    private var context: MonarchWebhookDeliveriesContext {
        MonarchWebhookDeliveriesContext(
            registry: registry,
            text: text,
            now: now ?? Date(),
            locale: locale,
            calendar: calendar,
            showsDestination: showsDestination
        )
    }

    private var rows: [MonarchWebhookDelivery] {
        filtersOnServer ? deliveries : filters.wrappedValue.apply(to: deliveries)
    }

    /// The content of the view.
    public var body: some View {
        let context = context
        let rows = rows
        VStack(alignment: .leading, spacing: MonarchWebhookDeliveriesTokens.spacing) {
            if isFilterable { filterBar }
            if let line = MonarchWebhookDeliveriesTokens.countLine(
                counts,
                order: MonarchWebhookDeliveriesTokens.statusOptions(filterStatuses, registry: context.registry),
                registry: context.registry,
                text: text,
                locale: locale
            ) {
                Text(line)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchWebhookDeliveriesTokens.muted])
            }
            switch MonarchWebhookDeliveriesTokens.content(
                isLoading: isLoading,
                hasError: loadError != nil,
                rowCount: rows.count,
                isFiltered: filters.wrappedValue.isActive
            ) {
            case .loading:
                MonarchLoadingState(text(\.loading, "loading"), variant: .rows)
            case .failed:
                MonarchInlineErrorRetry(loadError ?? text(\.loadError, "loadError"), onRetry: onRetry ?? {})
            case .filteredEmpty:
                MonarchEmptyState(
                    text(\.filteredTitle, "filteredTitle"),
                    message: text(\.filteredMessage, "filteredMessage"),
                    kind: .filtered,
                    size: .sm,
                    clearFiltersLabel: text(\.clearFilters, "clearFilters"),
                    onClearFilters: { filters.wrappedValue = MonarchWebhookDeliveryFilters() }
                )
            case .empty:
                MonarchEmptyState(
                    text(\.emptyTitle, "emptyTitle"),
                    message: text(\.emptyMessage, "emptyMessage"),
                    size: .sm,
                    actionTitle: emptyActionTitle,
                    action: onEmptyAction
                )
            case .rows:
                rowList(rows, context: context)
                if let onLoadMore {
                    MonarchLoadMore(
                        hasMore: hasMore,
                        isLoading: isLoadingMore,
                        isFailed: isLoadMoreFailed,
                        loadedCount: rows.count,
                        showsCount: false,
                        onLoadMore: onLoadMore
                    )
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .sheet(item: selectedBinding) { delivery in
            MonarchWebhookDeliveryDetail(
                delivery: delivery,
                context: context,
                canRedeliver: MonarchWebhookDeliveriesTokens.canRedeliver(delivery, rule: canRedeliver),
                isRedelivering: pending.contains(delivery.id),
                result: results[delivery.id],
                onRedeliver: redeliverAction(delivery)
            )
        }
    }

    // MARK: Filters

    @ViewBuilder private var filterBar: some View {
        let options = MonarchWebhookDeliveriesTokens.statusOptions(filterStatuses, registry: registry)
        VStack(alignment: .leading, spacing: MonarchWebhookDeliveriesTokens.filterSpacing) {
            // As on the web: the status, the event type and the event ID, each as a full-width field.
            Group {
                MonarchSelectField(
                    text(\.status, "status"),
                    selection: filters.status,
                    valueText: filters.status.wrappedValue.map { registry.resolve($0).label }
                        ?? text(\.anyStatus, "anyStatus"),
                    labelVisibility: .hidden
                ) {
                    Text(text(\.anyStatus, "anyStatus")).tag(String?.none)
                    ForEach(options, id: \.self) { key in
                        Text(registry.resolve(key).label).tag(String?.some(key))
                    }
                }
                if !eventTypes.isEmpty {
                    MonarchSelectField(
                        text(\.eventTypeFilter, "eventTypeFilter"),
                        selection: filters.eventType,
                        valueText: filters.eventType.wrappedValue ?? text(\.anyEventType, "anyEventType"),
                        labelVisibility: .hidden
                    ) {
                        Text(text(\.anyEventType, "anyEventType")).tag(String?.none)
                        ForEach(eventTypes, id: \.self) { type in
                            Text(type).tag(String?.some(type))
                        }
                    }
                }
            }
            MonarchQueryBuilderValueField(
                label: text(\.search, "search"),
                text: filters.query,
                prompt: text(\.searchPrompt, "searchPrompt"),
                isNumber: false
            )
            .autocorrectionDisabled()
            if filters.wrappedValue.isActive {
                Button(text(\.clearFilters, "clearFilters")) {
                    filters.wrappedValue = MonarchWebhookDeliveryFilters()
                }
                .buttonStyle(.monarch(.ghost, size: .sm))
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(text(\.label, "label"))
    }

    // MARK: Rows

    private func rowList(_ rows: [MonarchWebhookDelivery], context: MonarchWebhookDeliveriesContext) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(rows) { delivery in
                let redeliverable = MonarchWebhookDeliveriesTokens.canRedeliver(delivery, rule: canRedeliver)
                MonarchWebhookDeliveryRow(
                    delivery: delivery,
                    context: context,
                    canRedeliver: redeliverable,
                    onOpen: { open(delivery) },
                    onRedeliver: redeliverAction(delivery)
                )
                if delivery.id != rows.last?.id {
                    Rectangle()
                        .fill(theme.colors[role: MonarchWebhookDeliveriesTokens.separator])
                        .frame(height: MonarchShapes.borderWidth)
                        .accessibilityHidden(true)
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(text(\.label, "label"))
    }

    private var selectedBinding: Binding<MonarchWebhookDelivery?> {
        Binding(
            get: { selectedID.flatMap { id in deliveries.first { $0.id == id } } },
            set: { selectedID = $0?.id }
        )
    }

    private func open(_ delivery: MonarchWebhookDelivery) {
        if let onSelect {
            onSelect(delivery)
        } else {
            selectedID = delivery.id
        }
    }

    /// The redeliver action of a delivery, or `nil` when the app gives no `onRedeliver`.
    private func redeliverAction(_ delivery: MonarchWebhookDelivery) -> (@MainActor () -> Void)? {
        guard onRedeliver != nil else { return nil }
        return { redeliver(delivery) }
    }

    private func redeliver(_ delivery: MonarchWebhookDelivery) {
        guard let onRedeliver, !pending.contains(delivery.id) else { return }
        pending.insert(delivery.id)
        results[delivery.id] = nil
        let queued = text(\.redeliverQueued, "redeliverQueued")
        let failed = text(\.redeliverFailed, "redeliverFailed")
        Task { @MainActor in
            let succeeded: Bool
            do {
                try await onRedeliver(delivery)
                succeeded = true
            } catch {
                succeeded = false
            }
            pending.remove(delivery.id)
            results[delivery.id] = succeeded
            AccessibilityNotification.Announcement(succeeded ? queued : failed).post()
        }
    }
}

#if DEBUG
    #Preview("MonarchWebhookDeliveries") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        MonarchPreviewMatrix {
            ScrollView {
                MonarchWebhookDeliveries(
                    deliveries: [
                        MonarchWebhookDelivery(
                            id: "1",
                            status: "succeeded",
                            eventType: "invoice.paid",
                            eventID: "evt_1Q2w3E",
                            responseCode: 200,
                            durationMilliseconds: 184,
                            attempts: 1,
                            date: now.addingTimeInterval(-120)
                        ),
                        MonarchWebhookDelivery(
                            id: "2",
                            status: "retrying",
                            eventType: "invoice.payment_failed",
                            eventID: "evt_9Z8x7C",
                            responseCode: 500,
                            error: "The endpoint answered with a server error.",
                            durationMilliseconds: 1_204,
                            attempts: 3,
                            date: now.addingTimeInterval(-600),
                            nextAttempt: now.addingTimeInterval(300)
                        ),
                    ],
                    counts: ["succeeded": 12, "retrying": 1],
                    now: now,
                    onRedeliver: { _ in }
                )
                MonarchWebhookDeliveries(deliveries: [], isFilterable: false)
            }
        }
    }
#endif
