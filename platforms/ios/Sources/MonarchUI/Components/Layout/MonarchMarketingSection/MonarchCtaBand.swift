import SwiftUI

/// A closing call to action: a title, an optional sentence and one or two buttons on a subtle band.
///
/// ```swift
/// MonarchCtaBand("Start with a blank page.") {
///     MonarchButton("Start writing", variant: .primary) { start() }
/// }
/// ```
///
/// Accessibility: the title has the header trait; each action stays its own element.
public struct MonarchCtaBand<Actions: View>: View {
    private let title: String
    private let description: String?
    private let alignment: MonarchMarketingAlignment
    private let actions: Actions

    @Environment(\.monarchTheme) private var theme

    /// A call-to-action band.
    ///
    /// - Parameters:
    ///   - title: The invitation, already localised.
    ///   - description: One sentence under the title.
    ///   - alignment: Leading or centred text.
    ///   - actions: One or two buttons, the first one primary.
    public init(
        _ title: String,
        description: String? = nil,
        alignment: MonarchMarketingAlignment = .leading,
        @ViewBuilder actions: () -> Actions
    ) {
        self.title = title
        self.description = description
        self.alignment = alignment
        self.actions = actions()
    }

    /// The content of the view.
    public var body: some View {
        VStack(
            alignment: MonarchMarketingSectionTokens.horizontal(alignment),
            spacing: MonarchMarketingSectionTokens.gridSpacing
        ) {
            Text(title)
                .monarchTextStyle(.heading)
                .foregroundStyle(theme.colors[role: MonarchMarketingSectionTokens.titleColor])
                .accessibilityAddTraits(.isHeader)
            if let description {
                Text(description)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: MonarchMarketingSectionTokens.supportingColor])
            }
            MonarchFlowLayout(
                spacing: MonarchMarketingSectionTokens.actionSpacing,
                justification: alignment == .center ? .center : .leading
            ) {
                actions
            }
        }
        .multilineTextAlignment(MonarchMarketingSectionTokens.text(alignment))
        .fixedSize(horizontal: false, vertical: true)
        .padding(MonarchMarketingSectionTokens.cardPadding)
        .frame(maxWidth: .infinity, alignment: MonarchMarketingSectionTokens.frame(alignment))
        .background(theme.colors[role: MonarchMarketingSectionTokens.bandFill], in: MonarchShapes.container)
        .accessibilityElement(children: .contain)
    }
}
