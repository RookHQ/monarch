import Foundation

/// Turns model-supplied props into data a component can receive safely.
///
/// Props on iOS are already plain JSON (``MonarchAiJSON``) and are decoded
/// into the component's own `Decodable` type, so nothing is ever evaluated.
/// This pass adds the limits the web applies: every string that is a URL by
/// key (`href`, `src`, `url`…) or by shape (`javascript:…`, `data:…`) must be
/// http, https, mailto or relative, or it is dropped; strings are cut at
/// 20,000 characters, arrays and objects at 500 members, nesting at 8 levels,
/// and numbers that are not finite are dropped.
enum MonarchAiUiSanitizer {
    /// The deepest nesting that is kept.
    static let maxDepth = 8
    /// The most members of an array or object that are kept.
    static let maxItems = 500
    /// The longest string that is kept, in characters.
    static let maxString = 20_000

    /// Keys whose string values are URLs and must be safe.
    static let urlKeys: Set<String> = [
        "href", "src", "srcSet", "action", "formAction", "poster", "xlinkHref", "data", "url", "imageUrl",
        "avatar", "favicon", "cite", "background", "ping",
    ]

    /// Schemes that are never data a component should receive, whatever the key.
    static let unsafeSchemes: Set<String> = [
        "javascript", "vbscript", "livescript", "data", "file", "blob", "filesystem", "about", "jar",
        "view-source", "intent", "chrome", "chrome-extension", "moz-extension", "ms-browser-extension",
    ]

    /// The schemes a link may use.
    static let linkSchemes: Set<String> = ["http", "https", "mailto"]

    /// The props as plain, limited data, or `nil` when they are not an object.
    /// `null` props are an empty object.
    static func sanitize(_ props: MonarchAiJSON) -> MonarchAiJSON? {
        switch props {
        case .null: return .object([:])
        case .object: return clean(props, key: "", depth: 0)
        default: return MonarchAiJSON?.none
        }
    }

    /// One value, cleaned. `nil` drops it.
    static func clean(_ value: MonarchAiJSON, key: String, depth: Int) -> MonarchAiJSON? {
        switch value {
        case .null, .bool:
            return value
        case .number(let number):
            // A plain `nil` here would read as JSON null, because the value is ExpressibleByNilLiteral.
            guard number.isFinite else { return MonarchAiJSON?.none }
            return value
        case .string(let string):
            if (urlKeys.contains(key) || isURLShaped(string)) && !isSafeURL(string) { return MonarchAiJSON?.none }
            return string.count > maxString ? .string(String(string.prefix(maxString))) : value
        case .array(let items):
            guard depth < maxDepth else { return MonarchAiJSON?.none }
            return .array(items.prefix(maxItems).compactMap { clean($0, key: key, depth: depth + 1) })
        case .object(let members):
            guard depth < maxDepth else { return MonarchAiJSON?.none }
            var result: [String: MonarchAiJSON] = [:]
            for name in members.keys.sorted().prefix(maxItems) {
                if let cleaned = members[name].flatMap({ clean($0, key: name, depth: depth + 1) }) {
                    result[name] = cleaned
                }
            }
            return .object(result)
        }
    }

    /// The text without the whitespace and control characters browsers ignore inside a scheme.
    static func compact(_ text: String) -> String {
        String(
            String.UnicodeScalarView(
                text.trimmingCharacters(in: .whitespacesAndNewlines).unicodeScalars.filter { scalar in
                    !(scalar.value <= 0x20 || (0x7F...0x9F).contains(scalar.value))
                }
            )
        )
    }

    /// The lowercased scheme at the start of the text, if it has one.
    static func scheme(_ text: String) -> String? {
        let compacted = compact(text)
        guard let first = compacted.unicodeScalars.first, CharacterSet.letters.contains(first),
            let colon = compacted.firstIndex(of: ":")
        else { return nil }
        let candidate = compacted[..<colon]
        let allowed = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "+.-"))
        guard candidate.unicodeScalars.allSatisfy({ $0.isASCII && allowed.contains($0) }) else { return nil }
        return candidate.lowercased()
    }

    /// Whether a string is shaped like a URL, whatever its key: it starts with
    /// a scheme and either has no spaces or names a scheme that runs code.
    static func isURLShaped(_ text: String) -> Bool {
        guard let scheme = scheme(text) else { return false }
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        return unsafeSchemes.contains(scheme) || !trimmed.contains(where: \.isWhitespace)
    }

    /// Whether a URL may be passed on: http, https, mailto, or a relative path.
    static func isSafeURL(_ text: String) -> Bool {
        let compacted = compact(text)
        guard !compacted.isEmpty else { return false }
        guard let scheme = scheme(text) else { return true }
        return linkSchemes.contains(scheme)
    }

    /// Decodes sanitised props into a component's props type. Nothing is evaluated.
    static func decode<Props: Decodable>(_ type: Props.Type, from json: MonarchAiJSON) throws -> Props {
        try JSONDecoder().decode(type, from: JSONEncoder().encode(json))
    }
}
