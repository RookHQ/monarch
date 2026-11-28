/// The statuses a ``MonarchStatusBadge`` knows, by key.
///
/// ```swift
/// enum TicketStatus: String { case open, waiting }
///
/// let ticketStatuses = MonarchStatusRegistry([
///     TicketStatus.open: MonarchStatusDefinition("Open", tone: .info, systemImage: "circle"),
///     TicketStatus.waiting: MonarchStatusDefinition("Waiting on customer", tone: .warning, systemImage: "clock"),
/// ])
/// ```
///
/// A badge looks a key up in the registry it is given, then in the
/// registries installed with `monarchStatusRegistry(_:)`, then in the
/// built-in statuses. A key found nowhere is shown in the neutral "unknown"
/// look with a readable label made from the key ("pending_delete" reads
/// "Pending delete").
public struct MonarchStatusRegistry: Sendable, Hashable {
    /// The statuses by key.
    public let definitions: [String: MonarchStatusDefinition]

    /// A registry of string keys.
    ///
    /// - Parameter definitions: The statuses by key.
    public init(_ definitions: [String: MonarchStatusDefinition]) {
        self.definitions = definitions
    }

    /// A registry keyed by an enumeration with string raw values, so badges
    /// are typed by the app's own statuses.
    ///
    /// - Parameter definitions: The statuses by key.
    public init<Key: RawRepresentable>(_ definitions: [Key: MonarchStatusDefinition]) where Key.RawValue == String {
        self.definitions = Dictionary(uniqueKeysWithValues: definitions.map { ($0.key.rawValue, $0.value) })
    }

    /// The statuses Monarch ships: service health first (operational,
    /// degraded, partial, down, maintenance, unknown), then the generic set
    /// (success, warning, danger, info, neutral).
    public static var builtIn: MonarchStatusRegistry {
        MonarchStatusRegistry(MonarchStatusBadgeTokens.builtInDefinitions)
    }

    /// This registry with the statuses of `other` added; `other` wins where
    /// both have a key.
    ///
    /// - Parameter other: The statuses to add.
    /// - Returns: The combined registry.
    public func merging(_ other: MonarchStatusRegistry) -> MonarchStatusRegistry {
        MonarchStatusRegistry(definitions.merging(other.definitions) { _, new in new })
    }

    /// The status of a key: from this registry, then the built-in statuses,
    /// then the unknown look with a label made from the key.
    ///
    /// - Parameter key: The status key. `nil` is unknown.
    /// - Returns: The status to show.
    public func resolve(_ key: String?) -> MonarchStatusDefinition {
        MonarchStatusBadgeTokens.resolve(key, in: [self])
    }
}
