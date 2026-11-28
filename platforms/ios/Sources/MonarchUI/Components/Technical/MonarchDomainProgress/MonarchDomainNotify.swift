/// How the product tells the user that a domain went live. Changes the
/// "keeps checking" sentence of ``MonarchDomainProgress``.
public enum MonarchDomainNotify: Sendable, Hashable, CaseIterable {
    /// By email.
    case email
    /// Inside the app.
    case inApp
}
