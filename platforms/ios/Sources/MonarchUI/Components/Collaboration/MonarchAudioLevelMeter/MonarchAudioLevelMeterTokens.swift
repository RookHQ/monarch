import Foundation
import MonarchTokens

/// Component tokens, geometry and text rules for ``MonarchAudioLevelMeter``.
enum MonarchAudioLevelMeterTokens {
    /// The level at or above which there is sound.
    static let speakingThreshold: Double = 0.08
    /// The height of the bar variant before Dynamic Type is applied.
    static let barHeight: CGFloat = MonarchTokens.Space.space1_5
    /// The least width of the bar variant.
    static let minBarWidth: CGFloat = MonarchTokens.Space.space16

    /// The colours of the parts that are on and off. A muted meter has no
    /// status colour.
    static func roles(isMuted: Bool) -> (lit: MonarchColorRole, unlit: MonarchColorRole) {
        if isMuted {
            return (lit: \.textDisabled, unlit: \.bgMuted)
        }
        return (lit: \.successSolid, unlit: \.borderStrong)
    }

    /// The level to show and to speak.
    struct State: Equatable {
        /// The level, 0 through 1. Zero while muted.
        let level: Double
        /// The level drawn. With Reduce Motion it is 1 while there is sound and 0 otherwise.
        let shown: Double
        /// Whether there is sound.
        let isSpeaking: Bool
    }

    /// Limits the level and applies mute and Reduce Motion. Pure, so it is
    /// tested without a view. A level that is not finite counts as 0.
    static func state(level: Double, isMuted: Bool, reduceMotion: Bool) -> State {
        let raw = isMuted || !level.isFinite ? 0 : level
        let limited = min(max(raw, 0), 1)
        let isSpeaking = limited >= speakingThreshold
        return State(level: limited, shown: reduceMotion ? (isSpeaking ? 1 : 0) : limited, isSpeaking: isSpeaking)
    }

    /// What VoiceOver reads as the value: the level as a whole percentage, or
    /// the muted text.
    static func accessibilityValue(state: State, isMuted: Bool, mutedLabel: String, locale: Locale) -> String {
        isMuted ? mutedLabel : state.level.formatted(.percent.precision(.fractionLength(0)).locale(locale))
    }

    /// The sizes of the segments, with Dynamic Type applied.
    struct Metrics: Equatable {
        /// The height of the tallest segment.
        let height: CGFloat
        /// The width of one segment, and the height of a segment that is off.
        let segmentWidth: CGFloat
        /// The gap between two segments.
        let gap: CGFloat

        /// The corner radius of a segment: half its width.
        var cornerRadius: CGFloat { segmentWidth / 2 }

        /// The width of a meter with `count` segments.
        func width(count: Int) -> CGFloat {
            CGFloat(count) * segmentWidth + CGFloat(max(count - 1, 0)) * gap
        }
    }

    /// The metrics for a size at a Dynamic Type scale.
    static func metrics(_ size: MonarchAudioLevelMeterSize, scale: CGFloat) -> Metrics {
        switch size {
        case .sm:
            Metrics(
                height: MonarchTokens.Space.space3 * scale,
                segmentWidth: MonarchTokens.Space.space0_5 * scale,
                gap: MonarchTokens.Space.px * scale
            )
        case .md:
            Metrics(
                height: MonarchTokens.Space.space4 * scale,
                segmentWidth: (MonarchTokens.Space.space0_5 + MonarchTokens.Space.px) * scale,
                gap: MonarchTokens.Space.space0_5 * scale
            )
        }
    }

    /// One segment to draw.
    struct Segment: Equatable {
        let frame: CGRect
        let isLit: Bool
    }

    /// The number of segments that are on for a level.
    static func litCount(shown: Double, count: Int) -> Int {
        Int((shown * Double(count)).rounded())
    }

    /// The segments, standing on the bottom edge and rising towards the
    /// trailing side. A segment that is on has its full height; one that is
    /// off is a dot, so the level can be read without colour.
    static func segments(
        count: Int,
        shown: Double,
        in size: CGSize,
        metrics: Metrics,
        isRightToLeft: Bool
    ) -> [Segment] {
        let lit = litCount(shown: shown, count: count)
        return (0..<count).map { index in
            let isLit = index < lit
            let full = max(size.height * CGFloat(index + 1) / CGFloat(count), metrics.segmentWidth)
            let height = isLit ? full : metrics.segmentWidth
            let offset = CGFloat(index) * (metrics.segmentWidth + metrics.gap)
            let x = isRightToLeft ? size.width - offset - metrics.segmentWidth : offset
            return Segment(
                frame: CGRect(x: x, y: size.height - height, width: metrics.segmentWidth, height: height),
                isLit: isLit
            )
        }
    }

    /// The filled part of the bar variant, from the leading edge.
    static func barFill(shown: Double, in size: CGSize, isRightToLeft: Bool) -> CGRect {
        let width = size.width * min(max(shown, 0), 1)
        return CGRect(x: isRightToLeft ? size.width - width : 0, y: 0, width: width, height: size.height)
    }

    /// The corner radius of the bar variant: half its height.
    static func barRadius(_ size: CGSize) -> CGFloat {
        size.height / 2
    }
}
