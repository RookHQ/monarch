import SwiftUI

/// Picks one value on a rating scale: 1 to 5 by default, or any scale with a
/// label per value.
///
/// ```swift
/// MonarchRatingPicker("Overall", value: $rating, variant: .stars)
///
/// MonarchRatingPicker(
///     "Performance",
///     value: $score,
///     options: [
///         MonarchRatingOption(1, label: "Below"),
///         MonarchRatingOption(2, label: "Meets"),
///         MonarchRatingOption(3, label: "Exceeds"),
///     ]
/// )
/// ```
///
/// Accessibility: the picker is one adjustable element. Its label is `label`
/// and its value is read as "3 of 5", with the option's label when it has
/// one, or "No rating". Swiping up and down moves the rating one step. A
/// rating that can be cleared has a "Clear rating" action. The chosen tile
/// has a heavier border and filled stars differ from empty ones by shape, so
/// nothing depends on colour alone. Every tile and star has a 44-point touch
/// target. A read-only picker is read with its value and cannot be adjusted.
/// With `isRequired` a mark follows the label and the hint says "Required";
/// an `error` is shown under the scale with an icon, read in the hint and
/// announced when it appears.
public struct MonarchRatingPicker: View {
    private let label: String
    @Binding private var value: Int?
    private let options: [MonarchRatingOption]
    private let variant: MonarchRatingPickerVariant
    private let size: MonarchRatingPickerSize
    private let isClearable: Bool
    private let isReadOnly: Bool
    private let isRequired: Bool
    private let error: String?
    private let helper: String?
    private let valueText: ((Int?) -> String)?
    private let clearLabel: String?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @ScaledMetric private var starSize: CGFloat

    /// A rating picker over a scale with a label per value.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - value: The chosen value, or `nil` for no rating yet.
    ///   - options: The scale, in order.
    ///   - variant: Tiles or stars.
    ///   - size: The size of the tiles or stars.
    ///   - isClearable: Whether choosing the chosen value again clears the rating.
    ///   - isReadOnly: Whether the rating is shown but cannot be changed.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - error: An error message, shown under the scale and announced.
    ///   - helper: A line under the label that says what the rating is about; read in the hint.
    ///   - valueText: Replaces the built-in spoken value. It receives the chosen value, or `nil`.
    ///   - clearLabel: Replaces the built-in "Clear rating" name of the VoiceOver action.
    public init(
        _ label: String,
        value: Binding<Int?>,
        options: [MonarchRatingOption],
        variant: MonarchRatingPickerVariant = .scale,
        size: MonarchRatingPickerSize = .md,
        isClearable: Bool = false,
        isReadOnly: Bool = false,
        isRequired: Bool = false,
        error: String? = nil,
        helper: String? = nil,
        valueText: ((Int?) -> String)? = nil,
        clearLabel: String? = nil
    ) {
        self.label = label
        _value = value
        self.options = options
        self.variant = variant
        self.size = size
        self.isClearable = isClearable
        self.isReadOnly = isReadOnly
        self.isRequired = isRequired
        self.error = error
        self.helper = helper
        self.valueText = valueText
        self.clearLabel = clearLabel
        _starSize = ScaledMetric(wrappedValue: MonarchRatingPickerTokens.starSize(size), relativeTo: .body)
    }

    /// A rating picker over a range of whole numbers.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - value: The chosen value, or `nil` for no rating yet.
    ///   - range: The first and last value of the scale.
    ///   - variant: Tiles or stars.
    ///   - size: The size of the tiles or stars.
    ///   - isClearable: Whether choosing the chosen value again clears the rating.
    ///   - isReadOnly: Whether the rating is shown but cannot be changed.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - error: An error message, shown under the scale and announced.
    ///   - helper: A line under the label that says what the rating is about; read in the hint.
    ///   - valueText: Replaces the built-in spoken value. It receives the chosen value, or `nil`.
    ///   - clearLabel: Replaces the built-in "Clear rating" name of the VoiceOver action.
    public init(
        _ label: String,
        value: Binding<Int?>,
        in range: ClosedRange<Int> = 1...5,
        variant: MonarchRatingPickerVariant = .scale,
        size: MonarchRatingPickerSize = .md,
        isClearable: Bool = false,
        isReadOnly: Bool = false,
        isRequired: Bool = false,
        error: String? = nil,
        helper: String? = nil,
        valueText: ((Int?) -> String)? = nil,
        clearLabel: String? = nil
    ) {
        self.init(
            label,
            value: value,
            options: MonarchRatingPickerTokens.options(in: range),
            variant: variant,
            size: size,
            isClearable: isClearable,
            isReadOnly: isReadOnly,
            isRequired: isRequired,
            error: error,
            helper: helper,
            valueText: valueText,
            clearLabel: clearLabel
        )
    }

    private var isAdjustable: Bool { isEnabled && !isReadOnly && !options.isEmpty }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchRatingPickerTokens.stackSpacing) {
            MonarchToggleLabel(label: MonarchFieldLabelRow(label: label, isRequired: isRequired))
            if let helper, !helper.isEmpty {
                Text(helper)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            switch variant {
            case .scale: scale
            case .stars: stars
            }
            if let activeError = MonarchFieldChrome.activeError(error) {
                MonarchFieldErrorRow(message: activeError)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityHint(
            MonarchFieldChrome.accessibilityHint(
                helper: helper,
                error: error,
                isRequired: isRequired,
                isReadOnly: isReadOnly
            )
        )
        .monarchAnnouncesError(error)
        .accessibilityValue(valueText?(value) ?? MonarchRatingPickerTokens.accessibilityValue(value, options: options))
        .modifier(
            MonarchRatingAdjustable(isActive: isAdjustable) { direction in
                switch direction {
                case .increment: value = MonarchRatingPickerTokens.incremented(value, options: options)
                case .decrement: value = MonarchRatingPickerTokens.decremented(value, options: options)
                @unknown default: break
                }
            }
        )
        .accessibilityActions {
            if isAdjustable, isClearable, value != nil {
                Button(clearLabel ?? MonarchRatingPickerStrings.clear) { value = nil }
            }
        }
    }

    private func activate(_ option: MonarchRatingOption) {
        value = MonarchRatingPickerTokens.activated(option.value, current: value, isClearable: isClearable)
    }

    private var scale: some View {
        let spacing = MonarchRatingPickerTokens.tileSpacing(size)
        return MonarchFlowLayout(spacing: spacing, rowSpacing: spacing, alignment: .top) {
            ForEach(options) { option in
                Button {
                    activate(option)
                } label: {
                    VStack(alignment: .leading, spacing: MonarchRatingPickerTokens.tileTextSpacing) {
                        Text(option.value, format: .number).monarchTextStyle(.label, weight: .semibold)
                        if let label = option.label, !label.isEmpty {
                            Text(label).monarchTextStyle(.caption).multilineTextAlignment(.leading)
                        }
                    }
                }
                .buttonStyle(MonarchRatingTileStyle(isSelected: option.value == value, size: size))
            }
        }
        .allowsHitTesting(!isReadOnly)
    }

    private var stars: some View {
        let caption = options.first { $0.value == value }?.label
        let cell = max(starSize, MonarchAccessibility.minimumHitTarget)
        let column = GridItem(.adaptive(minimum: cell, maximum: cell), spacing: 0)
        return ViewThatFits(in: .horizontal) {
            HStack(spacing: MonarchRatingPickerTokens.captionSpacing) {
                HStack(spacing: 0) { starButtons }
                starCaption(caption)
            }
            VStack(alignment: .leading, spacing: MonarchRatingPickerTokens.tileTextSpacing) {
                LazyVGrid(columns: [column], alignment: .leading, spacing: 0) { starButtons }
                starCaption(caption)
            }
        }
        .allowsHitTesting(!isReadOnly)
    }

    private var starButtons: some View {
        ForEach(options) { option in
            let isFilled = MonarchRatingPickerTokens.isFilled(option.value, current: value)
            Button {
                activate(option)
            } label: {
                Image(
                    systemName: isFilled
                        ? MonarchRatingPickerTokens.filledStarSymbol : MonarchRatingPickerTokens.emptyStarSymbol
                )
                .font(.system(size: starSize))
                .foregroundStyle(
                    theme.colors[
                        role: MonarchRatingPickerTokens.starForeground(isFilled: isFilled, isEnabled: isEnabled)
                    ]
                )
                .monarchMinimumHitTarget()
            }
            .buttonStyle(.plain)
        }
    }

    @ViewBuilder
    private func starCaption(_ caption: String?) -> some View {
        if let caption, !caption.isEmpty {
            Text(caption)
                .monarchTextStyle(.label)
                .foregroundStyle(isEnabled ? theme.colors.textMuted : theme.colors.textDisabled)
        }
    }
}

/// Adds the adjustable action only while the rating can change, so a
/// read-only or disabled picker is not announced as adjustable.
private struct MonarchRatingAdjustable: ViewModifier {
    let isActive: Bool
    let adjust: (AccessibilityAdjustmentDirection) -> Void

    func body(content: Content) -> some View {
        if isActive {
            content.accessibilityAdjustableAction(adjust)
        } else {
            content
        }
    }
}

/// Draws a tile of the scale for the pressed, chosen and disabled states.
private struct MonarchRatingTileStyle: ButtonStyle {
    let isSelected: Bool
    let size: MonarchRatingPickerSize

    func makeBody(configuration: Configuration) -> some View {
        MonarchRatingTile(configuration: configuration, isSelected: isSelected, size: size)
    }
}

/// Reads the environment, which a `ButtonStyle` value cannot do itself.
private struct MonarchRatingTile: View {
    let configuration: ButtonStyleConfiguration
    let isSelected: Bool
    let size: MonarchRatingPickerSize

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let appearance = MonarchRatingPickerTokens.tileAppearance(
            isSelected: isSelected,
            isPressed: configuration.isPressed,
            isEnabled: isEnabled
        )
        let shape = RoundedRectangle(cornerRadius: MonarchRatingPickerTokens.cornerRadius, style: .continuous)
        configuration.label
            .foregroundStyle(theme.colors[role: appearance.foreground])
            .padding(.horizontal, MonarchRatingPickerTokens.tileHorizontalPadding(size))
            .padding(.vertical, MonarchRatingPickerTokens.tileVerticalPadding(size))
            .frame(minHeight: MonarchAccessibility.minimumHitTarget)
            .background {
                if let background = appearance.background {
                    shape.fill(theme.colors[role: background])
                }
            }
            .overlay {
                if let border = appearance.border {
                    shape.strokeBorder(
                        theme.colors[role: border],
                        lineWidth: MonarchRatingPickerTokens.tileBorderWidth(isSelected: isSelected && isEnabled)
                    )
                }
            }
            .contentShape(shape)
            .monarchAnimation(.fast, value: configuration.isPressed)
    }
}

#if DEBUG
    #Preview("MonarchRatingPicker") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchRatingPicker("Scale", value: .constant(4))
                MonarchRatingPicker(
                    "Labelled",
                    value: .constant(2),
                    options: [
                        MonarchRatingOption(1, label: "Below"),
                        MonarchRatingOption(2, label: "Meets"),
                        MonarchRatingOption(3, label: "Exceeds"),
                    ],
                    size: .sm
                )
                MonarchRatingPicker("Stars", value: .constant(3), variant: .stars)
                MonarchRatingPicker(
                    "Overall",
                    value: .constant(nil),
                    variant: .stars,
                    isRequired: true,
                    error: "Choose a rating."
                )
                MonarchRatingPicker("Disabled", value: .constant(2), variant: .stars, size: .sm).disabled(true)
            }
        }
    }
#endif
