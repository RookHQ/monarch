import SwiftUI

/// A circular score: a ring filled to the score with the number written in
/// the middle, such as an SEO or health score out of 100.
///
/// ```swift
/// MonarchScoreRing("Profile strength", value: 78)
/// MonarchScoreRing(value: 3.5, max: 5, color: .accent, size: .sm)
/// ```
///
/// It is a standard `ProgressView` with a ring style. The arc takes the tone
/// of the band the score falls in (80 and above success, 60 info, 40
/// warning, below 40 danger) unless a colour is given. The score is always
/// written as a number, so the colour is never the only signal.
///
/// Accessibility: the ring and the label are one element, named by the
/// label or "Score", whose value is "78 out of 100", or "No score yet"
/// without a score. The description is separate text. The ring and the
/// number scale with Dynamic Type; the number shrinks to stay inside.
public struct MonarchScoreRing: View {
    private let label: String?
    private let value: Double?
    private let max: Double
    private let description: String?
    private let color: MonarchScoreRingColor
    private let thresholds: [MonarchScoreRingThreshold]
    private let size: MonarchScoreRingSize
    private let showsMax: Bool?
    private let format: (@Sendable (Double) -> String)?
    private let labels: MonarchScoreRingLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// A score ring.
    ///
    /// - Parameters:
    ///   - label: Shown under the ring and read as its name. Already localised.
    ///   - value: The score. `nil` shows an empty ring and a dash. Limited to 0 through `max`.
    ///   - max: The top of the scale.
    ///   - description: A second line under the label, such as "Updated today".
    ///   - color: The colour of the arc: from the thresholds, the accent, or a fixed tone.
    ///   - thresholds: The bands `auto` uses.
    ///   - size: The diameter of the ring.
    ///   - showsMax: Whether "/ 100" is shown under the number. By default on
    ///     the md and lg sizes.
    ///   - format: Formats the number in the middle and in the spoken value.
    ///     By default a whole number in the current locale.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ label: String? = nil,
        value: Double?,
        max: Double = 100,
        description: String? = nil,
        color: MonarchScoreRingColor = .auto,
        thresholds: [MonarchScoreRingThreshold] = MonarchScoreRingThreshold.defaults,
        size: MonarchScoreRingSize = .md,
        showsMax: Bool? = nil,
        format: (@Sendable (Double) -> String)? = nil,
        labels: MonarchScoreRingLabels = MonarchScoreRingLabels()
    ) {
        self.label = label
        self.value = value
        self.max = max
        self.description = description
        self.color = color
        self.thresholds = thresholds
        self.size = size
        self.showsMax = showsMax
        self.format = format
        self.labels = labels
    }

    /// The content of the view.
    public var body: some View {
        let safeMax = MonarchScoreRingTokens.safeMax(max)
        let score = MonarchScoreRingTokens.clamped(value, max: safeMax)
        let text = score.map { format?($0) ?? MonarchScoreRingTokens.formatted($0, locale: locale) }
        let maxText = MonarchScoreRingTokens.formatted(safeMax, locale: locale)
        VStack(spacing: MonarchScoreRingTokens.spacing) {
            VStack(spacing: MonarchScoreRingTokens.spacing) {
                ProgressView(value: score ?? 0, total: safeMax) {
                    EmptyView()
                }
                .progressViewStyle(
                    MonarchScoreRingStyle(
                        fill: MonarchScoreRingTokens.fill(color: color, value: score, thresholds: thresholds),
                        valueFill: MonarchScoreRingTokens.valueRole(
                            color: color,
                            value: score,
                            thresholds: thresholds
                        ),
                        text: text,
                        maxText: MonarchScoreRingTokens.showsMax(showsMax, size: size, hasValue: score != nil)
                            ? (labels.ofMax?(maxText) ?? MonarchScoreRingStrings.ofMax(maxText)) : nil,
                        size: size
                    )
                )
                if let label, !label.isEmpty {
                    Text(label)
                        .monarchTextStyle(.label)
                        .foregroundStyle(theme.colors[role: MonarchScoreRingTokens.value])
                        .multilineTextAlignment(.center)
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(MonarchScoreRingTokens.accessibilityLabel(label: label, labels: labels))
            .accessibilityValue(MonarchScoreRingTokens.accessibilityValue(text: text, max: maxText, labels: labels))
            if let description, !description.isEmpty {
                Text(description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchScoreRingTokens.muted])
                    .multilineTextAlignment(.center)
            }
        }
    }
}

/// The ring for the standard `ProgressView`, with the number in the middle.
private struct MonarchScoreRingStyle: ProgressViewStyle {
    let fill: MonarchColorRole
    let valueFill: MonarchColorRole
    let text: String?
    let maxText: String?
    let size: MonarchScoreRingSize

    func makeBody(configuration: Configuration) -> some View {
        MonarchScoreRingDial(
            fraction: configuration.fractionCompleted ?? 0,
            fill: fill,
            valueFill: valueFill,
            text: text,
            maxText: maxText,
            size: size
        )
    }
}

/// Reads the environment, which a `ProgressViewStyle` value cannot do itself.
private struct MonarchScoreRingDial: View {
    let fraction: Double
    let fill: MonarchColorRole
    let valueFill: MonarchColorRole
    let text: String?
    let maxText: String?
    let size: MonarchScoreRingSize

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric private var diameter: CGFloat

    init(
        fraction: Double,
        fill: MonarchColorRole,
        valueFill: MonarchColorRole,
        text: String?,
        maxText: String?,
        size: MonarchScoreRingSize
    ) {
        self.fraction = fraction
        self.fill = fill
        self.valueFill = valueFill
        self.text = text
        self.maxText = maxText
        self.size = size
        _diameter = ScaledMetric(wrappedValue: MonarchScoreRingTokens.diameter(size), relativeTo: .body)
    }

    var body: some View {
        let lineWidth = MonarchScoreRingTokens.lineWidth(diameter: diameter)
        ZStack {
            Circle()
                .stroke(theme.colors[role: MonarchScoreRingTokens.track], lineWidth: lineWidth)
            if text != nil && fraction > 0 {
                Circle()
                    .trim(from: 0, to: fraction)
                    .stroke(theme.colors[role: fill], style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
                    .rotationEffect(MonarchScoreRingTokens.startAngle)
                    .monarchAnimation(.base, value: fraction)
            }
            VStack(spacing: 0) {
                Text(text ?? MonarchScoreRingTokens.noValueGlyph)
                    .monarchTextStyle(MonarchScoreRingTokens.valueStyle(size))
                    .monospacedDigit()
                    .foregroundStyle(theme.colors[role: text == nil ? MonarchScoreRingTokens.value : valueFill])
                if let maxText {
                    Text(maxText)
                        .monarchTextStyle(.caption)
                        .monospacedDigit()
                        .foregroundStyle(theme.colors[role: MonarchScoreRingTokens.muted])
                }
            }
            .lineLimit(1)
            .minimumScaleFactor(MonarchScoreRingTokens.valueMinimumScale)
            .padding(lineWidth * 2)
        }
        .padding(lineWidth / 2)
        .frame(width: diameter, height: diameter)
    }
}

#if DEBUG
    #Preview("MonarchScoreRing") {
        MonarchPreviewMatrix {
            HStack(alignment: .top) {
                MonarchScoreRing("SEO", value: 92, size: .sm)
                MonarchScoreRing("Profile", value: 64, description: "Updated today")
                MonarchScoreRing("Health", value: 31, size: .lg)
                MonarchScoreRing("New", value: nil)
            }
        }
    }
#endif
