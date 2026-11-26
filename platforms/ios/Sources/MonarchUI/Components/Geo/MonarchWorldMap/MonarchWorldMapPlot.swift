import MonarchTokens
import SwiftUI

/// The drawn map of ``MonarchWorldMap``: every country's outline, filled by
/// level, then the dots or bubbles, and the outline of the inspected country.
///
/// It is a `Canvas`, which SwiftUI does not mirror, and its layout direction
/// is fixed to left to right, so the map and its tap positions keep their
/// geography in right-to-left languages.
struct MonarchWorldMapPlot: View {
    let rows: [String: MonarchWorldMapDatum]
    let placed: [MonarchWorldMapDatum]
    let variant: MonarchWorldMapVariant
    let color: MonarchChartColor
    let scale: MonarchHeatmapTokens.Scale
    let maximum: Double
    let active: String?
    let onTap: (String?) -> Void

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let colors = theme.colors.tokens
        let base = colors[keyPath: MonarchChartLegendTokens.role(color)]
        let muted = colors.bgMuted
        let surface = colors.bgSurface
        let outline = colors.textDefault
        let marks = MonarchWorldMapTokens.marks(placed, variant: variant)
        GeometryReader { proxy in
            let ratio = proxy.size.width / MonarchWorldMapGeometry.size.width
            Canvas { context, _ in
                for (code, rings) in MonarchWorldMapGeometry.shapes {
                    let path = MonarchWorldMapGeometry.path(rings, scale: ratio)
                    let level = MonarchWorldMapTokens.level(rows[code], scale: scale, variant: variant)
                    context.fill(
                        path,
                        with: .color(Color(monarch: MonarchWorldMapTokens.fill(level: level, base: base, muted: muted)))
                    )
                    context.stroke(
                        path,
                        with: .color(Color(monarch: surface)),
                        style: StrokeStyle(lineWidth: MonarchWorldMapTokens.countryBorderWidth, lineJoin: .round)
                    )
                }
                if variant == .choropleth, let active, let rings = MonarchWorldMapGeometry.shapes[active] {
                    context.stroke(
                        MonarchWorldMapGeometry.path(rings, scale: ratio),
                        with: .color(Color(monarch: outline)),
                        style: StrokeStyle(lineWidth: MonarchWorldMapTokens.activeBorderWidth, lineJoin: .round)
                    )
                }
                for datum in marks {
                    guard let centre = MonarchWorldMapGeometry.centroids[datum.country] else { continue }
                    let radius =
                        MonarchWorldMapTokens.markRadius(datum.value, maximum: maximum, variant: variant) * ratio
                    let circle = Path(
                        ellipseIn: CGRect(origin: CGPoint(x: centre.x * ratio, y: centre.y * ratio), size: .zero)
                            .insetBy(dx: -radius, dy: -radius)
                    )
                    let isActive = datum.country == active
                    let fill =
                        variant == .bubble
                        ? MonarchWorldMapTokens.bubbleFill(base: base, surface: surface, isActive: isActive)
                        : MonarchWorldMapTokens.fill(level: scale.level(datum.value), base: base, muted: muted)
                    context.fill(circle, with: .color(Color(monarch: fill)))
                    context.stroke(
                        circle,
                        with: .color(Color(monarch: isActive ? outline : (variant == .bubble ? base : surface))),
                        lineWidth: isActive
                            ? MonarchWorldMapTokens.activeBorderWidth : MonarchWorldMapTokens.markBorderWidth
                    )
                }
            }
            .contentShape(Rectangle())
            .gesture(
                SpatialTapGesture()
                    .onEnded { value in
                        guard ratio > 0 else { return }
                        let point = CGPoint(x: value.location.x / ratio, y: value.location.y / ratio)
                        onTap(
                            MonarchWorldMapTokens.hit(
                                point,
                                placed: placed,
                                variant: variant,
                                maximum: maximum,
                                tolerance: MonarchWorldMapTokens.tapRadius / ratio
                            )
                        )
                    }
            )
        }
        .aspectRatio(MonarchWorldMapTokens.aspectRatio, contentMode: .fit)
        // Russia and Fiji run past the 180th meridian; keep them inside the map.
        .clipped()
        .environment(\.layoutDirection, .leftToRight)
    }
}
