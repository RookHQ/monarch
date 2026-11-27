import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchCheckboxToggleStyle``.
enum MonarchCheckboxTokens {
    /// The side of the box before Dynamic Type is applied.
    static let boxSize: CGFloat = MonarchTokens.Space.space5
    /// The point size of the glyph inside the box, before Dynamic Type.
    static let glyphSize: CGFloat = MonarchTokens.FontSize.xs
    /// The gap between the box and the label.
    static let labelSpacing: CGFloat = MonarchTokens.Space.space2
    /// The corner radius of the box.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.xs
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchTokens.Space.px
    /// The SF Symbol for the checked state.
    static let checkedSymbol = "checkmark"
    /// The SF Symbol for the mixed state.
    static let mixedSymbol = "minus"

    /// The colour roles of the box in one state.
    struct Appearance: Equatable {
        let fill: MonarchColorRole
        let border: MonarchColorRole
        let glyph: MonarchColorRole
    }

    /// The roles for a state. Pure, so it is tested without a view.
    static func appearance(isMarked: Bool, isEnabled: Bool) -> Appearance {
        switch (isMarked, isEnabled) {
        case (true, true):
            Appearance(fill: \.accentStrong, border: \.accentStrong, glyph: \.accentOnStrong)
        case (false, true):
            Appearance(fill: \.bgSurface, border: \.borderInput, glyph: \.accentOnStrong)
        case (true, false):
            // As on the web, a disabled box keeps its enabled colours at half opacity.
            Appearance(fill: \.accentStrong, border: \.accentStrong, glyph: \.accentOnStrong)
        case (false, false):
            Appearance(fill: \.bgSurface, border: \.borderInput, glyph: \.accentOnStrong)
        }
    }
}
