import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiTonePicker``.
enum MonarchAiTonePickerTokens {
    /// The colour roles of one row.
    struct Appearance: Equatable {
        /// The fill of the row.
        let background: MonarchColorRole
        /// The border of the row.
        let border: MonarchColorRole
        /// The radio mark.
        let mark: MonarchColorRole
        /// The tone name.
        let label: MonarchColorRole
        /// The description.
        let detail: MonarchColorRole
    }

    /// The gap between the legend, the rows and the custom field.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the label and the menu in the compact variant.
    static let menuSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the radio mark and the text.
    static let markSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the tone name and its description.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// Padding before and after the content of a row.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding above and below the content of a row.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space2

    /// The width of a row's border: a hairline at rest and selected. The
    /// selected row is told apart by its mark, fill and edge colour, not by a
    /// heavy border (PREMIUM rule 8).
    static func borderWidth(isSelected: Bool) -> CGFloat {
        MonarchTokens.Space.px
    }

    /// The SF Symbol of the radio mark.
    static func symbol(isSelected: Bool) -> String {
        isSelected ? "largecircle.fill.circle" : "circle"
    }

    /// The roles of a row in a state.
    static func appearance(isSelected: Bool, isEnabled: Bool) -> Appearance {
        guard isEnabled else {
            return Appearance(
                background: \.bgSurface,
                border: isSelected ? \.borderStrong : \.borderSubtle,
                mark: \.textDisabled,
                label: \.textDisabled,
                detail: \.textDisabled
            )
        }
        if isSelected {
            return Appearance(
                background: \.aiSubtle,
                border: \.aiBorder,
                mark: \.aiStrong,
                label: \.textDefault,
                detail: \.textMuted
            )
        }
        return Appearance(
            background: \.bgSurface,
            border: \.borderDefault,
            mark: \.borderInput,
            label: \.textDefault,
            detail: \.textMuted
        )
    }

    /// The options in order: the tones, then the Custom option if there is
    /// one. A tone that uses the Custom id is dropped when the Custom option
    /// is present, so ids stay unique.
    static func options(tones: [MonarchAiTone], custom: MonarchAiTone?) -> [MonarchAiTone] {
        guard let custom else { return tones }
        return tones.filter { $0.id != custom.id } + [custom]
    }

    /// Whether the custom text field is shown.
    static func showsCustomField(selection: String, allowsCustom: Bool) -> Bool {
        allowsCustom && selection == MonarchAiTone.customID
    }
}
