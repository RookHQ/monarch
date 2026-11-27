import Foundation

/// A JSON value for ``MonarchJsonViewer``, with object keys kept in the
/// order of the document.
///
/// ```swift
/// let payload = try MonarchJsonValue(parsing: text)
/// let literal: MonarchJsonValue = ["id": 42, "tags": ["a", "b"], "paid": true, "note": nil]
/// ```
///
/// Numbers keep the text they were written with, so `1.50` and large
/// integers are shown and copied exactly.
public enum MonarchJsonValue: Sendable, Equatable {
    /// An object: its members in document order.
    case object([(key: String, value: MonarchJsonValue)])
    /// An array.
    case array([MonarchJsonValue])
    /// A string.
    case string(String)
    /// A number, as written in the JSON text.
    case number(String)
    /// `true` or `false`.
    case bool(Bool)
    /// `null`.
    case null

    /// Parses JSON text. Object keys keep their order; a repeated key keeps
    /// its first position and its last value, as `JSON.parse` does.
    ///
    /// - Parameter text: The JSON text.
    /// - Throws: ``MonarchJsonParseError`` with the position of the problem.
    public init(parsing text: String) throws {
        self = try MonarchJsonViewerParser.parse(Array(text.utf8))
    }

    /// Parses UTF-8 JSON data.
    ///
    /// - Parameter data: The JSON data.
    /// - Throws: ``MonarchJsonParseError`` with the position of the problem.
    public init(data: Data) throws {
        self = try MonarchJsonViewerParser.parse([UInt8](data))
    }

    /// Whether two values are equal: same kind, same members in the same order.
    public static func == (lhs: MonarchJsonValue, rhs: MonarchJsonValue) -> Bool {
        switch (lhs, rhs) {
        case (.object(let a), .object(let b)):
            a.count == b.count && zip(a, b).allSatisfy { $0.key == $1.key && $0.value == $1.value }
        case (.array(let a), .array(let b)): a == b
        case (.string(let a), .string(let b)): a == b
        case (.number(let a), .number(let b)): a == b
        case (.bool(let a), .bool(let b)): a == b
        case (.null, .null): true
        default: false
        }
    }
}

extension MonarchJsonValue: ExpressibleByStringLiteral {
    /// A string value.
    public init(stringLiteral value: String) { self = .string(value) }
}

extension MonarchJsonValue: ExpressibleByIntegerLiteral {
    /// A number value.
    public init(integerLiteral value: Int) { self = .number(String(value)) }
}

extension MonarchJsonValue: ExpressibleByFloatLiteral {
    /// A number value.
    public init(floatLiteral value: Double) { self = .number(String(value)) }
}

extension MonarchJsonValue: ExpressibleByBooleanLiteral {
    /// A boolean value.
    public init(booleanLiteral value: Bool) { self = .bool(value) }
}

extension MonarchJsonValue: ExpressibleByNilLiteral {
    /// The null value.
    public init(nilLiteral: ()) { self = .null }
}

extension MonarchJsonValue: ExpressibleByArrayLiteral {
    /// An array value.
    public init(arrayLiteral elements: MonarchJsonValue...) { self = .array(elements) }
}

extension MonarchJsonValue: ExpressibleByDictionaryLiteral {
    /// An object value, with the keys in the order written.
    public init(dictionaryLiteral elements: (String, MonarchJsonValue)...) {
        self = .object(elements.map { (key: $0.0, value: $0.1) })
    }
}
