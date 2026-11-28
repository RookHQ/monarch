import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchFavicon``.
enum MonarchFaviconTokens {
    /// The side of the tile before Dynamic Type is applied.
    static func side(_ size: MonarchFaviconSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space4
        case .md: MonarchTokens.Space.space5
        case .lg: MonarchTokens.Space.space6
        }
    }

    /// The corner radius of a shape for a tile of a given side.
    static func cornerRadius(_ shape: MonarchFaviconShape, side: CGFloat) -> CGFloat {
        switch shape {
        case .rounded: MonarchTokens.Radius.xs
        case .circle: side / 2
        case .square: MonarchTokens.Radius.none
        }
    }

    /// The size of the fallback letter, as a part of the side of the tile.
    static let letterScale: CGFloat = 0.6
    /// The weight of the fallback letter.
    static let letterWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The fill role of the letter tile.
    static var tile: MonarchColorRole { \.bgMuted }
    /// The role of the letter.
    static var letter: MonarchColorRole { \.textMuted }
    /// The letter shown when the name has no letter or digit.
    static let unknownLetter = "?"

    /// The host name of a URL or bare domain, lower-cased and without
    /// `www.`: `https://www.Example.com/a` gives `example.com`. A plain name
    /// such as `Acme Inc` is returned trimmed and unchanged.
    static func hostname(_ name: String) -> String {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return "" }
        var host = trimmed
        if let scheme = host.range(of: "://") {
            host = String(host[scheme.upperBound...])
            if let end = host.firstIndex(where: { "/?#".contains($0) }) {
                host = String(host[..<end])
            }
            if let user = host.lastIndex(of: "@") {
                host = String(host[host.index(after: user)...])
            }
            if let port = host.lastIndex(of: ":") {
                host = String(host[..<port])
            }
            host = host.lowercased()
        }
        let prefix = "www."
        if host.lowercased().hasPrefix(prefix) {
            host = String(host.dropFirst(prefix.count)).lowercased()
        }
        return host
    }

    /// The fallback letter: the first letter or digit of the name or host
    /// name (the first letters of the first and last word of a name of several words), upper-cased, or a question mark when there is none.
    static func letter(for name: String) -> String {
        // A name of several words ("Survey tool") gives the first letters of its first and last word, as the web does.
        let initials = name.split(whereSeparator: \.isWhitespace).compactMap {
            $0.first(where: { $0.isLetter || $0.isNumber })
        }
        if initials.count >= 2, let first = initials.first, let last = initials.last {
            return (String(first) + String(last)).uppercased()
        }
        guard let first = hostname(name).first(where: { $0.isLetter || $0.isNumber }) else {
            return unknownLetter
        }
        return String(first).uppercased()
    }

    /// The URL to load, or `nil` when it is not safe to load as an icon.
    /// Only `http` and `https` URLs with a host are loaded; an app that has
    /// the picture in a file or in memory passes an `Image`.
    static func loadableURL(_ url: URL?) -> URL? {
        guard let url, let scheme = url.scheme?.lowercased(), scheme == "http" || scheme == "https" else {
            return nil
        }
        guard let host = url.host(), !host.isEmpty else { return nil }
        return url
    }
}
