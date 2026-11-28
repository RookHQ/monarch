/// One DNS record the user has to create at their DNS provider, shown by
/// ``MonarchDnsRecordTable``.
public struct MonarchDnsRecord: Identifiable, Sendable, Hashable {
    /// A stable identifier. By default the type, name and value joined.
    public let id: String
    /// The record type: `CNAME`, `TXT`, `A`, `AAAA`, `MX`, `CAA`.
    public var type: String
    /// The full record name, such as `book.example.com`. Shown relative to
    /// the zone when the table has one.
    public var name: String
    /// The record value or target.
    public var value: String
    /// What the record is for. Shown as the heading of its card.
    public var purpose: MonarchDnsRecordPurpose?
    /// The TTL to set, as the provider writes it ("3600", "Auto").
    public var ttl: String?
    /// The MX or SRV priority.
    public var priority: Int?
    /// Whether public DNS already shows the record: `true` is "Found",
    /// `false` is "Waiting", `nil` when the app has not checked.
    public var isFound: Bool?

    /// A DNS record.
    ///
    /// - Parameters:
    ///   - id: A stable identifier. By default the type, name and value joined.
    ///   - type: The record type, such as `CNAME`.
    ///   - name: The full record name, such as `book.example.com`.
    ///   - value: The record value or target.
    ///   - purpose: What the record is for.
    ///   - ttl: The TTL to set, as the provider writes it.
    ///   - priority: The MX or SRV priority.
    ///   - isFound: Whether public DNS already shows the record; `nil` when not checked.
    public init(
        id: String? = nil,
        type: String,
        name: String,
        value: String,
        purpose: MonarchDnsRecordPurpose? = nil,
        ttl: String? = nil,
        priority: Int? = nil,
        isFound: Bool? = nil
    ) {
        self.id = id ?? "\(type):\(name):\(value)"
        self.type = type
        self.name = name
        self.value = value
        self.purpose = purpose
        self.ttl = ttl
        self.priority = priority
        self.isFound = isFound
    }
}
