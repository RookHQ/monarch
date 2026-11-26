import MonarchTokens
import SwiftUI

/// Component tokens and colour logic for ``MonarchJsonViewer``.
enum MonarchJsonViewerTokens {
    /// The indentation of each level before Dynamic Type.
    static let indent: CGFloat = MonarchTokens.Space.space4
    /// The width of the chevron column before Dynamic Type.
    static let chevronColumn: CGFloat = MonarchTokens.Space.space4
    /// The padding at the start and end of a row.
    static let rowHorizontalPadding: CGFloat = MonarchTokens.Space.space2
    /// The padding above and below a row's text.
    static let rowVerticalPadding: CGFloat = MonarchTokens.Space.space1
    /// The gap between the chevron and the text of a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the toolbar, the tree and the footer.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The padding inside the toolbar and the footer.
    static let barPadding: CGFloat = MonarchTokens.Space.space2
    /// The gap between items in the toolbar and the footer.
    static let barSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the search icon and the search text.
    static let searchIconSpacing: CGFloat = MonarchTokens.Space.space2
    /// The corner radius of a row's selection.
    static let rowCornerRadius: CGFloat = MonarchTokens.Radius.sm
    /// The SF Symbol of a closed container.
    static let closedSymbol = "chevron.forward"
    /// The SF Symbol of an open container.
    static let openSymbol = "chevron.down"
    /// The SF Symbol in the search field.
    static let searchSymbol = "magnifyingglass"
    /// The SF Symbol of the clear button of the search field.
    static let clearSymbol = "xmark.circle.fill"
    /// The fill of the viewer.
    static var surfaceRole: MonarchColorRole { \.bgSurface }
    /// The border of the viewer and the line between its parts.
    static var borderRole: MonarchColorRole { \.borderSubtle }
    /// The fill of the selected row.
    static var selectedRole: MonarchColorRole { \.bgMuted }
    /// The fill behind matched text.
    static var matchRole: MonarchColorRole { \.warningSubtle }
    /// The colour of punctuation, the path and the match count.
    static var mutedRole: MonarchColorRole { \.textMuted }
    /// The colour of the "Show more" row.
    static var moreRole: MonarchColorRole { \.textLink }

    /// The chevron of a row, or `nil` when it does not open.
    static func chevron(isExpandable: Bool, isOpen: Bool) -> String? {
        guard isExpandable else { return nil }
        return isOpen ? openSymbol : closedSymbol
    }

    /// The colour of a key: array indexes are muted, object keys the info tone.
    static func keyRole(_ segment: MonarchJsonViewerSegment) -> MonarchColorRole {
        if case .index = segment { return \.textMuted }
        return \.infoStrong
    }

    /// The colour of a value by its kind. The kind is also shown by its
    /// punctuation (quotes, braces, brackets), not by colour alone.
    static func valueRole(_ type: MonarchJsonViewerType) -> MonarchColorRole {
        switch type {
        case .object, .array: \.textMuted
        case .string: \.successStrong
        case .number: \.accentStrong
        case .bool, .null: \.warningStrong
        }
    }
}
