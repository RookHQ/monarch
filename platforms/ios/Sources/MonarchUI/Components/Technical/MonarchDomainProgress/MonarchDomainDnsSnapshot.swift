/// What public DNS shows for a host right now, shown by
/// ``MonarchDomainProgress`` as one line ("CNAME → domains.example.net").
public struct MonarchDomainDnsSnapshot: Sendable, Hashable {
    /// The CNAME targets found. The first one is shown.
    public var cnames: [String]
    /// The A (IPv4) addresses found.
    public var a: [String]
    /// The AAAA (IPv6) addresses found.
    public var aaaa: [String]

    /// A DNS snapshot. All lists empty means no record was found.
    ///
    /// - Parameters:
    ///   - cnames: The CNAME targets found.
    ///   - a: The A addresses found.
    ///   - aaaa: The AAAA addresses found.
    public init(cnames: [String] = [], a: [String] = [], aaaa: [String] = []) {
        self.cnames = cnames
        self.a = a
        self.aaaa = aaaa
    }
}
