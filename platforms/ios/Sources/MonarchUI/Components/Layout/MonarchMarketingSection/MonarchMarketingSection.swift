import SwiftUI

/// A band of a marketing or about screen: a header with an eyebrow, a title and a description, then the
/// section's content, such as a ``MonarchFeatureGrid``, a ``MonarchStatStrip`` or a ``MonarchFaq``.
///
/// ```swift
/// MonarchMarketingSection(
///     "Every idea gets the right shape.",
///     eyebrow: "The editor",
///     description: "Type a slash and pick a block."
/// ) {
///     MonarchFeatureGrid(items: features)
/// }
/// ```
///
/// Use it on a welcome, about or upgrade screen that explains a product in an app. It draws no background, so
/// the screen keeps its own.
///
/// Accessibility: the section is a container, the title a heading, and the content keeps its own elements.
public struct MonarchMarketingSection<Content: View>: View {
    private let title: String
    private let eyebrow: String?
    private let description: String?
    private let alignment: MonarchMarketingAlignment
    private let content: Content

    /// A section with a header and content.
    ///
    /// - Parameters:
    ///   - title: The section title, already localised.
    ///   - eyebrow: A short label above the title.
    ///   - description: One or two sentences under the title.
    ///   - alignment: Leading or centred header text.
    ///   - content: The blocks of the section.
    public init(
        _ title: String,
        eyebrow: String? = nil,
        description: String? = nil,
        alignment: MonarchMarketingAlignment = .leading,
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.eyebrow = eyebrow
        self.description = description
        self.alignment = alignment
        self.content = content()
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchMarketingSectionTokens.sectionSpacing) {
            MonarchMarketingSectionHeader(title, eyebrow: eyebrow, description: description, alignment: alignment)
            content
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
    }
}

#if DEBUG
    #Preview("MonarchMarketingSection") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchMarketingSection(
                    "Every idea gets the right shape.",
                    eyebrow: "The editor",
                    description: "Type a slash and pick a block."
                ) {
                    MonarchFeatureGrid(items: [
                        MonarchFeatureGridItem(
                            "Slash commands",
                            description: "Tables, lists and callouts, one keystroke away."
                        ),
                        MonarchFeatureGridItem(
                            "Version history",
                            description: "Named snapshots you can restore safely."
                        ),
                    ])
                }
                MonarchStatStrip(items: [
                    MonarchStatStripItem("3", label: "Export formats"),
                    MonarchStatStripItem("13", label: "Interface languages"),
                ])
                MonarchFaq(items: [
                    MonarchFaqItem("Which formats can I export?", answer: "HTML, Markdown and PDF.")
                ])
                MonarchCtaBand("Start with a blank page.") {
                    MonarchButton("Start writing", variant: .primary) {}
                }
            }
        }
    }
#endif
