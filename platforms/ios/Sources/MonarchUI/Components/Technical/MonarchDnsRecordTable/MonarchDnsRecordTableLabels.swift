/// Replacements for the built-in strings of ``MonarchDnsRecordTable``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchDnsRecordTableLabels: Sendable {
    /// The heading of the record type. Built in: "Type".
    public var type: String?
    /// The heading of the record name. Built in: "Name".
    public var name: String?
    /// The heading of the record value. Built in: "Value".
    public var value: String?
    /// The heading of the TTL. Built in: "TTL".
    public var ttl: String?
    /// The heading of the priority. Built in: "Priority".
    public var priority: String?
    /// The heading of the status column of the table. Built in: "Status".
    public var status: String?
    /// The status of a record public DNS shows. Built in: "Found".
    public var found: String?
    /// The status of a record public DNS does not show yet. Built in: "Waiting".
    public var waiting: String?
    /// The heading of a card, by purpose. Receives the purpose, `nil` when
    /// the record has none. Built in: "Points your address here",
    /// "Proves you own the domain", "Allows the HTTPS certificate", or the
    /// custom text.
    public var purpose: (@Sendable (MonarchDnsRecordPurpose?) -> String)?
    /// The spoken name of a record's name and its copy button. Receives the
    /// record and the name as shown. Built in: "name of the CNAME record".
    public var copyName: (@Sendable (MonarchDnsRecord, String) -> String)?
    /// The spoken name of a record's value and its copy button. Receives the
    /// record and the name as shown. Built in: "value of the CNAME record book".
    public var copyValue: (@Sendable (MonarchDnsRecord, String) -> String)?
    /// The line under a card whose name is shown relative to the zone.
    /// Receives the full name. Built in: "Full name: book.example.com".
    public var fullName: (@Sendable (String) -> String)?
    /// The spoken name of the list. Receives the zone. Built in:
    /// "DNS records for example.com", or "DNS records" without a zone.
    public var label: (@Sendable (String?) -> String)?
    /// Announced when the number of found records changes. Receives the
    /// found and the checked counts. Built in: "1 of 2 records found."
    public var summary: (@Sendable (Int, Int) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        type: String? = nil,
        name: String? = nil,
        value: String? = nil,
        ttl: String? = nil,
        priority: String? = nil,
        status: String? = nil,
        found: String? = nil,
        waiting: String? = nil,
        purpose: (@Sendable (MonarchDnsRecordPurpose?) -> String)? = nil,
        copyName: (@Sendable (MonarchDnsRecord, String) -> String)? = nil,
        copyValue: (@Sendable (MonarchDnsRecord, String) -> String)? = nil,
        fullName: (@Sendable (String) -> String)? = nil,
        label: (@Sendable (String?) -> String)? = nil,
        summary: (@Sendable (Int, Int) -> String)? = nil
    ) {
        self.type = type
        self.name = name
        self.value = value
        self.ttl = ttl
        self.priority = priority
        self.status = status
        self.found = found
        self.waiting = waiting
        self.purpose = purpose
        self.copyName = copyName
        self.copyValue = copyValue
        self.fullName = fullName
        self.label = label
        self.summary = summary
    }

    var resolvedType: String { type ?? MonarchDnsRecordTableStrings.type }
    var resolvedName: String { name ?? MonarchDnsRecordTableStrings.name }
    var resolvedValue: String { value ?? MonarchDnsRecordTableStrings.value }
    var resolvedTTL: String { ttl ?? MonarchDnsRecordTableStrings.ttl }
    var resolvedPriority: String { priority ?? MonarchDnsRecordTableStrings.priority }
    var resolvedStatus: String { status ?? MonarchDnsRecordTableStrings.status }
    var resolvedFound: String { found ?? MonarchDnsRecordTableStrings.found }
    var resolvedWaiting: String { waiting ?? MonarchDnsRecordTableStrings.waiting }

    /// The heading of a card.
    func purposeText(_ purpose: MonarchDnsRecordPurpose?) -> String {
        if let custom = self.purpose { return custom(purpose) }
        return switch purpose {
        case .routing: MonarchDnsRecordTableStrings.purposeRouting
        case .certificate: MonarchDnsRecordTableStrings.purposeCertificate
        case .verification, nil: MonarchDnsRecordTableStrings.purposeVerification
        case .custom(let text): text
        }
    }

    /// The spoken name of a record's name.
    func copyNameText(_ record: MonarchDnsRecord, shown: String) -> String {
        copyName?(record, shown) ?? MonarchDnsRecordTableStrings.copyName(type: record.type)
    }

    /// The spoken name of a record's value.
    func copyValueText(_ record: MonarchDnsRecord, shown: String) -> String {
        copyValue?(record, shown) ?? MonarchDnsRecordTableStrings.copyValue(type: record.type, shownName: shown)
    }

    /// The line with the full name.
    func fullNameText(_ name: String) -> String {
        fullName?(name) ?? MonarchDnsRecordTableStrings.fullName(name)
    }

    /// The spoken name of the list.
    func labelText(zone: String?) -> String {
        label?(zone) ?? MonarchDnsRecordTableStrings.label(zone: zone)
    }

    /// The found announcement.
    func summaryText(found: Int, total: Int) -> String {
        summary?(found, total) ?? MonarchDnsRecordTableStrings.summary(found: found, total: total)
    }
}
