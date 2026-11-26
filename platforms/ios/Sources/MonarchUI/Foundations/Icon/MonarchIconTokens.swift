import CoreGraphics
import MonarchTokens

/// Component tokens for ``MonarchIcon``.
enum MonarchIconTokens {
    /// The point size of the symbol before Dynamic Type is applied.
    static func pointSize(_ size: MonarchIconSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space4
        case .md: MonarchTokens.Space.space5
        case .lg: MonarchTokens.Space.space6
        }
    }
}
