import MonarchTokens
import SwiftUI

/// A column of long-form blocks in a ``MonarchProseStyle``: the base type
/// and colour, the space between blocks and the reading measure.
///
/// ```swift
/// MonarchProse {
///     Text("Release notes").monarchProseBlock(.heading(1))
///     Text("Exports are faster.")
///     Text("Choose the columns to export.").monarchProseBlock(.listItem(marker: "•"))
/// }
/// ```
///
/// Give each child its kind with `monarchProseBlock(_:)`; a child without
/// one is a paragraph. The space between two blocks depends on both, as on
/// the web: less under a heading, more before one, little between list
/// items. With the reading measure the column is at most about 70
/// characters wide and sits at the leading edge.
///
/// Accessibility: the column adds no element. Blocks are read in order;
/// headings carry the header trait and their level.
public struct MonarchProse<Content: View>: View {
    private let style: MonarchProseStyle
    private let content: Content

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var scaledReference: CGFloat = MonarchProseTokens.referenceSize

    /// A column of prose.
    ///
    /// - Parameters:
    ///   - style: The base size and whether the line length is limited.
    ///   - content: The blocks, in reading order.
    public init(_ style: MonarchProseStyle = MonarchProseStyle(), @ViewBuilder content: () -> Content) {
        self.style = style
        self.content = content()
    }

    /// The content of the view.
    public var body: some View {
        let em = MonarchProseTokens.em(size: style.size, scaledReference: scaledReference)
        MonarchProseLayout(em: em) {
            content
        }
        .frame(
            maxWidth: style.limitsLineLength
                ? MonarchProseTokens.measureWidth(size: style.size, scaledReference: scaledReference) : .infinity,
            alignment: .leading
        )
        .foregroundStyle(theme.colors.textDefault)
        .tint(theme.colors[role: MonarchProseTokens.link])
        .environment(\.monarchProseStyle, style)
    }
}
