import SwiftUI

/// Shows how loud a microphone is, from a level the app measures.
///
/// ```swift
/// MonarchAudioLevelMeter(level)                      // 0 through 1
/// MonarchAudioLevelMeter(level, isMuted: isMuted)
/// ```
///
/// The meter draws; it does not record. Measure the level in the app, for
/// example with `AVAudioRecorder` metering or an `AVAudioEngine` tap, and
/// pass a value from 0 to 1.
///
/// With Reduce Motion the meter does not follow the level: it is full while
/// there is sound and empty otherwise.
///
/// Accessibility: the meter is one element named "Microphone level" whose
/// value is the level as a percentage, or "Muted". It has the
/// updates-frequently trait. Segments that are off are short and segments
/// that are on are tall, so the level does not depend on colour. Use
/// `isDecorative` when the same information is given elsewhere, such as a
/// call tile that already says who is speaking.
public struct MonarchAudioLevelMeter: View {
    private let level: Double
    private let label: String?
    private let variant: MonarchAudioLevelMeterVariant
    private let size: MonarchAudioLevelMeterSize
    private let segments: Int
    private let isMuted: Bool
    private let mutedLabel: String?
    private let isDecorative: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.layoutDirection) private var layoutDirection
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ScaledMetric(relativeTo: .body) private var scale: CGFloat = 1

    /// An audio level meter.
    ///
    /// - Parameters:
    ///   - level: The loudness, 0 through 1. Values outside are limited to that range.
    ///   - label: Replaces the built-in spoken name "Microphone level".
    ///   - variant: Rising segments, or one thin bar.
    ///   - size: The size of the segments.
    ///   - segments: The number of segments. At least one.
    ///   - isMuted: Whether the microphone is off. The meter is empty and reads "Muted".
    ///   - mutedLabel: Replaces the built-in spoken value "Muted".
    ///   - isDecorative: Whether the meter is hidden from assistive technology.
    public init(
        _ level: Double,
        label: String? = nil,
        variant: MonarchAudioLevelMeterVariant = .segments,
        size: MonarchAudioLevelMeterSize = .md,
        segments: Int = 5,
        isMuted: Bool = false,
        mutedLabel: String? = nil,
        isDecorative: Bool = false
    ) {
        self.level = level
        self.label = label
        self.variant = variant
        self.size = size
        self.segments = segments
        self.isMuted = isMuted
        self.mutedLabel = mutedLabel
        self.isDecorative = isDecorative
    }

    /// The content of the view.
    public var body: some View {
        let state = MonarchAudioLevelMeterTokens.state(level: level, isMuted: isMuted, reduceMotion: reduceMotion)
        if isDecorative {
            meter(state).accessibilityHidden(true)
        } else {
            meter(state)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label ?? MonarchAudioLevelMeterStrings.label)
                .accessibilityValue(
                    MonarchAudioLevelMeterTokens.accessibilityValue(
                        state: state,
                        isMuted: isMuted,
                        mutedLabel: mutedLabel ?? MonarchAudioLevelMeterStrings.muted,
                        locale: locale
                    )
                )
                .accessibilityAddTraits(.updatesFrequently)
        }
    }

    @ViewBuilder
    private func meter(_ state: MonarchAudioLevelMeterTokens.State) -> some View {
        let roles = MonarchAudioLevelMeterTokens.roles(isMuted: isMuted)
        let lit = theme.colors[role: roles.lit]
        let unlit = theme.colors[role: roles.unlit]
        let isRightToLeft = layoutDirection == .rightToLeft
        switch variant {
        case .segments:
            let count = max(segments, 1)
            let metrics = MonarchAudioLevelMeterTokens.metrics(size, scale: scale)
            Canvas { context, canvas in
                let bars = MonarchAudioLevelMeterTokens.segments(
                    count: count,
                    shown: state.shown,
                    in: canvas,
                    metrics: metrics,
                    isRightToLeft: isRightToLeft
                )
                for bar in bars {
                    context.fill(
                        Path(roundedRect: bar.frame, cornerRadius: metrics.cornerRadius),
                        with: .color(bar.isLit ? lit : unlit)
                    )
                }
            }
            .frame(width: metrics.width(count: count), height: metrics.height)
        case .bar:
            let height = MonarchAudioLevelMeterTokens.barHeight * scale
            Canvas { context, canvas in
                let track = CGRect(origin: .zero, size: canvas)
                let fill = MonarchAudioLevelMeterTokens.barFill(
                    shown: state.shown,
                    in: canvas,
                    isRightToLeft: isRightToLeft
                )
                context.clip(to: Path(roundedRect: track, cornerRadius: MonarchAudioLevelMeterTokens.barRadius(canvas)))
                context.fill(Path(track), with: .color(unlit))
                context.fill(Path(fill), with: .color(lit))
            }
            .frame(minWidth: MonarchAudioLevelMeterTokens.minBarWidth, maxWidth: .infinity)
            .frame(height: height)
        }
    }
}

#if DEBUG
    #Preview("MonarchAudioLevelMeter") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                HStack {
                    MonarchAudioLevelMeter(0)
                    MonarchAudioLevelMeter(0.4)
                    MonarchAudioLevelMeter(1)
                    MonarchAudioLevelMeter(0.6, size: .sm)
                    MonarchAudioLevelMeter(0.6, isMuted: true)
                }
                MonarchAudioLevelMeter(0.6, variant: .bar)
                MonarchAudioLevelMeter(0.6, variant: .bar, isMuted: true)
            }
        }
    }
#endif
