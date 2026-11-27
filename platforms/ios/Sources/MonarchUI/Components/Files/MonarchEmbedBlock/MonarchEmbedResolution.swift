import Foundation

/// How ``MonarchEmbedBlock`` shows a URL.
public enum MonarchEmbedResolution: Sendable {
    /// An https URL on an allow-listed host with an embeddable form: shown in
    /// a web view that loads `embedURL`, never the pasted URL.
    case frame(url: URL, embedURL: URL, provider: MonarchEmbedProvider)
    /// Any other http or https URL: shown as a link card. `host` has no `www.`.
    case link(url: URL, host: String)
    /// Anything else (another scheme, a relative or malformed URL, a URL
    /// with a user name or password): not shown.
    case invalid(String)

    /// The kind of the resolution, without its values.
    public var kind: MonarchEmbedResolutionKind {
        switch self {
        case .frame: .frame
        case .link: .link
        case .invalid: .invalid
        }
    }
}
