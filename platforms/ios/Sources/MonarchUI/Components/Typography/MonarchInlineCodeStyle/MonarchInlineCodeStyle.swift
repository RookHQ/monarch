import Foundation
import SwiftUI

/// The look of a code fragment inside running text: a function name, a
/// path, a command or a value to type.
///
/// SwiftUI draws running text with one `Text`, so inline code is a run of an
/// `AttributedString`, not a view. The style makes such runs:
///
/// ```swift
/// @Environment(\.monarchTheme) private var theme
///
/// Text(MonarchInlineCodeStyle.subtle.markdown("Run `swift build` first.", in: theme))
///     .monarchTextStyle(.body)
/// ```
///
/// The code takes the size of the text around it and its monospaced face,
/// so it fits every type role and scales with Dynamic Type.
///
/// Accessibility: the run is part of the text and is read with it. Code is
/// told apart by the monospaced face, not by the background alone.
public struct MonarchInlineCodeStyle: Sendable, Hashable {
    /// How the code is drawn.
    public let variant: MonarchInlineCodeVariant

    /// An inline code style.
    ///
    /// - Parameter variant: `subtle` for a muted background, `plain` for the
    ///   monospaced face only.
    public init(variant: MonarchInlineCodeVariant = .subtle) {
        self.variant = variant
    }

    /// Code on a muted background.
    public static let subtle = MonarchInlineCodeStyle(variant: .subtle)

    /// Code in the monospaced face only.
    public static let plain = MonarchInlineCodeStyle(variant: .plain)

    /// The attributes of a code run.
    ///
    /// - Parameters:
    ///   - theme: The theme, for the colours and the code font family.
    ///   - textStyle: The type role of the text around the code. It is used
    ///     only when the theme has its own code font family, to give that
    ///     font the size of the role.
    /// - Returns: The attributes: the code presentation intent, the text
    ///   colour, and for `subtle` the background colour.
    public func attributes(in theme: MonarchTheme, textStyle: MonarchTextStyle = .body) -> AttributeContainer {
        var container = AttributeContainer()
        // SwiftUI draws a run with this intent in the monospaced form of the
        // font around it, at the same size.
        container.inlinePresentationIntent = .code
        container.swiftUI.foregroundColor = theme.colors[role: MonarchInlineCodeTokens.foreground]
        if let background = MonarchInlineCodeTokens.background(variant: variant) {
            container.swiftUI.backgroundColor = theme.colors[role: background]
        }
        if let family = theme.typography.codeFontFamily {
            let spec = textStyle.spec
            container.swiftUI.font = .custom(family, size: spec.size, relativeTo: spec.relativeTo)
        }
        return container
    }

    /// A code fragment, to append to or insert in other attributed text.
    ///
    /// - Parameters:
    ///   - code: The code, shown as it is.
    ///   - theme: The theme.
    ///   - textStyle: The type role of the text around the code.
    /// - Returns: The code as one styled run.
    public func attributedString(
        _ code: String,
        in theme: MonarchTheme,
        textStyle: MonarchTextStyle = .body
    ) -> AttributedString {
        AttributedString(code, attributes: attributes(in: theme, textStyle: textStyle))
    }

    /// Text with the style on every code run it has.
    ///
    /// A code run is one whose inline presentation intent contains `code`,
    /// which is what a Markdown code span becomes. Other runs are unchanged.
    ///
    /// - Parameters:
    ///   - text: Attributed text, for example from `AttributedString(markdown:)`.
    ///   - theme: The theme.
    ///   - textStyle: The type role of the text.
    /// - Returns: The text, with its code runs styled.
    public func applied(
        to text: AttributedString,
        in theme: MonarchTheme,
        textStyle: MonarchTextStyle = .body
    ) -> AttributedString {
        let attributes = attributes(in: theme, textStyle: textStyle)
        var result = text
        for run in text.runs where run.inlinePresentationIntent?.contains(.code) == true {
            result[run.range].mergeAttributes(attributes)
        }
        return result
    }

    /// Text from inline Markdown, with the style on its code spans.
    ///
    /// Only inline Markdown is read (code spans, emphasis, links); white
    /// space and line breaks are kept. Text that is not valid Markdown is
    /// shown as it is.
    ///
    /// - Parameters:
    ///   - markdown: The text, with code between backticks.
    ///   - theme: The theme.
    ///   - textStyle: The type role of the text.
    /// - Returns: The text without the Markdown marks, with its code spans styled.
    public func markdown(
        _ markdown: String,
        in theme: MonarchTheme,
        textStyle: MonarchTextStyle = .body
    ) -> AttributedString {
        let options = AttributedString.MarkdownParsingOptions(interpretedSyntax: .inlineOnlyPreservingWhitespace)
        guard let parsed = try? AttributedString(markdown: markdown, options: options) else {
            return AttributedString(markdown)
        }
        return applied(to: parsed, in: theme, textStyle: textStyle)
    }
}

#if DEBUG
    private struct MonarchInlineCodePreview: View {
        @Environment(\.monarchTheme) private var theme

        var body: some View {
            VStack(alignment: .leading) {
                ForEach(MonarchInlineCodeVariant.allCases, id: \.self) { variant in
                    let style = MonarchInlineCodeStyle(variant: variant)
                    Text(style.markdown("Run `swift build` in `platforms/ios` first.", in: theme))
                        .monarchTextStyle(.body)
                    Text(style.markdown("Set `isLoading` to show progress.", in: theme, textStyle: .caption))
                        .monarchTextStyle(.caption)
                }
            }
            .foregroundStyle(theme.colors.textDefault)
        }
    }

    #Preview("MonarchInlineCodeStyle") {
        MonarchPreviewMatrix {
            MonarchInlineCodePreview()
        }
    }
#endif
