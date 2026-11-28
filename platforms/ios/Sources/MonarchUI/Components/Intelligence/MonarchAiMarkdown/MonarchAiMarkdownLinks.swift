import Foundation

/// Link safety of ``MonarchAiMarkdown``: which links may be followed, which
/// images may be handed to the app, and the host shown after a link so it
/// cannot pretend to go elsewhere.
enum MonarchAiMarkdownLinks {
    private static let linkSchemes: Set<String> = ["http", "https", "mailto"]
    private static let imageSchemes: Set<String> = ["http", "https"]

    /// The URL without white space and control characters, which browsers ignore inside a scheme.
    private static func compact(_ url: String) -> String {
        String(
            String.UnicodeScalarView(
                url.unicodeScalars.filter { scalar in
                    !(scalar.value <= 0x20 || (0x7F...0x9F).contains(scalar.value))
                }
            )
        )
    }

    /// The lower-cased scheme of a URL, or `nil` for a relative one.
    static func scheme(_ url: String) -> String? {
        let compacted = compact(url)
        guard let colon = compacted.firstIndex(of: ":") else { return nil }
        let candidate = compacted[..<colon]
        guard let first = candidate.first, first.isASCII, first.isLetter,
            candidate.allSatisfy({ $0.isASCII && ($0.isLetter || $0.isNumber || "+.-".contains($0)) })
        else { return nil }
        return candidate.lowercased()
    }

    /// Whether a link may be followed: `http`, `https` and `mailto` always;
    /// a relative link only when the app handles links; never
    /// `javascript:`, `data:`, any other scheme or a protocol-relative
    /// `//host` link.
    static func isSafeLink(_ href: String, allowsRelative: Bool) -> Bool {
        let compacted = compact(href)
        if compacted.isEmpty { return false }
        if let scheme = scheme(href) { return linkSchemes.contains(scheme) }
        if compacted.hasPrefix("//") { return false }
        return allowsRelative
    }

    /// Whether an image may be handed to the app: an absolute `http` or `https` source.
    static func isSafeImage(_ source: String) -> Bool {
        scheme(source).map(imageSchemes.contains) ?? false
    }

    /// The host of an absolute URL, lower-cased, or `nil` for a relative one or `mailto:`.
    static func host(of url: String) -> String? {
        var rest = Substring(url.trimmingCharacters(in: .whitespacesAndNewlines))
        if let colon = rest.firstIndex(of: ":"), scheme(String(rest)) != nil {
            rest = rest[rest.index(after: colon)...]
        }
        guard rest.hasPrefix("//") else { return nil }
        rest = rest.dropFirst(2)
        let authority = rest.prefix { !"/?#".contains($0) }
        var hostPart = authority
        if let at = authority.lastIndex(of: "@") {
            hostPart = authority[authority.index(after: at)...]
        }
        let host: Substring
        if hostPart.hasPrefix("[") {
            guard let close = hostPart.firstIndex(of: "]") else { return nil }
            host = hostPart[...close]
        } else {
            host = hostPart.prefix { $0 != ":" }
        }
        return host.isEmpty ? nil : host.lowercased()
    }

    /// The host to show after a link, or `nil` when the link text already
    /// names it (ignoring `www.`), so a link cannot pretend to go elsewhere.
    static func shownHost(href: String, linkText: String) -> String? {
        guard let host = host(of: href) else { return nil }
        let bare = host.hasPrefix("www.") ? String(host.dropFirst(4)) : host
        return linkText.lowercased().contains(bare) ? nil : host
    }

    /// The URL to open for a safe link, or `nil` when Foundation cannot read it.
    static func url(_ href: String) -> URL? {
        let trimmed = href.trimmingCharacters(in: .whitespacesAndNewlines)
        if let url = URL(string: trimmed) { return url }
        let encoded = trimmed.addingPercentEncoding(withAllowedCharacters: .urlFragmentAllowed)
        return encoded.flatMap(URL.init(string:))
    }
}
