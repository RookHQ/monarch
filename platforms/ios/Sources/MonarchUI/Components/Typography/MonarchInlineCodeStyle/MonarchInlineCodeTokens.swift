import SwiftUI

/// Component tokens for ``MonarchInlineCodeStyle``.
enum MonarchInlineCodeTokens {
    /// The role of the code text.
    static var foreground: MonarchColorRole { \.textDefault }

    /// The role of the background behind the code, or `nil` for none.
    static func background(variant: MonarchInlineCodeVariant) -> MonarchColorRole? {
        switch variant {
        case .subtle: \.bgMuted
        case .plain: nil
        }
    }
}
