import Foundation
import MonarchTokens

/// Component tokens for ``MonarchProgressViewStyle``.
enum MonarchProgressTokens {
    /// The height of the track: 8 points, 4 for the small bar, as the web.
    static func trackHeight(_ size: MonarchProgressSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space1
        case .md: MonarchTokens.Space.space2
        }
    }
    /// The gap between the label, the track and the value label.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The track role.
    static var track: MonarchColorRole { \.bgEmphasis }
    /// The fill role of a tone: its strong colour, from the shared tone
    /// palette, as the web bar (the muted text colour for neutral).
    static func fill(tone: MonarchTone) -> MonarchColorRole {
        tone == .neutral ? \.textMuted : MonarchTonePalette.palette(for: tone).strong
    }
    /// The part of the track the indeterminate segment covers.
    static let indeterminateSegment: CGFloat = 0.4
    /// How long the indeterminate segment takes to cross the track, in seconds.
    static let indeterminateDuration: TimeInterval = TimeInterval(MonarchTokens.Duration.shimmer) / 1000

    /// A fraction limited to 0 through 1. Not-a-number counts as 0.
    static func clamped(_ fraction: Double) -> Double {
        guard !fraction.isNaN else { return 0 }
        return min(max(fraction, 0), 1)
    }
}
