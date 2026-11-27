/// A JSON parser that keeps object keys in document order, which
/// `JSONSerialization` and `JSONDecoder` do not.
struct MonarchJsonViewerParser {
    /// The deepest nesting accepted, so a hostile document cannot exhaust the
    /// stack here or in the recursive search, flattening and copying.
    static let maxDepth = 100

    private let bytes: [UInt8]
    private var index = 0

    private init(_ bytes: [UInt8]) {
        self.bytes = bytes
    }

    /// Parses a whole document.
    static func parse(_ bytes: [UInt8]) throws -> MonarchJsonValue {
        var parser = MonarchJsonViewerParser(bytes)
        // Skip a UTF-8 byte order mark.
        if bytes.starts(with: [0xEF, 0xBB, 0xBF]) { parser.index = 3 }
        parser.skipSpace()
        let value = try parser.value(depth: 0)
        parser.skipSpace()
        guard parser.index == bytes.count else { throw MonarchJsonParseError(offset: parser.index) }
        return value
    }

    private var current: UInt8? { index < bytes.count ? bytes[index] : nil }

    private func failure() -> MonarchJsonParseError { MonarchJsonParseError(offset: index) }

    private mutating func skipSpace() {
        while let byte = current, byte == 0x20 || byte == 0x0A || byte == 0x0D || byte == 0x09 {
            index += 1
        }
    }

    private mutating func expect(_ byte: UInt8) throws {
        guard current == byte else { throw failure() }
        index += 1
    }

    private mutating func value(depth: Int) throws -> MonarchJsonValue {
        guard depth < Self.maxDepth else { throw failure() }
        switch current {
        case UInt8(ascii: "{"): return try object(depth: depth)
        case UInt8(ascii: "["): return try array(depth: depth)
        case UInt8(ascii: "\""): return .string(try string())
        case UInt8(ascii: "t"): return try literal("true", .bool(true))
        case UInt8(ascii: "f"): return try literal("false", .bool(false))
        case UInt8(ascii: "n"): return try literal("null", .null)
        case .some(let byte) where byte == UInt8(ascii: "-") || (0x30...0x39).contains(byte):
            return .number(try number())
        default: throw failure()
        }
    }

    private mutating func literal(_ word: String, _ result: MonarchJsonValue) throws -> MonarchJsonValue {
        for byte in word.utf8 { try expect(byte) }
        return result
    }

    private mutating func object(depth: Int) throws -> MonarchJsonValue {
        try expect(UInt8(ascii: "{"))
        var members: [(key: String, value: MonarchJsonValue)] = []
        skipSpace()
        if current == UInt8(ascii: "}") {
            index += 1
            return .object(members)
        }
        while true {
            skipSpace()
            let key = try string()
            skipSpace()
            try expect(UInt8(ascii: ":"))
            skipSpace()
            let member = try value(depth: depth + 1)
            // A repeated key keeps its first position and its last value, as JSON.parse does.
            if let existing = members.firstIndex(where: { $0.key == key }) {
                members[existing].value = member
            } else {
                members.append((key: key, value: member))
            }
            skipSpace()
            if current == UInt8(ascii: ",") {
                index += 1
                continue
            }
            try expect(UInt8(ascii: "}"))
            return .object(members)
        }
    }

    private mutating func array(depth: Int) throws -> MonarchJsonValue {
        try expect(UInt8(ascii: "["))
        var elements: [MonarchJsonValue] = []
        skipSpace()
        if current == UInt8(ascii: "]") {
            index += 1
            return .array(elements)
        }
        while true {
            skipSpace()
            elements.append(try value(depth: depth + 1))
            skipSpace()
            if current == UInt8(ascii: ",") {
                index += 1
                continue
            }
            try expect(UInt8(ascii: "]"))
            return .array(elements)
        }
    }

    private mutating func number() throws -> String {
        let start = index
        if current == UInt8(ascii: "-") { index += 1 }
        try digits()
        if current == UInt8(ascii: ".") {
            index += 1
            try digits()
        }
        if current == UInt8(ascii: "e") || current == UInt8(ascii: "E") {
            index += 1
            if current == UInt8(ascii: "+") || current == UInt8(ascii: "-") { index += 1 }
            try digits()
        }
        return String(decoding: bytes[start..<index], as: UTF8.self)
    }

    private mutating func digits() throws {
        let start = index
        while let byte = current, (0x30...0x39).contains(byte) { index += 1 }
        guard index > start else { throw failure() }
    }

    private mutating func string() throws -> String {
        try expect(UInt8(ascii: "\""))
        var scalars = String.UnicodeScalarView()
        var run = index
        func flush(_ end: Int) {
            scalars.append(contentsOf: String(decoding: bytes[run..<end], as: UTF8.self).unicodeScalars)
        }
        while let byte = current {
            switch byte {
            case UInt8(ascii: "\""):
                flush(index)
                index += 1
                return String(scalars)
            case UInt8(ascii: "\\"):
                flush(index)
                index += 1
                scalars.append(try escape())
                run = index
            case 0x00..<0x20:
                throw failure()
            default:
                index += 1
            }
        }
        throw failure()
    }

    private mutating func escape() throws -> Unicode.Scalar {
        guard let byte = current else { throw failure() }
        index += 1
        switch byte {
        case UInt8(ascii: "\""): return "\""
        case UInt8(ascii: "\\"): return "\\"
        case UInt8(ascii: "/"): return "/"
        case UInt8(ascii: "b"): return "\u{08}"
        case UInt8(ascii: "f"): return "\u{0C}"
        case UInt8(ascii: "n"): return "\n"
        case UInt8(ascii: "r"): return "\r"
        case UInt8(ascii: "t"): return "\t"
        case UInt8(ascii: "u"):
            let first = try hex4()
            if (0xD800..<0xDC00).contains(first), current == UInt8(ascii: "\\"),
                index + 1 < bytes.count, bytes[index + 1] == UInt8(ascii: "u")
            {
                let mark = index
                index += 2
                let second = try hex4()
                if (0xDC00..<0xE000).contains(second) {
                    let combined = 0x10000 + ((first - 0xD800) << 10) + (second - 0xDC00)
                    return Unicode.Scalar(combined) ?? "\u{FFFD}"
                }
                index = mark
            }
            return Unicode.Scalar(first) ?? "\u{FFFD}"
        default:
            throw MonarchJsonParseError(offset: index - 1)
        }
    }

    private mutating func hex4() throws -> UInt32 {
        guard index + 4 <= bytes.count else { throw failure() }
        var result: UInt32 = 0
        for _ in 0..<4 {
            guard let byte = current, let digit = Self.hexValue(byte) else { throw failure() }
            result = result << 4 | digit
            index += 1
        }
        return result
    }

    private static func hexValue(_ byte: UInt8) -> UInt32? {
        switch byte {
        case 0x30...0x39: UInt32(byte - 0x30)
        case 0x41...0x46: UInt32(byte - 0x41 + 10)
        case 0x61...0x66: UInt32(byte - 0x61 + 10)
        default: nil
        }
    }
}
