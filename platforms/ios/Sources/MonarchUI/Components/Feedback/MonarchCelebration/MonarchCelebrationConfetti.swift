import SwiftUI

/// The confetti behind the icon: drawn with `Canvas` in a `TimelineView`
/// that stops once the last piece has fallen. Under Reduce Motion the
/// pieces rest as a still scatter. Hidden from VoiceOver.
///
/// `Canvas` is not mirrored in right-to-left layouts; the scatter has no
/// direction, so it is drawn the same way in both.
struct MonarchCelebrationConfetti: View {
    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var start: Date?
    @State private var isFinished = false

    var body: some View {
        Group {
            if reduceMotion {
                canvas(elapsed: 0)
            } else {
                TimelineView(.animation(minimumInterval: nil, paused: isFinished || start == nil)) { context in
                    canvas(elapsed: start.map { context.date.timeIntervalSince($0) } ?? 0)
                }
            }
        }
        .clipped()
        .allowsHitTesting(false)
        .accessibilityHidden(true)
        .task {
            guard start == nil else { return }
            start = .now
            try? await Task.sleep(for: .seconds(MonarchCelebrationTokens.totalDuration))
            isFinished = true
        }
    }

    private func canvas(elapsed: Double) -> some View {
        let colors = MonarchCelebrationTokens.colors.map { theme.colors[role: $0] }
        let drawsNothing = !reduceMotion && (isFinished || MonarchCelebrationTokens.isFinished(elapsed: elapsed))
        return Canvas { context, size in
            guard !drawsNothing else { return }
            for piece in MonarchCelebrationTokens.pieces {
                let state = MonarchCelebrationTokens.state(of: piece, elapsed: elapsed, reduceMotion: reduceMotion)
                guard state.opacity > 0 else { continue }
                var layer = context
                layer.opacity = state.opacity
                layer.translateBy(x: size.width * piece.position + state.x, y: state.y)
                layer.rotate(by: .degrees(state.rotation))
                let path: Path
                switch piece.shape {
                case .bar:
                    let bar = MonarchCelebrationTokens.barSize
                    path = Path(
                        roundedRect: CGRect(
                            x: -bar.width / 2,
                            y: -bar.height / 2,
                            width: bar.width,
                            height: bar.height
                        ),
                        cornerRadius: MonarchCelebrationTokens.barCorner
                    )
                case .dot:
                    let side = MonarchCelebrationTokens.dotSize
                    path = Path(ellipseIn: CGRect(x: -side / 2, y: -side / 2, width: side, height: side))
                }
                layer.fill(path, with: .color(colors[piece.color % colors.count]))
            }
        }
    }
}
