import SwiftUI

/// A progress ring in Monarch's colours for the standard `ProgressView`.
///
/// ```swift
/// ProgressView("Uploading report.pdf", value: 0.42)
///     .progressViewStyle(.monarchCircular(size: .lg, showsValue: true))
/// ```
///
/// A `ProgressView` with a value draws an arc of that length; one with no
/// value draws a quarter arc that turns. The label, if there is one, is
/// drawn after the ring. For a system spinner, use the standard circular
/// style; for a bar, use ``MonarchProgressViewStyle``.
///
/// Accessibility: the ring and the label are one element whose value is
/// the percentage, in the user's locale, with the updates-frequently trait.
/// Give the `ProgressView` a label, or an `accessibilityLabel`, that says
/// what is in progress. The indeterminate arc stops turning when Reduce
/// Motion is on. The tone is a colour only: say what it means in the label.
public struct MonarchCircularProgressViewStyle: ProgressViewStyle {
    private let tone: MonarchTone?
    private let size: MonarchCircularProgressSize
    private let showsValue: Bool

    /// A circular progress style.
    ///
    /// - Parameters:
    ///   - tone: The colour of the arc. Without a tone it is the accent, the same as `.accent`.
    ///   - size: The diameter of the ring.
    ///   - showsValue: Whether the value is drawn in the middle of the ring.
    ///     Only the `lg` and `xl` sizes have room for it.
    public init(tone: MonarchTone? = nil, size: MonarchCircularProgressSize = .md, showsValue: Bool = false) {
        self.tone = tone
        self.size = size
        self.showsValue = showsValue
    }

    /// The styled view for a configuration.
    public func makeBody(configuration: Configuration) -> some View {
        MonarchCircularProgressBody(configuration: configuration, tone: tone, size: size, showsValue: showsValue)
    }
}

extension ProgressViewStyle where Self == MonarchCircularProgressViewStyle {
    /// The Monarch progress ring with the accent colour and the default size.
    public static var monarchCircular: MonarchCircularProgressViewStyle { MonarchCircularProgressViewStyle() }

    /// The Monarch progress ring.
    ///
    /// - Parameters:
    ///   - tone: The colour of the arc. Without a tone it is the accent, the same as `.accent`.
    ///   - size: The diameter of the ring.
    ///   - showsValue: Whether the value is drawn in the middle of the ring.
    ///     Only the `lg` and `xl` sizes have room for it.
    /// - Returns: The style.
    public static func monarchCircular(
        tone: MonarchTone? = nil,
        size: MonarchCircularProgressSize = .md,
        showsValue: Bool = false
    ) -> MonarchCircularProgressViewStyle {
        MonarchCircularProgressViewStyle(tone: tone, size: size, showsValue: showsValue)
    }
}

/// Reads the environment, which a `ProgressViewStyle` value cannot do itself.
private struct MonarchCircularProgressBody: View {
    let configuration: ProgressViewStyleConfiguration
    let tone: MonarchTone?
    let size: MonarchCircularProgressSize
    let showsValue: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    var body: some View {
        let fraction = configuration.fractionCompleted.map(MonarchCircularProgressTokens.clamped)
        HStack(spacing: MonarchCircularProgressTokens.labelSpacing) {
            MonarchCircularProgressRing(fraction: fraction, tone: tone, size: size)
                .overlay {
                    if MonarchCircularProgressTokens.showsValue(showsValue, size: size, fraction: fraction) {
                        valueLabel(fraction)
                    }
                }
            configuration.label
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textDefault)
        }
        .accessibilityElement(children: .combine)
        .accessibilityValue(MonarchCircularProgressTokens.valueText(fraction, locale: locale))
        .accessibilityAddTraits(.updatesFrequently)
    }

    /// The app's current value label if it gave one, otherwise the percentage.
    @ViewBuilder
    private func valueLabel(_ fraction: Double?) -> some View {
        Group {
            if let current = configuration.currentValueLabel {
                current
            } else {
                Text(MonarchCircularProgressTokens.valueText(fraction, locale: locale))
            }
        }
        .monarchTextStyle(MonarchCircularProgressTokens.valueStyle)
        .monospacedDigit()
        .foregroundStyle(theme.colors.textDefault)
        .lineLimit(1)
        .minimumScaleFactor(MonarchCircularProgressTokens.valueMinimumScale)
        .padding(.horizontal, MonarchCircularProgressTokens.lineWidth(size) * 2)
        .accessibilityHidden(true)
    }
}

/// The track and the arc. `fraction` is `nil` for indeterminate progress.
private struct MonarchCircularProgressRing: View {
    let fraction: Double?
    let tone: MonarchTone?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ScaledMetric private var diameter: CGFloat
    @ScaledMetric private var lineWidth: CGFloat
    @State private var hasTurned = false

    init(fraction: Double?, tone: MonarchTone?, size: MonarchCircularProgressSize) {
        self.fraction = fraction
        self.tone = tone
        _diameter = ScaledMetric(wrappedValue: MonarchCircularProgressTokens.diameter(size), relativeTo: .body)
        _lineWidth = ScaledMetric(wrappedValue: MonarchCircularProgressTokens.lineWidth(size), relativeTo: .body)
    }

    var body: some View {
        let fill = theme.colors[role: MonarchCircularProgressTokens.fill(tone: tone)]
        let stroke = StrokeStyle(lineWidth: lineWidth, lineCap: .round)
        ZStack {
            Circle()
                .stroke(theme.colors[role: MonarchCircularProgressTokens.track], lineWidth: lineWidth)
            if let fraction {
                Circle()
                    .trim(from: 0, to: fraction)
                    .stroke(fill, style: stroke)
                    .rotationEffect(MonarchCircularProgressTokens.startAngle)
                    .monarchAnimation(.base, value: fraction)
            } else {
                Circle()
                    .trim(from: 0, to: MonarchCircularProgressTokens.indeterminateArc)
                    .stroke(fill, style: stroke)
                    .rotationEffect(MonarchCircularProgressTokens.startAngle)
                    .rotationEffect(hasTurned && !reduceMotion ? MonarchCircularProgressTokens.fullTurn : .zero)
            }
        }
        .padding(lineWidth / 2)
        .frame(width: diameter, height: diameter)
        .onAppear { startIfNeeded() }
        .onChange(of: reduceMotion) { startIfNeeded() }
        .onChange(of: fraction == nil) { startIfNeeded() }
    }

    private func startIfNeeded() {
        guard fraction == nil, !reduceMotion, !hasTurned else { return }
        withAnimation(
            .linear(duration: MonarchCircularProgressTokens.indeterminateDuration).repeatForever(autoreverses: false)
        ) {
            hasTurned = true
        }
    }
}

#if DEBUG
    #Preview("MonarchCircularProgressViewStyle") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchCircularProgressSize.allCases, id: \.self) { size in
                    ProgressView("Uploading", value: 0.42)
                        .progressViewStyle(.monarchCircular(size: size, showsValue: true))
                }
                ProgressView(value: 1).progressViewStyle(.monarchCircular(tone: .success))
                ProgressView(value: 0.9).progressViewStyle(.monarchCircular(tone: .danger))
                ProgressView("Working").progressViewStyle(.monarchCircular)
            }
        }
    }
#endif
