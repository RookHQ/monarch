import MonarchTokens
import SwiftUI

/// A key path that names one semantic colour role, for example `\.bgCanvas`.
public typealias MonarchColorRole = KeyPath<MonarchColorTokens, MonarchRGBA>

/// The semantic colours of the current theme as SwiftUI colours.
///
/// Every role of the generated `MonarchColorTokens` is available by name, so a
/// role added to the tokens needs no change here:
///
/// ```swift
/// @Environment(\.monarchTheme) private var theme
/// Text("Hello").foregroundStyle(theme.colors.textDefault)
/// ```
@dynamicMemberLookup
public struct MonarchColors: Sendable, Hashable {
    /// The token colours this value reads from.
    public let tokens: MonarchColorTokens

    /// Wraps the colour roles of one theme.
    public init(_ tokens: MonarchColorTokens) {
        self.tokens = tokens
    }

    /// The colour for a role, by member name.
    public subscript(dynamicMember role: MonarchColorRole) -> Color {
        Color(monarch: tokens[keyPath: role])
    }

    /// The colour for a role held in a variable.
    public subscript(role role: MonarchColorRole) -> Color {
        Color(monarch: tokens[keyPath: role])
    }
}
