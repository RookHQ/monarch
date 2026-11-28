import SwiftUI

/// A linear progress bar in Monarch's colours for the standard `ProgressView`.
///
/// ```swift
/// ProgressView("Uploading", value: 0.4)
///     .progressViewStyle(.monarchLinear)
///
/// ProgressView("Checklist", value: 3, total: 5)
///     .progressViewStyle(.monarchLinear(tone: .success))
/// ```
///
/// The fill takes the solid colour of `tone`: the accent by default, or a
/// status tone for a meter (success when complete, warning near a limit,
/// danger over it). The tone is a colour only: say what it means in the
/// label or the value label.
///
/// A `ProgressView` with no value draws an indeterminate bar. For a spinner,
/// use the standard circular style; it takes the accent from the theme's tint.
///
/// Accessibility: the label, the bar and the value label are one element
/// whose value is the percentage, in the user's locale. The indeterminate
/// animation stops when Reduce Motion is on.
public struct MonarchProgressViewStyle: ProgressViewStyle {
    private let tone: MonarchTone
    private let size: MonarchProgressSize

    /// A linear progress style.
    ///
    /// - Parameters:
    ///   - tone: The colour of the fill. The accent by default.
    ///   - size: The thickness of the bar: `.md` (8 points) by default, `.sm` (4 points) for dense rows.
    public init(tone: MonarchTone = .accent, size: MonarchProgressSize = .md) {
        self.tone = tone
        self.size = size
    }

    /// The styled view for a configuration.
    public func makeBody(configuration: Configuration) -> some View {
        MonarchProgressBody(configuration: configuration, tone: tone, size: size)
    }
}

extension ProgressViewStyle where Self == MonarchProgressViewStyle {
    /// The Monarch linear progress style with the accent fill.
    public static var monarchLinear: MonarchProgressViewStyle { MonarchProgressViewStyle() }

    /// The Monarch linear progress style with the fill of a tone.
    ///
    /// - Parameter tone: The colour of the fill.
    /// - Returns: The style.
    public static func monarchLinear(tone: MonarchTone) -> MonarchProgressViewStyle {
        MonarchProgressViewStyle(tone: tone)
    }

    /// The Monarch linear progress style with a tone and a thickness.
    ///
    /// - Parameters:
    ///   - tone: The colour of the fill.
    ///   - size: The thickness of the bar.
    /// - Returns: The style.
    public static func monarchLinear(tone: MonarchTone = .accent, size: MonarchProgressSize) -> MonarchProgressViewStyle
    {
        MonarchProgressViewStyle(tone: tone, size: size)
    }
}

/// Reads the environment, which a `ProgressViewStyle` value cannot do itself.
private struct MonarchProgressBody: View {
    let configuration: ProgressViewStyleConfiguration
    let tone: MonarchTone
    let size: MonarchProgressSize

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let fraction = configuration.fractionCompleted.map(MonarchProgressTokens.clamped)
        VStack(alignment: .leading, spacing: MonarchProgressTokens.stackSpacing) {
            configuration.label
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textDefault)
            MonarchProgressBar(
                fraction: fraction,
                fill: MonarchProgressTokens.fill(tone: tone),
                height: MonarchProgressTokens.trackHeight(size)
            )
            configuration.currentValueLabel
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
        }
        .accessibilityElement(children: .combine)
        .accessibilityValue(fraction.map { $0.formatted(.percent.precision(.fractionLength(0))) } ?? "")
        .accessibilityAddTraits(.updatesFrequently)
    }
}

/// The track and its fill. `fraction` is `nil` for indeterminate progress.
private struct MonarchProgressBar: View {
    let fraction: Double?
    let fill: MonarchColorRole
    let height: CGFloat

    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var isAtEnd = false

    var body: some View {
        GeometryReader { proxy in
            let width = proxy.size.width
            ZStack(alignment: .leading) {
                Capsule().fill(theme.colors[role: MonarchProgressTokens.track])
                if let fraction {
                    Capsule()
                        .fill(theme.colors[role: fill])
                        .frame(width: width * fraction)
                        .monarchAnimation(.base, value: fraction)
                } else {
                    let segment = width * MonarchProgressTokens.indeterminateSegment
                    let travel = width - segment
                    Capsule()
                        .fill(theme.colors[role: fill])
                        .frame(width: segment)
                        .offset(x: reduceMotion ? 0 : (isAtEnd ? travel : 0))
                }
            }
        }
        .frame(height: height)
        .onAppear { startIfNeeded() }
        .onChange(of: reduceMotion) { startIfNeeded() }
    }

    private func startIfNeeded() {
        guard fraction == nil, !reduceMotion, !isAtEnd else { return }
        withAnimation(
            .easeInOut(duration: MonarchProgressTokens.indeterminateDuration).repeatForever(autoreverses: true)
        ) {
            isAtEnd = true
        }
    }
}

#if DEBUG
    #Preview("MonarchProgressViewStyle") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ProgressView("Uploading", value: 0.4)
                ProgressView(value: 1)
                ProgressView("Working")
            }
            .progressViewStyle(.monarchLinear)
            VStack(alignment: .leading) {
                ForEach(MonarchTone.allCases, id: \.self) { tone in
                    ProgressView(value: 0.6).progressViewStyle(.monarchLinear(tone: tone))
                }
            }
        }
    }
#endif
