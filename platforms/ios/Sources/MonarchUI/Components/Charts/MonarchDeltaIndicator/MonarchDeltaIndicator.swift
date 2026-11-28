import SwiftUI

/// A change between two periods: an arrow, a signed value and an optional
/// comparison, such as "+12% vs last month".
///
/// ```swift
/// MonarchDeltaIndicator(0.12, comparison: "vs last month")
/// MonarchDeltaIndicator(-0.08, goodDirection: .down)      // a fall in cost is good
/// ```
///
/// The change is a ratio by default: 0.12 is written "12%". Pass
/// `formatValue` to write a number or a currency. A change that is written as
/// zero is shown as "No change", and `nil` as "No earlier data".
///
/// Accessibility: the indicator is one element that reads a sentence, such as
/// "up 12% vs last month, better". The arrow, the sign and that sentence carry
/// the meaning, so it does not depend on the colour.
public struct MonarchDeltaIndicator: View {
    private let value: Double?
    private let comparison: String?
    private let goodDirection: MonarchDeltaIndicatorGoodDirection
    private let variant: MonarchDeltaIndicatorVariant
    private let size: MonarchDeltaIndicatorSize
    private let flatThreshold: Double
    private let flatLabel: String?
    private let noDataLabel: String?
    private let spokenLabel: String?
    private let showsIcon: Bool
    private let formatValue: ((Double) -> String)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    /// The arrow's size, close to the text beside it as on the web, scaling with Dynamic Type.
    @ScaledMetric(relativeTo: .caption) private var arrowSize: CGFloat = 11

    /// A change that is already known.
    ///
    /// - Parameters:
    ///   - value: The change. A ratio unless `formatValue` writes it another
    ///     way. `nil` means there is nothing to compare with.
    ///   - comparison: The comparison period after the change, such as "vs last month".
    ///   - goodDirection: Which direction is an improvement.
    ///   - variant: Plain text, or a tinted pill.
    ///   - size: The text size.
    ///   - flatThreshold: Changes whose size is at or below this count as no change.
    ///   - flatLabel: Replaces the built-in text "No change".
    ///   - noDataLabel: Replaces the built-in text "No earlier data".
    ///   - spokenLabel: Replaces the sentence VoiceOver reads.
    ///   - showsIcon: Whether the arrow is shown. The sign and the spoken direction remain.
    ///   - formatValue: Writes the size of the change, without a sign. The
    ///     default is a percentage in the current locale.
    public init(
        _ value: Double?,
        comparison: String? = nil,
        goodDirection: MonarchDeltaIndicatorGoodDirection = .up,
        variant: MonarchDeltaIndicatorVariant = .plain,
        size: MonarchDeltaIndicatorSize = .sm,
        flatThreshold: Double = 0,
        flatLabel: String? = nil,
        noDataLabel: String? = nil,
        spokenLabel: String? = nil,
        showsIcon: Bool = true,
        formatValue: ((Double) -> String)? = nil
    ) {
        self.value = value
        self.comparison = comparison
        self.goodDirection = goodDirection
        self.variant = variant
        self.size = size
        self.flatThreshold = flatThreshold
        self.flatLabel = flatLabel
        self.noDataLabel = noDataLabel
        self.spokenLabel = spokenLabel
        self.showsIcon = showsIcon
        self.formatValue = formatValue
    }

    /// A change worked out from two periods, as a ratio of the previous one:
    /// `(current − previous) / |previous|`.
    ///
    /// There is nothing to compare with when either value is `nil` or not
    /// finite, or when `previous` is zero.
    ///
    /// - Parameters:
    ///   - current: This period's value.
    ///   - previous: The previous period's value.
    ///   - comparison: The comparison period after the change, such as "vs last month".
    ///   - goodDirection: Which direction is an improvement.
    ///   - variant: Plain text, or a tinted pill.
    ///   - size: The text size.
    ///   - flatThreshold: Changes whose size is at or below this count as no change.
    ///   - flatLabel: Replaces the built-in text "No change".
    ///   - noDataLabel: Replaces the built-in text "No earlier data".
    ///   - spokenLabel: Replaces the sentence VoiceOver reads.
    ///   - showsIcon: Whether the arrow is shown.
    ///   - formatValue: Writes the size of the change, without a sign.
    public init(
        current: Double?,
        previous: Double?,
        comparison: String? = nil,
        goodDirection: MonarchDeltaIndicatorGoodDirection = .up,
        variant: MonarchDeltaIndicatorVariant = .plain,
        size: MonarchDeltaIndicatorSize = .sm,
        flatThreshold: Double = 0,
        flatLabel: String? = nil,
        noDataLabel: String? = nil,
        spokenLabel: String? = nil,
        showsIcon: Bool = true,
        formatValue: ((Double) -> String)? = nil
    ) {
        self.init(
            MonarchDeltaIndicatorTokens.ratio(current: current, previous: previous),
            comparison: comparison,
            goodDirection: goodDirection,
            variant: variant,
            size: size,
            flatThreshold: flatThreshold,
            flatLabel: flatLabel,
            noDataLabel: noDataLabel,
            spokenLabel: spokenLabel,
            showsIcon: showsIcon,
            formatValue: formatValue
        )
    }

    /// The content of the view.
    public var body: some View {
        let delta = MonarchDeltaIndicatorTokens.resolve(
            value: value,
            comparison: comparison,
            goodDirection: goodDirection,
            flatThreshold: flatThreshold,
            words: MonarchDeltaIndicatorTokens.Words(flat: flatLabel, noData: noDataLabel),
            format: formatValue ?? { MonarchDeltaIndicatorTokens.percent($0, locale: locale) }
        )
        let appearance = MonarchDeltaIndicatorTokens.appearance(tone: delta.tone, variant: variant)
        let textStyle = MonarchDeltaIndicatorTokens.textStyle(size)
        // A flow, so a long comparison moves whole to the next line, as on the web.
        MonarchFlowLayout(spacing: MonarchDeltaIndicatorTokens.spacing, rowSpacing: 0, alignment: .firstTextBaseline) {
            HStack(alignment: .firstTextBaseline, spacing: MonarchDeltaIndicatorTokens.iconSpacing) {
                if showsIcon, let symbol = MonarchDeltaIndicatorTokens.symbol(for: delta.direction) {
                    Image(systemName: symbol)
                        .font(.system(size: size == .sm ? arrowSize : arrowSize * 1.2, weight: .semibold))
                        .accessibilityHidden(true)
                }
                Text(delta.text)
                    .monarchTextStyle(textStyle, weight: MonarchDeltaIndicatorTokens.fontWeight)
                    .monospacedDigit()
            }
            .padding(.horizontal, MonarchDeltaIndicatorTokens.horizontalPadding(variant))
            .padding(.vertical, MonarchDeltaIndicatorTokens.verticalPadding(variant))
            .monarchSurface(appearance, in: Capsule())
            if let comparison, delta.direction != nil {
                Text(comparison)
                    .monarchTextStyle(textStyle)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(spokenLabel ?? delta.label)
    }
}

#if DEBUG
    #Preview("MonarchDeltaIndicator") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchDeltaIndicatorVariant.allCases, id: \.self) { variant in
                    MonarchDeltaIndicator(0.12, comparison: "vs last month", variant: variant)
                    MonarchDeltaIndicator(-0.08, comparison: "vs last month", variant: variant, size: .md)
                    MonarchDeltaIndicator(-0.2, goodDirection: .down, variant: variant)
                    MonarchDeltaIndicator(0.3, goodDirection: .neither, variant: variant)
                    MonarchDeltaIndicator(0, variant: variant)
                    MonarchDeltaIndicator(nil, variant: variant)
                }
            }
        }
    }
#endif
