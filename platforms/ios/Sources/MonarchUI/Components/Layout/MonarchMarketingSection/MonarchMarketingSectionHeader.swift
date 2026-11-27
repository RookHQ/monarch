import SwiftUI

/// The header of a marketing section: a short eyebrow, a title and a description.
///
/// ```swift
/// MonarchMarketingSectionHeader(
///     "Every idea gets the right shape.",
///     eyebrow: "The editor",
///     description: "Type a slash and pick a block."
/// )
/// ```
///
/// Accessibility: the title has the header trait, so the Headings rotor stops on it. The eyebrow is read before
/// the title and the description after it.
public struct MonarchMarketingSectionHeader: View {
    private let title: String
    private let eyebrow: String?
    private let description: String?
    private let alignment: MonarchMarketingAlignment

    @Environment(\.monarchTheme) private var theme

    /// A section header.
    ///
    /// - Parameters:
    ///   - title: The section title, already localised.
    ///   - eyebrow: A short label above the title, such as the feature area.
    ///   - description: One or two sentences under the title.
    ///   - alignment: Leading or centred text.
    public init(
        _ title: String,
        eyebrow: String? = nil,
        description: String? = nil,
        alignment: MonarchMarketingAlignment = .leading
    ) {
        self.title = title
        self.eyebrow = eyebrow
        self.description = description
        self.alignment = alignment
    }

    /// The content of the view.
    public var body: some View {
        VStack(
            alignment: MonarchMarketingSectionTokens.horizontal(alignment),
            spacing: MonarchMarketingSectionTokens.headerSpacing
        ) {
            if let eyebrow {
                Text(eyebrow)
                    .monarchTextStyle(
                        MonarchMarketingSectionTokens.eyebrowStyle,
                        weight: MonarchMarketingSectionTokens.eyebrowWeight
                    )
                    .foregroundStyle(theme.colors[role: MonarchMarketingSectionTokens.eyebrowColor])
            }
            Text(title)
                .monarchTextStyle(MonarchMarketingSectionTokens.titleStyle)
                .foregroundStyle(theme.colors[role: MonarchMarketingSectionTokens.titleColor])
                .accessibilityAddTraits(.isHeader)
                .fixedSize(horizontal: false, vertical: true)
            if let description {
                Text(description)
                    .monarchTextStyle(MonarchMarketingSectionTokens.descriptionStyle)
                    .foregroundStyle(theme.colors[role: MonarchMarketingSectionTokens.supportingColor])
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .multilineTextAlignment(MonarchMarketingSectionTokens.text(alignment))
        .frame(maxWidth: .infinity, alignment: MonarchMarketingSectionTokens.frame(alignment))
    }
}
