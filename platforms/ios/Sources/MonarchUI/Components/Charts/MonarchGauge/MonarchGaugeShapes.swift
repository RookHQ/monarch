import SwiftUI

/// Part of the half circle, from one fraction of the scale to another.
/// `inset` is the distance from the outer edge of the dial to the centre line
/// of the stroke.
struct MonarchGaugeArc: Shape {
    var from: Double
    var to: Double
    let inset: CGFloat

    var animatableData: Double {
        get { to }
        set { to = newValue }
    }

    func path(in rect: CGRect) -> Path {
        var path = Path()
        guard to > from else { return path }
        let steps = MonarchGaugeTokens.arcSteps
        for step in 0...steps {
            let fraction = from + (to - from) * Double(step) / Double(steps)
            let point = MonarchGaugeTokens.point(at: fraction, inset: inset, in: rect)
            if step == 0 {
                path.move(to: point)
            } else {
                path.addLine(to: point)
            }
        }
        return path
    }
}

/// A short line across the track at one fraction of the scale.
struct MonarchGaugeTick: Shape {
    let fraction: Double
    let innerInset: CGFloat
    let outerInset: CGFloat

    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: MonarchGaugeTokens.point(at: fraction, inset: innerInset, in: rect))
        path.addLine(to: MonarchGaugeTokens.point(at: fraction, inset: outerInset, in: rect))
        return path
    }
}

/// A line from the centre of the dial towards one fraction of the scale,
/// with a small hub at the centre.
struct MonarchGaugeNeedle: Shape {
    var fraction: Double
    let inset: CGFloat

    var animatableData: Double {
        get { fraction }
        set { fraction = newValue }
    }

    func path(in rect: CGRect) -> Path {
        var path = Path()
        let centre = MonarchGaugeTokens.centre(of: rect)
        let hub = MonarchGaugeTokens.hubRadius
        path.move(to: centre)
        path.addLine(to: MonarchGaugeTokens.point(at: fraction, inset: inset, in: rect))
        path.addEllipse(in: CGRect(x: centre.x - hub, y: centre.y - hub, width: hub * 2, height: hub * 2))
        return path
    }
}
