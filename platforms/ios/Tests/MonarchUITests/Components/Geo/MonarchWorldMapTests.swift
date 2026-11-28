import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let worldMapSample = [
    MonarchWorldMapDatum("us", value: 1_200, details: [MonarchWorldMapDetail("Users", value: "340")]),
    MonarchWorldMapDatum("DE", value: 300, details: [MonarchWorldMapDetail("Users", value: "90")]),
    MonarchWorldMapDatum("IN", value: 150),
    MonarchWorldMapDatum("SG", value: 40),
    MonarchWorldMapDatum("XX", value: 5),
]

enum MonarchWorldMapFixtures: ComponentFixtures {
    static let id = "world-map"
    static let fixtures = [
        Fixture("choropleth with a dot, an unplaced code and extras") {
            MonarchWorldMap(
                "Workspaces by country",
                data: worldMapSample,
                description: "Last 30 days",
                extras: [MonarchWorldMapExtra("Global", value: 75), MonarchWorldMapExtra("EU", value: 410)]
            )
        },
        Fixture("bubbles in another colour") {
            MonarchWorldMap("Sessions", data: worldMapSample, variant: .bubble, color: .data3)
        },
        Fixture("thresholds, no legend, footer") {
            MonarchWorldMap(
                "Revenue",
                data: worldMapSample,
                thresholds: [50, 200, 1_000],
                showsLegend: false,
                footer: "Source: billing"
            )
        },
        Fixture("one country, custom names and format") {
            MonarchWorldMap(
                "Signups",
                data: [MonarchWorldMapDatum("BR", value: 0.42)],
                countryName: { "Country \($0)" },
                formatValue: { "\(Int($0 * 100))%" }
            )
        },
        Fixture("only extras") {
            MonarchWorldMap("Traffic", data: [], extras: [MonarchWorldMapExtra("Global", value: 12)])
        },
        Fixture("empty") {
            MonarchWorldMap("Traffic", data: [], emptyMessage: "Data appears after the first visit.")
        },
        Fixture("loading") {
            MonarchWorldMap("Traffic", data: worldMapSample, status: .loading)
        },
        Fixture("error with retry") {
            MonarchWorldMap("Traffic", data: worldMapSample, status: .error, onRetry: {})
        },
    ]
}

@Suite("MonarchWorldMap")
struct MonarchWorldMapTests {
    private typealias Tokens = MonarchWorldMapTokens
    private typealias Geometry = MonarchWorldMapGeometry

    private let english = Locale(identifier: "en_US")

    private func name(_ code: String) -> String {
        Tokens.countryName(code, locale: english, custom: nil)
    }

    private func format(_ value: Double) -> String {
        value.formatted(.number.locale(english))
    }

    @Test("The outline has every country by code, and small countries have a centroid only")
    func geometry() throws {
        #expect(Geometry.shapes.count == 174)
        #expect(Geometry.centroids.count == 235)
        #expect(Geometry.shapes.keys.allSatisfy { $0.count == 2 && $0.allSatisfy(\.isUppercase) })
        #expect(Geometry.shapes["SG"] == nil)
        #expect(Geometry.centroids["SG"] == CGPoint(x: 788, y: 229))
        #expect(Geometry.isPlaced("SG"))
        #expect(!Geometry.isPlaced("XX"))
        let france = try #require(Geometry.shapes["FR"])
        #expect(france.count >= 2, "mainland France and Corsica")
        // Russia and Fiji cross the 180th meridian and run a little past the right edge, as on the web.
        for (code, rings) in Geometry.shapes {
            let overflow: CGFloat = ["RU", "FJ"].contains(code) ? 30 : 0
            for ring in rings {
                #expect(ring.allSatisfy { $0.x >= 0 && $0.x <= Geometry.size.width + overflow }, "\(code)")
                #expect(ring.allSatisfy { $0.y >= 0 && $0.y <= Geometry.size.height }, "\(code)")
            }
        }
    }

    @Test("Path data is read as moves, relative lines and closes")
    func parsing() throws {
        let shapes = Geometry.parseShapes("AA M0 0l10 0l0 10l-10 0z\n  M20 20l5 0l0 5z\nBB M1 1L4 1L4 4z")
        let first = try #require(shapes["AA"])
        #expect(first.count == 2)
        #expect(first[0] == [CGPoint(x: 0, y: 0), CGPoint(x: 10, y: 0), CGPoint(x: 10, y: 10), CGPoint(x: 0, y: 10)])
        #expect(first[1] == [CGPoint(x: 20, y: 20), CGPoint(x: 25, y: 20), CGPoint(x: 25, y: 25)])
        #expect(shapes["BB"]?.first == [CGPoint(x: 1, y: 1), CGPoint(x: 4, y: 1), CGPoint(x: 4, y: 4)])
        let centroids = Geometry.parseCentroids("AA 1 2  BB 30 40")
        #expect(centroids == ["AA": CGPoint(x: 1, y: 2), "BB": CGPoint(x: 30, y: 40)])
    }

    @Test("A point is inside by the even-odd rule")
    func containment() throws {
        let square = [[CGPoint(x: 0, y: 0), CGPoint(x: 10, y: 0), CGPoint(x: 10, y: 10), CGPoint(x: 0, y: 10)]]
        #expect(Geometry.contains(square, CGPoint(x: 5, y: 5)))
        #expect(!Geometry.contains(square, CGPoint(x: 15, y: 5)))
        let hole = square + [[CGPoint(x: 3, y: 3), CGPoint(x: 7, y: 3), CGPoint(x: 7, y: 7), CGPoint(x: 3, y: 7)]]
        #expect(!Geometry.contains(hole, CGPoint(x: 5, y: 5)))
        #expect(Geometry.contains(hole, CGPoint(x: 1, y: 1)))
        let germany = try #require(Geometry.shapes["DE"])
        #expect(Geometry.contains(germany, CGPoint(x: 529, y: 91)))
        #expect(!Geometry.contains(germany, CGPoint(x: 225, y: 123)))
    }

    @Test("Rows are cleaned: upper-case codes, finite values, the last entry wins, largest first")
    func rows() {
        let rows = Tokens.rows(
            worldMapSample + [
                MonarchWorldMapDatum(" de ", value: 310),
                MonarchWorldMapDatum("FR", value: .nan),
                MonarchWorldMapDatum("AT", value: 150),
            ]
        )
        #expect(rows.map(\.country) == ["US", "DE", "AT", "IN", "SG", "XX"])
        #expect(rows[1].value == 310)
        #expect(Tokens.placed(rows).map(\.country) == ["US", "DE", "AT", "IN", "SG"])
        #expect(Tokens.unplaced(rows).map(\.country) == ["XX"])
        #expect(Tokens.maximum(rows) == 1_200)
        #expect(Tokens.maximum([]) == 0)
    }

    @Test("A ready map without values or extras is empty")
    func status() {
        #expect(Tokens.status(.ready, rows: [], extras: []) == .empty)
        #expect(Tokens.status(.ready, rows: [], extras: [MonarchWorldMapExtra("Global", value: 1)]) == .ready)
        #expect(Tokens.status(.ready, rows: Tokens.rows(worldMapSample), extras: []) == .ready)
        #expect(Tokens.status(.loading, rows: [], extras: []) == .loading)
        #expect(Tokens.status(.error, rows: [], extras: []) == .error)
    }

    @Test("Countries fill by level; bubbles and countries without a value are neutral")
    func levels() {
        let rows = Tokens.rows(worldMapSample)
        let scale = Tokens.scale(rows, thresholds: nil)
        #expect(scale.thresholds == [300, 600, 900])
        #expect(Tokens.level(rows[0], scale: scale, variant: .choropleth) == 4)
        #expect(Tokens.level(rows[1], scale: scale, variant: .choropleth) == 2)
        #expect(Tokens.level(rows[2], scale: scale, variant: .choropleth) == 1)
        #expect(Tokens.level(nil, scale: scale, variant: .choropleth) == nil)
        #expect(Tokens.level(rows[0], scale: scale, variant: .bubble) == nil)
        #expect(Tokens.scale(rows, thresholds: [10, 20, 30]).thresholds == [10, 20, 30])
        let base = MonarchRGBA(red: 1, green: 0, blue: 0)
        let muted = MonarchRGBA(red: 0, green: 0, blue: 1)
        #expect(Tokens.fill(level: nil, base: base, muted: muted) == muted)
        #expect(Tokens.fill(level: 0, base: base, muted: muted) == muted)
        #expect(Tokens.fill(level: 4, base: base, muted: muted) == base)
        #expect(Tokens.fill(level: 2, base: base, muted: muted) == MonarchRGBA(red: 0.5, green: 0, blue: 0.5))
        let surface = MonarchRGBA(red: 1, green: 1, blue: 1)
        #expect(
            Tokens.bubbleFill(base: base, surface: surface, isActive: false)
                == MonarchRGBA(red: 1, green: 0.75, blue: 0.75)
        )
        #expect(
            Tokens.bubbleFill(base: base, surface: surface, isActive: true)
                == MonarchRGBA(red: 1, green: 0.5, blue: 0.5)
        )
    }

    @Test("Small countries get a dot; bubbles grow by area")
    func marks() {
        let placed = Tokens.placed(Tokens.rows(worldMapSample))
        #expect(Tokens.marks(placed, variant: .choropleth).map(\.country) == ["SG"])
        #expect(Tokens.marks(placed, variant: .bubble).map(\.country) == ["US", "DE", "IN", "SG"])
        #expect(Tokens.markRadius(40, maximum: 1_200, variant: .choropleth) == Tokens.dotRadius)
        #expect(Tokens.markRadius(1_200, maximum: 1_200, variant: .bubble) == 26)
        #expect(Tokens.markRadius(300, maximum: 1_200, variant: .bubble) == 15)
        #expect(Tokens.markRadius(0, maximum: 1_200, variant: .bubble) == 4)
        #expect(Tokens.markRadius(5, maximum: 0, variant: .bubble) == 4)
    }

    @Test("A tap finds the mark, then the outline, then the nearest country with a value")
    func hitTesting() {
        let placed = Tokens.placed(Tokens.rows(worldMapSample))
        func hit(
            _ x: CGFloat,
            _ y: CGFloat,
            _ variant: MonarchWorldMapVariant = .choropleth,
            tolerance: CGFloat = 10
        ) -> String? {
            Tokens.hit(CGPoint(x: x, y: y), placed: placed, variant: variant, maximum: 1_200, tolerance: tolerance)
        }
        #expect(hit(529, 91) == "DE")
        #expect(hit(225, 123) == "US")
        #expect(hit(788, 229) == "SG", "the dot of a country without an outline")
        #expect(hit(795, 229) == "SG", "within the tolerance of its centre")
        #expect(hit(507, 104) == nil, "France has no value and Germany is too far")
        #expect(hit(507, 104, tolerance: 30) == "DE", "the nearest country with a value within the tolerance")
        #expect(hit(100, 300) == nil, "open sea")
        #expect(hit(500, 300, .bubble) == nil)
        #expect(hit(230, 123, .bubble) == "US", "inside the bubble")
    }

    @Test("VoiceOver swipes go through the countries by value and stop at the ends")
    func adjusting() {
        #expect(Tokens.adjusted(nil, forward: true, count: 4) == 0)
        #expect(Tokens.adjusted(nil, forward: false, count: 4) == 3)
        #expect(Tokens.adjusted(0, forward: true, count: 4) == 1)
        #expect(Tokens.adjusted(3, forward: true, count: 4) == 3)
        #expect(Tokens.adjusted(0, forward: false, count: 4) == 0)
        #expect(Tokens.adjusted(nil, forward: true, count: 0) == nil)
    }

    @Test("The summary names the count and the three largest values")
    func summary() {
        let rows = Tokens.rows(worldMapSample)
        let labels = MonarchWorldMapLabels()
        let top = Tokens.top(rows, name: name, format: format)
        #expect(top == "United States 1,200, Germany 300, India 150")
        #expect(
            Tokens.summary(count: 5, countText: "5", top: top, noValue: "No value", labels: labels)
                == "5 countries. Highest: United States 1,200, Germany 300, India 150."
        )
        #expect(
            Tokens.summary(count: 1, countText: "1", top: "Brazil 3", noValue: "No value", labels: labels)
                == "1 country. Highest: Brazil 3."
        )
        #expect(Tokens.summary(count: 0, countText: "0", top: "", noValue: "No value", labels: labels) == "No value")
        let custom = MonarchWorldMapLabels(summary: { count, top in "\(count): \(top)" })
        #expect(Tokens.summary(count: 2, countText: "2", top: "A, B", noValue: "", labels: custom) == "2: A, B")
    }

    @Test("A country is read with its value and details, named in the locale")
    func describing() {
        #expect(name("DE") == "Germany")
        #expect(name("XX") == "XX", "an unknown code is shown as is")
        #expect(Tokens.countryName("DE", locale: english, custom: { "<\($0)>" }) == "<DE>")
        #expect(Tokens.countryName("DE", locale: Locale(identifier: "de_DE"), custom: nil) == "Deutschland")
        let us = MonarchWorldMapDatum("US", value: 1_200, details: [MonarchWorldMapDetail("Users", value: "340")])
        #expect(
            Tokens.describe(name: "United States", value: "1,200", details: us.details)
                == "United States: 1,200, Users 340"
        )
        #expect(Tokens.describe(name: "India", value: "150", details: []) == "India: 150")
    }

    @Test("Codes that are not on the map are listed with their total")
    func unplacedNote() {
        let labels = MonarchWorldMapLabels()
        let unplaced = [MonarchWorldMapDatum("XX", value: 5), MonarchWorldMapDatum("QQ", value: 7)]
        #expect(Tokens.unplacedNote([], name: name, format: format, labels: labels) == nil)
        #expect(
            Tokens.unplacedNote(unplaced, name: name, format: format, labels: labels) == "Not on the map: XX, QQ (12)"
        )
        let custom = MonarchWorldMapLabels(unplaced: { names, total in "\(names)=\(total)" })
        #expect(Tokens.unplacedNote(unplaced, name: name, format: format, labels: custom) == "XX, QQ=12")
    }

    @Test("The table lists every country, its details and the extras")
    func table() {
        let rows = Tokens.rows(worldMapSample)
        let table = Tokens.table(
            rows: rows,
            extras: [MonarchWorldMapExtra("Global", value: 75)],
            name: name,
            format: format,
            labels: MonarchWorldMapLabels()
        )
        #expect(table.categoryTitle == "Country")
        #expect(table.categories == ["United States", "Germany", "India", "Singapore", "XX", "Global"])
        #expect(table.series.map(\.name) == ["Value", "Users"])
        #expect(table.series[0].formattedValues == ["1,200", "300", "150", "40", "5", "75"])
        #expect(table.series[1].formattedValues == ["340", "90", "", "", "", ""])
        #expect(table.series[1].values.allSatisfy { $0 == nil })
        let plain = Tokens.table(
            rows: rows,
            extras: [],
            name: name,
            format: format,
            labels: MonarchWorldMapLabels(country: "Land", value: "Wert"),
            includesDetails: false
        )
        #expect(plain.categoryTitle == "Land")
        #expect(plain.series.map(\.name) == ["Wert"])
    }

    @Test("The legend reads the map's own words")
    func legend() {
        let labels = Tokens.legendLabels(MonarchWorldMapLabels(less: "Fewer", more: "Most", range: "%1$@–%2$@"))
        #expect(labels.less == "Fewer")
        #expect(labels.more == "Most")
        #expect(labels.range == "%1$@–%2$@")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchWorldMapStrings.country == "Country")
        #expect(MonarchWorldMapStrings.value == "Value")
        #expect(MonarchWorldMapStrings.extras == "Other values")
        #expect(MonarchWorldMapStrings.unplaced("XX", "5") == "Not on the map: XX (5)")
        #expect(MonarchWorldMapStrings.summaryOne("1", "Brazil 3") == "1 country. Highest: Brazil 3.")
        #expect(MonarchWorldMapStrings.summary("2", "A, B") == "2 countries. Highest: A, B.")
        #expect(MonarchWorldMapStrings.countryValue("India", "150") == "India: 150")
    }

    @Test("The map keeps its geography: aspect ratio and a tap radius of half the touch target")
    func measures() {
        #expect(Tokens.aspectRatio == CGFloat(1_000) / CGFloat(394))
        #expect(Tokens.tapRadius * 2 == MonarchAccessibility.minimumHitTarget)
    }
}
