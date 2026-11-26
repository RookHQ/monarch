import Foundation

/// One attempt, or series of attempts, to deliver one event to one endpoint, for
/// ``MonarchWebhookDeliveries``.
@_spi(Experimental) public struct MonarchWebhookDelivery: Identifiable, Hashable, Sendable {
    /// A stable, unique id.
    public let id: String
    /// A status key: a built-in one (`pending`, `sending`, `succeeded`, `retrying`, `failed`,
    /// `gone`) or one from the `statuses` registry.
    public let status: String
    /// The event name, such as `invoice.paid`.
    public let eventType: String
    /// The event's id, shown with a copy button.
    public let eventID: String?
    /// Where it was sent, when one list covers several endpoints.
    public let destination: String?
    /// The HTTP status the endpoint answered with; `nil` when there was no answer.
    public let responseCode: Int?
    /// What went wrong, in plain words, already localised.
    public let error: String?
    /// How long the last attempt took, in milliseconds.
    public let durationMilliseconds: Double?
    /// The attempts made so far.
    public let attempts: Int
    /// When it was last attempted or delivered, or created if never attempted.
    public let date: Date
    /// When the next automatic retry happens, for pending or retrying deliveries.
    public let nextAttempt: Date?

    /// A delivery.
    ///
    /// - Parameters:
    ///   - id: A stable, unique id.
    ///   - status: A status key.
    ///   - eventType: The event name.
    ///   - eventID: The event's id.
    ///   - destination: Where it was sent.
    ///   - responseCode: The HTTP status the endpoint answered with.
    ///   - error: What went wrong, already localised.
    ///   - durationMilliseconds: How long the last attempt took.
    ///   - attempts: The attempts made so far.
    ///   - date: When it was last attempted or delivered.
    ///   - nextAttempt: When the next automatic retry happens.
    public init(
        id: String,
        status: String,
        eventType: String,
        eventID: String? = nil,
        destination: String? = nil,
        responseCode: Int? = nil,
        error: String? = nil,
        durationMilliseconds: Double? = nil,
        attempts: Int,
        date: Date,
        nextAttempt: Date? = nil
    ) {
        self.id = id
        self.status = status
        self.eventType = eventType
        self.eventID = eventID
        self.destination = destination
        self.responseCode = responseCode
        self.error = error
        self.durationMilliseconds = durationMilliseconds
        self.attempts = attempts
        self.date = date
        self.nextAttempt = nextAttempt
    }
}
