import MonarchTokens
import SwiftUI

/// Draws a filled shape behind the content, once per shadow layer.
struct MonarchElevationModifier<S: Shape>: ViewModifier {
    let level: MonarchElevationLevel
    let fill: MonarchColorRole
    let shape: S

    @Environment(\.monarchTheme) private var theme

    func body(content: Content) -> some View {
        let color = theme.colors[role: fill]
        let layers = theme.elevation.layers(for: level)
        content.background {
            ZStack {
                ForEach(Array(layers.enumerated()), id: \.offset) { _, layer in
                    shape.fill(color.shadow(Self.dropShadow(layer)))
                }
                shape.fill(color)
            }
        }
    }

    /// SwiftUI takes a blur radius, which is half the blur length of the
    /// token. It has no spread, so the token's spread is not drawn.
    static func dropShadow(_ layer: MonarchShadowLayer) -> ShadowStyle {
        .drop(
            color: Color(monarch: layer.color),
            radius: layer.blur / 2,
            x: layer.offsetX,
            y: layer.offsetY
        )
    }
}

extension View {
    /// Puts a filled surface with the theme's shadow behind this view.
    ///
    /// Each shadow layer of the level is drawn once, so a level with two
    /// layers does not cast a shadow of a shadow. The token's spread value is
    /// not drawn, because SwiftUI shadows have no spread.
    ///
    /// - Parameters:
    ///   - level: The elevation level.
    ///   - fill: The colour role of the surface. The default is `bgSurface`.
    ///   - shape: The shape of the surface.
    /// - Returns: The view on its surface.
    public func monarchElevation<S: Shape>(
        _ level: MonarchElevationLevel,
        fill: MonarchColorRole = \.bgSurface,
        in shape: S
    ) -> some View {
        modifier(MonarchElevationModifier(level: level, fill: fill, shape: shape))
    }
}
