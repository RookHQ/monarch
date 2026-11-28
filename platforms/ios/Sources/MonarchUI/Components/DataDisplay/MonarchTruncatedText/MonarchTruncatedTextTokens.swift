import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchTruncatedText``.
enum MonarchTruncatedTextTokens {
    /// The type role of the text.
    static let textStyle: MonarchTextStyle = .body
    /// The type role of the button.
    static let buttonTextStyle: MonarchTextStyle = .label
    /// The colour role of the text.
    static var text: MonarchColorRole { \.textDefault }
    /// The colour role of the button.
    static var button: MonarchColorRole { \.textLink }
    /// The gap between the text and the button.
    static let spacing: CGFloat = MonarchTokens.Space.space0
    /// Heights closer than this count as equal, so rounding does not show a button.
    static let heightTolerance: CGFloat = MonarchTokens.Space.px

    /// The line limit in use: at least one line.
    static func lineLimit(_ requested: Int) -> Int {
        max(1, requested)
    }

    /// Whether the text is cut: its full height is more than the height of
    /// the limited lines.
    static func isTruncated(fullHeight: CGFloat, limitedHeight: CGFloat) -> Bool {
        fullHeight - limitedHeight > heightTolerance
    }

    /// Whether the button is shown. It stays while the text is expanded, so
    /// the text can be cut again.
    static func showsButton(isExpandable: Bool, isTruncated: Bool, isExpanded: Bool) -> Bool {
        isExpandable && (isTruncated || isExpanded)
    }

    /// The title of the button for a state.
    static func buttonTitle(isExpanded: Bool, showMoreLabel: String?, showLessLabel: String?) -> String {
        if isExpanded {
            return showLessLabel ?? MonarchTruncatedTextStrings.showLess
        }
        return showMoreLabel ?? MonarchTruncatedTextStrings.showMore
    }
}
