import SwiftUI

/// Collects the bounds of every card in the chart, by node id, so the dotted
/// reporting lines can be drawn between real card positions.
struct MonarchOrgChartCardFrames: PreferenceKey {
    static var defaultValue: [String: Anchor<CGRect>] { [:] }

    static func reduce(value: inout [String: Anchor<CGRect>], nextValue: () -> [String: Anchor<CGRect>]) {
        value.merge(nextValue()) { current, _ in current }
    }
}

/// A dotted reporting line: the curve, or the dot at its end.
struct MonarchOrgChartDottedShape: Shape {
    let curve: MonarchOrgChartTokens.Curve
    let isDot: Bool

    func path(in rect: CGRect) -> Path {
        var path = Path()
        if isDot {
            let side = MonarchOrgChartTokens.dottedDot
            path.addEllipse(
                in: CGRect(x: curve.end.x - side / 2, y: curve.end.y - side / 2, width: side, height: side)
            )
        } else {
            path.move(to: curve.start)
            path.addCurve(to: curve.end, control1: curve.control1, control2: curve.control2)
        }
        return path
    }
}
