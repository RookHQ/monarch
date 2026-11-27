/// Where a custom domain is in its connection, as reported by the product.
/// Shown by ``MonarchDomainProgress``.
public enum MonarchDomainStage: String, Sendable, Hashable, CaseIterable {
    /// Waiting for the routing record.
    case dns
    /// Waiting for a verification record.
    case ownership
    /// Issuing the HTTPS certificate.
    case certificate
    /// Checking that the site loads.
    case page
    /// Connected.
    case live
}
