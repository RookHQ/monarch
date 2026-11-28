import SwiftUI

extension View {
    /// Marks this view as a block of long-form text inside ``MonarchProse``
    /// and gives it the type, colour and shape of its kind.
    ///
    /// ```swift
    /// Text("Options").monarchProseBlock(.heading(3))
    /// Text("Small steps every day add up.").monarchProseBlock(.quote)
    /// Divider().monarchProseBlock(.rule)
    /// ```
    ///
    /// A heading gets the header trait and its level. A code block reads
    /// left to right and scrolls sideways instead of wrapping.
    ///
    /// - Parameter block: The kind of block.
    /// - Returns: The view, styled as the block.
    public func monarchProseBlock(_ block: MonarchProseBlock) -> some View {
        modifier(MonarchProseBlockModifier(block: block))
    }
}

/// Applies the type, colour and shape of a prose block.
private struct MonarchProseBlockModifier: ViewModifier {
    let block: MonarchProseBlock

    @Environment(\.monarchProseStyle) private var style
    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var scaledReference: CGFloat = MonarchProseTokens.referenceSize

    func body(content: Content) -> some View {
        typeset(shaped(content))
            .layoutValue(key: MonarchProseBlockKey.self, value: block)
    }

    private var fontSize: CGFloat {
        MonarchProseTokens.fontSize(of: block, size: style.size, scaledReference: scaledReference)
    }

    private var font: Font {
        let weight = MonarchTextStyleSpec.fontWeight(MonarchProseTokens.weight(of: block))
        let isMonospaced = MonarchProseTokens.isMonospaced(block)
        let family = isMonospaced ? theme.typography.codeFontFamily : theme.typography.fontFamily
        if let family {
            if !isMonospaced, let face = theme.typography.face(for: weight) {
                return .custom(face, fixedSize: fontSize)
            }
            return .custom(family, fixedSize: fontSize).weight(weight)
        }
        return .system(size: fontSize, weight: weight, design: isMonospaced ? .monospaced : .default)
    }

    private func typeset(_ content: some View) -> some View {
        content
            .font(font)
            .tracking(MonarchProseTokens.tracking(of: block) * fontSize)
            .lineSpacing(MonarchProseTokens.lineSpacing(of: block, size: style.size, fontSize: fontSize))
            .foregroundStyle(theme.colors[role: MonarchProseTokens.foreground(of: block)])
    }

    @ViewBuilder private func shaped(_ content: some View) -> some View {
        let em = MonarchProseTokens.em(size: style.size, scaledReference: scaledReference)
        switch block {
        case .heading(let level):
            content
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityAddTraits(.isHeader)
                .accessibilityHeading(MonarchProseTokens.accessibilityLevel(level))
        case .listItem(let marker, let depth):
            HStack(alignment: .firstTextBaseline, spacing: 0) {
                Text(marker)
                    .foregroundStyle(theme.colors[role: MonarchProseTokens.markerForeground])
                    .frame(minWidth: MonarchProseTokens.listIndent * em, alignment: .leading)
                    .accessibilityHidden(!MonarchProseTokens.isMarkerSpoken(marker))
                content
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.leading, CGFloat(max(0, depth)) * MonarchProseTokens.listIndent * em)
        case .quote:
            content
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.leading, MonarchProseTokens.quoteIndent * em)
                .overlay(alignment: .leading) {
                    Rectangle()
                        .fill(theme.colors[role: MonarchProseTokens.quoteBar])
                        .frame(width: MonarchProseTokens.quoteBarWidth)
                        .accessibilityHidden(true)
                }
        case .code:
            ScrollView(.horizontal) {
                content
                    .fixedSize(horizontal: true, vertical: false)
                    .padding(.vertical, MonarchProseTokens.codeVerticalPadding)
                    .padding(.horizontal, MonarchProseTokens.codeHorizontalPadding)
            }
            .environment(\.layoutDirection, .leftToRight)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.colors[role: MonarchProseTokens.codeBackground], in: MonarchProseTokens.codeShape)
            .overlay {
                MonarchProseTokens.codeShape
                    .strokeBorder(
                        theme.colors[role: MonarchProseTokens.codeBorder],
                        lineWidth: MonarchShapes.borderWidth
                    )
            }
        case .rule:
            content
                .frame(maxWidth: .infinity)
                .overlay {
                    Rectangle()
                        .fill(theme.colors[role: MonarchProseTokens.foreground(of: .rule)])
                        .frame(height: MonarchShapes.borderWidth)
                }
                .accessibilityHidden(true)
        case .paragraph, .caption:
            content
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}
