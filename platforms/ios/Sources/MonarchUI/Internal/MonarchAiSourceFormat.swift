import Foundation

/// How an AI source is drawn and opened, shared by the citation, the source
/// list, the source preview and the claim sources, so a source looks and
/// behaves the same in each.
enum MonarchAiSourceFormat {
    /// The SF Symbol of a kind of source, shown when there is no favicon.
    static func symbol(_ kind: MonarchAiSourceKind) -> String {
        switch kind {
        case .web: "globe"
        case .doc: "doc.text"
        case .record: "list.bullet.rectangle"
        case .file: "doc"
        }
    }

    /// The source's address when it is safe to open (see ``MonarchSafeURL``).
    static func safeURL(_ string: String?) -> URL? {
        MonarchSafeURL.url(string)
    }

    /// The favicon address when it is safe to load: `http` or `https` only.
    static func faviconURL(_ string: String?) -> URL? {
        guard let url = safeURL(string), url.scheme?.lowercased() != "mailto" else { return nil }
        return url
    }

    /// The host of a source without `www.`, such as `example.com`.
    static func host(_ string: String?) -> String? {
        MonarchSafeURL.displayHost(string)
    }
}
