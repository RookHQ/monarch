import Foundation

/// A JSON value: the arguments and the output of a tool call, the props of
/// generative UI, a diff, or extra request fields. The web model types these
/// fields as `unknown`.
///
/// ```swift
/// let input: MonarchAiJSON = ["query": "invoices", "limit": 10]
/// ```
public enum MonarchAiJSON: Sendable, Hashable, Codable {
    /// JSON `null`.
    case null
    /// A boolean.
    case bool(Bool)
    /// A number.
    case number(Double)
    /// A string.
    case string(String)
    /// An array.
    case array([MonarchAiJSON])
    /// An object.
    case object([String: MonarchAiJSON])

    /// Decodes any JSON value.
    public init(from decoder: any Decoder) throws {
        let container = try decoder.singleValueContainer()
        if container.decodeNil() {
            self = .null
        } else if let value = try? container.decode(Bool.self) {
            self = .bool(value)
        } else if let value = try? container.decode(Double.self) {
            self = .number(value)
        } else if let value = try? container.decode(String.self) {
            self = .string(value)
        } else if let value = try? container.decode([MonarchAiJSON].self) {
            self = .array(value)
        } else {
            self = .object(try container.decode([String: MonarchAiJSON].self))
        }
    }

    /// Encodes the value as JSON.
    public func encode(to encoder: any Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case .null: try container.encodeNil()
        case .bool(let value): try container.encode(value)
        case .number(let value): try container.encode(value)
        case .string(let value): try container.encode(value)
        case .array(let value): try container.encode(value)
        case .object(let value): try container.encode(value)
        }
    }

    /// The string, if the value is one.
    public var stringValue: String? {
        if case .string(let value) = self { return value }
        return nil
    }

    /// The member of an object with this key, if the value is an object that has it.
    public subscript(key: String) -> MonarchAiJSON? {
        if case .object(let value) = self { return value[key] }
        return nil
    }
}

extension MonarchAiJSON: ExpressibleByNilLiteral, ExpressibleByBooleanLiteral, ExpressibleByIntegerLiteral,
    ExpressibleByFloatLiteral, ExpressibleByStringLiteral, ExpressibleByArrayLiteral, ExpressibleByDictionaryLiteral
{
    /// JSON `null`.
    public init(nilLiteral: ()) {
        self = .null
    }

    /// A boolean.
    public init(booleanLiteral value: Bool) {
        self = .bool(value)
    }

    /// A whole number.
    public init(integerLiteral value: Int) {
        self = .number(Double(value))
    }

    /// A number.
    public init(floatLiteral value: Double) {
        self = .number(value)
    }

    /// A string.
    public init(stringLiteral value: String) {
        self = .string(value)
    }

    /// An array.
    public init(arrayLiteral elements: MonarchAiJSON...) {
        self = .array(elements)
    }

    /// An object. A key that is given twice keeps its last value.
    public init(dictionaryLiteral elements: (String, MonarchAiJSON)...) {
        self = .object(Dictionary(elements, uniquingKeysWith: { _, last in last }))
    }
}
