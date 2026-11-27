import SwiftUI

/// One drawn part of a sparkline. The paths come from the pure geometry in
/// ``MonarchSparklineTokens``.
struct MonarchSparklineMark: Shape {
    /// Which part to draw.
    enum Part: Equatable {
        case line
        case area
        case bars
        case dots(includesLast: Bool, radius: CGFloat)
    }

    let plot: MonarchSparklineTokens.Plot
    let part: Part

    func path(in rect: CGRect) -> Path {
        var path = Path()
        switch part {
        case .line:
            for run in plot.runs(in: rect.size) where run.count > 1 {
                path.addLines(run)
            }
        case .area:
            let baseline = plot.baseline(in: rect.size)
            for run in plot.runs(in: rect.size) {
                guard run.count > 1, let first = run.first, let last = run.last else { continue }
                // `addLines` would start a new subpath and drop the baseline corner.
                path.move(to: CGPoint(x: first.x, y: baseline))
                for point in run {
                    path.addLine(to: point)
                }
                path.addLine(to: CGPoint(x: last.x, y: baseline))
                path.closeSubpath()
            }
        case .bars:
            for bar in plot.bars(in: rect.size) {
                path.addRoundedRect(in: bar, cornerSize: CGSize(width: plot.barRadius, height: plot.barRadius))
            }
        case .dots(let includesLast, let radius):
            for point in plot.dots(in: rect.size, includesLast: includesLast) {
                path.addEllipse(
                    in: CGRect(x: point.x - radius, y: point.y - radius, width: radius * 2, height: radius * 2)
                )
            }
        }
        return path.offsetBy(dx: rect.minX, dy: rect.minY)
    }
}
