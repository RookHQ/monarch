/// What a related entity is. The common kinds are static members; any
/// other string is allowed and shown as given, as on the web.
public struct MonarchAiRelatedEntityKind: RawRepresentable, Sendable, Hashable, Codable, ExpressibleByStringLiteral {
    /// The kind as a string, such as `person`.
    public var rawValue: String

    /// A kind from its string.
    public init(rawValue: String) {
        self.rawValue = rawValue
    }

    /// A kind from a string literal.
    public init(stringLiteral value: String) {
        self.rawValue = value
    }

    /// A person.
    public static let person: MonarchAiRelatedEntityKind = "person"
    /// An organisation.
    public static let org: MonarchAiRelatedEntityKind = "org"
    /// A project.
    public static let project: MonarchAiRelatedEntityKind = "project"
    /// A document.
    public static let document: MonarchAiRelatedEntityKind = "document"
    /// A record, such as a deal or a ticket.
    public static let record: MonarchAiRelatedEntityKind = "record"
    /// A place.
    public static let place: MonarchAiRelatedEntityKind = "place"
    /// Anything else.
    public static let other: MonarchAiRelatedEntityKind = "other"
}
