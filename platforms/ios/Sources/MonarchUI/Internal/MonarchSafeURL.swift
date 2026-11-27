import Foundation

/// The links Monarch opens: `http` and `https` with a host, and `mailto`
/// with an address. Anything else (`javascript:`, `data:`, `file:`, custom
/// schemes) is refused, so text from a model, a user or a server can never
/// open something unexpected. The rule matches the web's `isSafeUrl`.
enum MonarchSafeURL {
    /// The schemes that may be opened.
    static let schemes: Set<String> = ["http", "https", "mailto"]

    /// The URL of `string` when it is safe to open, otherwise `nil`.
    /// Whitespace around the text is ignored.
    static func url(_ string: String?) -> URL? {
        guard let string else { return nil }
        // Browsers and URL parsers ignore control characters inside a scheme
        // ("java\nscript:"), so they are refused rather than removed.
        let trimmed = string.trimmingCharacters(in: .whitespacesAndNewlines)
        let hasControl = trimmed.unicodeScalars.contains { CharacterSet.controlCharacters.contains($0) }
        guard !trimmed.isEmpty, !hasControl,
            let parsed = URL(string: trimmed)
        else { return nil }
        return url(parsed)
    }

    /// `url` when it is safe to open, otherwise `nil`.
    static func url(_ url: URL) -> URL? {
        guard let scheme = url.scheme?.lowercased(), schemes.contains(scheme) else { return nil }
        switch scheme {
        case "mailto": return url.absoluteString.count > "mailto:".count ? url : nil
        default: return url.host?.isEmpty == false ? url : nil
        }
    }

    /// Whether `string` is a URL that is safe to open.
    static func isSafe(_ string: String?) -> Bool {
        url(string) != nil
    }

    /// The host of a safe web address without `www.`, such as `example.com`.
    static func displayHost(_ string: String?) -> String? {
        guard let url = url(string), let host = url.host?.lowercased(), !host.isEmpty else { return nil }
        return host.hasPrefix("www.") ? String(host.dropFirst(4)) : host
    }
}

/// Turns what someone typed as a link into a link target, as the web's
/// `normalizeLinkInput` does: a safe URL is kept, a bare domain
/// ("example.com/x") gets `https://`, an email address gets `mailto:`, a
/// relative path is kept, and anything unsafe or empty gives `nil`.
enum MonarchLinkAddress {
    /// The link target for typed text, or `nil` when it cannot be a safe link.
    static func normalized(_ input: String) -> String? {
        let value = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !value.isEmpty else { return nil }
        if value.firstMatch(of: /^[A-Za-z][A-Za-z0-9+.\-]*:/) != nil {
            return isSafeWithScheme(value) ? value : nil
        }
        if value.wholeMatch(of: /[^\s@\/]+@[^\s@\/]+\.[^\s@\/]+/) != nil { return "mailto:\(value)" }
        if value.firstMatch(of: /^(\/|#|\?|\.\/|\.\.\/)/) != nil { return value }
        if value.firstMatch(of: /^[^\s\/]+\.[^\s\/]+/) != nil { return "https://\(value)" }
        return nil
    }

    /// The web's check for text that has a scheme: only the allowed schemes,
    /// compared without control characters and spaces.
    private static func isSafeWithScheme(_ value: String) -> Bool {
        let compact = String(
            String.UnicodeScalarView(
                value.unicodeScalars.filter { $0.value > 0x20 && !(0x7F...0x9F).contains($0.value) }
            )
        )
        guard let match = compact.firstMatch(of: /^([A-Za-z][A-Za-z0-9+.\-]*):/) else { return false }
        return MonarchSafeURL.schemes.contains(String(match.output.1).lowercased())
    }
}
