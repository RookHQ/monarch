import SwiftUI

/// The grip dots of a drag handle: two columns of three for a vertical
/// axis, three columns of two for a horizontal one, three by three for both.
/// The pattern is symmetric, so it needs no mirroring right to left.
struct MonarchDragHandleGrip: Shape {
    /// The axis, which selects the pattern.
    let axis: MonarchDragHandleAxis

    func path(in rect: CGRect) -> Path {
        let unit = min(rect.width, rect.height) / MonarchDragHandleTokens.gridUnits
        let radius = MonarchDragHandleTokens.dotRadius * unit
        var path = Path()
        for dot in MonarchDragHandleTokens.dots(axis: axis) {
            path.addEllipse(
                in: CGRect(
                    x: rect.midX + (dot.x - MonarchDragHandleTokens.gridUnits / 2) * unit - radius,
                    y: rect.midY + (dot.y - MonarchDragHandleTokens.gridUnits / 2) * unit - radius,
                    width: radius * 2,
                    height: radius * 2
                )
            )
        }
        return path
    }
}
