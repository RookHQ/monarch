import SwiftUI

/// How sure the AI is about a value, as low, medium or high.
///
/// Each band has a text label and an icon as well as a colour; low reads
/// "Needs check" so people know to verify the value.
///
/// ```swift
/// MonarchAiConfidenceMeter(value: 0.42)
/// MonarchAiConfidenceMeter(value: 0.91, explanation: "Matches two sources.", variant: .bar)
/// ```
///
/// Accessibility: the meter is one element. The badge is read as "Needs
/// check, 42% confidence"; the bar has the label "Confidence" and that text
/// as its value. The explanation is the hint. The band is always in words
/// and has its own icon, so nothing depends on colour. Text scales with
/// Dynamic Type. The meter is not interactive.
public struct MonarchAiConfidenceMeter: View {
    private let value: Double
    private let explanation: String?
    private let variant: MonarchAiConfidenceMeterVariant
    private let size: MonarchAiConfidenceMeterSize
    private let lowThreshold: Double
    private let highThreshold: Double
    private let showsValue: Bool?
    private let levelLabel: String?
    private let meterLabel: String?
    private let valueText: ((String, String) -> String)?

    @Environment(\.monarchTheme) private var theme

    /// A confidence meter.
    ///
    /// - Parameters:
    ///   - value: How confident the model is, from 0 to 1. Values outside the range are clamped.
    ///   - explanation: Why the model is, or is not, confident. Shown under the bar; for the badge it is
    ///     only the VoiceOver hint.
    ///   - variant: A badge or a bar.
    ///   - size: The size.
    ///   - lowThreshold: The edge under which confidence is low.
    ///   - highThreshold: The edge from which confidence is high.
    ///   - showsValue: Whether to show the percentage. By default only the bar shows it.
    ///   - levelLabel: Replaces the built-in label of the band, such as "Needs check".
    ///   - meterLabel: Replaces the built-in "Confidence" spoken name of the bar.
    ///   - valueText: Replaces the built-in spoken value. It receives the band label and the percentage.
    public init(
        value: Double,
        explanation: String? = nil,
        variant: MonarchAiConfidenceMeterVariant = .badge,
        size: MonarchAiConfidenceMeterSize = .md,
        lowThreshold: Double = 0.6,
        highThreshold: Double = 0.85,
        showsValue: Bool? = nil,
        levelLabel: String? = nil,
        meterLabel: String? = nil,
        valueText: ((String, String) -> String)? = nil
    ) {
        self.value = value
        self.explanation = explanation
        self.variant = variant
        self.size = size
        self.lowThreshold = lowThreshold
        self.highThreshold = highThreshold
        self.showsValue = showsValue
        self.levelLabel = levelLabel
        self.meterLabel = meterLabel
        self.valueText = valueText
    }

    private var level: MonarchAiConfidenceLevel {
        .level(for: value, lowThreshold: lowThreshold, highThreshold: highThreshold)
    }

    private var label: String { levelLabel ?? MonarchAiConfidenceMeterStrings.label(level) }

    private var spokenValue: String {
        MonarchAiConfidenceMeterTokens.valueText(value: value, level: level, levelLabel: levelLabel, format: valueText)
    }

    private var showsPercent: Bool {
        MonarchAiConfidenceMeterTokens.showsValue(showsValue, variant: variant)
    }

    /// The content of the view.
    public var body: some View {
        switch variant {
        case .badge:
            badge
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(spokenValue)
                .accessibilityHint(explanation ?? "")
        case .bar:
            bar
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(meterLabel ?? MonarchAiConfidenceMeterStrings.name)
                .accessibilityValue(spokenValue)
                .accessibilityHint(explanation ?? "")
        }
    }

    private var badge: some View {
        let appearance = MonarchAiConfidenceMeterTokens.badgeAppearance(level)
        return HStack(spacing: MonarchAiConfidenceMeterTokens.badgeSpacing) {
            MonarchIcon(systemName: MonarchAiConfidenceMeterTokens.symbol(level), size: .sm)
            Text(label)
                .monarchTextStyle(.caption, weight: MonarchAiConfidenceMeterTokens.fontWeight)
                .lineLimit(1)
            if showsPercent {
                Text(MonarchAiConfidenceMeterTokens.percent(value))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchAiConfidenceMeterTokens.secondary])
            }
        }
        .fixedSize(horizontal: true, vertical: false)
        .padding(.horizontal, MonarchAiConfidenceMeterTokens.badgeHorizontalPadding(size))
        .padding(.vertical, MonarchAiConfidenceMeterTokens.badgeVerticalPadding)
        .monarchSurface(appearance, in: Capsule())
    }

    private var bar: some View {
        let foreground = theme.colors[role: MonarchAiConfidenceMeterTokens.foreground(level)]
        let secondary = theme.colors[role: MonarchAiConfidenceMeterTokens.secondary]
        let fraction = MonarchAiConfidenceMeterTokens.clamped(value)
        return VStack(alignment: .leading, spacing: MonarchAiConfidenceMeterTokens.barSpacing(size)) {
            HStack(alignment: .firstTextBaseline, spacing: MonarchAiConfidenceMeterTokens.headerSpacing) {
                HStack(spacing: MonarchAiConfidenceMeterTokens.badgeSpacing) {
                    MonarchIcon(systemName: MonarchAiConfidenceMeterTokens.symbol(level), size: .sm)
                    Text(label)
                        .monarchTextStyle(
                            MonarchAiConfidenceMeterTokens.barTextStyle(size),
                            weight: MonarchAiConfidenceMeterTokens.fontWeight
                        )
                }
                .foregroundStyle(foreground)
                Spacer(minLength: 0)
                if showsPercent {
                    Text(MonarchAiConfidenceMeterTokens.percent(value))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(secondary)
                }
            }
            GeometryReader { proxy in
                ZStack(alignment: .leading) {
                    Capsule().fill(theme.colors[role: MonarchAiConfidenceMeterTokens.track])
                    Capsule()
                        .fill(foreground)
                        .frame(width: proxy.size.width * fraction)
                        .monarchAnimation(.base, value: fraction)
                }
            }
            .frame(height: MonarchAiConfidenceMeterTokens.trackHeight(size))
            if let explanation, !explanation.isEmpty {
                Text(explanation)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(secondary)
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchAiConfidenceMeter") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                HStack {
                    MonarchAiConfidenceMeter(value: 0.42)
                    MonarchAiConfidenceMeter(value: 0.7, size: .sm, showsValue: true)
                    MonarchAiConfidenceMeter(value: 0.93)
                }
                MonarchAiConfidenceMeter(value: 0.42, explanation: "Only one source mentions it.", variant: .bar)
                MonarchAiConfidenceMeter(value: 0.93, variant: .bar, size: .sm)
            }
        }
    }
#endif
