import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchRadioToggleStyle``.
enum MonarchRadioTokens {
    /// The diameter of the ring before Dynamic Type is applied: 16 points.
    static let ringSize: CGFloat = MonarchControlMetrics.checkSize
    /// The diameter of the dot of the selected radio: 10 points.
    static let dotSize: CGFloat = MonarchControlMetrics.radioDotSize
    /// The gap between the ring and the label.
    static let labelSpacing: CGFloat = MonarchTokens.Space.space2_5
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchControlMetrics.borderWidth

    /// The colour roles of the ring in one state.
    struct Appearance: Equatable {
        let fill: MonarchColorRole
        let border: MonarchColorRole
        let dot: MonarchColorRole
    }

    /// The roles for a state. The selected ring has the strong accent border and a
    /// strong accent dot on the surface, as the web radio.
    static func appearance(isSelected: Bool, isEnabled: Bool) -> Appearance {
        // As on the web, a disabled radio keeps its enabled colours; the view draws it at half opacity.
        isSelected
            ? Appearance(fill: \.bgSurface, border: \.accentStrong, dot: \.accentStrong)
            : Appearance(fill: \.bgSurface, border: \.borderInput, dot: \.accentStrong)
    }

    /// The colour of the label.
    static func labelColor(isEnabled: Bool) -> MonarchColorRole {
        \.textDefault
    }
}
