import Foundation

/// The filter values of ``MonarchWebhookDeliveries``. `nil` and empty text mean "any".
@_spi(Experimental) public struct MonarchWebhookDeliveryFilters: Hashable, Sendable {
    /// The status key to show, or `nil` for any status.
    public var status: String?
    /// The event type to show, or `nil` for any event type.
    public var eventType: String?
    /// Text matched against the event id (exactly, ignoring case) and the event type (contains).
    public var query: String

    /// Filter values.
    ///
    /// - Parameters:
    ///   - status: The status key to show, or `nil` for any.
    ///   - eventType: The event type to show, or `nil` for any.
    ///   - query: Text matched against the event id and the event type.
    public init(status: String? = nil, eventType: String? = nil, query: String = "") {
        self.status = status
        self.eventType = eventType
        self.query = query
    }

    /// Whether any filter narrows the list.
    public var isActive: Bool {
        status != nil || eventType != nil || !query.trimmingCharacters(in: .whitespaces).isEmpty
    }

    /// The deliveries that pass the filters, in their order: the same rule the view uses when it
    /// filters on its own.
    public func apply(to deliveries: [MonarchWebhookDelivery]) -> [MonarchWebhookDelivery] {
        let wanted = query.trimmingCharacters(in: .whitespaces).localizedLowercase
        return deliveries.filter { delivery in
            (status == nil || delivery.status == status)
                && (eventType == nil || delivery.eventType == eventType)
                && (wanted.isEmpty || delivery.eventID?.localizedLowercase == wanted
                    || delivery.eventType.localizedLowercase.contains(wanted))
        }
    }
}
