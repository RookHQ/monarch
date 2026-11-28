/// One of the three steps ``MonarchDomainProgress`` shows: DNS record found,
/// HTTPS certificate issued, live.
public enum MonarchDomainStep: String, Sendable, Hashable, CaseIterable {
    /// The DNS record (routing or ownership) is found.
    case dns
    /// The HTTPS certificate is issued.
    case certificate
    /// The site loads on the domain.
    case live
}
