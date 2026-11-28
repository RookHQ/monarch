import Foundation

/// A document the person agrees to in ``MonarchTermsAcceptance``, such as
/// the terms of service or the privacy policy.
public struct MonarchTermsDocument: Sendable, Hashable, Identifiable {
    /// The stable key used in the accepted versions. The URL by default.
    public let id: String
    /// The name people see, already localised, such as "Terms of Service".
    public let title: String
    /// Where the document is published.
    public let url: URL
    /// The version shown, such as `2026-11-01`. Reported when the box changes.
    public let version: String?

    /// A document.
    ///
    /// - Parameters:
    ///   - title: The name people see, already localised.
    ///   - url: Where the document is published.
    ///   - version: The version shown, reported in the acceptance details.
    ///   - id: The key used in the accepted versions. The URL by default.
    public init(_ title: String, url: URL, version: String? = nil, id: String? = nil) {
        self.title = title
        self.url = url
        self.version = version
        self.id = id ?? url.absoluteString
    }

    /// The versions of the documents that have one, keyed by ``id``.
    ///
    /// A server can call this with the same documents to record what the
    /// person saw.
    public static func versions(of documents: [MonarchTermsDocument]) -> [String: String] {
        var versions: [String: String] = [:]
        for document in documents {
            if let version = document.version { versions[document.id] = version }
        }
        return versions
    }
}
