/// Built-in strings of the DNS record table, from the `MonarchDnsRecordTable` strings table.
enum MonarchDnsRecordTableStrings {
    private static let table = "MonarchDnsRecordTable"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.dnsRecordTable.\(key)", table: table)
    }

    /// The heading of the record type.
    static var type: String { string("type") }
    /// The heading of the record name.
    static var name: String { string("name") }
    /// The heading of the record value.
    static var value: String { string("value") }
    /// The heading of the TTL.
    static var ttl: String { string("ttl") }
    /// The heading of the priority.
    static var priority: String { string("priority") }
    /// The heading of the status column.
    static var status: String { string("status") }
    /// The status of a record public DNS shows.
    static var found: String { string("found") }
    /// The status of a record public DNS does not show yet.
    static var waiting: String { string("waiting") }
    /// The heading of a routing record.
    static var purposeRouting: String { string("purposeRouting") }
    /// The heading of a verification record, and of a record without a purpose.
    static var purposeVerification: String { string("purposeVerification") }
    /// The heading of a certificate record.
    static var purposeCertificate: String { string("purposeCertificate") }

    /// The spoken name of a record's name.
    static func copyName(type: String) -> String {
        MonarchStrings.string("monarch.dnsRecordTable.copyName", table: table, type)
    }

    /// The spoken name of a record's value.
    static func copyValue(type: String, shownName: String) -> String {
        MonarchStrings.string("monarch.dnsRecordTable.copyValue", table: table, type, shownName)
    }

    /// The line under a card whose name is relative to the zone.
    static func fullName(_ name: String) -> String {
        MonarchStrings.string("monarch.dnsRecordTable.fullName", table: table, name)
    }

    /// The spoken name of the list.
    static func label(zone: String?) -> String {
        guard let zone, !zone.isEmpty else { return string("label") }
        return MonarchStrings.string("monarch.dnsRecordTable.labelZone", table: table, zone)
    }

    /// The announcement of how many records are found.
    static func summary(found: Int, total: Int) -> String {
        MonarchStrings.string("monarch.dnsRecordTable.summary", table: table, found, total)
    }
}
