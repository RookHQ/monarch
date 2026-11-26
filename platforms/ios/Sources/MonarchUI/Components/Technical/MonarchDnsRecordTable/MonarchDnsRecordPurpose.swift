/// What a DNS record in a ``MonarchDnsRecordTable`` is for. The three
/// common purposes have built-in headings; any other is shown as given.
public enum MonarchDnsRecordPurpose: Sendable, Hashable {
    /// Points the domain at the product. "Points your address here".
    case routing
    /// Proves the user owns the domain. "Proves you own the domain".
    case verification
    /// Allows the HTTPS certificate to be issued. "Allows the HTTPS certificate".
    case certificate
    /// Any other purpose, with the heading to show. Already localised.
    case custom(String)
}
