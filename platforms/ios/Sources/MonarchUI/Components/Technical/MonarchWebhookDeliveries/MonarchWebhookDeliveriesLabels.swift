/// Replacements for the built-in strings of ``MonarchWebhookDeliveries``.
/// A property that is `nil` keeps the built-in English string. Status names come from the
/// `statuses` registry.
@_spi(Experimental) public struct MonarchWebhookDeliveriesLabels: Sendable {
    /// The spoken name of the log. Built in: "Webhook deliveries".
    public var label: String?
    /// The label of the status filter and fact. Built in: "Status".
    public var status: String?
    /// The label of the event type filter. Built in: "Event type".
    public var eventTypeFilter: String?
    /// The status filter's choice for every status. Built in: "Any status".
    public var anyStatus: String?
    /// The event type filter's choice for every type. Built in: "Any event type".
    public var anyEventType: String?
    /// The spoken name of the search field. Built in: "Find by event ID".
    public var search: String?
    /// The placeholder of the search field. Built in: "Event ID".
    public var searchPrompt: String?
    /// The button that clears the filters. Built in: "Clear filters".
    public var clearFilters: String?
    /// The redeliver button. Built in: "Redeliver".
    public var redeliver: String?
    /// Announced when a redelivery was accepted. Built in: "Redelivery queued.".
    public var redeliverQueued: String?
    /// Announced when a redelivery failed. Built in: "Could not redeliver. Try again.".
    public var redeliverFailed: String?
    /// What the copy button copies, for VoiceOver. Built in: "event ID".
    public var eventIDTarget: String?
    /// The response of a delivery never attempted. Built in: "Not attempted yet".
    public var notAttempted: String?
    /// The response of a delivery with no answer and no error. Built in: "No response".
    public var noResponse: String?
    /// The title when there are no deliveries. Built in: "No deliveries yet".
    public var emptyTitle: String?
    /// The text when there are no deliveries. Built in: "Deliveries appear here when an event this endpoint subscribes to happens.".
    public var emptyMessage: String?
    /// The title when the filters hide every delivery. Built in: "No deliveries match".
    public var filteredTitle: String?
    /// The text when the filters hide every delivery. Built in: "Change or clear the filters. Event IDs must match exactly.".
    public var filteredMessage: String?
    /// Read while the first page loads. Built in: "Loading deliveries".
    public var loading: String?
    /// Shown when the list failed to load. Built in: "Deliveries did not load.".
    public var loadError: String?
    /// The spoken hint of a row. Built in: "Shows the delivery details.".
    public var detailsHint: String?
    /// The title of the details sheet. Built in: "Delivery".
    public var detailsTitle: String?
    /// The button that closes the details sheet. Built in: "Close".
    public var close: String?
    /// The label of the event fact. Built in: "Event".
    public var event: String?
    /// The label of the event ID fact. Built in: "Event ID".
    public var eventID: String?
    /// The label of the destination fact. Built in: "Destination".
    public var destination: String?
    /// The label of the response fact. Built in: "Response".
    public var response: String?
    /// The label of the error fact. Built in: "Error".
    public var error: String?
    /// The label of the attempts fact. Built in: "Attempts".
    public var attemptsLabel: String?
    /// The label of the time fact. Built in: "When".
    public var when: String?
    /// An HTTP status, from the code. Built in: "HTTP 500".
    public var httpStatus: (@Sendable (Int) -> String)?
    /// How long an attempt took, from milliseconds. Built in: "1,204 ms".
    public var duration: (@Sendable (Double) -> String)?
    /// When the next retry happens, from a relative time. Built in: "Next attempt in 5 minutes".
    public var nextAttempt: (@Sendable (String) -> String)?
    /// The number of attempts. Built in: "3 attempts".
    public var attempts: (@Sendable (Int) -> String)?
    /// One part of the counts line, from the number and the status name. Built in: "12 delivered".
    public var count: (@Sendable (Int, String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional; each is described on its property.
    public init(
        label: String? = nil,
        status: String? = nil,
        eventTypeFilter: String? = nil,
        anyStatus: String? = nil,
        anyEventType: String? = nil,
        search: String? = nil,
        searchPrompt: String? = nil,
        clearFilters: String? = nil,
        redeliver: String? = nil,
        redeliverQueued: String? = nil,
        redeliverFailed: String? = nil,
        eventIDTarget: String? = nil,
        notAttempted: String? = nil,
        noResponse: String? = nil,
        emptyTitle: String? = nil,
        emptyMessage: String? = nil,
        filteredTitle: String? = nil,
        filteredMessage: String? = nil,
        loading: String? = nil,
        loadError: String? = nil,
        detailsHint: String? = nil,
        detailsTitle: String? = nil,
        close: String? = nil,
        event: String? = nil,
        eventID: String? = nil,
        destination: String? = nil,
        response: String? = nil,
        error: String? = nil,
        attemptsLabel: String? = nil,
        when: String? = nil,
        httpStatus: (@Sendable (Int) -> String)? = nil,
        duration: (@Sendable (Double) -> String)? = nil,
        nextAttempt: (@Sendable (String) -> String)? = nil,
        attempts: (@Sendable (Int) -> String)? = nil,
        count: (@Sendable (Int, String) -> String)? = nil
    ) {
        self.label = label
        self.status = status
        self.eventTypeFilter = eventTypeFilter
        self.anyStatus = anyStatus
        self.anyEventType = anyEventType
        self.search = search
        self.searchPrompt = searchPrompt
        self.clearFilters = clearFilters
        self.redeliver = redeliver
        self.redeliverQueued = redeliverQueued
        self.redeliverFailed = redeliverFailed
        self.eventIDTarget = eventIDTarget
        self.notAttempted = notAttempted
        self.noResponse = noResponse
        self.emptyTitle = emptyTitle
        self.emptyMessage = emptyMessage
        self.filteredTitle = filteredTitle
        self.filteredMessage = filteredMessage
        self.loading = loading
        self.loadError = loadError
        self.detailsHint = detailsHint
        self.detailsTitle = detailsTitle
        self.close = close
        self.event = event
        self.eventID = eventID
        self.destination = destination
        self.response = response
        self.error = error
        self.attemptsLabel = attemptsLabel
        self.when = when
        self.httpStatus = httpStatus
        self.duration = duration
        self.nextAttempt = nextAttempt
        self.attempts = attempts
        self.count = count
    }
}
