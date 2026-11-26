import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchRangeSlider``.
enum MonarchRangeSliderTokens {
    /// The height of the touch area of the track and each thumb.
    static let hitTarget: CGFloat = MonarchAccessibility.minimumHitTarget
    /// The gap between the shown range and the track.
    static let valueSpacing: CGFloat = MonarchTokens.Space.space1
    /// The width of the border of a thumb.
    static let thumbBorderWidth: CGFloat = MonarchTokens.Space.space0_5

    /// The height of the track.
    static func trackHeight(_ size: MonarchRangeSliderSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space1
        case .md: MonarchTokens.Space.space1_5
        }
    }

    /// The diameter of a thumb.
    static func thumbDiameter(_ size: MonarchRangeSliderSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space4
        case .md: MonarchTokens.Space.space5
        }
    }

    /// The colour roles of a range slider.
    struct Appearance: Equatable {
        let track: MonarchColorRole
        let range: MonarchColorRole
        let thumbFill: MonarchColorRole
        let thumbBorder: MonarchColorRole
        let value: MonarchColorRole
    }

    /// The roles for a state. The range uses the accent of the theme, the
    /// danger colour when invalid, and the disabled text colour when disabled.
    static func appearance(isInvalid: Bool, isEnabled: Bool) -> Appearance {
        if !isEnabled {
            return Appearance(
                track: \.bgMuted,
                range: \.textDisabled,
                thumbFill: \.bgSubtle,
                thumbBorder: \.textDisabled,
                value: \.textDisabled
            )
        }
        let range: MonarchColorRole = isInvalid ? \.dangerSolid : \.accentSolid
        return Appearance(
            track: \.bgEmphasis,
            range: range,
            thumbFill: \.bgSurface,
            thumbBorder: range,
            value: \.textMuted
        )
    }

    /// The spoken name of a thumb: the label and "Minimum" or "Maximum".
    static func thumbLabel(
        _ thumb: MonarchRangeSliderModel.Thumb,
        label: String,
        labels: MonarchRangeSliderLabels
    ) -> String {
        let name: String
        switch thumb {
        case .lower: name = labels.minimum ?? MonarchRangeSliderStrings.minimum
        case .upper: name = labels.maximum ?? MonarchRangeSliderStrings.maximum
        }
        return MonarchRangeSliderStrings.thumb(label: label, name: name)
    }

    /// The default text of a value: the number in the current locale.
    static func defaultValueText(_ value: Double) -> String {
        value.formatted(.number)
    }

    /// The shown range, "20 – 80".
    static func rangeText(lower: String, upper: String, labels: MonarchRangeSliderLabels) -> String {
        labels.range?(lower, upper) ?? MonarchRangeSliderStrings.range(lower, upper)
    }
}
