import MonarchTokens
import SwiftUI

/// The shapes components share, so corners match across the library.
enum MonarchShapes {
    /// Buttons, fields and other controls: an 8-point continuous corner.
    static var control: RoundedRectangle {
        RoundedRectangle(cornerRadius: MonarchTokens.Radius.md, style: .continuous)
    }

    /// Cards, banners and other containers: a 12-point continuous corner.
    static var container: RoundedRectangle {
        RoundedRectangle(cornerRadius: MonarchTokens.Radius.lg, style: .continuous)
    }

    /// Checkboxes, tags and other small marks: a 4-point continuous corner.
    static var mark: RoundedRectangle {
        RoundedRectangle(cornerRadius: MonarchTokens.Radius.xs, style: .continuous)
    }

    /// The width of a border or hairline.
    static let borderWidth: CGFloat = MonarchTokens.Space.px
}
