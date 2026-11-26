import CoreGraphics
import SwiftUI

/// The parsed outline of ``MonarchWorldMap``: rings of points per country and
/// a centroid per country, in the 1000 by 394 map space. Parsed once.
enum MonarchWorldMapGeometry {
    /// The size of the map space.
    static let size = CGSize(width: MonarchWorldMapGeometrySource.width, height: MonarchWorldMapGeometrySource.height)

    /// The outline of each country with one: closed rings of points.
    static let shapes: [String: [[CGPoint]]] = parseShapes(MonarchWorldMapGeometrySource.shapes)

    /// The position of each country's largest landmass, including countries too small for an outline.
    static let centroids: [String: CGPoint] = parseCentroids(MonarchWorldMapGeometrySource.centroids)

    /// Whether a country can be drawn: it has an outline or a centroid.
    static func isPlaced(_ code: String) -> Bool {
        shapes[code] != nil || centroids[code] != nil
    }

    /// Reads entries of a two-letter code followed by path data with the
    /// commands `M` (absolute move), `l` (relative line), `L` (absolute line)
    /// and `z` (close).
    static func parseShapes(_ source: String) -> [String: [[CGPoint]]] {
        var result: [String: [[CGPoint]]] = [:]
        var scanner = MonarchWorldMapPathScanner(Array(source))
        var code: String?
        var rings: [[CGPoint]] = []
        var ring: [CGPoint] = []
        var current = CGPoint.zero
        var start = CGPoint.zero
        var command: Character = "M"

        func closeRing() {
            if ring.count > 2 { rings.append(ring) }
            ring = []
        }
        func finishCountry() {
            closeRing()
            if let code, !rings.isEmpty { result[code] = rings }
            rings = []
        }

        while let token = scanner.next() {
            switch token {
            case .code(let next):
                finishCountry()
                code = next
                current = .zero
                command = "M"
            case .command(let next):
                command = next
                if next == "z" || next == "Z" {
                    closeRing()
                    current = start
                }
            case .number(let x):
                guard case .number(let y) = scanner.next() else { continue }
                switch command {
                case "M", "m":
                    closeRing()
                    current = command == "M" ? CGPoint(x: x, y: y) : CGPoint(x: current.x + x, y: current.y + y)
                    start = current
                    ring = [current]
                    // Pairs after a move are lines, as in SVG.
                    command = command == "M" ? "L" : "l"
                case "L":
                    current = CGPoint(x: x, y: y)
                    ring.append(current)
                default:
                    current = CGPoint(x: current.x + x, y: current.y + y)
                    ring.append(current)
                }
            }
        }
        finishCountry()
        return result
    }

    /// Reads entries of a two-letter code followed by x and y.
    static func parseCentroids(_ source: String) -> [String: CGPoint] {
        var result: [String: CGPoint] = [:]
        var scanner = MonarchWorldMapPathScanner(Array(source))
        while let token = scanner.next() {
            guard case .code(let code) = token, case .number(let x) = scanner.next(),
                case .number(let y) = scanner.next()
            else { continue }
            result[code] = CGPoint(x: x, y: y)
        }
        return result
    }

    /// Whether a point is inside the rings, by the even-odd rule, so holes and islands work.
    static func contains(_ rings: [[CGPoint]], _ point: CGPoint) -> Bool {
        var inside = false
        for ring in rings {
            guard var previous = ring.last else { continue }
            for vertex in ring {
                if (vertex.y > point.y) != (previous.y > point.y) {
                    let crossing = (previous.x - vertex.x) * (point.y - vertex.y) / (previous.y - vertex.y) + vertex.x
                    if point.x < crossing { inside.toggle() }
                }
                previous = vertex
            }
        }
        return inside
    }

    /// The rings as a path, scaled from the map space.
    static func path(_ rings: [[CGPoint]], scale: CGFloat) -> Path {
        var path = Path()
        for ring in rings {
            path.addLines(ring.map { CGPoint(x: $0.x * scale, y: $0.y * scale) })
            path.closeSubpath()
        }
        return path
    }
}

/// Splits outline data into country codes, path commands and numbers.
private struct MonarchWorldMapPathScanner {
    enum Token: Equatable {
        case code(String)
        case command(Character)
        case number(CGFloat)
    }

    private let characters: [Character]
    private var index = 0

    init(_ characters: [Character]) {
        self.characters = characters
    }

    mutating func next() -> Token? {
        while index < characters.count, characters[index].isWhitespace || characters[index] == "," { index += 1 }
        guard index < characters.count else { return nil }
        let character = characters[index]
        if character.isUppercase, index + 1 < characters.count, characters[index + 1].isUppercase {
            let code = String(characters[index...(index + 1)])
            index += 2
            return .code(code)
        }
        if character.isLetter {
            index += 1
            return .command(character)
        }
        let start = index
        if character == "-" || character == "+" { index += 1 }
        while index < characters.count, characters[index].isNumber || characters[index] == "." { index += 1 }
        guard index > start, let value = Double(String(characters[start..<index])) else {
            index = max(index, start + 1)
            return next()
        }
        return .number(CGFloat(value))
    }
}
