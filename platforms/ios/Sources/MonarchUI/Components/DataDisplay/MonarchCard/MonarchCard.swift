import SwiftUI

/// A surface that groups related content.
///
/// The card draws a surface colour, a border, rounded corners and an optional
/// shadow, and pads its content. It adds no behaviour: to make a card
/// tappable, put it in a `Button` or a `NavigationLink`.
///
/// ```swift
/// MonarchCard {
///     Text("Invoice 1042").monarchTextStyle(.subheading)
///     Text("Due in 3 days").monarchTextStyle(.body)
/// }
/// ```
///
/// With `padding: .flush` the content runs to the card's edges, for a list
/// or an image; a `footer` sits under the content on its own padded row,
/// after a divider, for actions or a note:
///
/// ```swift
/// MonarchCard(padding: .flush) {
///     ForEach(invoices) { InvoiceRow($0) }
/// } footer: {
///     MonarchButton("View all", variant: .ghost, size: .sm) { showAll() }
/// }
/// ```
///
/// Accessibility: the card is a container. Its children stay separate
/// elements, in reading order.
public struct MonarchCard<Content: View>: View {
    private let elevation: MonarchElevationLevel
    private let padding: MonarchCardPadding
    private let content: Content
    private let footer: AnyView?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.monarchCardOverride) private var override

    /// A card.
    ///
    /// - Parameters:
    ///   - elevation: The shadow under the card. None by default: an inline card is a hairline border on the surface; give a shadow only to a card that floats above the page.
    ///   - padding: The room around the content: standard, or flush for content that runs to the edges.
    ///   - content: The content, laid out in a leading-aligned vertical stack.
    public init(
        elevation: MonarchElevationLevel = .none,
        padding: MonarchCardPadding = .standard,
        @ViewBuilder content: () -> Content
    ) {
        self.elevation = elevation
        self.padding = padding
        self.content = content()
        self.footer = nil
    }

    /// A card with a footer under the content, after a divider.
    ///
    /// - Parameters:
    ///   - elevation: The shadow under the card. None by default: an inline card is a hairline border on the surface; give a shadow only to a card that floats above the page.
    ///   - padding: The room around the content. The footer always has the standard padding.
    ///   - content: The content, laid out in a leading-aligned vertical stack.
    ///   - footer: Actions or a note under the content.
    public init<Footer: View>(
        elevation: MonarchElevationLevel = .none,
        padding: MonarchCardPadding = .standard,
        @ViewBuilder content: () -> Content,
        @ViewBuilder footer: () -> Footer
    ) {
        self.elevation = elevation
        self.padding = padding
        self.content = content()
        self.footer = AnyView(footer())
    }

    /// The content of the view.
    public var body: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchCardTokens.cornerRadius, style: .continuous)
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: theme.spacing.space2) {
                content
            }
            .padding(MonarchCardTokens.contentPadding(padding))
            .frame(maxWidth: .infinity, alignment: .leading)
            if let footer {
                MonarchDivider()
                HStack(spacing: theme.spacing.space2) { footer }
                    .padding(.horizontal, MonarchCardTokens.padding)
                    .padding(.vertical, MonarchCardTokens.footerVerticalPadding)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .monarchClipped(padding == .flush, to: shape)
        .monarchElevation(elevation, fill: override?.fill ?? MonarchCardTokens.background, in: shape)
        .overlay(
            shape.strokeBorder(
                theme.colors[role: override?.border ?? MonarchCardTokens.border],
                lineWidth: override?.borderWidth ?? MonarchCardTokens.borderWidth
            )
        )
        .accessibilityElement(children: .contain)
    }
}

extension View {
    /// Clips flush content to the card's corners; padded content is left
    /// unclipped so focus rings and shadows inside it are not cut.
    @ViewBuilder
    fileprivate func monarchClipped(_ isClipped: Bool, to shape: some Shape) -> some View {
        if isClipped {
            clipShape(shape)
        } else {
            self
        }
    }
}

#if DEBUG
    #Preview("MonarchCard") {
        MonarchPreviewMatrix {
            MonarchCard(elevation: .md) {
                Text("Invoice 1042").monarchTextStyle(.subheading)
                Text("Due in 3 days").monarchTextStyle(.body)
            }
            MonarchCard(padding: .flush) {
                Text("Edge to edge").padding()
            } footer: {
                MonarchButton("View all", variant: .ghost, size: .sm) {}
            }
        }
    }
#endif
