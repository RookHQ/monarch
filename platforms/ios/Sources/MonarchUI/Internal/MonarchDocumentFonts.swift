import SwiftUI

/// The fonts written into an editable document (the rich-text editor, and
/// any `AttributedString` text a component styles by role): body text and
/// headings, from the theme's typography. Formatted runs carry a font so
/// they can be bold or italic; plain body text carries none and takes the
/// editor's font.
struct MonarchDocumentFonts: Hashable {
    /// The body font.
    let body: Font
    /// The heading fonts by level, 1 to 3.
    let headings: [Int: Font]

    /// The heading levels the editor supports.
    static let levels = [1, 2, 3]

    /// The fonts for a theme's typography. Levels 1 to 3 use the title,
    /// heading and subheading roles. With the system font they follow the
    /// title, title 2 and title 3 text styles, so every level has its own
    /// size (the subheading role's headline style is the size of body text).
    init(typography: MonarchTypography) {
        body = Self.font(.body, systemStyle: .body, typography: typography)
        headings = [
            1: Self.font(.title, systemStyle: .title, typography: typography),
            2: Self.font(.heading, systemStyle: .title2, typography: typography),
            3: Self.font(.subheading, systemStyle: .title3, typography: typography),
        ]
    }

    /// The font of a block style: 0 for body text, 1 to 3 for headings.
    func base(_ level: Int) -> Font {
        headings[level] ?? body
    }

    /// A font for a type role that scales with Dynamic Type: the theme's
    /// family at the token size relative to the role's text style, or the
    /// system font of `systemStyle` with the role's weight. A document
    /// stores fonts, not sizes, so SwiftUI's text styles are what scale.
    static func font(
        _ style: MonarchTextStyle,
        systemStyle: Font.TextStyle,
        typography: MonarchTypography
    ) -> Font {
        let spec = style.spec
        let weight = MonarchTextStyleSpec.fontWeight(spec.weight)
        if let family = typography.fontFamily {
            return .custom(family, size: spec.size, relativeTo: spec.relativeTo).weight(weight)
        }
        return .system(systemStyle, weight: weight)
    }
}
