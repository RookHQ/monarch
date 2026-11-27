import SwiftUI

/// The three dots at one moment, or their still form when `time` is `nil`.
/// ``MonarchAiTypingDots`` draws it once per frame, or once for Reduce Motion.
struct MonarchAiTypingDotsFrame: View {
    /// The diameter of a dot, already scaled for Dynamic Type.
    let dot: CGFloat
    /// The moment to draw, in seconds, or `nil` for the still form.
    let time: TimeInterval?

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        HStack(spacing: dot * MonarchAiTypingDotsTokens.gapRatio) {
            ForEach(0..<MonarchAiTypingDotsTokens.count, id: \.self) { index in
                let lift = time.map { MonarchAiTypingDotsTokens.lift(at: $0, index: index) }
                Circle()
                    .fill(theme.colors[role: MonarchAiTypingDotsTokens.color])
                    .opacity(MonarchAiTypingDotsTokens.opacity(lift: lift))
                    .frame(width: dot, height: dot)
                    .offset(y: -(lift ?? 0) * dot * MonarchAiTypingDotsTokens.liftRatio)
            }
        }
        .padding(.vertical, dot * MonarchAiTypingDotsTokens.paddingRatio)
    }
}
