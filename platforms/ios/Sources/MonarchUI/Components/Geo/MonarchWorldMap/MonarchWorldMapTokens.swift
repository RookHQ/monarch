import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchWorldMap``.
enum MonarchWorldMapTokens {
    /// The least height kept for the map in every status: 120 points.
    static let minimumHeight: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space6
    /// The gap between the map, the country details, the legend and the tiles.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between tiles under the map.
    static let tileSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between a tile's label and its value.
    static let tileContentSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// Padding before and after a tile's text.
    static let tileHorizontalPadding: CGFloat = MonarchTokens.Space.space2_5
    /// Padding above and below a tile's text.
    static let tileVerticalPadding: CGFloat = MonarchTokens.Space.space1
    /// The share of the border width used between countries.
    static let countryBorderRatio: CGFloat = 0.5
    /// The width of the line between countries.
    static let countryBorderWidth: CGFloat = MonarchShapes.borderWidth * countryBorderRatio
    /// The width of the outline of the inspected country or mark.
    static let activeBorderWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The width of the outline of a dot or bubble.
    static let markBorderWidth: CGFloat = MonarchShapes.borderWidth
    /// The radius of the dot of a country too small for its outline, in map units.
    static let dotRadius: CGFloat = 3.5
    /// The radius of the smallest bubble, in map units.
    static let bubbleMinimumRadius: CGFloat = 4
    /// How much larger than the smallest the largest bubble is, in map units.
    static let bubbleRadiusRange: CGFloat = 22
    /// How much of the data colour a bubble mixes into the surface.
    static let bubbleFillMix = 0.25
    /// How much of the data colour the inspected bubble mixes into the surface.
    static let activeBubbleFillMix = 0.5
    /// How many countries the summary names.
    static let topCount = 3
    /// Width over height of the map.
    static let aspectRatio: CGFloat = MonarchWorldMapGeometry.size.width / MonarchWorldMapGeometry.size.height
    /// How far from a country's centre, in points, a tap still finds it.
    static let tapRadius: CGFloat = MonarchAccessibility.minimumHitTarget / 2

    // MARK: - Data

    /// The values to show: finite values only, the last entry per code wins,
    /// largest first, then by code.
    static func rows(_ data: [MonarchWorldMapDatum]) -> [MonarchWorldMapDatum] {
        var byCode: [String: MonarchWorldMapDatum] = [:]
        for datum in data where datum.value.isFinite && !datum.country.isEmpty {
            byCode[datum.country] = datum
        }
        return byCode.values.sorted { $0.value != $1.value ? $0.value > $1.value : $0.country < $1.country }
    }

    /// The rows the map can draw, in value order.
    static func placed(_ rows: [MonarchWorldMapDatum]) -> [MonarchWorldMapDatum] {
        rows.filter { MonarchWorldMapGeometry.isPlaced($0.country) }
    }

    /// The rows with codes the map cannot draw.
    static func unplaced(_ rows: [MonarchWorldMapDatum]) -> [MonarchWorldMapDatum] {
        rows.filter { !MonarchWorldMapGeometry.isPlaced($0.country) }
    }

    /// The status shown: empty when the map is ready but has nothing to show.
    static func status(
        _ status: MonarchChartFrameStatus,
        rows: [MonarchWorldMapDatum],
        extras: [MonarchWorldMapExtra]
    ) -> MonarchChartFrameStatus {
        status == .ready && rows.isEmpty && extras.isEmpty ? .empty : status
    }

    /// The largest value, never below zero.
    static func maximum(_ rows: [MonarchWorldMapDatum]) -> Double {
        max(0, rows.map(\.value).max() ?? 0)
    }

    /// The colour scale: three given cut points, or quarters of the largest value, as in the heatmap.
    static func scale(_ rows: [MonarchWorldMapDatum], thresholds: [Double]?) -> MonarchHeatmapTokens.Scale {
        MonarchHeatmapTokens.scale(values: rows.map(\.value), thresholds: thresholds)
    }

    /// The countries drawn as circles, largest first so smaller ones sit on
    /// top: every placed country for bubbles, and countries without an
    /// outline for the choropleth.
    static func marks(_ placed: [MonarchWorldMapDatum], variant: MonarchWorldMapVariant) -> [MonarchWorldMapDatum] {
        placed.filter {
            MonarchWorldMapGeometry.centroids[$0.country] != nil
                && (variant == .bubble || MonarchWorldMapGeometry.shapes[$0.country] == nil)
        }
    }

    /// The radius of a mark in map units: a dot, or a bubble whose area grows with the value.
    static func markRadius(_ value: Double, maximum: Double, variant: MonarchWorldMapVariant) -> CGFloat {
        guard variant == .bubble else { return dotRadius }
        guard maximum > 0 else { return bubbleMinimumRadius }
        return bubbleMinimumRadius + CGFloat((max(0, value) / maximum).squareRoot()) * bubbleRadiusRange
    }

    /// The level of a country's fill: `nil` (neutral) without a value or for bubbles.
    static func level(
        _ datum: MonarchWorldMapDatum?,
        scale: MonarchHeatmapTokens.Scale,
        variant: MonarchWorldMapVariant
    ) -> Int? {
        guard variant == .choropleth, let datum else { return nil }
        return scale.level(datum.value)
    }

    /// The fill of a country or dot: the muted background without a level, else the heatmap step.
    static func fill(level: Int?, base: MonarchRGBA, muted: MonarchRGBA) -> MonarchRGBA {
        MonarchHeatmapTokens.fill(level: level, base: base, muted: muted) ?? muted
    }

    /// The fill of a bubble: the data colour mixed into the surface, stronger when inspected.
    static func bubbleFill(base: MonarchRGBA, surface: MonarchRGBA, isActive: Bool) -> MonarchRGBA {
        MonarchHeatmapTokens.mix(base, into: surface, amount: isActive ? activeBubbleFillMix : bubbleFillMix)
    }

    // MARK: - Interaction

    /// The country at a point in map units: a dot or bubble first (the
    /// smallest on top), then the outline of a country with a value, then the
    /// nearest country with a value within `tolerance` map units.
    static func hit(
        _ point: CGPoint,
        placed: [MonarchWorldMapDatum],
        variant: MonarchWorldMapVariant,
        maximum largest: Double,
        tolerance: CGFloat
    ) -> String? {
        for datum in marks(placed, variant: variant).reversed() {
            guard let centre = MonarchWorldMapGeometry.centroids[datum.country] else { continue }
            let radius = markRadius(datum.value, maximum: largest, variant: variant)
            if distance(centre, point) <= radius { return datum.country }
        }
        for datum in placed {
            if let rings = MonarchWorldMapGeometry.shapes[datum.country],
                MonarchWorldMapGeometry.contains(rings, point)
            {
                return datum.country
            }
        }
        var nearest: (code: String, distance: CGFloat)?
        for datum in placed {
            guard let centre = MonarchWorldMapGeometry.centroids[datum.country] else { continue }
            let gap = distance(centre, point)
            if gap <= tolerance, gap < (nearest?.distance ?? .infinity) {
                nearest = (datum.country, gap)
            }
        }
        return nearest?.code
    }

    /// The index after a VoiceOver swipe through the placed countries by value:
    /// the first swipe goes to the largest (or the smallest, backwards).
    static func adjusted(_ index: Int?, forward: Bool, count: Int) -> Int? {
        guard count > 0 else { return nil }
        guard let index else { return forward ? 0 : count - 1 }
        return min(count - 1, max(0, index + (forward ? 1 : -1)))
    }

    // MARK: - Words

    /// A country's name: the app's, else the locale's name of the region, else the code.
    static func countryName(_ code: String, locale: Locale, custom: ((String) -> String)?) -> String {
        if let custom { return custom(code) }
        return locale.localizedString(forRegionCode: code) ?? code
    }

    /// A country as read: "Germany: 300, Users 90".
    static func describe(name: String, value: String, details: [MonarchWorldMapDetail]) -> String {
        let parts = [value] + details.map { "\($0.label) \($0.value)" }
        return MonarchWorldMapStrings.countryValue(name, parts.joined(separator: ", "))
    }

    /// The largest values as text: "United States 1,200, Germany 300, India 150".
    static func top(_ rows: [MonarchWorldMapDatum], name: (String) -> String, format: (Double) -> String) -> String {
        rows.prefix(topCount).map { "\(name($0.country)) \(format($0.value))" }.joined(separator: ", ")
    }

    /// The generated summary, which names the map for VoiceOver.
    static func summary(
        count: Int,
        countText: String,
        top: String,
        noValue: String,
        labels: MonarchWorldMapLabels
    ) -> String {
        if let summary = labels.summary { return summary(count, top) }
        guard count > 0 else { return noValue }
        return count == 1
            ? MonarchWorldMapStrings.summaryOne(countText, top) : MonarchWorldMapStrings.summary(countText, top)
    }

    /// The note for codes the map cannot draw, or `nil` when every code is placed.
    static func unplacedNote(
        _ unplaced: [MonarchWorldMapDatum],
        name: (String) -> String,
        format: (Double) -> String,
        labels: MonarchWorldMapLabels
    ) -> String? {
        guard !unplaced.isEmpty else { return nil }
        let names = unplaced.map { name($0.country) }.joined(separator: ", ")
        let total = format(unplaced.reduce(0) { $0 + $1.value })
        return labels.unplaced?(names, total) ?? MonarchWorldMapStrings.unplaced(names, total)
    }

    /// The labels of every detail line, in the order they first appear.
    static func detailLabels(_ rows: [MonarchWorldMapDatum]) -> [String] {
        var labels: [String] = []
        for datum in rows {
            for detail in datum.details where !labels.contains(detail.label) {
                labels.append(detail.label)
            }
        }
        return labels
    }

    /// The data table: one row per country, then one per extra; a value
    /// column and one column per detail label.
    static func table(
        rows: [MonarchWorldMapDatum],
        extras: [MonarchWorldMapExtra],
        name: (String) -> String,
        format: (Double) -> String,
        labels: MonarchWorldMapLabels,
        includesDetails: Bool = true
    ) -> MonarchChartTable {
        let valueTitle = labels.value ?? MonarchWorldMapStrings.value
        var series = [
            MonarchChartTableSeries(
                valueTitle,
                values: rows.map(\.value) + extras.map(\.value),
                formattedValues: rows.map { format($0.value) } + extras.map { format($0.value) }
            )
        ]
        if includesDetails {
            for label in detailLabels(rows) {
                let texts = rows.map { datum in datum.details.first { $0.label == label }?.value ?? "" }
                series.append(
                    MonarchChartTableSeries(
                        label,
                        values: Array(repeating: nil, count: rows.count + extras.count),
                        formattedValues: texts + Array(repeating: "", count: extras.count)
                    )
                )
            }
        }
        return MonarchChartTable(
            categoryTitle: labels.country ?? MonarchWorldMapStrings.country,
            categories: rows.map { name($0.country) } + extras.map(\.label),
            series: series,
            valueTitle: valueTitle
        )
    }

    /// The labels the heatmap legend reads, from the map's labels.
    static func legendLabels(_ labels: MonarchWorldMapLabels) -> MonarchHeatmapLabels {
        MonarchHeatmapLabels(
            frame: labels.frame,
            less: labels.less,
            more: labels.more,
            rangeBelow: labels.rangeBelow,
            range: labels.range,
            rangeAbove: labels.rangeAbove
        )
    }

    private static func distance(_ a: CGPoint, _ b: CGPoint) -> CGFloat {
        ((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)).squareRoot()
    }
}
